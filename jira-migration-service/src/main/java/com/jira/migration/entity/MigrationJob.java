package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "migration_jobs", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_type", nullable = false, length = 20)
    private String jobType; // IMPORT, EXPORT

    @Column(name = "job_status", nullable = false, length = 20)
    @Builder.Default
    private String jobStatus = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED

    @Column(name = "import_source", length = 50)
    private String importSource; // JIRA_DC, CSV, BACKUP, OTHER

    // Progress tracking
    @Column(name = "total_entities")
    @Builder.Default
    private Integer totalEntities = 0;

    @Column(name = "processed_entities")
    @Builder.Default
    private Integer processedEntities = 0;

    @Column(name = "failed_entities")
    @Builder.Default
    private Integer failedEntities = 0;

    @Column(name = "progress_percentage", precision = 5)
    @Builder.Default
    private Double progressPercentage = 0.0;

    // Configuration stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private Map<String, Object> options;

    // User tracking
    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @CreationTimestamp
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Error tracking
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private Map<String, Object> errorDetails;

    // Source/target info
    @Column(name = "source_project_id")
    private UUID sourceProjectId;

    @Column(name = "target_project_id")
    private UUID targetProjectId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Version
    @Column(name = "optimistic_lock_version")
    private Long optimisticLockVersion;

    // Rollback support
    @Column(name = "can_rollback")
    @Builder.Default
    private Boolean canRollback = false;

    @Column(name = "rollback_job_id")
    private UUID rollbackJobId;

    // Result metadata
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_metadata", columnDefinition = "jsonb")
    private Map<String, Object> resultMetadata;

    public void incrementProcessed() {
        this.processedEntities++;
        updateProgress();
    }

    public void incrementFailed() {
        this.failedEntities++;
        updateProgress();
    }

    private void updateProgress() {
        if (totalEntities > 0) {
            this.progressPercentage = (processedEntities.doubleValue() / totalEntities.doubleValue()) * 100.0;
        }
    }

    public void markStarted() {
        this.jobStatus = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.jobStatus = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100.0;
    }

    public void markFailed(String errorMessage) {
        this.jobStatus = "FAILED";
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}