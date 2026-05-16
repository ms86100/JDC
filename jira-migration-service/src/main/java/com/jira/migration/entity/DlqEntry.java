package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Dead Letter Queue entry entity for persistent storage.
 * Ensures failed operations survive service restarts.
 */
@Entity
@Table(name = "dlq_entries", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_key", length = 255)
    private String entityKey;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @CreationTimestamp
    @Column(name = "first_failure", nullable = false, updatable = false)
    private LocalDateTime firstFailure;

    @Column(name = "last_attempt")
    private LocalDateTime lastAttempt;

    @Column(name = "next_retry")
    private LocalDateTime nextRetry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private DlqStatus status = DlqStatus.PENDING;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "source_system")
    private String sourceSystem;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution")
    private String resolution;

    @Version
    @Column(name = "optimistic_lock_version")
    private Long optimisticLockVersion;

    public enum DlqStatus {
        PENDING,
        SCHEDULED,
        RETRYING,
        COMPLETED,
        FAILED,
        DISCARDED
    }

    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttempt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = DlqStatus.COMPLETED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = DlqStatus.FAILED;
        this.lastError = error;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markDiscarded(String reason) {
        this.status = DlqStatus.DISCARDED;
        this.resolution = reason;
        this.resolvedAt = LocalDateTime.now();
    }

    public void scheduleRetry(long delaySeconds) {
        this.status = DlqStatus.SCHEDULED;
        this.nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);
    }

    public boolean isEligibleForRetry(int maxAttempts) {
        return this.attemptCount < maxAttempts &&
               (this.nextRetry == null || this.nextRetry.isBefore(LocalDateTime.now()));
    }
}