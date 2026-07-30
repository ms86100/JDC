package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.WorkflowTransitionResult;
import com.avionics_systems.test.entity.BenchDefect;
import com.avionics_systems.test.entity.ProblemReport;
import com.avionics_systems.test.entity.TechEvent;
import com.avionics_systems.test.repository.BenchDefectRepository;
import com.avionics_systems.test.repository.ProblemReportRepository;
import com.avionics_systems.test.repository.TechEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechEventWorkflowService {

    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;
    private final WorkflowNotificationService notificationService;
    private final WorkflowBridgeService workflowBridge;

    // ========== Status Transitions ==========

    /**
     * Transition a TechEvent status through the workflow engine.
     * Delegates to WorkflowBridgeService which calls the workflow-service
     * for condition/validator/post-function evaluation, falling back to
     * local transition-map validation when the workflow-service is unavailable.
     */
    @Transactional
    public TechEvent transitionStatus(UUID techEventId, String targetStatus, UUID userId, String comment) {
        // Capture previous status before the bridge updates it
        String previousStatus = workflowBridge.resolveCurrentStatus("TECH_EVENT", techEventId);

        WorkflowTransitionResult result = workflowBridge.executeTransition(
                "TECH_EVENT", techEventId, targetStatus, userId, comment);

        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getErrorMessage());
        }

        // Re-fetch entity after bridge updated it
        TechEvent te = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        // Send notifications after successful transition
        notificationService.notifyAfterTransition(
                "TechEvent", te.getId(), te.getIssueKey(),
                previousStatus, targetStatus, userId,
                te.getAssigneeId(), comment);

        // Send critical status notification if applicable
        if (List.of("CANCELLED", "UNRESOLVED").contains(targetStatus)) {
            notificationService.notifyCriticalStatusReached(
                    "TechEvent", te.getId(), te.getIssueKey(),
                    targetStatus, te.getAssigneeId());
        }

        return te;
    }

    // ========== Supplier Analysis Action ==========

    /**
     * Creates a copy of TechEvent in supplier project for supplier analysis.
     */
    @Transactional
    public TechEvent shareWithSupplier(UUID techEventId, UUID supplierProjectId) {
        TechEvent original = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        if (!"UNDER_RESOLVER_ANALYSIS".equals(original.getStatus())) {
            throw new IllegalStateException(
                    "Supplier analysis only available in UNDER_RESOLVER_ANALYSIS status");
        }

        long seq = techEventRepo.countByProjectId(supplierProjectId) + 1;
        TechEvent supplierCopy = TechEvent.builder()
                .projectId(supplierProjectId)
                .issueKey("TE-" + seq)
                .summary(original.getSummary())
                .description(original.getDescription())
                .status("OPEN")
                .reporterTeamId(original.getReporterTeamId())
                .teamForAnalysisId(original.getTeamForAnalysisId())
                .detectedOnProgramId(original.getDetectedOnProgramId())
                .detectedOnDate(original.getDetectedOnDate())
                .detectedOnTestMeanId(original.getDetectedOnTestMeanId())
                .impactedAcSystemId(original.getImpactedAcSystemId())
                .impactedAtaChapterId(original.getImpactedAtaChapterId())
                .defectType(original.getDefectType())
                .defectOrigin(original.getDefectOrigin())
                .defectImpact(original.getDefectImpact())
                .defectImpactRationale(original.getDefectImpactRationale())
                .affectsVersionId(original.getAffectsVersionId())
                .testConfiguration(original.getTestConfiguration())
                .recordingReference(original.getRecordingReference())
                .priority(original.getPriority())
                .reporterId(original.getReporterId())
                .supplierSyncProjectId(original.getProjectId())
                .supplierSyncIssueId(original.getId())
                .build();

        supplierCopy = techEventRepo.save(supplierCopy);

        // Update original with sync reference
        original.setSupplierSyncProjectId(supplierProjectId);
        original.setSupplierSyncIssueId(supplierCopy.getId());
        techEventRepo.save(original);

        log.info("TechEvent {} shared with supplier project {} as {}",
                original.getIssueKey(), supplierProjectId, supplierCopy.getIssueKey());
        return supplierCopy;
    }

    // ========== Sync supplier fields back to original ==========

    @Transactional
    public void syncFromSupplier(UUID supplierTechEventId) {
        TechEvent supplier = techEventRepo.findById(supplierTechEventId)
                .orElseThrow(() -> new RuntimeException("Supplier TechEvent not found"));

        if (supplier.getSupplierSyncIssueId() == null) {
            throw new IllegalStateException("Not a supplier-synced TechEvent");
        }

        techEventRepo.findById(supplier.getSupplierSyncIssueId()).ifPresent(original -> {
            original.setSupplierAnalysis(supplier.getSupplierAnalysis());
            original.setSupplierResponse(supplier.getSupplierResponse());
            original.setFinalAirbusResponse(supplier.getFinalAirbusResponse());
            if (supplier.getApplicableToProgramIds() != null) {
                original.setApplicableToProgramIds(supplier.getApplicableToProgramIds());
            }
            techEventRepo.save(original);
            log.info("Synced supplier analysis from {} back to {}",
                    supplier.getIssueKey(), original.getIssueKey());
        });
    }

    // ========== Create Bench Defect from TechEvent ==========

    @Transactional
    public BenchDefect createBenchDefectFromTechEvent(UUID techEventId) {
        TechEvent te = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        long seq = benchDefectRepo.countByProjectId(te.getProjectId()) + 1;
        BenchDefect bd = BenchDefect.builder()
                .projectId(te.getProjectId())
                .issueKey("BD-" + seq)
                .summary("Bench Defect from " + te.getIssueKey() + ": " + te.getSummary())
                .description(te.getDescription())
                .status("OPEN")
                .detectedOnProgramId(te.getDetectedOnProgramId())
                .detectedOnDate(te.getDetectedOnDate())
                .detectedOnTestMeanId(te.getDetectedOnTestMeanId())
                .affectsVersionId(te.getAffectsVersionId())
                .testConfiguration(te.getTestConfiguration())
                .sourceTechEventId(te.getId())
                .reporterId(te.getReporterId())
                .priority(te.getPriority())
                .build();

        bd = benchDefectRepo.save(bd);
        log.info("Created BenchDefect {} from TechEvent {}", bd.getIssueKey(), te.getIssueKey());
        return bd;
    }

    // ========== Create Problem Report from TechEvent ==========

    @Transactional
    public ProblemReport createProblemReportFromTechEvent(UUID techEventId, String prOrigin, String prType) {
        TechEvent te = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        long seq = problemReportRepo.countByProjectId(te.getProjectId()) + 1;
        ProblemReport pr = ProblemReport.builder()
                .projectId(te.getProjectId())
                .issueKey("PR-" + seq)
                .summary("Problem Report from " + te.getIssueKey() + ": " + te.getSummary())
                .description(te.getDescription())
                .status("OPEN")
                .prOrigin(prOrigin != null ? prOrigin : "VV_ACTIVITY")
                .prType(prType)
                .detectedOnProgramId(te.getDetectedOnProgramId())
                .detectedOnAcSystemId(te.getImpactedAcSystemId())
                .applicableToProgramIds(te.getApplicableToProgramIds())
                .affectsVersionId(te.getAffectsVersionId())
                .linkedTechEventId(te.getId())
                .reporterId(te.getReporterId())
                .systemSupplierId(te.getSystemSupplierId())
                .build();

        pr = problemReportRepo.save(pr);

        // Update TechEvent with link
        te.setLinkedProblemReportId(pr.getId());
        techEventRepo.save(te);

        log.info("Created ProblemReport {} from TechEvent {}", pr.getIssueKey(), te.getIssueKey());
        return pr;
    }

    // ========== Get available transitions for current status ==========

    /**
     * Get available transitions from the workflow engine, falling back to local map.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableTransitions(UUID techEventId) {
        return workflowBridge.getAvailableTransitions("TECH_EVENT", techEventId);
    }
}
