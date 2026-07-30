package com.avionics_systems.workflow.client;

import com.avionics_systems.workflow.config.PatchCapableRestTemplate;
import com.avionics_systems.workflow.dto.EvaluateConditionsRequest;
import com.avionics_systems.workflow.dto.EvaluateConditionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for calling workflow condition evaluation from external services.
 * Can be used by avionics-systems-issue-service or other services that need to evaluate
 * workflow conditions without going through the full transition execution flow.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowConditionEvaluationClient {

    private final PatchCapableRestTemplate patchCapableRestTemplate;

    private RestTemplate restTemplate() {
        return patchCapableRestTemplate.get();
    }

    @Value("${avionics-systems.services.workflow-url:http://localhost:8085}")
    private String workflowServiceUrl;

    /**
     * Evaluate conditions for a transition.
     * Called by external services (like avionics-systems-issue-service).
     *
     * @param request The evaluation request
     * @return Evaluation response with results
     */
    public EvaluateConditionsResponse evaluateConditions(EvaluateConditionsRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (request.getUserId() != null) {
                headers.set("X-User-Id", request.getUserId().toString());
            }

            String url = workflowServiceUrl + "/api/workflows/conditions/evaluate";
            return restTemplate().postForObject(
                    url,
                    new HttpEntity<>(request, headers),
                    EvaluateConditionsResponse.class);
        } catch (Exception e) {
            log.error("Failed to evaluate conditions for transition {}: {}",
                    request.getTransitionId(), e.getMessage());
            return EvaluateConditionsResponse.failure(
                    request.getTransitionId(),
                    List.of("Failed to evaluate conditions: " + e.getMessage()),
                    List.of(),
                    0);
        }
    }

    /**
     * Convenience method to evaluate conditions using a context map.
     * This is a simplified interface for callers that have a map-based context.
     *
     * @param contextMap Context map containing:
     *                   - userId: UUID
     *                   - userGroups: Set<String>
     *                   - issueId: UUID
     *                   - projectId: UUID
     *                   - fields: Map<String, Object>
     *                   - previousStatusId: UUID
     *                   - currentStatusId: UUID
     *                   - transitionId: UUID
     * @param transitionId The transition to evaluate
     * @return Evaluation response with results
     */
    @SuppressWarnings("unchecked")
    public EvaluateConditionsResponse evaluateFromContext(Map<String, Object> contextMap, UUID transitionId) {
        EvaluateConditionsRequest.EvaluateConditionsRequestBuilder builder = EvaluateConditionsRequest.builder()
                .transitionId(transitionId);

        // Extract from context map
        if (contextMap.containsKey("userId")) {
            builder.userId(parseUuid(contextMap.get("userId")));
        }
        if (contextMap.containsKey("userGroups")) {
            builder.userGroups((java.util.Set<String>) contextMap.get("userGroups"));
        }
        if (contextMap.containsKey("issueId")) {
            builder.issueId(parseUuid(contextMap.get("issueId")));
        }
        if (contextMap.containsKey("projectId")) {
            builder.projectId(parseUuid(contextMap.get("projectId")));
        }
        if (contextMap.containsKey("fields")) {
            builder.fields((Map<String, Object>) contextMap.get("fields"));
        }
        if (contextMap.containsKey("previousStatusId")) {
            builder.previousStatusId(parseUuid(contextMap.get("previousStatusId")));
        }
        if (contextMap.containsKey("currentStatusId")) {
            builder.currentStatusId(parseUuid(contextMap.get("currentStatusId")));
        }
        if (contextMap.containsKey("screenInput")) {
            builder.screenInput((Map<String, Object>) contextMap.get("screenInput"));
        }
        if (contextMap.containsKey("reporterId")) {
            builder.reporterId(parseUuid(contextMap.get("reporterId")));
        }
        if (contextMap.containsKey("assigneeId")) {
            builder.assigneeId(parseUuid(contextMap.get("assigneeId")));
        }

        return evaluateConditions(builder.build());
    }

    /**
     * Check if a user can perform a specific transition.
     * Convenience method that just returns a boolean.
     *
     * @param userId       The user ID
     * @param transitionId The transition ID
     * @param contextMap   Context map with issue and user info
     * @return true if user can perform the transition (all conditions pass)
     */
    public boolean canPerformTransition(UUID userId, UUID transitionId, Map<String, Object> contextMap) {
        if (contextMap != null) {
            contextMap.put("userId", userId);
        }
        EvaluateConditionsResponse response = evaluateFromContext(contextMap, transitionId);
        return response.isConditionsMet();
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}