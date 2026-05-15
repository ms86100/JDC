package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a distributed lock stored in the database.
 * Used for coordinating access to resources across multiple nodes.
 */
@Entity
@Table(name = "distributed_locks", schema = "jira_migration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributedLock {

    @Id
    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(name = "lock_id", nullable = false, length = 64)
    private String lockId;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "lock_type", length = 50)
    @Builder.Default
    private String lockType = "EXCLUSIVE";

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Check if this lock has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if this lock is still valid (not expired and owned by the given owner).
     */
    public boolean isValid(String ownerId) {
        return !isExpired() && this.ownerId.equals(ownerId);
    }

    /**
     * Extend the lock expiration time.
     */
    public void extend(Instant newExpiresAt) {
        this.expiresAt = newExpiresAt;
    }

    /**
     * Get remaining TTL in seconds.
     */
    public long getRemainingTtlSeconds() {
        if (expiresAt == null) {
            return 0;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}