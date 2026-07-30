package com.avionics_systems.migration.batch.event;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * Event published when an entity is imported.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class EntityImportedEvent implements MigrationEvent {

    private final String jobId;
    private final String entityType;
    private final String entityId;
    private final String entityKey;
    private final String targetId;
    private final Instant timestamp;

    @Builder
    public EntityImportedEvent(String jobId, String entityType, String entityId,
                               String entityKey, String targetId, Instant timestamp) {
        this.jobId = jobId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityKey = entityKey;
        this.targetId = targetId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    @Override
    public String getEventType() {
        return "ENTITY_IMPORTED";
    }

    @Override
    public Map<String, Object> getDetails() {
        return Map.of(
                "jobId", jobId,
                "entityType", entityType,
                "entityId", entityId,
                "entityKey", entityKey,
                "targetId", targetId
        );
    }

    @Override
    public EventCategory getCategory() {
        return EventCategory.ENTITY;
    }
}