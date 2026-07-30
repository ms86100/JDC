package com.avionics_systems.migration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a cluster node in the distributed system.
 * Tracks node health, registration, and metadata.
 */
@Entity
@Table(name = "cluster_nodes", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNodeEntity {

    @Id
    @Column(name = "node_id", length = 64)
    private String nodeId;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NodeState state = NodeState.STARTING;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "max_jobs")
    @Builder.Default
    private Integer maxJobs = 10;

    @Column(name = "current_jobs")
    @Builder.Default
    private Integer currentJobs = 0;

    /**
     * Check if this node is considered active (has recent heartbeat).
     */
    public boolean isActive(Instant threshold) {
        return lastHeartbeat != null && lastHeartbeat.isAfter(threshold);
    }

    /**
     * Check if node can accept more jobs.
     */
    public boolean canAcceptJobs() {
        return state == NodeState.ACTIVE && currentJobs < maxJobs;
    }

    /**
     * Update heartbeat timestamp.
     */
    public void heartbeat() {
        this.lastHeartbeat = Instant.now();
    }

    /**
     * Increment current job count.
     */
    public void incrementJobs() {
        this.currentJobs++;
    }

    /**
     * Decrement current job count.
     */
    public void decrementJobs() {
        this.currentJobs = Math.max(0, this.currentJobs - 1);
    }

    /**
     * Transition to a new state.
     */
    public void transitionTo(NodeState newState) {
        this.state = newState;
    }
}