package com.jira.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTechEventRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String summary;

    private String description;

    // Reporter/Team
    private UUID reporterId;
    private UUID reporterTeamId;
    private UUID teamForAnalysisId;

    // Detection context
    @NotNull
    private UUID detectedOnProgramId;

    private LocalDateTime detectedOnDate;
    private UUID detectedOnTestMeanId;

    // Impact
    private UUID impactedAcSystemId;
    private UUID impactedAtaChapterId;
    private String impactedMsf;
    private UUID impactedFunctionId;
    private String impactedPartition;
    private UUID systemSupplierId;

    // Classification
    private String defectType;
    private String defectOrigin;
    private String defectImpact;
    private String defectImpactRationale;

    // Versions
    private UUID affectsVersionId;
    private UUID fixVersionId;

    // Program applicability
    private List<String> applicableToProgramIds;

    // Analysis
    private String publicAnalysis;
    private String abstractText;
    private String testConfiguration;
    private String recordingReference;
    private String operationalImpact;
    private String requirementImpact;
    private String workaround;

    // Rejection
    private String rejectionRationale;
    private String rejectionType;

    // Supplier sync
    private String supplierAnalysis;
    private String supplierResponse;
    private String supplierStatus;
    private String finalAirbusResponse;
    private UUID supplierSyncProjectId;
    private UUID supplierSyncIssueId;

    // Linked items
    private UUID linkedChangeCardId;
    private UUID linkedProblemReportId;

    // Assignment
    private UUID assigneeId;
    private UUID resolvedBy;
    private String priority;
    private List<String> labels;
    private String vvActivity;
    private String detectedBy;

    // Status (for updates)
    private String status;
}
