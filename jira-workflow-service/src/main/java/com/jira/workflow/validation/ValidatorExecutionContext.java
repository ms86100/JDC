package com.jira.workflow.validation;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Context passed to validators during workflow transition validation.
 * Contains all the information a validator needs to evaluate its conditions.
 */
@Data
@Builder
public class ValidatorExecutionContext {

    /**
     * Current user performing the transition.
     */
    private UUID currentUserId;

    /**
     * Map of field name to value for the issue being transitioned.
     * Includes: summary, description, priority, priorityId, assigneeId, reporterId,
     * fixVersion, components, labels, custom fields, etc.
     */
    @Builder.Default
    private Map<String, Object> issueFields = Map.of();

    /**
     * ID of the transition being attempted.
     */
    private UUID transitionId;

    /**
     * ID of the issue being transitioned.
     */
    private UUID issueId;

    /**
     * ID of the project the issue belongs to.
     */
    private UUID projectId;

    /**
     * Optional comment provided with the transition.
     */
    private Optional<String> comment;

    /**
     * List of attachment metadata for the issue.
     */
    @Builder.Default
    private List<Attachment> attachments = List.of();

    /**
     * Map of link type to linked issue (e.g., "blocks" -> issue, "is blocked by" -> issue).
     */
    @Builder.Default
    private Map<String, Issue> linkedIssues = Map.of();

    /**
     * List of subtask issues for the parent issue.
     */
    @Builder.Default
    private List<Issue> subtasks = List.of();

    /**
     * Original estimate for time tracking (in seconds).
     */
    private Long originalEstimate;

    /**
     * Remaining estimate for time tracking (in seconds).
     */
    private Long remainingEstimate;

    /**
     * Gets a field value from issueFields, returning null if not present.
     */
    public Object getFieldValue(String fieldName) {
        return issueFields != null ? issueFields.get(fieldName) : null;
    }

    /**
     * Gets a field value as a String, returning empty Optional if not present.
     */
    public Optional<String> getFieldValueAsString(String fieldName) {
        Object val = getFieldValue(fieldName);
        return val != null ? Optional.of(val.toString()) : Optional.empty();
    }

    /**
     * Gets a field value as UUID, if parseable.
     */
    public Optional<UUID> getFieldValueAsUUID(String fieldName) {
        Object val = getFieldValue(fieldName);
        if (val == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(val.toString()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if a field exists and is not blank.
     */
    public boolean hasFieldValue(String fieldName) {
        Object val = getFieldValue(fieldName);
        if (val == null) {
            return false;
        }
        if (val instanceof String str) {
            return !str.isBlank();
        }
        return true;
    }

    /**
     * Gets the comment as a String, or empty if not present.
     */
    public String getCommentOrEmpty() {
        return comment != null ? comment.orElse("") : "";
    }

    /**
     * Checks if a comment is present and not blank.
     */
    public boolean hasComment() {
        return comment != null && comment.isPresent() && !comment.get().isBlank();
    }

    /**
     * Gets the number of attachments.
     */
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }

    /**
     * Gets the number of subtasks.
     */
    public int getSubtaskCount() {
        return subtasks != null ? subtasks.size() : 0;
    }

    /**
     * Simple attachment representation.
     */
    @Data
    @Builder
    public static class Attachment {
        private UUID id;
        private String filename;
        private String mimeType;
        private Long size;
        private UUID authorId;
    }

    /**
     * Simple issue representation for linked issues and subtasks.
     */
    @Data
    @Builder
    public static class Issue {
        private UUID id;
        private String key;
        private String summary;
        private UUID statusId;
        private String statusName;
        private UUID resolutionId;
        private String resolutionName;
        private UUID assigneeId;
        private UUID reporterId;
    }
}