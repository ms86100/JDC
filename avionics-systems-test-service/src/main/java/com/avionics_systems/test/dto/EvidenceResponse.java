package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceResponse {

    private UUID id;
    private UUID executionId;
    private UUID stepResultId;
    private String evidenceType;
    private String classificationLevel;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String url;
    private String thumbnailUrl;
    private String content;
    private Map<String, String> metadata;
    private UUID retentionPolicyId;
    private String retentionPolicyName;
    private Boolean isArchived;
    private LocalDateTime archivedAt;
    private UUID createdBy;
    private LocalDateTime createdAt;
}