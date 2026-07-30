package com.avionics_systems.test.dto;

import lombok.*;

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