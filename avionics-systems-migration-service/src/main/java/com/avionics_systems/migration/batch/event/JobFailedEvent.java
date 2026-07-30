package com.avionics_systems.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a job fails.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class JobFailedEvent implements MigrationEvent {

    private final String jobId;
    private final String error;
    private final String errorCode;
    private final Instant timestamp;
    private final String stackTrace;

    @Builder
    public JobFailedEvent(String jobId, String error, String errorCode,
                         Instant timestamp, String stackTrace) {
        this.jobId = jobId;
        this.error = error;
        this.errorCode = errorCode != null ? errorCode : "UNKNOWN";
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.stackTrace = stackTrace;
    }

    @Override
    public String getEventType() {
        return "JOB_FAILED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "error", error,
                "errorCode", errorCode,
                "hasStackTrace", stackTrace != null
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.ERROR;
    }
}