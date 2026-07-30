package com.avionics_systems.document.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private String title;
    private String content;
    private UUID projectId;
    private UUID issueId;
    private UUID ownerId;
    private String documentType;
    private String space;
    private UUID parentDocumentId;
    private String versionLabel;
    private String attachmentUrl;
    private String mimeType;
    private Long fileSize;
    private String metadata;
    private Boolean isPublished;
    private Boolean isArchived;
    private String pageLayout;
    private String[] labels;
    private String externalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Integer childPosition;
    private Integer versionCount;
}
