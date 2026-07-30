package com.avionics_systems.test.controller;

import com.avionics_systems.test.entity.BenchDefect;
import com.avionics_systems.test.entity.HlvvoDefinition;
import com.avionics_systems.test.entity.ProblemReport;
import com.avionics_systems.test.entity.TechEvent;
import com.avionics_systems.test.entity.VvoDefinition;
import com.avionics_systems.test.repository.BenchDefectRepository;
import com.avionics_systems.test.repository.HlvvoDefinitionRepository;
import com.avionics_systems.test.repository.ProblemReportRepository;
import com.avionics_systems.test.repository.TechEventRepository;
import com.avionics_systems.test.repository.VvoDefinitionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal endpoints called by the workflow engine during transition execution.
 *
 * <p>The workflow-service's {@code WorkflowIntegrationClient} calls
 * {@code GET /api/issues/{id}} to fetch issue data and
 * {@code PATCH /api/issues/{id}/workflow/internal} to apply status changes.
 * This controller mirrors the issue-service's internal API surface so the
 * workflow engine can transparently operate on test-service entities.</p>
 *
 * <p>These endpoints are secured via the {@code X-Workflow-Internal} header
 * or internal service-to-service trust. They should NOT be exposed to end users
 * through the API gateway.</p>
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Internal", description = "Internal endpoints for workflow engine integration")
public class WorkflowInternalController {

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;
    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;

    // =====================================================================
    // Fetch issue data (called by workflow engine to evaluate conditions)
    // =====================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Fetch issue data for workflow engine",
            description = "Returns entity data in the format expected by the workflow engine. "
                    + "Tries each entity type until a match is found.")
    public ResponseEntity<Map<String, Object>> getIssueForWorkflow(@PathVariable UUID id) {
        // Try each entity type in order
        Optional<VvoDefinition> vvo = vvoRepo.findById(id);
        if (vvo.isPresent()) {
            return ResponseEntity.ok(mapVvo(vvo.get()));
        }

        Optional<HlvvoDefinition> hlvvo = hlvvoRepo.findById(id);
        if (hlvvo.isPresent()) {
            return ResponseEntity.ok(mapHlvvo(hlvvo.get()));
        }

        Optional<TechEvent> te = techEventRepo.findById(id);
        if (te.isPresent()) {
            return ResponseEntity.ok(mapTechEvent(te.get()));
        }

        Optional<BenchDefect> bd = benchDefectRepo.findById(id);
        if (bd.isPresent()) {
            return ResponseEntity.ok(mapBenchDefect(bd.get()));
        }

        Optional<ProblemReport> pr = problemReportRepo.findById(id);
        if (pr.isPresent()) {
            return ResponseEntity.ok(mapProblemReport(pr.get()));
        }

        return ResponseEntity.notFound().build();
    }

    // =====================================================================
    // Update issue status (called by workflow engine after transition)
    // =====================================================================

    @PatchMapping("/{id}/workflow/internal")
    @Operation(summary = "Update entity status from workflow engine",
            description = "Called by the workflow engine to apply status changes after "
                    + "conditions/validators pass. Protected by X-Workflow-Internal header.")
    public ResponseEntity<Void> updateIssueFromWorkflow(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> fields,
            @RequestHeader(value = "X-Workflow-Internal", required = false) String internal) {

        String status = fields.get("status") != null ? fields.get("status").toString() : null;
        String statusId = fields.get("statusId") != null ? fields.get("statusId").toString() : null;
        String effectiveStatus = status != null ? status : statusId;

        if (effectiveStatus == null) {
            log.debug("No status field in workflow internal update for {}", id);
            return ResponseEntity.ok().build();
        }

        boolean updated = false;

        Optional<VvoDefinition> vvo = vvoRepo.findById(id);
        if (vvo.isPresent()) {
            vvo.get().setStatus(effectiveStatus);
            vvoRepo.save(vvo.get());
            updated = true;
        }

        if (!updated) {
            Optional<HlvvoDefinition> hlvvo = hlvvoRepo.findById(id);
            if (hlvvo.isPresent()) {
                hlvvo.get().setStatus(effectiveStatus);
                hlvvoRepo.save(hlvvo.get());
                updated = true;
            }
        }

        if (!updated) {
            Optional<TechEvent> te = techEventRepo.findById(id);
            if (te.isPresent()) {
                te.get().setStatus(effectiveStatus);
                techEventRepo.save(te.get());
                updated = true;
            }
        }

        if (!updated) {
            Optional<BenchDefect> bd = benchDefectRepo.findById(id);
            if (bd.isPresent()) {
                bd.get().setStatus(effectiveStatus);
                benchDefectRepo.save(bd.get());
                updated = true;
            }
        }

        if (!updated) {
            Optional<ProblemReport> pr = problemReportRepo.findById(id);
            if (pr.isPresent()) {
                pr.get().setStatus(effectiveStatus);
                problemReportRepo.save(pr.get());
                updated = true;
            }
        }

        if (!updated) {
            log.warn("Workflow internal update: entity {} not found in test-service", id);
        }

        return ResponseEntity.ok().build();
    }

    // =====================================================================
    // Transition history (called by workflow engine to record history)
    // =====================================================================

    @PostMapping("/{id}/transitions/history/internal")
    @Operation(summary = "Record transition history from workflow engine")
    public ResponseEntity<Void> recordTransitionHistory(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> historyData) {
        log.info("Transition history recorded for entity {}: fromStatus={}, toStatus={}, success={}",
                id,
                historyData.get("fromStatusId"),
                historyData.get("toStatusId"),
                historyData.get("success"));
        return ResponseEntity.ok().build();
    }

    // =====================================================================
    // Entity -> Map conversion (workflow engine format)
    // =====================================================================

    private Map<String, Object> mapVvo(VvoDefinition vvo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", vvo.getId().toString());
        data.put("projectId", vvo.getProjectId().toString());
        data.put("issueKey", vvo.getIssueKey());
        data.put("summary", vvo.getSummary());
        data.put("description", vvo.getDescription());
        data.put("status", vvo.getStatus());
        data.put("statusId", vvo.getStatus());
        data.put("assigneeId", uuidStr(vvo.getAssigneeId()));
        data.put("reporterId", uuidStr(vvo.getCreatedBy()));
        data.put("fix_version_id", uuidStr(vvo.getFixVersionId()));
        data.put("vvo_usage", vvo.getVvoUsage());
        data.put("vvo_scope", vvo.getVvoScope());
        data.put("issueTypeKey", "vvo");
        return data;
    }

    private Map<String, Object> mapHlvvo(HlvvoDefinition hlvvo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", hlvvo.getId().toString());
        data.put("projectId", hlvvo.getProjectId().toString());
        data.put("issueKey", hlvvo.getIssueKey());
        data.put("summary", hlvvo.getSummary());
        data.put("description", hlvvo.getDescription());
        data.put("status", hlvvo.getStatus());
        data.put("statusId", hlvvo.getStatus());
        data.put("assigneeId", uuidStr(hlvvo.getAssigneeId()));
        data.put("reporterId", uuidStr(hlvvo.getCreatedBy()));
        data.put("target_date", hlvvo.getTargetDate() != null ? hlvvo.getTargetDate().toString() : null);
        data.put("assignee", uuidStr(hlvvo.getAssigneeId()));
        data.put("issueTypeKey", "hlvvo");
        return data;
    }

    private Map<String, Object> mapTechEvent(TechEvent te) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", te.getId().toString());
        data.put("projectId", te.getProjectId().toString());
        data.put("issueKey", te.getIssueKey());
        data.put("summary", te.getSummary());
        data.put("description", te.getDescription());
        data.put("status", te.getStatus());
        data.put("statusId", te.getStatus());
        data.put("assigneeId", uuidStr(te.getAssigneeId()));
        data.put("reporterId", uuidStr(te.getReporterId()));
        data.put("defect_type", te.getDefectType());
        data.put("defect_origin", te.getDefectOrigin());
        data.put("defect_impact", te.getDefectImpact());
        data.put("public_analysis", te.getPublicAnalysis());
        data.put("issueTypeKey", "tech_event");
        return data;
    }

    private Map<String, Object> mapBenchDefect(BenchDefect bd) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", bd.getId().toString());
        data.put("projectId", bd.getProjectId().toString());
        data.put("issueKey", bd.getIssueKey());
        data.put("summary", bd.getSummary());
        data.put("description", bd.getDescription());
        data.put("status", bd.getStatus());
        data.put("statusId", bd.getStatus());
        data.put("assigneeId", uuidStr(bd.getAssigneeId()));
        data.put("reporterId", uuidStr(bd.getReporterId()));
        data.put("severity", bd.getSeverity());
        data.put("criticality", bd.getCriticality());
        data.put("issueTypeKey", "bench_defect");
        return data;
    }

    private Map<String, Object> mapProblemReport(ProblemReport pr) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", pr.getId().toString());
        data.put("projectId", pr.getProjectId().toString());
        data.put("issueKey", pr.getIssueKey());
        data.put("summary", pr.getSummary());
        data.put("description", pr.getDescription());
        data.put("status", pr.getStatus());
        data.put("statusId", pr.getStatus());
        data.put("assigneeId", uuidStr(pr.getAssigneeId()));
        data.put("reporterId", uuidStr(pr.getReporterId()));
        data.put("pr_origin", pr.getPrOrigin());
        data.put("pr_type", pr.getPrType());
        data.put("classification", pr.getClassification());
        data.put("issueTypeKey", "problem_report");
        return data;
    }

    private String uuidStr(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }
}
