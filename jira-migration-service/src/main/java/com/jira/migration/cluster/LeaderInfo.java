package com.jira.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Information about a leader in a leadership group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderInfo {

    /**
     * Leadership group name.
     */
    private String group;

    /**
     * Leader identifier.
     */
    private String leaderId;

    /**
     * Node ID of the leader.
     */
    private String nodeId;

    /**
     * Host of the leader.
     */
    private String host;

    /**
     * Port of the leader.
     */
    private Integer port;

    /**
     * When the leader was elected.
     */
    private Instant electedAt;

    /**
     * When the last heartbeat was received.
     */
    private Instant lastHeartbeat;

    /**
     * When the lease expires.
     */
    private Instant leaseExpiresAt;

    /**
     * Current term (for leader election algorithm).
     */
    private long term;

    /**
     * Number of votes received.
     */
    private int votes;

    /**
     * Check if the leader lease has expired.
     */
    public boolean isLeaseExpired() {
        return leaseExpiresAt != null && leaseExpiresAt.isBefore(Instant.now());
    }

    /**
     * Get remaining lease time in seconds.
     */
    public long getRemainingLeaseSeconds() {
        if (leaseExpiresAt == null) {
            return 0;
        }
        long remaining = leaseExpiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /**
     * Check if this node is the current leader.
     */
    public boolean isLeader(String nodeId) {
        return this.nodeId != null && this.nodeId.equals(nodeId) && !isLeaseExpired();
    }
}