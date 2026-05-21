package com.jira.test.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPermissionResponse {

    private UUID folderId;
    private String folderName;

    private List<FolderAccessEntry> users;
    private List<FolderAccessEntry> groups;

    private Boolean inheritanceEnabled;
    private String effectivePermission;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class FolderAccessEntry {
    private UUID id;
    private String name;
    private String email;
    private String type;
    private String permissionLevel;
    private LocalDateTime grantedAt;
    private UUID grantedBy;
}