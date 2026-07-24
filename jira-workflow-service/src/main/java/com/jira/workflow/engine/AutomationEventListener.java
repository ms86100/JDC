package com.jira.workflow.engine;

import com.jira.workflow.service.AutomationRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener that bridges workflow/issue events into the automation rule engine.
 * Called by WorkflowEventOutboxProcessor after transitions complete, and by
 * issue-service callbacks when issues are created/updated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutomationEventListener {

    private final AutomationRuleService ruleService;

    /**
     * Called after a workflow transition completes (by WorkflowEventOutboxProcessor).
     * Fires STATUS_CHANGED automation rules.
     *
     * @param issueId    the issue that transitioned
     * @param projectId  the project the issue belongs to
     * @param fromStatus the status before transition
     * @param toStatus   the status after transition
     * @param issueData  current issue field data
     */
    public void onTransitionCompleted(UUID issueId, UUID projectId,
                                       String fromStatus, String toStatus,
                                       Map<String, Object> issueData) {
        log.info("Automation event: STATUS_CHANGED for issue={}, from={} to={}", issueId, fromStatus, toStatus);
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("fromStatus", fromStatus);
            eventData.put("toStatus", toStatus);
            if (issueData != null) {
                eventData.put("issueData", issueData);
                eventData.putAll(issueData);
            }
            ruleService.evaluateRules("STATUS_CHANGED", issueId, projectId, eventData);
        } catch (Exception e) {
            log.error("Failed to evaluate automation rules for STATUS_CHANGED on issue {}: {}",
                    issueId, e.getMessage(), e);
        }
    }

    /**
     * Called when an issue is created (via webhook or REST callback from issue-service).
     * Fires ISSUE_CREATED automation rules.
     *
     * @param issueId   the newly created issue
     * @param projectId the project the issue belongs to
     * @param issueData the issue field data
     */
    public void onIssueCreated(UUID issueId, UUID projectId, Map<String, Object> issueData) {
        log.info("Automation event: ISSUE_CREATED for issue={}", issueId);
        try {
            Map<String, Object> eventData = new HashMap<>();
            if (issueData != null) {
                eventData.putAll(issueData);
            }
            ruleService.evaluateRules("ISSUE_CREATED", issueId, projectId, eventData);
        } catch (Exception e) {
            log.error("Failed to evaluate automation rules for ISSUE_CREATED on issue {}: {}",
                    issueId, e.getMessage(), e);
        }
    }

    /**
     * Called when an issue is updated (via webhook or REST callback from issue-service).
     * Fires ISSUE_UPDATED rules and FIELD_CHANGED rules for each changed field.
     *
     * @param issueId       the updated issue
     * @param projectId     the project the issue belongs to
     * @param changedFields map of field names to their new values
     */
    public void onIssueUpdated(UUID issueId, UUID projectId, Map<String, Object> changedFields) {
        log.info("Automation event: ISSUE_UPDATED for issue={}, fields={}", issueId,
                changedFields != null ? changedFields.keySet() : "none");
        try {
            // Fire ISSUE_UPDATED rules with all changed fields
            ruleService.evaluateRules("ISSUE_UPDATED", issueId, projectId, changedFields);

            // Fire FIELD_CHANGED rules for each individual field
            if (changedFields != null) {
                for (Map.Entry<String, Object> entry : changedFields.entrySet()) {
                    Map<String, Object> fieldEventData = new HashMap<>();
                    fieldEventData.put("fieldName", entry.getKey());
                    fieldEventData.put("newValue", entry.getValue());
                    ruleService.evaluateRules("FIELD_CHANGED", issueId, projectId, fieldEventData);
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate automation rules for ISSUE_UPDATED on issue {}: {}",
                    issueId, e.getMessage(), e);
        }
    }

    /**
     * Called when a comment is added to an issue.
     * Fires COMMENT_ADDED automation rules.
     *
     * @param issueId     the issue the comment was added to
     * @param projectId   the project the issue belongs to
     * @param commentData the comment data (content, author, etc.)
     */
    public void onCommentAdded(UUID issueId, UUID projectId, Map<String, Object> commentData) {
        log.info("Automation event: COMMENT_ADDED for issue={}", issueId);
        try {
            Map<String, Object> eventData = new HashMap<>();
            if (commentData != null) {
                eventData.putAll(commentData);
            }
            ruleService.evaluateRules("COMMENT_ADDED", issueId, projectId, eventData);
        } catch (Exception e) {
            log.error("Failed to evaluate automation rules for COMMENT_ADDED on issue {}: {}",
                    issueId, e.getMessage(), e);
        }
    }
}
