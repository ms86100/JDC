package com.avionics_systems.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a batch completes processing.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class BatchCompletedEvent implements MigrationEvent {

    private final String jobId;
    private final int batchNumber;
    private final int successCount;
    private final int errorCount;
    private final long durationMs;
    private final Instant timestamp;

    @Builder
    public BatchCompletedEvent(String jobId, int batchNumber, int successCount,
                               int errorCount, long durationMs, Instant timestamp) {
        this.jobId = jobId;
        this.batchNumber = batchNumber;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.durationMs = durationMs;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    @Override
    public String getEventType() {
        return "BATCH_COMPLETED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "batchNumber", batchNumber,
                "successCount", successCount,
                "errorCount", errorCount,
                "durationMs", durationMs
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.BATCH;
    }
}