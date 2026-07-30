package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for clone issue response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneIssueResponse {
    private UUID originalIssueId;
    private String originalIssueKey;
    private UUID clonedIssueId;
    private String clonedIssueKey;
    private LocalDateTime clonedAt;
    private UUID clonedBy;
    private boolean includeComments;
    private boolean includeAttachments;
}