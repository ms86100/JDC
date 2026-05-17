package com.jira.workflow.dto;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WorkflowMigrationIssueResponse {
    private UUID id;
    private UUID issueId;
    private String issueKey;
    private UUID oldStatusId;
    private UUID newStatusId;
    private String migrationStatus;
    private LocalDateTime processedAt;
    private String errorMessage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateMigrationRequest {
    private UUID workflowId;
    private UUID oldStatusId;
    private UUID newStatusId;
    private String migrationType;
    private UUID userId;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MigrationPreviewResponse {
    private UUID workflowId;
    private UUID oldStatusId;
    private String oldStatusName;
    private UUID newStatusId;
    private String newStatusName;
    private Integer issueCount;
    private java.util.List<IssuePreview> issues;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class IssuePreview {
    private UUID issueId;
    private String issueKey;
    private String summary;
    private UUID currentStatusId;
}