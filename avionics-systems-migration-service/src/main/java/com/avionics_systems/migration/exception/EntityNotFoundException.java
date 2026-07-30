package com.avionics_systems.migration.exception;

public class EntityNotFoundException extends MigrationException {
    private final String entityType;
    private final String entityKey;

    public EntityNotFoundException(String entityType, String entityKey) {
        super(entityType + " not found: " + entityKey, "ENTITY_NOT_FOUND");
        this.entityType = entityType;
        this.entityKey = entityKey;
    }

    public EntityNotFoundException(String entityType, String entityKey, String additionalInfo) {
        super(entityType + " not found: " + entityKey + ". " + additionalInfo, "ENTITY_NOT_FOUND");
        this.entityType = entityType;
        this.entityKey = entityKey;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityKey() {
        return entityKey;
    }
}