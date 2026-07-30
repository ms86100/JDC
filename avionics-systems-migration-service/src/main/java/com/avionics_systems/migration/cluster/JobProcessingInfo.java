package com.avionics_systems.migration.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Information about job processing status across the cluster.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProcessingInfo {

    /**
     * Job identifier.
     */
    private String jobId;

    /**
     * Whether the job is currently in progress.
     */
    private boolean inProgress;

    /**
     * Node currently processing the job.
     */
    private String processingNode;

    /**
     * Current progress percentage.
     */
    private double progressPercentage;

    /**
     * When the last update occurred.
     */
    private Instant lastUpdate;

    /**
     * Job type.
     */
    private String jobType;

    /**
     * When the job was started.
     */
    private Instant startedAt;

    /**
     * Estimated time remaining in seconds.
     */
    private Long estimatedRemainingSeconds;

    /**
     * Check if the processing is stale (no update for a while).
     */
    public boolean isStale(Instant threshold) {
        return lastUpdate != null && lastUpdate.isBefore(threshold);
    }
}