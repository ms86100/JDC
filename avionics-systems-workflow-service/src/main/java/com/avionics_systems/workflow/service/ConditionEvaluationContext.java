package com.avionics_systems.workflow.service;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Context object for condition evaluation.
 * Contains all information needed to evaluate workflow conditions.
 * Matches Avionics Systems DC's ISecurityType and related infrastructure.
 */
@Data
@Builder
public class ConditionEvaluationContext {

    /**
     * Current user performing the transition
     */
    private UUID currentUserId;

    /**
     * Groups the current user belongs to
     */
    private Set<String> currentUserGroups;

    /**
     * Issue being transitioned (full issue data as map)
     */
    private Map<String, Object> issue;

    /**
     * Individual issue fields for quick access
     */
    private Map<String, Object> issueFields;

    /**
     * Previous status ID before the transition
     */
    private UUID previousStatusId;

    /**
     * Current status ID (may be same as previousStatusId in some flows)
     */
    private UUID currentStatusId;

    /**
     * The transition being attempted
     */
    private UUID transitionId;

    /**
     * Optional permission checker function.
     * Signature: (userId, permission) -> boolean
     * Can be used to override default permission checking behavior.
     */
    @Builder.Default
    private BiFunction<UUID, String, Boolean> permissionChecker = null;

    /**
     * Reporter of the issue (extracted from issue)
     */
    private UUID reporterId;

    /**
     * Assignee of the issue (extracted from issue)
     */
    private UUID assigneeId;

    /**
     * Project ID for permission checks
     */
    private UUID projectId;

    /**
     * Direct issue ID if available (may be extracted from issue map)
     */
    private UUID issueId;

    /**
     * Screen input values (fields being changed in this transition)
     */
    private Map<String, Object> screenInput;

    /**
     * Factory method to create context from a map (for external callers)
     */
    public static ConditionEvaluationContext fromMap(Map<String, Object> contextMap) {
        ConditionEvaluationContext.ConditionEvaluationContextBuilder builder = builder();

        // Extract user info
        if (contextMap.containsKey("userId")) {
            builder.currentUserId(parseUuid(contextMap.get("userId")));
        }
        if (contextMap.containsKey("userGroups")) {
            @SuppressWarnings("unchecked")
            Set<String> groups = (Set<String>) contextMap.get("userGroups");
            builder.currentUserGroups(groups);
        }

        // Extract issue info
        if (contextMap.containsKey("issueId")) {
            // Issue ID as UUID
        }
        if (contextMap.containsKey("issue")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> issueData = (Map<String, Object>) contextMap.get("issue");
            builder.issue(issueData);
            builder.issueFields(issueData);
            extractIssueUsers(builder, issueData);
        } else if (contextMap.containsKey("fields")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) contextMap.get("fields");
            builder.issueFields(fields);
            builder.issue(fields);
        }

        // Extract status info
        if (contextMap.containsKey("previousStatusId")) {
            builder.previousStatusId(parseUuid(contextMap.get("previousStatusId")));
        }
        if (contextMap.containsKey("currentStatusId")) {
            builder.currentStatusId(parseUuid(contextMap.get("currentStatusId")));
        }

        // Extract transition ID
        if (contextMap.containsKey("transitionId")) {
            builder.transitionId(parseUuid(contextMap.get("transitionId")));
        }

        // Extract project ID
        if (contextMap.containsKey("projectId")) {
            builder.projectId(parseUuid(contextMap.get("projectId")));
        }

        // Extract screen input
        if (contextMap.containsKey("screenInput")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> screenInput = (Map<String, Object>) contextMap.get("screenInput");
            builder.screenInput(screenInput);
        }

        return builder.build();
    }

    private static void extractIssueUsers(ConditionEvaluationContextBuilder builder, Map<String, Object> issueData) {
        if (issueData.containsKey("reporterId")) {
            builder.reporterId(parseUuid(issueData.get("reporterId")));
        }
        if (issueData.containsKey("assigneeId")) {
            builder.assigneeId(parseUuid(issueData.get("assigneeId")));
        }
        if (issueData.containsKey("assignee")) {
            builder.assigneeId(parseUuid(issueData.get("assignee")));
        }
    }

    /**
     * Helper method to extract a field value from issue fields
     */
    public Object getFieldValue(String fieldName) {
        if (issueFields != null && issueFields.containsKey(fieldName)) {
            return issueFields.get(fieldName);
        }
        if (issue != null && issue.containsKey(fieldName)) {
            return issue.get(fieldName);
        }
        return null;
    }

    /**
     * Check if current user is in a specific group
     */
    public boolean isInGroup(String groupName) {
        if (currentUserGroups == null || groupName == null) {
            return false;
        }
        return currentUserGroups.stream()
                .anyMatch(g -> g.equalsIgnoreCase(groupName));
    }

    /**
     * Check if current user is the reporter
     */
    public boolean isReporter() {
        return currentUserId != null && currentUserId.equals(reporterId);
    }

    /**
     * Check if current user is the assignee
     */
    public boolean isAssignee() {
        return currentUserId != null && currentUserId.equals(assigneeId);
    }

    private static UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}