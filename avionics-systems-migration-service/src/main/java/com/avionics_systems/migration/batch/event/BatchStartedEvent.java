package com.avionics_systems.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a batch starts processing.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class BatchStartedEvent implements MigrationEvent {

    private final String jobId;
    private final int batchNumber;
    private final int totalBatches;
    private final int batchSize;
    private final String entityType;
    private final Instant timestamp;

    @Builder
    public BatchStartedEvent(String jobId, int batchNumber, int totalBatches,
                            int batchSize, String entityType, Instant timestamp) {
        this.jobId = jobId;
        this.batchNumber = batchNumber;
        this.totalBatches = totalBatches;
        this.batchSize = batchSize;
        this.entityType = entityType;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    @Override
    public String getEventType() {
        return "BATCH_STARTED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "batchNumber", batchNumber,
                "totalBatches", totalBatches,
                "batchSize", batchSize,
                "entityType", entityType
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.BATCH;
    }
}