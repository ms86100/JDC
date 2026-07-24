package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workflow_migrations", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMigration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "workflow_version_id")
    private UUID workflowVersionId;

    @Column(name = "old_status_id", nullable = false)
    private UUID oldStatusId;

    @Column(name = "new_status_id", nullable = false)
    private UUID newStatusId;

    @Column(name = "migration_type", nullable = false, length = 20)
    private String migrationType;

    @Column(name = "issue_count")
    @Builder.Default
    private Integer issueCount = 0;

    @Column(name = "migrated_count")
    @Builder.Default
    private Integer migratedCount = 0;

    @Column(name = "migration_status", length = 20)
    @Builder.Default
    private String migrationStatus = "PENDING";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static final String TYPE_STATUS_CHANGE = "STATUS_CHANGE";
    public static final String TYPE_WORKFLOW_REPLACE = "WORKFLOW_REPLACE";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}