package com.jira.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published periodically to report import progress.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class ImportProgressEvent implements MigrationEvent {

    private final String jobId;
    private final int percentage;
    private final int processedCount;
    private final int totalCount;
    private final int successCount;
    private final int errorCount;
    private final long estimatedRemainingMs;
    private final Instant timestamp;

    @Builder
    public ImportProgressEvent(String jobId, int percentage, int processedCount,
                                int totalCount, int successCount, int errorCount,
                                long estimatedRemainingMs, Instant timestamp) {
        this.jobId = jobId;
        this.percentage = percentage;
        this.processedCount = processedCount;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.estimatedRemainingMs = estimatedRemainingMs;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    @Override
    public String getEventType() {
        return "IMPORT_PROGRESS";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "percentage", percentage,
                "processedCount", processedCount,
                "totalCount", totalCount,
                "successCount", successCount,
                "errorCount", errorCount,
                "estimatedRemainingMs", estimatedRemainingMs
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.JOB;
    }
}