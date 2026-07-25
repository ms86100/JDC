package com.jira.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotBlank(message = "{validation.document.title.required}")
    private String title;

    private String content;

    private UUID projectId;
    private UUID issueId;

    @NotNull(message = "{validation.document.type.required}")
    private String documentType;

    private String space;
    private UUID parentDocumentId;
    private String versionLabel;
    private String attachmentUrl;
    private String metadata;
    private Boolean isPublished = false;
    private String pageLayout;
    private String[] labels;
    private String externalUrl;
}