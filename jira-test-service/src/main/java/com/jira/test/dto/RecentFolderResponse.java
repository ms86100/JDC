package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentFolderResponse {

    private List<FolderAccessRecord> recentlyAccessed;

    private List<FolderAccessRecord> recentlyModified;

    private List<FolderAccessRecord> favorites;

    private Integer maxResults;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class FolderAccessRecord {
    private UUID folderId;
    private String folderName;
    private UUID projectId;
    private String path;
    private LocalDateTime accessedAt;
    private LocalDateTime modifiedAt;
    private UUID accessedBy;
}