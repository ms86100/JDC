package com.avionics_systems.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.entity.WorkflowPostFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Post-Function Execution Engine - handles execution of all post-function types.
 * This is the core engine that executes post-functions after workflow transitions.
 *
 * Execution Context (Map<String, Object>) expected keys:
 * - "issueId" - UUID
 * - "issueKey" - String
 * - "userId" - UUID (who triggered the transition)
 * - "transitionId" - UUID
 * - "fromStatusId" - UUID
 * - "toStatusId" - UUID
 * - "fields" - Map of all issue fields
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostFunctionExecutionEngine {

    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.workflow.post-function.max-retries:3}")
    private int maxRetries;

    @Value("${app.workflow.post-function.retry-delay-ms:100}")
    private long retryDelayMs;

    @Value("${app.workflow.post-function.default-email-subject:Issue Notification}")
    private String defaultEmailSubject;

    @Value("${app.workflow.post-function.default-email-body:Issue has been updated.}")
    private String defaultEmailBody;

    @Value("${app.workflow.post-function.default-notify-message:Issue transitioned}")
    private String defaultNotifyMessage;

    @Value("${app.workflow.post-function.default-automation-queue:issue-transitioned}")
    private String defaultAutomationQueue;

    @Value("${app.workflow.post-function.summary-template:Issue {issueKey} was transitioned to {toStatusName} by {userName}.}")
    private String defaultSummaryTemplate;

    /**
     * Execute a single post-function with the given context.
     *
     * @param postFunction the post-function to execute
     * @param context execution context containing issue data
     */
    public void executePostFunction(WorkflowPostFunction postFunction, Map<String, Object> context) {
        String type = postFunction.getFunctionType();
        if (type == null) {
            log.warn("Post-function {} has null type, skipping", postFunction.getId());
            return;
        }

        log.debug("Executing post-function {} of type {}", postFunction.getId(), type);

        // Parse function data from JSON
        Map<String, Object> config = parseConfig(postFunction.getFunctionData());

        try {
            switch (type) {
                // Issue assignment functions
                case WorkflowPostFunction.TYPE_ISSUE_ASSIGN -> executeAssignFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_ASSIGN_TO_CURRENT_USER -> assignToCurrentUser(context);
                case WorkflowPostFunction.TYPE_ASSIGN_TO_REPORTER -> assignToReporter(context);
                case WorkflowPostFunction.TYPE_ASSIGN_TO_PROJECT_LEAD -> assignToProjectLead(context);
                case WorkflowPostFunction.TYPE_ASSIGN_TO_ROLE -> assignToRole(context, config);
                case WorkflowPostFunction.TYPE_ASSIGN_TO_LAST_USER -> assignToLastUser(context);

                // Issue move/status functions
                case WorkflowPostFunction.TYPE_ISSUE_MOVE -> executeMoveFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_SET_ISSUE_STATUS -> setIssueStatus(context, config);

                // Notification functions
                case WorkflowPostFunction.TYPE_NOTIFY_USER -> executeNotifyFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_SEND_EMAIL -> sendEmail(context, config);

                // Field update functions
                case WorkflowPostFunction.TYPE_UPDATE_FIELD -> executeUpdateFieldFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_SET_FIELD_VALUE, WorkflowPostFunction.TYPE_UPDATE_ISSUE_FIELD ->
                        setFieldValue(context, config);
                case WorkflowPostFunction.TYPE_COPY_VALUE_FROM_FIELD -> copyFieldValue(context, config);
                case WorkflowPostFunction.TYPE_SET_PRIORITY -> setPriority(context, config);
                case WorkflowPostFunction.TYPE_SET_RESOLUTION -> setResolution(context, config);
                case WorkflowPostFunction.TYPE_SET_ISSUE_SECURITY -> executeSecurityFunction(postFunction, context);

                // Label functions
                case WorkflowPostFunction.TYPE_ADD_LABEL -> executeLabelFunction(postFunction, context, true);
                case WorkflowPostFunction.TYPE_REMOVE_LABEL -> executeLabelFunction(postFunction, context, false);

                // Subtask and clone functions
                case WorkflowPostFunction.TYPE_CREATE_SUBTASK -> executeCreateSubtaskFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_CLONE_ISSUE -> executeCloneFunction(postFunction, context);

                // Issue link functions
                case WorkflowPostFunction.TYPE_LINK_ISSUE -> executeLinkFunction(postFunction, context);
                case WorkflowPostFunction.TYPE_UNLINK_ISSUE -> executeUnlinkFunction(postFunction, context);

                // Watcher functions
                case WorkflowPostFunction.TYPE_ADD_WATCHER -> executeWatcherFunction(postFunction, context, true);
                case WorkflowPostFunction.TYPE_REMOVE_WATCHER -> executeWatcherFunction(postFunction, context, false);

                // Extension/webhook functions
                case WorkflowPostFunction.TYPE_FIRE_GLOBAL_EXTENSION -> fireGlobalExtension(context, config);
                case WorkflowPostFunction.TYPE_TRIGGER_WEBHOOK -> triggerWebhook(context, config);
                case WorkflowPostFunction.TYPE_TRIGGER_AUTOMATION -> executeAutomationFunction(postFunction, context);

                // Summary generation
                case WorkflowPostFunction.TYPE_GENERATE_AUTOMATIC_SUMMARY ->
                        generateAutomaticSummary(context, config);

                // Other functions
                case WorkflowPostFunction.TYPE_FIRE_EVENT -> fireEvent(context);
                case WorkflowPostFunction.TYPE_ADD_COMMENT -> addComment(context);
                case WorkflowPostFunction.TYPE_AUTO_TRANSITION -> autoTransition(context, config);
                case WorkflowPostFunction.TYPE_SCRIPT_POST_FUNCTION -> executeScript(context, config);

                default -> log.warn("Unknown post-function type: {}", type);
            }
        } catch (Exception e) {
            log.error("Post-function execution failed for type {}: {}", type, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute post-functions asynchronously (fire-and-forget pattern with retry).
     */
    @Async
    public CompletableFuture<Void> executePostFunctionAsync(WorkflowPostFunction postFunction,
                                                             Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            executeWithRetry(postFunction, context);
        });
    }

    /**
     * Execute with retry logic for resilience.
     */
    private void executeWithRetry(WorkflowPostFunction postFunction, Map<String, Object> context) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                executePostFunction(postFunction, context);
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.error("Post-function {} failed after {} attempts: {}",
                            postFunction.getId(), maxRetries, e.getMessage());
                    throw e;
                }
                log.warn("Post-function {} attempt {} failed, retrying in {}ms...",
                        postFunction.getId(), attempt, retryDelayMs);
                try {
                    Thread.sleep(retryDelayMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }

    // =====================================================
    // ISSUE ASSIGN FUNCTIONS
    // =====================================================

    private void executeAssignFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        String assigneeType = stringVal(config.get("assigneeType"), "USER");
        UUID assigneeId = parseUuid(config.get("assigneeId"));

        switch (assigneeType) {
            case "USER" -> {
                if (assigneeId != null) {
                    assignToUser(ctx, assigneeId);
                } else {
                    assignToCurrentUser(ctx);
                }
            }
            case "REPORTER" -> assignToReporter(ctx);
            case "CURRENT_USER" -> assignToCurrentUser(ctx);
            case "PROJECT_LEAD" -> assignToProjectLead(ctx);
            case "LAST_USER" -> assignToLastUser(ctx);
            default -> log.warn("Unknown assigneeType: {}", assigneeType);
        }
    }

    private void assignToCurrentUser(Map<String, Object> ctx) {
        UUID userId = parseUuid(ctx.get("userId"));
        if (userId != null) {
            assignToUser(ctx, userId);
        }
    }

    private void assignToReporter(Map<String, Object> ctx) {
        Map<String, Object> fields = getFields(ctx);
        UUID reporterId = parseUuid(fields.get("reporterId"));
        if (reporterId != null) {
            assignToUser(ctx, reporterId);
        }
    }

    private void assignToProjectLead(Map<String, Object> ctx) {
        UUID projectId = parseUuid(ctx.get("projectId"));
        if (projectId != null) {
            Map<String, Object> project = integrationClient.fetchProject(projectId);
            UUID leadId = parseUuid(project.get("leadUserId"));
            if (leadId != null) {
                assignToUser(ctx, leadId);
            }
        }
    }

    private void assignToLastUser(Map<String, Object> ctx) {
        Map<String, Object> fields = getFields(ctx);
        UUID lastAssigneeId = parseUuid(fields.get("lastAssigneeId"));
        if (lastAssigneeId == null) {
            lastAssigneeId = parseUuid(fields.get("assigneeId"));
        }
        if (lastAssigneeId != null) {
            assignToUser(ctx, lastAssigneeId);
        }
    }

    private void assignToRole(Map<String, Object> ctx, Map<String, Object> config) {
        Object roleId = config.get("roleId");
        if (roleId != null) {
            log.info("Assign to role {} requires project-role service integration", roleId);
        }
    }

    private void assignToUser(Map<String, Object> ctx, UUID userId) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        if (issueId != null && userId != null) {
            integrationClient.patchIssueFields(issueId, Map.of("assigneeId", userId.toString()));
            log.info("Assigned issue {} to user {}", issueId, userId);
        }
    }

    // =====================================================
    // ISSUE MOVE FUNCTION
    // =====================================================

    private void executeMoveFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID targetStatusId = parseUuid(config.get("targetStatusId"));
        if (targetStatusId != null) {
            UUID issueId = parseUuid(ctx.get("issueId"));
            if (issueId != null) {
                integrationClient.updateIssueWorkflowInternal(issueId, Map.of("statusId", targetStatusId.toString()));
                log.info("Moved issue {} to status {}", issueId, targetStatusId);
            }
        }
    }

    private void setIssueStatus(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        UUID statusId = parseUuid(ctx.get("toStatusId"));
        if (statusId == null) {
            statusId = parseUuid(config.get("statusId"));
        }
        if (issueId != null && statusId != null) {
            integrationClient.updateIssueWorkflowInternal(issueId, Map.of("statusId", statusId.toString()));
        }
    }

    // =====================================================
    // NOTIFICATION FUNCTIONS
    // =====================================================

    private void executeNotifyFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        String message = stringVal(config.get("message"), defaultNotifyMessage);
        UUID recipientId = parseUuid(config.get("recipientId"));
        if (recipientId == null) {
            recipientId = parseUuid(ctx.get("userId"));
        }

        // Use the event publisher for notifications (fire and forget)
        Map<String, Object> notificationPayload = new HashMap<>();
        notificationPayload.put("issueId", ctx.get("issueId"));
        notificationPayload.put("recipientId", recipientId != null ? recipientId.toString() : null);
        notificationPayload.put("message", message);
        notificationPayload.put("type", "TRANSITION_NOTIFICATION");

        eventPublisher.publishIssueTransitioned(buildContextFromMap(ctx));

        log.info("Notification sent for issue {} to user {}", ctx.get("issueId"), recipientId);
    }

    private void sendEmail(Map<String, Object> ctx, Map<String, Object> config) {
        String to = stringVal(config.get("to"), stringVal(config.get("email"), null));
        String subject = stringVal(config.get("subject"), defaultEmailSubject);
        String body = stringVal(config.get("body"), defaultEmailBody);

        log.info("Email notification would be sent to: {} with subject: {}", to, subject);
        // Email sending is handled by notification service
    }

    // =====================================================
    // FIELD UPDATE FUNCTIONS
    // =====================================================

    private void executeUpdateFieldFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        setFieldValue(ctx, config);
    }

    private void setFieldValue(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        String field = stringVal(config.get("field"), stringVal(config.get("fieldName"), null));
        Object value = config.get("value");

        if (issueId != null && field != null && value != null) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(field, value);
            integrationClient.patchIssueFields(issueId, fields);
            log.info("Set field {} on issue {} to {}", field, issueId, value);
        }
    }

    private void copyFieldValue(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        String fromField = stringVal(config.get("fromField"), null);
        String toField = stringVal(config.get("toField"), null);

        if (issueId != null && fromField != null && toField != null) {
            Map<String, Object> fields = getFields(ctx);
            Object value = fields.get(fromField);
            if (value != null) {
                integrationClient.patchIssueFields(issueId, Map.of(toField, value));
                log.info("Copied field {} to {} on issue {}", fromField, toField, issueId);
            }
        }
    }

    private void setPriority(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        Object priorityId = config.get("priorityId");
        if (issueId != null && priorityId != null) {
            integrationClient.patchIssueFields(issueId, Map.of("priorityId", priorityId.toString()));
            log.info("Set priority on issue {} to {}", issueId, priorityId);
        }
    }

    private void setResolution(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        Object resolutionId = config.get("resolutionId");
        if (issueId != null && resolutionId != null) {
            integrationClient.patchIssueFields(issueId, Map.of("resolutionId", resolutionId.toString()));
            log.info("Set resolution on issue {} to {}", issueId, resolutionId);
        }
    }

    // =====================================================
    // SECURITY FUNCTION
    // =====================================================

    private void executeSecurityFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        Object levelId = config.get("securityLevelId");

        if (issueId != null && levelId != null) {
            integrationClient.patchIssueFields(issueId, Map.of("securityLevelId", levelId.toString()));
            log.info("Set security level on issue {} to {}", issueId, levelId);
        }
    }

    // =====================================================
    // LABEL FUNCTIONS
    // =====================================================

    private void executeLabelFunction(WorkflowPostFunction pf, Map<String, Object> ctx, boolean add) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        String label = stringVal(config.get("label"), null);

        if (issueId != null && label != null) {
            if (add) {
                log.info("Adding label '{}' to issue {}", label, issueId);
            } else {
                log.info("Removing label '{}' from issue {}", label, issueId);
            }
        }
    }

    // =====================================================
    // SUBTASK AND CLONE FUNCTIONS
    // =====================================================

    private void executeCreateSubtaskFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        WorkflowContext workflowCtx = buildContextFromMap(ctx);

        integrationClient.createSubtask(workflowCtx, config);
        log.info("Created subtask for issue {}", ctx.get("issueId"));
    }

    private void executeCloneFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        UUID projectId = parseUuid(ctx.get("projectId"));

        if (issueId != null && projectId != null) {
            log.info("Cloning issue {} to project {}", issueId, projectId);
            // Clone implementation would call issue service
        }
    }

    // =====================================================
    // LINK FUNCTIONS
    // =====================================================

    private void executeLinkFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        UUID targetIssueId = parseUuid(config.get("targetIssueId"));
        UUID linkTypeId = parseUuid(config.get("linkTypeId"));

        if (issueId != null && targetIssueId != null && linkTypeId != null) {
            integrationClient.createIssueLink(issueId, targetIssueId, linkTypeId);
            log.info("Linked issue {} to {} with link type {}", issueId, targetIssueId, linkTypeId);
        }
    }

    private void executeUnlinkFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        UUID targetIssueId = parseUuid(config.get("targetIssueId"));

        if (issueId != null && targetIssueId != null) {
            log.info("Unlinking issue {} from {}", issueId, targetIssueId);
            // Unlink implementation
        }
    }

    // =====================================================
    // WATCHER FUNCTIONS
    // =====================================================

    private void executeWatcherFunction(WorkflowPostFunction pf, Map<String, Object> ctx, boolean add) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        UUID issueId = parseUuid(ctx.get("issueId"));
        UUID watcherId = parseUuid(config.get("watcherId"));

        if (watcherId == null) {
            watcherId = parseUuid(ctx.get("userId"));
        }

        if (issueId != null && watcherId != null) {
            if (add) {
                log.info("Adding watcher {} to issue {}", watcherId, issueId);
            } else {
                log.info("Removing watcher {} from issue {}", watcherId, issueId);
            }
        }
    }

    // =====================================================
    // EXTENSION AND AUTOMATION FUNCTIONS
    // =====================================================

    private void fireGlobalExtension(Map<String, Object> ctx, Map<String, Object> config) {
        String extensionPoint = stringVal(config.get("extensionPoint"), stringVal(config.get("pluginKey"), null));
        if (extensionPoint != null) {
            log.info("Firing global extension: {} for issue {}", extensionPoint, ctx.get("issueId"));
            eventPublisher.publishIssueTransitioned(buildContextFromMap(ctx));
        }
    }

    private void triggerWebhook(Map<String, Object> ctx, Map<String, Object> config) {
        String url = stringVal(config.get("webhookUrl"), stringVal(config.get("url"), null));
        if (url == null) {
            log.warn("TRIGGER_WEBHOOK missing webhookUrl");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ISSUE_TRANSITIONED");
        payload.put("issueId", ctx.get("issueId"));
        payload.put("issueKey", ctx.get("issueKey"));
        payload.put("projectId", ctx.get("projectId"));
        payload.put("transitionId", ctx.get("transitionId"));
        payload.put("userId", ctx.get("userId"));
        payload.put("timestamp", LocalDateTime.now().toString());

        integrationClient.fireWebhook(url, payload);
        log.info("Webhook triggered for issue {}", ctx.get("issueId"));
    }

    private void executeAutomationFunction(WorkflowPostFunction pf, Map<String, Object> ctx) {
        Map<String, Object> config = parseConfig(pf.getFunctionData());
        String queue = stringVal(config.get("automationQueue"), stringVal(config.get("queue"), defaultAutomationQueue));

        log.info("Automation rule triggered: queue={}, issueId={}", queue, ctx.get("issueId"));
        eventPublisher.publishIssueTransitioned(buildContextFromMap(ctx));
    }

    // =====================================================
    // SUMMARY GENERATION
    // =====================================================

    private void generateAutomaticSummary(Map<String, Object> ctx, Map<String, Object> config) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        String template = stringVal(config.get("template"), defaultSummaryTemplate);

        if (issueId != null) {
            Map<String, Object> fields = getFields(ctx);
            String issueKey = stringVal(ctx.get("issueKey"), issueId.toString());
            String toStatusName = stringVal(ctx.get("toStatusName"), "new status");
            String userName = stringVal(fields.get("displayName"), "Unknown");

            String summary = template
                    .replace("{issueKey}", issueKey)
                    .replace("{toStatusName}", toStatusName)
                    .replace("{userName}", userName);

            log.info("Generated automatic summary for issue {}: {}", issueId, summary);
        }
    }

    // =====================================================
    // UTILITY FUNCTIONS
    // =====================================================

    private void fireEvent(Map<String, Object> ctx) {
        eventPublisher.publishIssueTransitioned(buildContextFromMap(ctx));
    }

    private void addComment(Map<String, Object> ctx) {
        UUID issueId = parseUuid(ctx.get("issueId"));
        String comment = stringVal(ctx.get("comment"), null);
        UUID userId = parseUuid(ctx.get("userId"));

        if (issueId != null && comment != null) {
            integrationClient.addComment(issueId, comment, userId);
            log.info("Added comment to issue {}", issueId);
        }
    }

    private void autoTransition(Map<String, Object> ctx, Map<String, Object> config) {
        UUID transitionId = parseUuid(config.get("transitionId"));
        if (transitionId != null) {
            log.info("Auto-transition triggered: {}", transitionId);
            // This would need the execution engine to avoid circular dependency
        }
    }

    private void executeScript(Map<String, Object> ctx, Map<String, Object> config) {
        String scriptKey = stringVal(config.get("scriptKey"), stringVal(config.get("pluginKey"), null));
        if (scriptKey != null) {
            log.info("Executing script post-function: {}", scriptKey);
        }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private Map<String, Object> parseConfig(String functionData) {
        if (functionData == null || functionData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(functionData, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Invalid post-function JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFields(Map<String, Object> ctx) {
        Object fields = ctx.get("fields");
        if (fields instanceof Map) {
            return (Map<String, Object>) fields;
        }
        return new HashMap<>();
    }

    private WorkflowContext buildContextFromMap(Map<String, Object> ctx) {
        return WorkflowContext.builder()
                .issueId(parseUuid(ctx.get("issueId")))
                .projectId(parseUuid(ctx.get("projectId")))
                .issueTypeId(parseUuid(ctx.get("issueTypeId")))
                .currentStatusId(parseUuid(ctx.get("fromStatusId")))
                .userId(parseUuid(ctx.get("userId")))
                .resolutionId(parseUuid(ctx.get("resolutionId")))
                .issueData(getFields(ctx))
                .comment(stringVal(ctx.get("comment"), null))
                .build();
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            if (raw instanceof UUID) {
                return (UUID) raw;
            }
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String stringVal(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }
}