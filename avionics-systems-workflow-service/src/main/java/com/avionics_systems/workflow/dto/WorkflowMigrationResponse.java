package com.avionics_systems.workflow.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMigrationResponse {
    private UUID id;
    private UUID workflowId;
    private UUID workflowVersionId;
    private UUID oldStatusId;
    private String oldStatusName;
    private UUID newStatusId;
    private String newStatusName;
    private String migrationType;
    private Integer issueCount;
    private Integer migratedCount;
    private String migrationStatus;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private List<WorkflowMigrationIssueResponse> issues;
}