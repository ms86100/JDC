package com.avionics_systems.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when a job starts processing.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class JobStartedEvent implements MigrationEvent {

    private final String jobId;
    private final String jobType;
    private final String source;
    private final Instant timestamp;
    private final Map<String, Object> options;

    @Builder
    public JobStartedEvent(String jobId, String jobType, String source,
                          Instant timestamp, Map<String, Object> options) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.source = source;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.options = options != null ? options : Map.of();
    }

    @Override
    public String getEventType() {
        return "JOB_STARTED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "jobType", jobType,
                "source", source,
                "options", options
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.JOB;
    }
}