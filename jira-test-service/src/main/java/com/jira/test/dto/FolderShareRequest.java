package com.jira.test.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderShareRequest {

    private UUID folderId;

    private List<UUID> userIds;

    private List<UUID> groupIds;

    private String shareLevel;

    private Boolean sendNotification;

    private String message;
}