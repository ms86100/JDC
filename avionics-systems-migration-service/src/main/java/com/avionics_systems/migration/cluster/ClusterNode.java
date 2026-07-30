package com.avionics_systems.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a node in the cluster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNode {

    /**
     * Unique identifier for the node.
     */
    private String nodeId;

    /**
     * Host address of the node.
     */
    private String host;

    /**
     * Port for inter-node communication.
     */
    private int port;

    /**
     * Current state of the node.
     */
    private String state;

    /**
     * When the node was registered.
     */
    private Instant registeredAt;

    /**
     * When the last heartbeat was received.
     */
    private Instant lastHeartbeat;

    /**
     * Additional metadata about the node.
     */
    private Map<String, Object> metadata;

    /**
     * Current number of jobs running on this node.
     */
    private int currentJobs;

    /**
     * Maximum number of jobs this node can handle.
     */
    private int maxJobs;

    /**
     * Version for optimistic locking.
     */
    private int version;

    /**
     * Check if this node is active.
     */
    public boolean isActive() {
        return "ACTIVE".equals(state);
    }

    /**
     * Check if this node can accept more jobs.
     */
    public boolean canAcceptJobs() {
        return isActive() && currentJobs < maxJobs;
    }

    /**
     * Check if the node heartbeat is stale.
     */
    public boolean isHeartbeatStale(Instant threshold) {
        return lastHeartbeat == null || lastHeartbeat.isBefore(threshold);
    }
}