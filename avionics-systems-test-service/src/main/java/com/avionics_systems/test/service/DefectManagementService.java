package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.BenchDefect;
import com.avionics_systems.test.entity.ProblemReport;
import com.avionics_systems.test.entity.TechEvent;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.BenchDefectRepository;
import com.avionics_systems.test.repository.ProblemReportRepository;
import com.avionics_systems.test.repository.TechEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefectManagementService {

    private final TechEventRepository techEventRepo;
    private final BenchDefectRepository benchDefectRepo;
    private final ProblemReportRepository problemReportRepo;

    // ========================================================================
    // TechEvent CRUD
    // ========================================================================

    @Transactional
    public TechEventResponse createTechEvent(CreateTechEventRequest request) {
        log.info("Creating TechEvent for project: {}", request.getProjectId());

        TechEvent entity = TechEvent.builder()
                .projectId(request.getProjectId())
                .summary(request.getSummary())
                .description(request.getDescription())
                .status("OPEN")
                .reporterId(request.getReporterId())
                .reporterTeamId(request.getReporterTeamId())
                .teamForAnalysisId(request.getTeamForAnalysisId())
                .detectedOnProgramId(request.getDetectedOnProgramId())
                .detectedOnDate(request.getDetectedOnDate())
                .detectedOnTestMeanId(request.getDetectedOnTestMeanId())
                .impactedAcSystemId(request.getImpactedAcSystemId())
                .impactedAtaChapterId(request.getImpactedAtaChapterId())
                .impactedMsf(request.getImpactedMsf())
                .impactedFunctionId(request.getImpactedFunctionId())
                .impactedPartition(request.getImpactedPartition())
                .systemSupplierId(request.getSystemSupplierId())
                .defectType(request.getDefectType())
                .defectOrigin(request.getDefectOrigin())
                .defectImpact(request.getDefectImpact())
                .defectImpactRationale(request.getDefectImpactRationale())
                .affectsVersionId(request.getAffectsVersionId())
                .fixVersionId(request.getFixVersionId())
                .applicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of())
                .publicAnalysis(request.getPublicAnalysis())
                .abstractText(request.getAbstractText())
                .testConfiguration(request.getTestConfiguration())
                .recordingReference(request.getRecordingReference())
                .operationalImpact(request.getOperationalImpact())
                .requirementImpact(request.getRequirementImpact())
                .workaround(request.getWorkaround())
                .rejectionRationale(request.getRejectionRationale())
                .rejectionType(request.getRejectionType())
                .supplierAnalysis(request.getSupplierAnalysis())
                .supplierResponse(request.getSupplierResponse())
                .supplierStatus(request.getSupplierStatus())
                .finalAirbusResponse(request.getFinalAirbusResponse())
                .supplierSyncProjectId(request.getSupplierSyncProjectId())
                .supplierSyncIssueId(request.getSupplierSyncIssueId())
                .linkedChangeCardId(request.getLinkedChangeCardId())
                .linkedProblemReportId(request.getLinkedProblemReportId())
                .assigneeId(request.getAssigneeId())
                .resolvedBy(request.getResolvedBy())
                .priority(request.getPriority())
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .vvActivity(request.getVvActivity())
                .detectedBy(request.getDetectedBy())
                .build();

        entity = techEventRepo.save(entity);
        log.info("Created TechEvent with id: {}", entity.getId());
        return mapTechEventToResponse(entity);
    }

    @Transactional(readOnly = true)
    public TechEventResponse getTechEvent(UUID id) {
        TechEvent entity = techEventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechEvent", "id", id));
        return mapTechEventToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TechEventResponse> getTechEventsByProject(UUID projectId) {
        return techEventRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapTechEventToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TechEventResponse> getTechEventsByProgram(UUID programId) {
        return techEventRepo.findByDetectedOnProgramId(programId).stream()
                .map(this::mapTechEventToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TechEventResponse> getTechEventsByStatus(String status) {
        return techEventRepo.findByStatus(status).stream()
                .map(this::mapTechEventToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TechEventResponse updateTechEvent(UUID id, CreateTechEventRequest request) {
        log.info("Updating TechEvent: {}", id);

        TechEvent entity = techEventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TechEvent", "id", id));

        entity.setProjectId(request.getProjectId());
        entity.setSummary(request.getSummary());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setReporterId(request.getReporterId());
        entity.setReporterTeamId(request.getReporterTeamId());
        entity.setTeamForAnalysisId(request.getTeamForAnalysisId());
        entity.setDetectedOnProgramId(request.getDetectedOnProgramId());
        entity.setDetectedOnDate(request.getDetectedOnDate());
        entity.setDetectedOnTestMeanId(request.getDetectedOnTestMeanId());
        entity.setImpactedAcSystemId(request.getImpactedAcSystemId());
        entity.setImpactedAtaChapterId(request.getImpactedAtaChapterId());
        entity.setImpactedMsf(request.getImpactedMsf());
        entity.setImpactedFunctionId(request.getImpactedFunctionId());
        entity.setImpactedPartition(request.getImpactedPartition());
        entity.setSystemSupplierId(request.getSystemSupplierId());
        entity.setDefectType(request.getDefectType());
        entity.setDefectOrigin(request.getDefectOrigin());
        entity.setDefectImpact(request.getDefectImpact());
        entity.setDefectImpactRationale(request.getDefectImpactRationale());
        entity.setAffectsVersionId(request.getAffectsVersionId());
        entity.setFixVersionId(request.getFixVersionId());
        entity.setApplicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of());
        entity.setPublicAnalysis(request.getPublicAnalysis());
        entity.setAbstractText(request.getAbstractText());
        entity.setTestConfiguration(request.getTestConfiguration());
        entity.setRecordingReference(request.getRecordingReference());
        entity.setOperationalImpact(request.getOperationalImpact());
        entity.setRequirementImpact(request.getRequirementImpact());
        entity.setWorkaround(request.getWorkaround());
        entity.setRejectionRationale(request.getRejectionRationale());
        entity.setRejectionType(request.getRejectionType());
        entity.setSupplierAnalysis(request.getSupplierAnalysis());
        entity.setSupplierResponse(request.getSupplierResponse());
        entity.setSupplierStatus(request.getSupplierStatus());
        entity.setFinalAirbusResponse(request.getFinalAirbusResponse());
        entity.setSupplierSyncProjectId(request.getSupplierSyncProjectId());
        entity.setSupplierSyncIssueId(request.getSupplierSyncIssueId());
        entity.setLinkedChangeCardId(request.getLinkedChangeCardId());
        entity.setLinkedProblemReportId(request.getLinkedProblemReportId());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setResolvedBy(request.getResolvedBy());
        entity.setPriority(request.getPriority());
        entity.setLabels(request.getLabels() != null ? request.getLabels() : List.of());
        entity.setVvActivity(request.getVvActivity());
        entity.setDetectedBy(request.getDetectedBy());

        entity = techEventRepo.save(entity);
        log.info("Updated TechEvent: {}", id);
        return mapTechEventToResponse(entity);
    }

    // ========================================================================
    // BenchDefect CRUD
    // ========================================================================

    @Transactional
    public BenchDefectResponse createBenchDefect(CreateBenchDefectRequest request) {
        log.info("Creating BenchDefect for project: {}", request.getProjectId());

        BenchDefect entity = BenchDefect.builder()
                .projectId(request.getProjectId())
                .summary(request.getSummary())
                .description(request.getDescription())
                .status("OPEN")
                .severity(request.getSeverity())
                .criticality(request.getCriticality())
                .defectType(request.getDefectType())
                .defectOrigin(request.getDefectOrigin())
                .defectImpact(request.getDefectImpact())
                .defectImpactRationale(request.getDefectImpactRationale())
                .ltmDefectType(request.getLtmDefectType())
                .defectOriginCategoryId(request.getDefectOriginCategoryId())
                .defectOriginSubItemId(request.getDefectOriginSubItemId())
                .detectedOnProgramId(request.getDetectedOnProgramId())
                .detectedOnDate(request.getDetectedOnDate())
                .detectedOnTestMeanId(request.getDetectedOnTestMeanId())
                .applicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of())
                .applicableToTestMeans(request.getApplicableToTestMeans() != null ? request.getApplicableToTestMeans() : List.of())
                .affectedAta(request.getAffectedAta())
                .affectsVersionId(request.getAffectsVersionId())
                .fixVersionId(request.getFixVersionId())
                .testConfiguration(request.getTestConfiguration())
                .workaround(request.getWorkaround())
                .changeReference(request.getChangeReference())
                .objectiveDateAnalysis(request.getObjectiveDateAnalysis())
                .objectiveDateClosure(request.getObjectiveDateClosure())
                .sourceTechEventId(request.getSourceTechEventId())
                .reporterId(request.getReporterId())
                .assigneeId(request.getAssigneeId())
                .priority(request.getPriority())
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .build();

        entity = benchDefectRepo.save(entity);
        log.info("Created BenchDefect with id: {}", entity.getId());
        return mapBenchDefectToResponse(entity);
    }

    @Transactional(readOnly = true)
    public BenchDefectResponse getBenchDefect(UUID id) {
        BenchDefect entity = benchDefectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BenchDefect", "id", id));
        return mapBenchDefectToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<BenchDefectResponse> getBenchDefectsByProject(UUID projectId) {
        return benchDefectRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapBenchDefectToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BenchDefectResponse> getBenchDefectsByTechEvent(UUID techEventId) {
        return benchDefectRepo.findBySourceTechEventId(techEventId).stream()
                .map(this::mapBenchDefectToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BenchDefectResponse updateBenchDefect(UUID id, CreateBenchDefectRequest request) {
        log.info("Updating BenchDefect: {}", id);

        BenchDefect entity = benchDefectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BenchDefect", "id", id));

        entity.setProjectId(request.getProjectId());
        entity.setSummary(request.getSummary());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setSeverity(request.getSeverity());
        entity.setCriticality(request.getCriticality());
        entity.setDefectType(request.getDefectType());
        entity.setDefectOrigin(request.getDefectOrigin());
        entity.setDefectImpact(request.getDefectImpact());
        entity.setDefectImpactRationale(request.getDefectImpactRationale());
        entity.setLtmDefectType(request.getLtmDefectType());
        entity.setDefectOriginCategoryId(request.getDefectOriginCategoryId());
        entity.setDefectOriginSubItemId(request.getDefectOriginSubItemId());
        entity.setDetectedOnProgramId(request.getDetectedOnProgramId());
        entity.setDetectedOnDate(request.getDetectedOnDate());
        entity.setDetectedOnTestMeanId(request.getDetectedOnTestMeanId());
        entity.setApplicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of());
        entity.setApplicableToTestMeans(request.getApplicableToTestMeans() != null ? request.getApplicableToTestMeans() : List.of());
        entity.setAffectedAta(request.getAffectedAta());
        entity.setAffectsVersionId(request.getAffectsVersionId());
        entity.setFixVersionId(request.getFixVersionId());
        entity.setTestConfiguration(request.getTestConfiguration());
        entity.setWorkaround(request.getWorkaround());
        entity.setChangeReference(request.getChangeReference());
        entity.setObjectiveDateAnalysis(request.getObjectiveDateAnalysis());
        entity.setObjectiveDateClosure(request.getObjectiveDateClosure());
        entity.setSourceTechEventId(request.getSourceTechEventId());
        entity.setReporterId(request.getReporterId());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setPriority(request.getPriority());
        entity.setLabels(request.getLabels() != null ? request.getLabels() : List.of());

        entity = benchDefectRepo.save(entity);
        log.info("Updated BenchDefect: {}", id);
        return mapBenchDefectToResponse(entity);
    }

    // ========================================================================
    // ProblemReport CRUD
    // ========================================================================

    @Transactional
    public ProblemReportResponse createProblemReport(CreateProblemReportRequest request) {
        log.info("Creating ProblemReport for project: {}", request.getProjectId());

        ProblemReport entity = ProblemReport.builder()
                .projectId(request.getProjectId())
                .summary(request.getSummary())
                .description(request.getDescription())
                .status("OPEN")
                .prOrigin(request.getPrOrigin())
                .prType(request.getPrType())
                .prTypeRationale(request.getPrTypeRationale())
                .potentialEffects(request.getPotentialEffects())
                .justificationMitigation(request.getJustificationMitigation())
                .detectedOnProgramId(request.getDetectedOnProgramId())
                .detectedOnAcSystemId(request.getDetectedOnAcSystemId())
                .applicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of())
                .rejectionType(request.getRejectionType())
                .rejectionRationale(request.getRejectionRationale())
                .linkedTechEventId(request.getLinkedTechEventId())
                .affectsVersionId(request.getAffectsVersionId())
                .fixVersionId(request.getFixVersionId())
                .classification(request.getClassification())
                .reporterId(request.getReporterId())
                .assigneeId(request.getAssigneeId())
                .systemSupplierId(request.getSystemSupplierId())
                .priority(request.getPriority())
                .labels(request.getLabels() != null ? request.getLabels() : List.of())
                .build();

        entity = problemReportRepo.save(entity);
        log.info("Created ProblemReport with id: {}", entity.getId());
        return mapProblemReportToResponse(entity);
    }

    @Transactional(readOnly = true)
    public ProblemReportResponse getProblemReport(UUID id) {
        ProblemReport entity = problemReportRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProblemReport", "id", id));
        return mapProblemReportToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ProblemReportResponse> getProblemReportsByProject(UUID projectId) {
        return problemReportRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapProblemReportToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProblemReportResponse> getProblemReportsByTechEvent(UUID techEventId) {
        return problemReportRepo.findByLinkedTechEventId(techEventId).stream()
                .map(this::mapProblemReportToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProblemReportResponse updateProblemReport(UUID id, CreateProblemReportRequest request) {
        log.info("Updating ProblemReport: {}", id);

        ProblemReport entity = problemReportRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProblemReport", "id", id));

        entity.setProjectId(request.getProjectId());
        entity.setSummary(request.getSummary());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setPrOrigin(request.getPrOrigin());
        entity.setPrType(request.getPrType());
        entity.setPrTypeRationale(request.getPrTypeRationale());
        entity.setPotentialEffects(request.getPotentialEffects());
        entity.setJustificationMitigation(request.getJustificationMitigation());
        entity.setDetectedOnProgramId(request.getDetectedOnProgramId());
        entity.setDetectedOnAcSystemId(request.getDetectedOnAcSystemId());
        entity.setApplicableToProgramIds(request.getApplicableToProgramIds() != null ? request.getApplicableToProgramIds() : List.of());
        entity.setRejectionType(request.getRejectionType());
        entity.setRejectionRationale(request.getRejectionRationale());
        entity.setLinkedTechEventId(request.getLinkedTechEventId());
        entity.setAffectsVersionId(request.getAffectsVersionId());
        entity.setFixVersionId(request.getFixVersionId());
        entity.setClassification(request.getClassification());
        entity.setReporterId(request.getReporterId());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setSystemSupplierId(request.getSystemSupplierId());
        entity.setPriority(request.getPriority());
        entity.setLabels(request.getLabels() != null ? request.getLabels() : List.of());

        entity = problemReportRepo.save(entity);
        log.info("Updated ProblemReport: {}", id);
        return mapProblemReportToResponse(entity);
    }

    // ========================================================================
    // Private mapping methods
    // ========================================================================

    private TechEventResponse mapTechEventToResponse(TechEvent entity) {
        return TechEventResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .reporterId(entity.getReporterId())
                .reporterTeamId(entity.getReporterTeamId())
                .teamForAnalysisId(entity.getTeamForAnalysisId())
                .detectedOnProgramId(entity.getDetectedOnProgramId())
                .detectedOnDate(entity.getDetectedOnDate())
                .detectedOnTestMeanId(entity.getDetectedOnTestMeanId())
                .impactedAcSystemId(entity.getImpactedAcSystemId())
                .impactedAtaChapterId(entity.getImpactedAtaChapterId())
                .impactedMsf(entity.getImpactedMsf())
                .impactedFunctionId(entity.getImpactedFunctionId())
                .impactedPartition(entity.getImpactedPartition())
                .systemSupplierId(entity.getSystemSupplierId())
                .defectType(entity.getDefectType())
                .defectOrigin(entity.getDefectOrigin())
                .defectImpact(entity.getDefectImpact())
                .defectImpactRationale(entity.getDefectImpactRationale())
                .affectsVersionId(entity.getAffectsVersionId())
                .fixVersionId(entity.getFixVersionId())
                .applicableToProgramIds(entity.getApplicableToProgramIds())
                .publicAnalysis(entity.getPublicAnalysis())
                .abstractText(entity.getAbstractText())
                .testConfiguration(entity.getTestConfiguration())
                .recordingReference(entity.getRecordingReference())
                .operationalImpact(entity.getOperationalImpact())
                .requirementImpact(entity.getRequirementImpact())
                .workaround(entity.getWorkaround())
                .rejectionRationale(entity.getRejectionRationale())
                .rejectionType(entity.getRejectionType())
                .supplierAnalysis(entity.getSupplierAnalysis())
                .supplierResponse(entity.getSupplierResponse())
                .supplierStatus(entity.getSupplierStatus())
                .finalAirbusResponse(entity.getFinalAirbusResponse())
                .supplierSyncProjectId(entity.getSupplierSyncProjectId())
                .supplierSyncIssueId(entity.getSupplierSyncIssueId())
                .linkedChangeCardId(entity.getLinkedChangeCardId())
                .linkedProblemReportId(entity.getLinkedProblemReportId())
                .assigneeId(entity.getAssigneeId())
                .resolvedBy(entity.getResolvedBy())
                .priority(entity.getPriority())
                .labels(entity.getLabels())
                .vvActivity(entity.getVvActivity())
                .detectedBy(entity.getDetectedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BenchDefectResponse mapBenchDefectToResponse(BenchDefect entity) {
        return BenchDefectResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .severity(entity.getSeverity())
                .criticality(entity.getCriticality())
                .defectType(entity.getDefectType())
                .defectOrigin(entity.getDefectOrigin())
                .defectImpact(entity.getDefectImpact())
                .defectImpactRationale(entity.getDefectImpactRationale())
                .ltmDefectType(entity.getLtmDefectType())
                .defectOriginCategoryId(entity.getDefectOriginCategoryId())
                .defectOriginSubItemId(entity.getDefectOriginSubItemId())
                .detectedOnProgramId(entity.getDetectedOnProgramId())
                .detectedOnDate(entity.getDetectedOnDate())
                .detectedOnTestMeanId(entity.getDetectedOnTestMeanId())
                .applicableToProgramIds(entity.getApplicableToProgramIds())
                .applicableToTestMeans(entity.getApplicableToTestMeans())
                .affectedAta(entity.getAffectedAta())
                .affectsVersionId(entity.getAffectsVersionId())
                .fixVersionId(entity.getFixVersionId())
                .testConfiguration(entity.getTestConfiguration())
                .workaround(entity.getWorkaround())
                .changeReference(entity.getChangeReference())
                .objectiveDateAnalysis(entity.getObjectiveDateAnalysis())
                .objectiveDateClosure(entity.getObjectiveDateClosure())
                .sourceTechEventId(entity.getSourceTechEventId())
                .reporterId(entity.getReporterId())
                .assigneeId(entity.getAssigneeId())
                .priority(entity.getPriority())
                .labels(entity.getLabels())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ProblemReportResponse mapProblemReportToResponse(ProblemReport entity) {
        return ProblemReportResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .prOrigin(entity.getPrOrigin())
                .prType(entity.getPrType())
                .prTypeRationale(entity.getPrTypeRationale())
                .potentialEffects(entity.getPotentialEffects())
                .justificationMitigation(entity.getJustificationMitigation())
                .detectedOnProgramId(entity.getDetectedOnProgramId())
                .detectedOnAcSystemId(entity.getDetectedOnAcSystemId())
                .applicableToProgramIds(entity.getApplicableToProgramIds())
                .rejectionType(entity.getRejectionType())
                .rejectionRationale(entity.getRejectionRationale())
                .linkedTechEventId(entity.getLinkedTechEventId())
                .affectsVersionId(entity.getAffectsVersionId())
                .fixVersionId(entity.getFixVersionId())
                .classification(entity.getClassification())
                .reporterId(entity.getReporterId())
                .assigneeId(entity.getAssigneeId())
                .systemSupplierId(entity.getSystemSupplierId())
                .priority(entity.getPriority())
                .labels(entity.getLabels())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
