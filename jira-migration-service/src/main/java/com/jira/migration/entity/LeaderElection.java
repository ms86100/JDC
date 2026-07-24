package com.jira.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * Entity representing a leader election entry.
 * Stores leader information for different leadership groups.
 */
@Entity
@Table(name = "leader_elections", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderElection {

    @Id
    @Column(name = "leadership_group", length = 255)
    private String leadershipGroup;

    @Column(name = "leader_id", nullable = false, length = 64)
    private String leaderId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "elected_at", nullable = false)
    private Instant electedAt;

    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat;

    @Column(name = "lease_expires_at", nullable = false)
    private Instant leaseExpiresAt;

    @Column(name = "term", nullable = false)
    @Builder.Default
    private Long term = 1L;

    @Column(name = "votes", nullable = false)
    @Builder.Default
    private Integer votes = 1;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Check if the leader lease has expired.
     */
    public boolean isLeaseExpired() {
        return leaseExpiresAt != null && leaseExpiresAt.isBefore(Instant.now());
    }

    /**
     * Check if leader heartbeat is stale.
     */
    public boolean isHeartbeatStale(Instant threshold) {
        return lastHeartbeat == null || lastHeartbeat.isBefore(threshold);
    }

    /**
     * Renew the leader lease.
     */
    public void renewLease(Instant newExpiry, long newTerm) {
        this.lastHeartbeat = Instant.now();
        this.leaseExpiresAt = newExpiry;
        this.term = newTerm;
    }

    /**
     * Check if this node is the current leader.
     */
    public boolean isLeader(String nodeId) {
        return this.nodeId != null && this.nodeId.equals(nodeId) && !isLeaseExpired();
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
}