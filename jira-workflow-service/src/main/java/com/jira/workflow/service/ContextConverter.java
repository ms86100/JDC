package com.jira.workflow.service;

import com.jira.workflow.engine.WorkflowContext;
import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * Utility to convert between WorkflowContext and ConditionEvaluationContext.
 * Used to bridge the new condition evaluation service with the existing
 * transition execution flow.
 */
@UtilityClass
public class ContextConverter {

    /**
     * Convert WorkflowContext to ConditionEvaluationContext.
     * This allows the new WorkflowConditionEvaluationService to work
     * with the existing workflow execution context.
     */
    @SuppressWarnings("unchecked")
    public static ConditionEvaluationContext toEvaluationContext(WorkflowContext ctx) {
        if (ctx == null) {
            return ConditionEvaluationContext.builder().build();
        }

        ConditionEvaluationContext.ConditionEvaluationContextBuilder builder = ConditionEvaluationContext.builder()
                .currentUserId(ctx.getUserId())
                .transitionId(ctx.getTransition() != null ? ctx.getTransition().getId() : null)
                .issue(ctx.getIssueData())
                .issueFields(ctx.getIssueData())
                .screenInput(ctx.getScreenInput());

        // Extract user groups from userData
        if (ctx.getUserData() != null) {
            Object groups = ctx.getUserData().get("groups");
            if (groups instanceof Set<?>) {
                Set<String> groupSet = new HashSet<>();
                ((Set<?>) groups).forEach(g -> groupSet.add(g.toString()));
                builder.currentUserGroups(groupSet);
            } else if (groups instanceof List<?>) {
                Set<String> groupSet = new HashSet<>();
                ((List<?>) groups).forEach(g -> groupSet.add(g.toString()));
                builder.currentUserGroups(groupSet);
            }
        }

        // Extract status IDs from issue data
        if (ctx.getIssueData() != null) {
            Object previousStatus = ctx.getIssueData().get("previousStatusId");
            if (previousStatus != null) {
                builder.previousStatusId(parseUuid(previousStatus));
            }
            if (ctx.getCurrentStatusId() != null) {
                builder.currentStatusId(ctx.getCurrentStatusId());
            }

            // Extract reporter and assignee
            Object reporterId = ctx.getIssueData().get("reporterId");
            if (reporterId != null) {
                builder.reporterId(parseUuid(reporterId));
            }
            Object assigneeId = ctx.getIssueData().get("assigneeId");
            if (assigneeId != null) {
                builder.assigneeId(parseUuid(assigneeId));
            } else {
                Object assignee = ctx.getIssueData().get("assignee");
                if (assignee != null) {
                    builder.assigneeId(parseUuid(assignee));
                }
            }
        }

        builder.projectId(ctx.getProjectId());

        // Extract issue ID directly from WorkflowContext if available
        if (ctx.getIssueId() != null) {
            builder.issueId(ctx.getIssueId());
        } else if (ctx.getIssueData() != null) {
            Object id = ctx.getIssueData().get("id");
            if (id != null) {
                builder.issueId(parseUuid(id));
            }
        }

        return builder.build();
    }

    /**
     * Convert a Map-based context (as used by external services) to ConditionEvaluationContext.
     * This is a convenience method for API callers.
     */
    public static ConditionEvaluationContext fromMap(Map<String, Object> contextMap) {
        return ConditionEvaluationContext.fromMap(contextMap);
    }

    /**
     * Convert ConditionEvaluationContext to a Map.
     * Useful for logging or serialization.
     */
    public static Map<String, Object> toMap(ConditionEvaluationContext ctx) {
        if (ctx == null) {
            return Map.of();
        }

        Map<String, Object> map = new HashMap<>();

        if (ctx.getCurrentUserId() != null) {
            map.put("userId", ctx.getCurrentUserId().toString());
        }
        if (ctx.getCurrentUserGroups() != null) {
            map.put("userGroups", ctx.getCurrentUserGroups());
        }
        if (ctx.getIssueId() != null) {
            map.put("issueId", ctx.getIssueId().toString());
        }
        if (ctx.getProjectId() != null) {
            map.put("projectId", ctx.getProjectId().toString());
        }
        if (ctx.getIssueFields() != null) {
            map.put("fields", ctx.getIssueFields());
        }
        if (ctx.getPreviousStatusId() != null) {
            map.put("previousStatusId", ctx.getPreviousStatusId().toString());
        }
        if (ctx.getCurrentStatusId() != null) {
            map.put("currentStatusId", ctx.getCurrentStatusId().toString());
        }
        if (ctx.getTransitionId() != null) {
            map.put("transitionId", ctx.getTransitionId().toString());
        }
        if (ctx.getScreenInput() != null) {
            map.put("screenInput", ctx.getScreenInput());
        }

        return map;
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