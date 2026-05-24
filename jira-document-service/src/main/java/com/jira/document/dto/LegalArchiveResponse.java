package com.jira.document.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalArchiveResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private UUID legalMatterId;
    private String matterReference;
    private String archiveType;
    private String status;
    private LocalDateTime retentionDate;
    private LocalDateTime dispositionDate;
    private String dispositionAction;
    private String legalBasis;
    private String reason;
    private UUID archivedBy;
    private UUID[] relatedDocumentIds;
    private UUID[] relatedIssueIds;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime reviewDate;
}