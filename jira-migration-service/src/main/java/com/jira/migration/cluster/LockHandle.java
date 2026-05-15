package com.jira.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a handle to an acquired distributed lock.
 * This handle is required to release or extend the lock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockHandle {

    /**
     * The resource name that is locked.
     */
    private String resource;

    /**
     * Unique identifier for this lock instance.
     */
    private String lockId;

    /**
     * Identifier for the owner of this lock.
     */
    private String ownerId;

    /**
     * Node that acquired this lock.
     */
    private String nodeId;

    /**
     * Timestamp when the lock was acquired.
     */
    private Instant acquiredAt;

    /**
     * Timestamp when the lock will expire.
     */
    private Instant expiresAt;

    /**
     * Type of lock (e.g., EXCLUSIVE, SHARED).
     */
    private String lockType;

    /**
     * Check if the lock has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
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

    /**
     * Check if this handle can release the given lock.
     */
    public boolean canRelease(String resource, String lockId) {
        return this.resource.equals(resource) && this.lockId.equals(lockId);
    }
}