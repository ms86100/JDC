package com.jira.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a job completes successfully.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class JobCompletedEvent implements MigrationEvent {

    private final String jobId;
    private final int successCount;
    private final int failCount;
    private final long durationMs;
    private final Instant timestamp;
    private final Map<String, Object> resultMetadata;

    @Builder
    public JobCompletedEvent(String jobId, int successCount, int failCount,
                             long durationMs, Instant timestamp,
                             Map<String, Object> resultMetadata) {
        this.jobId = jobId;
        this.successCount = successCount;
        this.failCount = failCount;
        this.durationMs = durationMs;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.resultMetadata = resultMetadata != null ? resultMetadata : Map.of();
    }

    @Override
    public String getEventType() {
        return "JOB_COMPLETED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "successCount", successCount,
                "failCount", failCount,
                "durationMs", durationMs,
                "successRate", failCount > 0 ? (double) successCount / (successCount + failCount) : 1.0,
                "resultMetadata", resultMetadata
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.JOB;
    }
}