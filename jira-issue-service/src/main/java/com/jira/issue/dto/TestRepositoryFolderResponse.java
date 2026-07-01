package com.jira.issue.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for test repository folder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRepositoryFolderResponse {

    private UUID id;
    private UUID projectId;
    private UUID parentFolderId;
    private String name;
    private String description;
    private String path;
    private Integer depth;
    private Integer sortOrder;
    private Boolean isSmartFolder;
    private String smartFolderQuery;
    private LocalDateTime createdAt;
}