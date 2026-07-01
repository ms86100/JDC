package com.jira.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for watcher responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatcherResponse {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private LocalDateTime createdAt;
}