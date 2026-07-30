package com.avionics_systems.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Information about a distributed lock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockInfo {

    /**
     * The resource name.
     */
    private String resource;

    /**
     * Lock identifier.
     */
    private String lockId;

    /**
     * Owner identifier.
     */
    private String ownerId;

    /**
     * Node that holds the lock.
     */
    private String nodeId;

    /**
     * When the lock was acquired.
     */
    private Instant acquiredAt;

    /**
     * When the lock expires.
     */
    private Instant expiresAt;

    /**
     * Whether the lock is currently held.
     */
    private boolean isHeld;

    /**
     * Type of lock.
     */
    private String lockType;

    /**
     * Remaining TTL in seconds.
     */
    private long remainingTtlSeconds;

    /**
     * Create from a LockHandle.
     */
    public static LockInfo fromHandle(LockHandle handle) {
        return LockInfo.builder()
                .resource(handle.getResource())
                .lockId(handle.getLockId())
                .ownerId(handle.getOwnerId())
                .nodeId(handle.getNodeId())
                .acquiredAt(handle.getAcquiredAt())
                .expiresAt(handle.getExpiresAt())
                .isHeld(true)
                .lockType(handle.getLockType())
                .remainingTtlSeconds(handle.getRemainingTtlSeconds())
                .build();
    }
}