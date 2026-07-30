package com.avionics_systems.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Statistics about the cluster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterStatistics {

    /**
     * Number of active jobs in the cluster.
     */
    private int activeJobs;

    /**
     * Number of queued jobs.
     */
    private int queuedJobs;

    /**
     * Number of completed jobs today.
     */
    private int completedJobsToday;

    /**
     * Jobs categorized by type.
     */
    private Map<String, Integer> jobsByType;

    /**
     * Nodes categorized by state.
     */
    private Map<String, Integer> nodesByState;

    /**
     * Total locks held across the cluster.
     */
    private int totalLocksHeld;

    /**
     * Total leaders active.
     */
    private int activeLeaders;

    /**
     * Average job processing time in milliseconds.
     */
    private long averageProcessingTimeMs;

    /**
     * Total data processed in bytes.
     */
    private long totalDataProcessedBytes;

    /**
     * Timestamp of the statistics.
     */
    private long timestamp;
}