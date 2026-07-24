package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "workflow_migration_issues", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMigrationIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "migration_id", nullable = false)
    private UUID migrationId;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "old_status_id", nullable = false)
    private UUID oldStatusId;

    @Column(name = "new_status_id", nullable = false)
    private UUID newStatusId;

    @Column(name = "migration_status", length = 20)
    @Builder.Default
    private String migrationStatus = "PENDING";

    @Column(name = "processed_at")
    private java.time.LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_MIGRATED = "MIGRATED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";
}