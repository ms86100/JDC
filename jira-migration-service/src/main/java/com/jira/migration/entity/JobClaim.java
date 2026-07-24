package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Entity representing a job claim for distributed job coordination.
 * Tracks which node has claimed a specific job for processing.
 */
@Entity
@Table(name = "job_claims", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobClaim {

    @Id
    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(name = "lock_id", nullable = false, length = 64)
    private String lockId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "job_type", length = 50)
    private String jobType;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "progress_percentage", precision = 5)
    @Builder.Default
    private Double progressPercentage = 0.0;

    @Column(name = "last_update")
    private Instant lastUpdate;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isOwnedBy(String nodeId) {
        return this.nodeId != null && this.nodeId.equals(nodeId);
    }

    public void updateProgress(double progress) {
        this.progressPercentage = progress;
        this.lastUpdate = Instant.now();
    }

    public void extend(Instant newExpiresAt) {
        this.expiresAt = newExpiresAt;
        this.lastUpdate = Instant.now();
    }

    public long getRemainingTtlSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
