package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBenchDefectRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String summary;

    @NotBlank
    private String description;

    @NotBlank
    private String severity;

    private String criticality;

    private String defectType;
    private String defectOrigin;
    private String defectImpact;
    private String defectImpactRationale;
    private String ltmDefectType;

    // Origin category (cascading)
    private UUID defectOriginCategoryId;
    private UUID defectOriginSubItemId;

    // Detection
    @NotNull
    private UUID detectedOnProgramId;

    private LocalDateTime detectedOnDate;
    private UUID detectedOnTestMeanId;

    // Applicability
    private List<String> applicableToProgramIds;
    private List<String> applicableToTestMeans;
    private String affectedAta;

    // Versions
    private UUID affectsVersionId;
    private UUID fixVersionId;

    // Analysis
    private String testConfiguration;
    private String workaround;
    private String changeReference;

    // Dates
    private LocalDate objectiveDateAnalysis;
    private LocalDate objectiveDateClosure;

    // Source
    private UUID sourceTechEventId;

    // Assignment
    private UUID reporterId;
    private UUID assigneeId;
    private String priority;
    private List<String> labels;

    // Status (for updates)
    private String status;
}
