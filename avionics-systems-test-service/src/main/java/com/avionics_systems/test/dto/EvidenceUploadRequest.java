package com.avionics_systems.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceUploadRequest {

    @NotNull(message = "Execution ID is required")
    private UUID executionId;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID stepResultId;

    @NotNull(message = "Evidence type is required")
    private String evidenceType; // SCREENSHOT, VIDEO, LOG, HAR, PDF, FILE, COMMENT

    private String classificationLevel; // STEP_LEVEL, RUN_LEVEL, ENVIRONMENT_LEVEL

    private String fileName;

    private Long fileSize;

    private String mimeType;

    private String url; // URL if already uploaded to CDN

    private String content; // For inline comments

    private Map<String, String> metadata;

    private UUID retentionPolicyId;

    private UUID createdBy;
}