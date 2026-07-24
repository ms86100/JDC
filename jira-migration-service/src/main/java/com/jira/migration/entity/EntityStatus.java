package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "entity_status", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PROJECT, ISSUE, WORKFLOW, USER, etc.

    @Column(name = "entity_key", length = 255)
    private String entityKey; // PROJECT-1, user email, etc.

    @Column(name = "entity_id")
    private UUID entityId; // Internal ID if created

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED

    @Column(name = "processing_order")
    private Integer processingOrder;

    // Error details
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_row")
    private Integer errorRow; // For CSV: row number with error

    @Column(name = "error_field", length = 100)
    private String errorField; // Field that caused the error

    @Column(name = "error_context", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> errorContext; // Additional context

    @CreationTimestamp
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    // Validation results
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> validationErrors;

    @Column(name = "warnings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String warnings;

    @Column(name = "source_identifier", length = 255)
    private String sourceIdentifier;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(name = "optimistic_lock_version")
    private Long optimisticLockVersion;

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markCompleted(UUID entityId) {
        this.status = "COMPLETED";
        this.entityId = entityId;
        this.completedAt = LocalDateTime.now();
        if (startedAt != null) {
            this.durationMs = (int) java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    public void markFailed(String errorCode, String errorMessage, String errorField) {
        this.status = "FAILED";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorField = errorField;
        this.completedAt = LocalDateTime.now();
        if (startedAt != null) {
            this.durationMs = (int) java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    public void markSkipped(String reason) {
        this.status = "SKIPPED";
        this.errorMessage = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Status types for entity tracking.
     */
    public enum StatusType {
        PROJECT,
        ISSUE,
        COMMENT,
        ATTACHMENT,
        WORKFLOW,
        SPRINT,
        ISSUE_LINK,
        CUSTOM_FIELD,
        COMPONENT,
        VERSION,
        LABEL,
        USER
    }

    /**
     * Status values for entity processing.
     */
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        SKIPPED,
        DLQ
    }
}