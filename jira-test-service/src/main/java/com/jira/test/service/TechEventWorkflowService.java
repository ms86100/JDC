package com.jira.test.service;

import com.jira.test.entity.BenchDefect;
import com.jira.test.entity.ProblemReport;
import com.jira.test.entity.TechEvent;
import com.jira.test.repository.BenchDefectRepository;
import com.jira.test.repository.ProblemReportRepository;
import com.jira.test.repository.TechEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechEventWorkflowService {

    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;

    /**
     * Allowed transitions per the M1668 workflow state machine.
     */
    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry("OPEN", List.of("UNDER_ORIGINATOR_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_ORIGINATOR_ANALYSIS", List.of("UNDER_RESOLVER_ANALYSIS", "UNDER_TEST_MEAN_ANALYSIS", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_RESOLVER_ANALYSIS", List.of("READY_FOR_REVIEW", "CLASSIFIED", "PROPOSED_FOR_CANCELLATION", "TO_BE_REFINED")),
            Map.entry("UNDER_TEST_MEAN_ANALYSIS", List.of("UNDER_ORIGINATOR_ANALYSIS", "CLOSED", "CANCELLED")),
            Map.entry("READY_FOR_REVIEW", List.of("CLASSIFIED", "UNDER_RESOLVER_ANALYSIS")),
            Map.entry("CLASSIFIED", List.of("TO_BE_ASSESSED")),
            Map.entry("TO_BE_ASSESSED", List.of("RESOLVED_CORRECTED", "RESOLVED_CONTAINED")),
            Map.entry("RESOLVED_CORRECTED", List.of("CLOSED")),
            Map.entry("RESOLVED_CONTAINED", List.of("UNDER_RESOLVER_ANALYSIS", "UNRESOLVED")),
            Map.entry("PROPOSED_FOR_CANCELLATION", List.of("CANCELLED", "UNDER_ORIGINATOR_ANALYSIS")),
            Map.entry("CANCELLED", List.of("OPEN")),
            Map.entry("CLOSED", List.of("OPEN")),
            Map.entry("TO_BE_REFINED", List.of("UNDER_ORIGINATOR_ANALYSIS")),
            Map.entry("UNRESOLVED", List.of("UNDER_RESOLVER_ANALYSIS"))
    );

    // ========== Status Transitions ==========

    @Transactional
    public TechEvent transitionStatus(UUID techEventId, String targetStatus, UUID userId, String comment) {
        TechEvent te = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        validateTransition(te.getStatus(), targetStatus);

        String previousStatus = te.getStatus();
        te.setStatus(targetStatus);

        // Auto-set resolved_by on final states
        if (List.of("CLOSED", "CANCELLED").contains(targetStatus)) {
            te.setResolvedBy(userId);
        }

        te = techEventRepo.save(te);
        log.info("TechEvent {} transitioned {} -> {} by user {}",
                te.getIssueKey(), previousStatus, targetStatus, userId);
        return te;
    }

    private void validateTransition(String fromStatus, String toStatus) {
        List<String> validTargets = ALLOWED_TRANSITIONS.getOrDefault(fromStatus, List.of());
        if (!validTargets.contains(toStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from " + fromStatus + " to " + toStatus);
        }
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

    @Transactional(readOnly = true)
    public List<String> getAvailableTransitions(UUID techEventId) {
        TechEvent te = techEventRepo.findById(techEventId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + techEventId));

        return ALLOWED_TRANSITIONS.getOrDefault(te.getStatus(), List.of());
    }
}
