package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderPermissionRequest {

    private UUID folderId;

    private List<UUID> userIds;

    private List<UUID> groupIds;

    private String permissionLevel;

    private Boolean inheritFromParent;
}