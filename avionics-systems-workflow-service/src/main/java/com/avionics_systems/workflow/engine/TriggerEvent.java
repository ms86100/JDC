package com.avionics_systems.workflow.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event object representing a trigger event that can initiate workflow transitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerEvent {

    private UUID eventId;

    private String eventType;

    private UUID issueId;

    private UUID triggerType;

    private Object previousValue;

    private Object newValue;

    private LocalDateTime timestamp;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // Event type constants
    public static final String TYPE_ISSUE_UPDATED = "ISSUE_UPDATED";
    public static final String TYPE_FIELD_CHANGED = "FIELD_CHANGED";
    public static final String TYPE_COMMENT_ADDED = "COMMENT_ADDED";
    public static final String TYPE_ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String TYPE_LINK_CREATED = "LINK_CREATED";
    public static final String TYPE_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String TYPE_DATE_REACHED = "DATE_REACHED";
    public static final String TYPE_API_CALL = "API_CALL";
    public static final String TYPE_SPRINT_STARTED = "SPRINT_STARTED";
    public static final String TYPE_SPRINT_COMPLETED = "SPRINT_COMPLETED";
    public static final String TYPE_BUILD_SUCCESS = "BUILD_SUCCESS";
    public static final String TYPE_BUILD_FAILED = "BUILD_FAILED";
    public static final String TYPE_PULL_REQUEST_MERGED = "PULL_REQUEST_MERGED";
    public static final String TYPE_DEPLOYMENT_SUCCESS = "DEPLOYMENT_SUCCESS";

    // Metadata keys
    public static final String META_FIELD_NAME = "fieldName";
    public static final String META_USER_ID = "userId";
    public static final String META_COMMENT_TEXT = "commentText";
    public static final String META_ATTACHMENT_NAME = "attachmentName";
    public static final String META_LINK_TYPE = "linkType";
    public static final String META_LINKED_ISSUE_ID = "linkedIssueId";
    public static final String META_SPRINT_ID = "sprintId";
    public static final String META_BUILD_ID = "buildId";
    public static final String META_BUILD_NAME = "buildName";
    public static final String META_PR_NUMBER = "pullRequestNumber";
    public static final String META_REPOSITORY = "repository";
    public static final String META_DEPLOYMENT_ENV = "deploymentEnvironment";
    public static final String META_WEBHOOK_SOURCE = "webhookSource";

    /**
     * Create an event with a new UUID and current timestamp.
     */
    public static TriggerEvent create(String eventType, UUID issueId) {
        return TriggerEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .issueId(issueId)
                .timestamp(LocalDateTime.now())
                .metadata(new HashMap<>())
                .build();
    }

    /**
     * Add a metadata entry.
     */
    public TriggerEvent withMeta(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Get a metadata value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key) {
        if (metadata == null) return null;
        return (T) metadata.get(key);
    }

    /**
     * Get metadata with a default value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key, T defaultValue) {
        if (metadata == null) return defaultValue;
        return (T) metadata.getOrDefault(key, defaultValue);
    }
}