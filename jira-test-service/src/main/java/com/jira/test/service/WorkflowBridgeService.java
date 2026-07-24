package com.jira.test.service;

import com.jira.test.dto.WorkflowTransitionResult;
import com.jira.test.entity.BenchDefect;
import com.jira.test.entity.HlvvoDefinition;
import com.jira.test.entity.ProblemReport;
import com.jira.test.entity.TechEvent;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.BenchDefectRepository;
import com.jira.test.repository.HlvvoDefinitionRepository;
import com.jira.test.repository.ProblemReportRepository;
import com.jira.test.repository.TechEventRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridge between test-service entities (VVO, HLVVO, TechEvent, BenchDefect, ProblemReport)
 * and the workflow-service engine.
 *
 * <p>When the workflow-service is reachable, transitions are executed through its engine
 * pipeline (idempotency, conditions, validators, post-functions, history). When it is
 * unreachable, the service falls back to local transition-map validation so the system
 * continues to function in degraded mode.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowBridgeService {

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;
    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;
    private final RestTemplate restTemplate;

    @Value("${workflow.service.url:http://localhost:8085}")
    private String workflowServiceUrl;

    // =====================================================================
    // Public API
    // =====================================================================

    /**
     * Execute a workflow transition through the workflow engine.
     * Falls back to local validation if the workflow-service is unavailable.
     *
     * @param entityType   one of VVO, HLVVO, TECH_EVENT, BENCH_DEFECT, PROBLEM_REPORT
     * @param entityId     the entity's UUID
     * @param targetStatus the desired target status string
     * @param userId       the acting user (nullable)
     * @param comment      optional transition comment
     * @param screenInput  optional extra fields submitted with the transition
     * @return result indicating success or failure
     */
    @Transactional
    public WorkflowTransitionResult executeTransition(
            String entityType, UUID entityId, String targetStatus,
            UUID userId, String comment, Map<String, Object> screenInput) {

        String currentStatus = resolveCurrentStatus(entityType, entityId);
        UUID projectId = resolveProjectId(entityType, entityId);

        // --- Attempt workflow-service execution ---
        try {
            Map<String, Object> request = buildTransitionRequest(
                    entityId, projectId, userId, comment, targetStatus, screenInput);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (userId != null) {
                headers.set("X-User-Id", userId.toString());
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    workflowServiceUrl + "/api/workflows/transitions/execute",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                boolean success = Boolean.TRUE.equals(body.get("success"));
                if (success) {
                    updateEntityStatus(entityType, entityId, targetStatus, userId);
                    log.info("Workflow transition via engine: {} {} -> {} (entity: {})",
                            entityType, currentStatus, targetStatus, entityId);
                    return WorkflowTransitionResult.success(entityId, currentStatus, targetStatus);
                } else {
                    String error = extractError(body);
                    log.warn("Workflow transition blocked: {} {} -> {}: {}",
                            entityType, currentStatus, targetStatus, error);
                    return WorkflowTransitionResult.failure(error);
                }
            }
        } catch (Exception e) {
            log.warn("Workflow-service unavailable, falling back to local validation: {}",
                    e.getMessage());
        }

        // --- Fallback: local transition validation ---
        if (!isTransitionAllowed(entityType, currentStatus, targetStatus)) {
            return WorkflowTransitionResult.failure(
                    "Invalid transition from " + currentStatus + " to " + targetStatus);
        }

        updateEntityStatus(entityType, entityId, targetStatus, userId);
        log.info("Workflow transition (local fallback): {} {} -> {} (entity: {})",
                entityType, currentStatus, targetStatus, entityId);
        return WorkflowTransitionResult.success(entityId, currentStatus, targetStatus);
    }

    /**
     * Convenience overload without screenInput.
     */
    @Transactional
    public WorkflowTransitionResult executeTransition(
            String entityType, UUID entityId, String targetStatus,
            UUID userId, String comment) {
        return executeTransition(entityType, entityId, targetStatus, userId, comment, null);
    }

    /**
     * Get available transitions from the workflow engine, falling back to local map.
     */
    public List<String> getAvailableTransitions(String entityType, UUID entityId) {
        String currentStatus = resolveCurrentStatus(entityType, entityId);
        UUID projectId = resolveProjectId(entityType, entityId);

        try {
            String url = workflowServiceUrl + "/api/workflows/issues/" + entityId
                    + "/available-transitions?projectId=" + projectId;
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object transitions = response.getBody().get("transitions");
                if (transitions instanceof List<?> list) {
                    List<String> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            Object toName = map.get("toStatusName");
                            if (toName != null && !toName.toString().isEmpty()) {
                                result.add(toName.toString());
                            }
                        }
                    }
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Workflow-service unavailable for available transitions, using local map");
        }

        return getLocalAllowedTransitions(entityType, currentStatus);
    }

    // =====================================================================
    // Entity resolution
    // =====================================================================

    public String resolveCurrentStatus(String entityType, UUID entityId) {
        return switch (entityType) {
            case "VVO" -> vvoRepo.findById(entityId)
                    .map(VvoDefinition::getStatus)
                    .orElseThrow(() -> new RuntimeException("VVO not found: " + entityId));
            case "HLVVO" -> hlvvoRepo.findById(entityId)
                    .map(HlvvoDefinition::getStatus)
                    .orElseThrow(() -> new RuntimeException("HLVVO not found: " + entityId));
            case "TECH_EVENT" -> techEventRepo.findById(entityId)
                    .map(TechEvent::getStatus)
                    .orElseThrow(() -> new RuntimeException("TechEvent not found: " + entityId));
            case "BENCH_DEFECT" -> benchDefectRepo.findById(entityId)
                    .map(BenchDefect::getStatus)
                    .orElseThrow(() -> new RuntimeException("BenchDefect not found: " + entityId));
            case "PROBLEM_REPORT" -> problemReportRepo.findById(entityId)
                    .map(ProblemReport::getStatus)
                    .orElseThrow(() -> new RuntimeException("ProblemReport not found: " + entityId));
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
    }

    private UUID resolveProjectId(String entityType, UUID entityId) {
        return switch (entityType) {
            case "VVO" -> vvoRepo.findById(entityId)
                    .map(VvoDefinition::getProjectId).orElseThrow();
            case "HLVVO" -> hlvvoRepo.findById(entityId)
                    .map(HlvvoDefinition::getProjectId).orElseThrow();
            case "TECH_EVENT" -> techEventRepo.findById(entityId)
                    .map(TechEvent::getProjectId).orElseThrow();
            case "BENCH_DEFECT" -> benchDefectRepo.findById(entityId)
                    .map(BenchDefect::getProjectId).orElseThrow();
            case "PROBLEM_REPORT" -> problemReportRepo.findById(entityId)
                    .map(ProblemReport::getProjectId).orElseThrow();
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
    }

    // =====================================================================
    // Entity status updates
    // =====================================================================

    private void updateEntityStatus(String entityType, UUID entityId, String newStatus, UUID userId) {
        switch (entityType) {
            case "VVO" -> {
                VvoDefinition vvo = vvoRepo.findById(entityId).orElseThrow();
                vvo.setStatus(newStatus);
                vvoRepo.save(vvo);
            }
            case "HLVVO" -> {
                HlvvoDefinition hlvvo = hlvvoRepo.findById(entityId).orElseThrow();
                hlvvo.setStatus(newStatus);
                hlvvoRepo.save(hlvvo);
            }
            case "TECH_EVENT" -> {
                TechEvent te = techEventRepo.findById(entityId).orElseThrow();
                te.setStatus(newStatus);
                if (List.of("CLOSED", "CANCELLED").contains(newStatus) && userId != null) {
                    te.setResolvedBy(userId);
                }
                techEventRepo.save(te);
            }
            case "BENCH_DEFECT" -> {
                BenchDefect bd = benchDefectRepo.findById(entityId).orElseThrow();
                bd.setStatus(newStatus);
                benchDefectRepo.save(bd);
            }
            case "PROBLEM_REPORT" -> {
                ProblemReport pr = problemReportRepo.findById(entityId).orElseThrow();
                pr.setStatus(newStatus);
                problemReportRepo.save(pr);
            }
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
    }

    // =====================================================================
    // Local transition maps (fallback when workflow-service is unavailable)
    // =====================================================================

    private boolean isTransitionAllowed(String entityType, String from, String to) {
        return getLocalAllowedTransitions(entityType, from).contains(to);
    }

    private List<String> getLocalAllowedTransitions(String entityType, String currentStatus) {
        Map<String, List<String>> map = getTransitionMap(entityType);
        return map.getOrDefault(currentStatus, List.of());
    }

    @SuppressWarnings("java:S1452")
    private Map<String, List<String>> getTransitionMap(String entityType) {
        return switch (entityType) {
            case "VVO" -> VVO_TRANSITIONS;
            case "HLVVO" -> HLVVO_TRANSITIONS;
            case "TECH_EVENT" -> TECH_EVENT_TRANSITIONS;
            case "BENCH_DEFECT" -> BENCH_DEFECT_TRANSITIONS;
            case "PROBLEM_REPORT" -> PROBLEM_REPORT_TRANSITIONS;
            default -> Map.of();
        };
    }

    // --- VVO ---
    static final Map<String, List<String>> VVO_TRANSITIONS = Map.of(
            "NEW", List.of("TO_BE_VERIFIED"),
            "TO_BE_VERIFIED", List.of("VERIFIED", "NEW"),
            "VERIFIED", List.of("RELEASED", "CANCELLED", "SUPERSEDED"),
            "RELEASED", List.of("CANCELLED", "SUPERSEDED"),
            "CANCELLED", List.of("SUPERSEDED"),
            "SUPERSEDED", List.of()
    );

    // --- HLVVO ---
    static final Map<String, List<String>> HLVVO_TRANSITIONS = Map.of(
            "NEW", List.of("PLAN"),
            "PLAN", List.of("VVO_WRITING_IN_PROGRESS", "NEW"),
            "VVO_WRITING_IN_PROGRESS", List.of("SUPPLIER_IN_REVIEW", "NEW"),
            "SUPPLIER_IN_REVIEW", List.of("AUTHORIZE", "NEW"),
            "AUTHORIZE", List.of("NEW")
    );

    // --- TechEvent (M1668 14-state machine) ---
    // Made package-visible so TechEventWorkflowService can reference it.
    static final Map<String, List<String>> TECH_EVENT_TRANSITIONS = Map.ofEntries(
            Map.entry("OPEN", List.of("UNDER_ORIGINATOR_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_ORIGINATOR_ANALYSIS", List.of("UNDER_RESOLVER_ANALYSIS", "UNDER_TEST_MEAN_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_RESOLVER_ANALYSIS", List.of("READY_FOR_REVIEW", "CLASSIFIED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_TEST_MEAN_ANALYSIS", List.of("UNDER_ORIGINATOR_ANALYSIS", "CLOSED", "CANCELLED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("READY_FOR_REVIEW", List.of("CLASSIFIED", "UNDER_RESOLVER_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("CLASSIFIED", List.of("TO_BE_ASSESSED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("TO_BE_ASSESSED", List.of("RESOLVED_CORRECTED", "RESOLVED_CONTAINED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("RESOLVED_CORRECTED", List.of("CLOSED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("RESOLVED_CONTAINED", List.of("UNDER_RESOLVER_ANALYSIS", "UNRESOLVED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("PROPOSED_FOR_CANCELLATION", List.of("CANCELLED", "UNDER_ORIGINATOR_ANALYSIS")),
            Map.entry("CANCELLED", List.of("OPEN")),
            Map.entry("CLOSED", List.of("OPEN")),
            Map.entry("TO_BE_REFINED", List.of("UNDER_ORIGINATOR_ANALYSIS")),
            Map.entry("UNRESOLVED", List.of("UNDER_RESOLVER_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED"))
    );

    // --- BenchDefect ---
    static final Map<String, List<String>> BENCH_DEFECT_TRANSITIONS = Map.of(
            "OPEN", List.of("UNDER_ANALYSIS"),
            "UNDER_ANALYSIS", List.of("TO_BE_CORRECTED", "CANCELLED", "OPEN"),
            "TO_BE_CORRECTED", List.of("CORRECTED", "OPEN"),
            "CORRECTED", List.of("CLOSED", "OPEN"),
            "CLOSED", List.of("OPEN"),
            "CANCELLED", List.of("OPEN")
    );

    // --- ProblemReport ---
    static final Map<String, List<String>> PROBLEM_REPORT_TRANSITIONS = Map.of(
            "OPEN", List.of("UNDER_ANALYSIS"),
            "UNDER_ANALYSIS", List.of("CLOSED", "REJECTED"),
            "CLOSED", List.of("OPEN"),
            "REJECTED", List.of("OPEN")
    );

    // =====================================================================
    // Helpers
    // =====================================================================

    private Map<String, Object> buildTransitionRequest(
            UUID entityId, UUID projectId, UUID userId,
            String comment, String targetStatus, Map<String, Object> screenInput) {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("issueId", entityId.toString());
        request.put("projectId", projectId.toString());
        if (userId != null) {
            request.put("userId", userId.toString());
        }
        request.put("comment", comment);

        // Include target status in screenInput so the workflow engine
        // can resolve the correct transition when transitionId is not provided.
        Map<String, Object> input = screenInput != null
                ? new HashMap<>(screenInput)
                : new HashMap<>();
        input.put("_targetStatus", targetStatus);
        request.put("screenInput", input);

        return request;
    }

    private String extractError(Map<String, Object> body) {
        if (body.get("error") != null) {
            return body.get("error").toString();
        }
        Object errors = body.get("errors");
        if (errors instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        Object validationErrors = body.get("validationErrors");
        if (validationErrors instanceof Map<?, ?> map && !map.isEmpty()) {
            return map.values().iterator().next().toString();
        }
        return "Transition blocked by workflow engine";
    }
}
