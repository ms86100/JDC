package com.jira.migration.batch.event;

import java.time.Instant;
import java.util.Map;

/**
 * Base interface for all migration events.
 * Events are published to track job progress and enable monitoring.
 */
public interface MigrationEvent {

    /**
     * Get the type of event.
     */
    String getEventType();

    /**
     * Get the job ID this event belongs to.
     */
    String getJobId();

    /**
     * Get the timestamp when this event occurred.
     */
    Instant getTimestamp();

    /**
     * Get additional event details as a map.
     */
    Map<String, Object> getDetails();

    /**
     * Get event category for filtering.
     */
    default EventCategory getCategory() {
        return EventCategory.GENERAL;
    }
}