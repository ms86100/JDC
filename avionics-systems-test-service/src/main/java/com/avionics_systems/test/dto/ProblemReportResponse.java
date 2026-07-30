package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemReportResponse {

    private UUID id;
    private UUID projectId;
    private String issueKey;
    private String summary;
    private String description;
    private String status;

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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
