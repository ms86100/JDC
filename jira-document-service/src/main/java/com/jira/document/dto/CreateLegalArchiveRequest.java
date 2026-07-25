package com.jira.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLegalArchiveRequest {

    @NotBlank(message = "{validation.archive.name.required}")
    private String name;

    private String description;
    private UUID projectId;
    private UUID legalMatterId;
    private String matterReference;

    @NotNull(message = "{validation.archive.type.required}")
    private String archiveType;

    private LocalDateTime retentionDate;
    private String dispositionAction;
    private String legalBasis;
    private String reason;
    private UUID[] relatedDocumentIds;
    private UUID[] relatedIssueIds;
    private String metadata;
}