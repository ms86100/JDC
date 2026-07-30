package com.avionics_systems.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Health status of the cluster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterHealth {

    /**
     * Overall health status.
     */
    private HealthStatus status;

    /**
     * Number of active nodes.
     */
    private int activeNodes;

    /**
     * Total number of nodes.
     */
    private int totalNodes;

    /**
     * List of unhealthy node IDs.
     */
    private List<String> unhealthyNodes;

    /**
     * List of warning messages.
     */
    private List<String> warnings;

    /**
     * When the health check was performed.
     */
    private long timestamp;

    /**
     * Cluster availability percentage.
     */
    private double availabilityPercentage;

    /**
     * Health status enumeration.
     */
    public enum HealthStatus {
        /**
         * All nodes are healthy.
         */
        HEALTHY,

        /**
         * Some nodes are degraded but cluster is operational.
         */
        DEGRADED,

        /**
         * Cluster is not operational.
         */
        UNHEALTHY,

        /**
         * Health check failed or is unknown.
         */
        UNKNOWN
    }

    /**
     * Check if operations can proceed.
     */
    public boolean canProceedWithOperations() {
        return status == HealthStatus.HEALTHY || status == HealthStatus.DEGRADED;
    }

    /**
     * Get health summary.
     */
    public String getSummary() {
        return String.format("%s: %d/%d nodes active, %d warnings",
                status, activeNodes, totalNodes,
                warnings != null ? warnings.size() : 0);
    }
}