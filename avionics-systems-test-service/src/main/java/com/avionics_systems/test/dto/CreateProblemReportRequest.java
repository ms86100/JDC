package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProblemReportRequest {

    @NotNull
    private UUID projectId;

    @NotBlank
    private String summary;

    private String description;

    private String prOrigin;
    private String prType;
    private String prTypeRationale;

    private String potentialEffects;
    private String justificationMitigation;

    // Detection
    private UUID detectedOnProgramId;
    private UUID detectedOnAcSystemId;
    private List<String> applicableToProgramIds;

    // Rejection
    private String rejectionType;
    private String rejectionRationale;

    // Linked
    private UUID linkedTechEventId;

    // Versions
    private UUID affectsVersionId;
    private UUID fixVersionId;

    // Classification
    private String classification;

    // Assignment
    private UUID reporterId;
    private UUID assigneeId;
    private UUID systemSupplierId;
    private String priority;
    private List<String> labels;

    // Status (for updates)
    private String status;
}
