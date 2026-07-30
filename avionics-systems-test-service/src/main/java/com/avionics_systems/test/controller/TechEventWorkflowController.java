package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.BenchDefectResponse;
import com.avionics_systems.test.dto.ProblemReportResponse;
import com.avionics_systems.test.dto.TechEventResponse;
import com.avionics_systems.test.entity.BenchDefect;
import com.avionics_systems.test.entity.ProblemReport;
import com.avionics_systems.test.entity.TechEvent;
import com.avionics_systems.test.service.TechEventWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tech-events")
@RequiredArgsConstructor
@Tag(name = "Tech Event Workflow", description = "TechEvent workflow actions — transitions, supplier sync, defect creation")
public class TechEventWorkflowController {

    private final TechEventWorkflowService workflowService;

    @PostMapping("/{id}/transition")
    @Operation(summary = "Transition TechEvent status")
    public ResponseEntity<TechEventResponse> transition(
            @PathVariable UUID id,
            @RequestParam String targetStatus,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String comment) {
        TechEvent te = workflowService.transitionStatus(id, targetStatus, userId, comment);
        return ResponseEntity.ok(mapToResponse(te));
    }

    @GetMapping("/{id}/available-transitions")
    @Operation(summary = "Get available transitions for current status")
    public ResponseEntity<List<String>> getAvailableTransitions(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getAvailableTransitions(id));
    }

    @PostMapping("/{id}/share-supplier")
    @Operation(summary = "Share TechEvent with supplier project")
    public ResponseEntity<TechEventResponse> shareWithSupplier(
            @PathVariable UUID id,
            @RequestParam UUID supplierProjectId) {
        TechEvent copy = workflowService.shareWithSupplier(id, supplierProjectId);
        return ResponseEntity.ok(mapToResponse(copy));
    }

    @PostMapping("/{id}/sync-from-supplier")
    @Operation(summary = "Sync supplier analysis back to original")
    public ResponseEntity<Void> syncFromSupplier(@PathVariable UUID id) {
        workflowService.syncFromSupplier(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/create-bench-defect")
    @Operation(summary = "Create Bench Defect from TechEvent")
    public ResponseEntity<BenchDefectResponse> createBenchDefect(@PathVariable UUID id) {
        BenchDefect bd = workflowService.createBenchDefectFromTechEvent(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToBdResponse(bd));
    }

    @PostMapping("/{id}/create-problem-report")
    @Operation(summary = "Create Problem Report from TechEvent")
    public ResponseEntity<ProblemReportResponse> createProblemReport(
            @PathVariable UUID id,
            @RequestParam(required = false) String prOrigin,
            @RequestParam(required = false) String prType) {
        ProblemReport pr = workflowService.createProblemReportFromTechEvent(id, prOrigin, prType);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToPrResponse(pr));
    }

    // ========== Private mapping methods ==========

    private TechEventResponse mapToResponse(TechEvent te) {
        return TechEventResponse.builder()
                .id(te.getId())
                .projectId(te.getProjectId())
                .issueKey(te.getIssueKey())
                .summary(te.getSummary())
                .description(te.getDescription())
                .status(te.getStatus())
                .reporterId(te.getReporterId())
                .reporterTeamId(te.getReporterTeamId())
                .teamForAnalysisId(te.getTeamForAnalysisId())
                .detectedOnProgramId(te.getDetectedOnProgramId())
                .detectedOnDate(te.getDetectedOnDate())
                .detectedOnTestMeanId(te.getDetectedOnTestMeanId())
                .impactedAcSystemId(te.getImpactedAcSystemId())
                .impactedAtaChapterId(te.getImpactedAtaChapterId())
                .impactedMsf(te.getImpactedMsf())
                .impactedFunctionId(te.getImpactedFunctionId())
                .impactedPartition(te.getImpactedPartition())
                .systemSupplierId(te.getSystemSupplierId())
                .defectType(te.getDefectType())
                .defectOrigin(te.getDefectOrigin())
                .defectImpact(te.getDefectImpact())
                .defectImpactRationale(te.getDefectImpactRationale())
                .affectsVersionId(te.getAffectsVersionId())
                .fixVersionId(te.getFixVersionId())
                .applicableToProgramIds(te.getApplicableToProgramIds())
                .publicAnalysis(te.getPublicAnalysis())
                .abstractText(te.getAbstractText())
                .testConfiguration(te.getTestConfiguration())
                .recordingReference(te.getRecordingReference())
                .operationalImpact(te.getOperationalImpact())
                .requirementImpact(te.getRequirementImpact())
                .workaround(te.getWorkaround())
                .rejectionRationale(te.getRejectionRationale())
                .rejectionType(te.getRejectionType())
                .supplierAnalysis(te.getSupplierAnalysis())
                .supplierResponse(te.getSupplierResponse())
                .supplierStatus(te.getSupplierStatus())
                .finalAirbusResponse(te.getFinalAirbusResponse())
                .supplierSyncProjectId(te.getSupplierSyncProjectId())
                .supplierSyncIssueId(te.getSupplierSyncIssueId())
                .linkedChangeCardId(te.getLinkedChangeCardId())
                .linkedProblemReportId(te.getLinkedProblemReportId())
                .assigneeId(te.getAssigneeId())
                .resolvedBy(te.getResolvedBy())
                .priority(te.getPriority())
                .labels(te.getLabels())
                .vvActivity(te.getVvActivity())
                .detectedBy(te.getDetectedBy())
                .createdAt(te.getCreatedAt())
                .updatedAt(te.getUpdatedAt())
                .build();
    }

    private BenchDefectResponse mapToBdResponse(BenchDefect bd) {
        return BenchDefectResponse.builder()
                .id(bd.getId())
                .projectId(bd.getProjectId())
                .issueKey(bd.getIssueKey())
                .summary(bd.getSummary())
                .description(bd.getDescription())
                .status(bd.getStatus())
                .severity(bd.getSeverity())
                .criticality(bd.getCriticality())
                .defectType(bd.getDefectType())
                .defectOrigin(bd.getDefectOrigin())
                .defectImpact(bd.getDefectImpact())
                .defectImpactRationale(bd.getDefectImpactRationale())
                .ltmDefectType(bd.getLtmDefectType())
                .defectOriginCategoryId(bd.getDefectOriginCategoryId())
                .defectOriginSubItemId(bd.getDefectOriginSubItemId())
                .detectedOnProgramId(bd.getDetectedOnProgramId())
                .detectedOnDate(bd.getDetectedOnDate())
                .detectedOnTestMeanId(bd.getDetectedOnTestMeanId())
                .applicableToProgramIds(bd.getApplicableToProgramIds())
                .applicableToTestMeans(bd.getApplicableToTestMeans())
                .affectedAta(bd.getAffectedAta())
                .affectsVersionId(bd.getAffectsVersionId())
                .fixVersionId(bd.getFixVersionId())
                .testConfiguration(bd.getTestConfiguration())
                .workaround(bd.getWorkaround())
                .changeReference(bd.getChangeReference())
                .objectiveDateAnalysis(bd.getObjectiveDateAnalysis())
                .objectiveDateClosure(bd.getObjectiveDateClosure())
                .sourceTechEventId(bd.getSourceTechEventId())
                .reporterId(bd.getReporterId())
                .assigneeId(bd.getAssigneeId())
                .priority(bd.getPriority())
                .labels(bd.getLabels())
                .createdAt(bd.getCreatedAt())
                .updatedAt(bd.getUpdatedAt())
                .build();
    }

    private ProblemReportResponse mapToPrResponse(ProblemReport pr) {
        return ProblemReportResponse.builder()
                .id(pr.getId())
                .projectId(pr.getProjectId())
                .issueKey(pr.getIssueKey())
                .summary(pr.getSummary())
                .description(pr.getDescription())
                .status(pr.getStatus())
                .prOrigin(pr.getPrOrigin())
                .prType(pr.getPrType())
                .prTypeRationale(pr.getPrTypeRationale())
                .potentialEffects(pr.getPotentialEffects())
                .justificationMitigation(pr.getJustificationMitigation())
                .detectedOnProgramId(pr.getDetectedOnProgramId())
                .detectedOnAcSystemId(pr.getDetectedOnAcSystemId())
                .applicableToProgramIds(pr.getApplicableToProgramIds())
                .rejectionType(pr.getRejectionType())
                .rejectionRationale(pr.getRejectionRationale())
                .linkedTechEventId(pr.getLinkedTechEventId())
                .affectsVersionId(pr.getAffectsVersionId())
                .fixVersionId(pr.getFixVersionId())
                .classification(pr.getClassification())
                .reporterId(pr.getReporterId())
                .assigneeId(pr.getAssigneeId())
                .systemSupplierId(pr.getSystemSupplierId())
                .priority(pr.getPriority())
                .labels(pr.getLabels())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .build();
    }
}
