package com.avionics_systems.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowMigrationIssueResponse {
    private UUID id;
    private UUID issueId;
    private String issueKey;
    private UUID oldStatusId;
    private UUID newStatusId;
    private String migrationStatus;
    private LocalDateTime processedAt;
    private String errorMessage;
}