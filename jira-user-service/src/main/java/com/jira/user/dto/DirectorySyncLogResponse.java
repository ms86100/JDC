package com.jira.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorySyncLogResponse {
    private UUID id;
    private UUID directoryId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int usersAdded;
    private int usersUpdated;
    private int usersRemoved;
    private int groupsSynced;
    private String status;
    private String errors;
}
