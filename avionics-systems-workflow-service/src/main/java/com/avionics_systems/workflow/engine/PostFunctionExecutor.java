package com.avionics_systems.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.dto.ExecuteTransitionRequest;
import com.avionics_systems.workflow.engine.plugin.WorkflowPluginRegistry;
import com.avionics_systems.workflow.entity.WorkflowPostFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Avionics Systems DC post-function implementations invoked by {@link PostFunctionPipeline}.
 */
@Component
@Slf4j
public class PostFunctionExecutor {

    private static final ThreadLocal<Integer> AUTO_TRANSITION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_AUTO_TRANSITION_DEPTH = 5;

    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowPluginRegistry pluginRegistry;
    private final WorkflowExecutionEngine executionEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.workflow.post-function.default-automation-queue:issue-transitioned}")
    private String defaultAutomationQueue;

    @Value("${app.workflow.post-function.default-email-subject:Workflow Notification}")
    private String defaultEmailSubject;

    @Autowired
    public PostFunctionExecutor(
            WorkflowIntegrationClient integrationClient,
            WorkflowEventPublisher eventPublisher,
            WorkflowPluginRegistry pluginRegistry,
            @Lazy WorkflowExecutionEngine executionEngine) {
        this.integrationClient = integrationClient;
        this.eventPublisher = eventPublisher;
        this.pluginRegistry = pluginRegistry;
        this.executionEngine = executionEngine;
    }

    public void executeEssentialChain(WorkflowContext ctx) {
        String oldStatusId = ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null;
        String oldStatusName = stringVal(ctx.getIssueData().get("statusName"), oldStatusId);
        applyStatus(ctx);
        addComment(ctx);
        recordChangeHistory(ctx, oldStatusId, oldStatusName);
        reindexIssue(ctx);
    }

    public void executeConfigured(WorkflowPostFunction pf, WorkflowContext ctx) {
        String type = pf.getFunctionType();
        if (type == null) {
            return;
        }
        Map<String, Object> config = parseConfig(pf.getFunctionData());

        switch (type) {
            case WorkflowPostFunction.TYPE_FIRE_EVENT -> fireEvent(ctx);
            case WorkflowPostFunction.TYPE_ADD_COMMENT -> addComment(ctx);
            case WorkflowPostFunction.TYPE_ASSIGN_TO_CURRENT_USER -> assignTo(ctx, ctx.getUserId());
            case WorkflowPostFunction.TYPE_ASSIGN_TO_REPORTER -> assignTo(ctx, parseUuid(ctx.getIssueData().get("reporterId")));
            case WorkflowPostFunction.TYPE_ASSIGN_TO_PROJECT_LEAD -> {
                Map<String, Object> project = integrationClient.fetchProject(ctx.getProjectId());
                assignTo(ctx, parseUuid(project.get("leadUserId")));
            }
            case WorkflowPostFunction.TYPE_ASSIGN_TO_ROLE -> assignToRole(ctx, config);
            case WorkflowPostFunction.TYPE_ASSIGN_TO_COMPONENT_LEAD -> assignToComponentLead(ctx, config);
            case WorkflowPostFunction.TYPE_ADD_WATCHER -> {
                UUID watchUserId = parseUuid(config.getOrDefault("userId", ctx.getUserId()));
                if (watchUserId != null) {
                    integrationClient.addWatcher(ctx.getIssueId(), watchUserId);
                }
            }
            case WorkflowPostFunction.TYPE_REMOVE_WATCHER -> {
                UUID unwatchUserId = parseUuid(config.getOrDefault("userId", ctx.getUserId()));
                if (unwatchUserId != null) {
                    integrationClient.removeWatcher(ctx.getIssueId(), unwatchUserId);
                }
            }
            case WorkflowPostFunction.TYPE_ASSIGN_TO_LAST_USER -> {
                Object last = ctx.getIssueData().get("lastAssigneeId");
                assignTo(ctx, parseUuid(last != null ? last : ctx.getIssueData().get("assigneeId")));
            }
            case WorkflowPostFunction.TYPE_SET_RESOLUTION -> setResolution(ctx, config);
            case WorkflowPostFunction.TYPE_SET_PRIORITY -> setPriority(ctx, config);
            case WorkflowPostFunction.TYPE_SET_ISSUE_SECURITY -> setSecurity(ctx, config);
            case WorkflowPostFunction.TYPE_SET_FIELD_VALUE, WorkflowPostFunction.TYPE_UPDATE_ISSUE_FIELD ->
                    setField(ctx, config);
            case WorkflowPostFunction.TYPE_COPY_VALUE_FROM_FIELD -> copyField(ctx, config);
            case WorkflowPostFunction.TYPE_SET_ISSUE_STATUS -> applyStatus(ctx);
            // These three are already handled in the essential chain (executeEssentialChain);
            // explicit no-op cases prevent the default log.warn for "unknown type".
            case WorkflowPostFunction.TYPE_GENERATE_CHANGE_HISTORY,
                 WorkflowPostFunction.TYPE_STORE_ISSUE,
                 WorkflowPostFunction.TYPE_REINDEX_ISSUE -> { /* no-op: handled in essential chain */ }
            case WorkflowPostFunction.TYPE_CREATE_SUBTASK -> integrationClient.createSubtask(ctx, config);
            case WorkflowPostFunction.TYPE_LINK_ISSUE -> linkIssue(ctx, config);
            case WorkflowPostFunction.TYPE_UNLINK_ISSUE -> unlinkIssue(ctx, config);
            case WorkflowPostFunction.TYPE_AUTO_TRANSITION -> autoTransition(ctx, config);
            case WorkflowPostFunction.TYPE_SEND_EMAIL -> sendEmail(ctx, config);
            case "CLONE_ISSUE" -> cloneIssue(ctx);
            case "ISSUE_MOVE" -> moveIssue(ctx, config);
            case "ADD_LABEL" -> addLabelPostFn(ctx, config);
            case "REMOVE_LABEL" -> removeLabelPostFn(ctx, config);
            case WorkflowPostFunction.TYPE_SCRIPT_POST_FUNCTION -> executeScript(ctx, config);
            case WorkflowPostFunction.TYPE_TRIGGER_WEBHOOK -> triggerWebhook(ctx, config);
            case WorkflowPostFunction.TYPE_TRIGGER_AUTOMATION -> triggerAutomation(ctx, config);
            case WorkflowPostFunction.TYPE_LOG_WORK -> logWork(ctx, config);
            default -> log.warn("Unknown post-function type: {}", type);
        }
    }

    private void applyStatus(WorkflowContext ctx) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusId", ctx.getTransition().getToStatusId().toString());
        if (ctx.getResolutionId() != null) {
            body.put("resolutionId", ctx.getResolutionId().toString());
        }
        if (ctx.getScreenInput() != null) {
            body.put("screenInput", ctx.getScreenInput());
        }
        integrationClient.updateIssueWorkflowInternal(ctx.getIssueId(), body);
    }

    private void addComment(WorkflowContext ctx) {
        integrationClient.addComment(ctx.getIssueId(), ctx.getComment(), ctx.getUserId());
    }

    private void recordChangeHistory(WorkflowContext ctx) {
        recordChangeHistory(ctx, ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null,
                stringVal(ctx.getIssueData().get("statusName"), null));
    }

    private void recordChangeHistory(WorkflowContext ctx, String oldStatusId, String oldStatusName) {
        String newStatus = ctx.getTransition().getName();

        List<Map<String, Object>> changes = new ArrayList<>();
        changes.add(changeItem("status", oldStatusId, ctx.getTransition().getToStatusId().toString(),
                oldStatusName != null ? oldStatusName : oldStatusId, newStatus));

        if (ctx.getResolutionId() != null) {
            changes.add(changeItem("resolution", null, ctx.getResolutionId().toString(), null, ctx.getResolutionId().toString()));
        }

        String authorName = stringVal(ctx.getUserData().get("displayName"), stringVal(ctx.getUserData().get("username"), "Workflow"));
        integrationClient.recordChangeHistory(ctx.getIssueId(), ctx.getUserId(), authorName, changes);
    }

    private void reindexIssue(WorkflowContext ctx) {
        Map<String, Object> issue = ctx.getIssueData();
        String key = stringVal(issue.get("issueKey"), ctx.getIssueId().toString());
        String summary = stringVal(issue.get("summary"), "");
        integrationClient.indexIssue(ctx.getIssueId(), key + (summary.isBlank() ? "" : " — " + summary),
                "Transition: " + ctx.getTransition().getName());
    }

    public void fireEvent(WorkflowContext ctx) {
        eventPublisher.publishIssueTransitioned(ctx);
    }

    private void assignTo(WorkflowContext ctx, UUID userId) {
        if (userId == null) {
            return;
        }
        String oldAssignee = stringVal(ctx.getIssueData().get("assigneeId"), null);
        integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("assigneeId", userId.toString()));
        recordFieldChange(ctx, "assignee", oldAssignee, userId.toString(),
                stringVal(ctx.getIssueData().get("assigneeName"), oldAssignee),
                userId.toString());
    }

    private void assignToRole(WorkflowContext ctx, Map<String, Object> config) {
        Object roleId = config.get("roleId");
        if (roleId == null) {
            log.warn("ASSIGN_TO_ROLE missing roleId in functionData");
            return;
        }
        String roleIdStr = roleId.toString();
        List<Map<String, Object>> roles = integrationClient.fetchProjectRoles(ctx.getProjectId());
        for (Map<String, Object> role : roles) {
            if (roleIdStr.equals(String.valueOf(role.get("id")))) {
                Object members = role.get("members");
                if (members instanceof List<?> memberList && !memberList.isEmpty()) {
                    Object firstMember = memberList.get(0);
                    UUID memberId = null;
                    if (firstMember instanceof Map<?, ?> memberMap) {
                        memberId = parseUuid(memberMap.get("userId"));
                    } else {
                        memberId = parseUuid(firstMember);
                    }
                    if (memberId != null) {
                        assignTo(ctx, memberId);
                        return;
                    }
                }
                log.warn("ASSIGN_TO_ROLE: role {} has no members in project {}", roleIdStr, ctx.getProjectId());
                return;
            }
        }
        log.warn("ASSIGN_TO_ROLE: role {} not found in project {}", roleIdStr, ctx.getProjectId());
    }

    @SuppressWarnings("unchecked")
    private void assignToComponentLead(WorkflowContext ctx, Map<String, Object> config) {
        Object componentIdRaw = config.get("componentId");
        if (componentIdRaw == null) {
            componentIdRaw = ctx.getIssueData().get("componentId");
        }
        if (componentIdRaw == null) {
            log.warn("ASSIGN_TO_COMPONENT_LEAD: no componentId in config or issueData");
            return;
        }
        String componentId = componentIdRaw.toString();
        List<Map<String, Object>> components = integrationClient.fetchProjectComponents(ctx.getProjectId());
        for (Map<String, Object> comp : components) {
            if (componentId.equals(String.valueOf(comp.get("id")))) {
                UUID leadUserId = parseUuid(comp.get("leadUserId"));
                if (leadUserId != null) {
                    assignTo(ctx, leadUserId);
                } else {
                    log.warn("ASSIGN_TO_COMPONENT_LEAD: component {} has no leadUserId", componentId);
                }
                return;
            }
        }
        log.warn("ASSIGN_TO_COMPONENT_LEAD: component {} not found in project {}", componentId, ctx.getProjectId());
    }

    private void setResolution(WorkflowContext ctx, Map<String, Object> config) {
        Object res = config.getOrDefault("resolutionId", ctx.getResolutionId());
        if (res == null && ctx.getScreenInput() != null) {
            res = ctx.getScreenInput().get("resolutionId");
        }
        if (res != null) {
            integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("resolutionId", res.toString()));
        }
    }

    private void setPriority(WorkflowContext ctx, Map<String, Object> config) {
        Object priorityId = config.get("priorityId");
        if (priorityId != null) {
            String oldPriority = stringVal(ctx.getIssueData().get("priorityId"), null);
            integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("priorityId", priorityId.toString()));
            recordFieldChange(ctx, "priority", oldPriority, priorityId.toString(),
                    stringVal(ctx.getIssueData().get("priorityName"), oldPriority),
                    priorityId.toString());
        }
    }

    private void setSecurity(WorkflowContext ctx, Map<String, Object> config) {
        Object levelId = config.get("securityLevelId");
        if (levelId != null) {
            integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("securityLevelId", levelId.toString()));
        }
    }

    private void setField(WorkflowContext ctx, Map<String, Object> config) {
        String field = stringVal(config.get("field"), stringVal(config.get("fieldName"), null));
        Object value = config.get("value");
        if (field == null || value == null) {
            return;
        }
        String oldValue = stringVal(ctx.getIssueData().get(field), null);
        integrationClient.patchIssueFields(ctx.getIssueId(), Map.of(field, value));
        recordFieldChange(ctx, field, oldValue, value.toString(), oldValue, value.toString());
    }

    private void copyField(WorkflowContext ctx, Map<String, Object> config) {
        String from = stringVal(config.get("fromField"), null);
        String to = stringVal(config.get("toField"), null);
        if (from == null || to == null) {
            return;
        }
        Object value = ctx.getIssueData().get(from);
        if (value == null && ctx.getScreenInput() != null) {
            value = ctx.getScreenInput().get(from);
        }
        if (value != null) {
            integrationClient.patchIssueFields(ctx.getIssueId(), Map.of(to, value));
        }
    }

    private void linkIssue(WorkflowContext ctx, Map<String, Object> config) {
        Object targetId = config.get("targetIssueId");
        Object linkTypeId = config.get("linkTypeId");
        if (targetId != null && linkTypeId != null) {
            integrationClient.createIssueLink(
                    ctx.getIssueId(),
                    UUID.fromString(targetId.toString()),
                    UUID.fromString(linkTypeId.toString()));
        }
    }

    private void autoTransition(WorkflowContext ctx, Map<String, Object> config) {
        Object transitionId = config.get("transitionId");
        if (transitionId == null) {
            return;
        }

        int depth = AUTO_TRANSITION_DEPTH.get();
        if (depth >= MAX_AUTO_TRANSITION_DEPTH) {
            log.error("AUTO_TRANSITION aborted: recursion depth {} reached max {} for issue {}",
                    depth, MAX_AUTO_TRANSITION_DEPTH, ctx.getIssueId());
            return;
        }

        AUTO_TRANSITION_DEPTH.set(depth + 1);
        try {
            ExecuteTransitionRequest req = new ExecuteTransitionRequest();
            req.setIssueId(ctx.getIssueId());
            req.setProjectId(ctx.getProjectId());
            req.setUserId(ctx.getUserId());
            req.setTransitionId(UUID.fromString(transitionId.toString()));
            executionEngine.execute(req);
        } finally {
            AUTO_TRANSITION_DEPTH.set(depth);
        }
    }

    private void triggerWebhook(WorkflowContext ctx, Map<String, Object> config) {
        String url = stringVal(config.get("webhookUrl"), stringVal(config.get("url"), null));
        if (url == null) {
            log.warn("TRIGGER_WEBHOOK missing webhookUrl");
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ISSUE_TRANSITIONED");
        payload.put("issueId", ctx.getIssueId().toString());
        payload.put("projectId", ctx.getProjectId() != null ? ctx.getProjectId().toString() : null);
        payload.put("transitionId", ctx.getTransition().getId().toString());
        payload.put("transitionName", ctx.getTransition().getName());
        payload.put("fromStatusId", ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null);
        payload.put("toStatusId", ctx.getTransition().getToStatusId().toString());
        payload.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        integrationClient.fireWebhook(url, payload);
    }

    private void triggerAutomation(WorkflowContext ctx, Map<String, Object> config) {
        String queue = stringVal(config.get("automationQueue"), stringVal(config.get("queue"), defaultAutomationQueue));
        log.info("Automation hook queued: {} for issue {}", queue, ctx.getIssueId());
        eventPublisher.publishIssueTransitioned(ctx);
    }

    private void executeScript(WorkflowContext ctx, Map<String, Object> config) {
        String pluginKey = stringVal(config.get("pluginKey"), stringVal(config.get("scriptKey"), null));
        if (pluginKey == null) {
            return;
        }
        Map<String, Object> pluginCtx = buildEnrichedContext(ctx);
        pluginRegistry.executePostFunction(pluginKey, pluginCtx);
    }

    private Map<String, Object> buildEnrichedContext(WorkflowContext ctx) {
        Map<String, Object> pluginCtx = new HashMap<>();
        pluginCtx.put("issueId", ctx.getIssueId() != null ? ctx.getIssueId().toString() : null);
        pluginCtx.put("projectId", ctx.getProjectId() != null ? ctx.getProjectId().toString() : null);
        pluginCtx.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        pluginCtx.put("issueTypeId", ctx.getIssueTypeId() != null ? ctx.getIssueTypeId().toString() : null);
        pluginCtx.put("currentStatusId", ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null);
        pluginCtx.put("transitionId", ctx.getTransition() != null ? ctx.getTransition().getId().toString() : null);
        pluginCtx.put("transitionName", ctx.getTransition() != null ? ctx.getTransition().getName() : null);
        pluginCtx.put("fromStatusId", ctx.getCurrentStatusId() != null ? ctx.getCurrentStatusId().toString() : null);
        pluginCtx.put("toStatusId", ctx.getTransition() != null ? ctx.getTransition().getToStatusId().toString() : null);
        pluginCtx.put("issueData", ctx.getIssueData() != null ? ctx.getIssueData() : Map.of());
        pluginCtx.put("userData", ctx.getUserData() != null ? ctx.getUserData() : Map.of());
        pluginCtx.put("screenInput", ctx.getScreenInput() != null ? ctx.getScreenInput() : Map.of());
        pluginCtx.put("comment", ctx.getComment());
        pluginCtx.put("resolutionId", ctx.getResolutionId() != null ? ctx.getResolutionId().toString() : null);
        return pluginCtx;
    }

    private void sendEmail(WorkflowContext ctx, Map<String, Object> config) {
        String to = stringVal(config.get("to"), stringVal(config.get("email"), null));
        String subject = stringVal(config.get("subject"), defaultEmailSubject);
        String body = stringVal(config.get("body"), stringVal(config.get("message"), ""));
        if (to == null || to.isBlank()) {
            log.debug("SEND_EMAIL skipped: no recipient specified");
            return;
        }
        // Write to outbox for reliable delivery instead of direct HTTP call.
        // The outbox processor will retry delivery if the notification service is down.
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "EMAIL_NOTIFICATION");
        payload.put("to", to);
        payload.put("subject", subject);
        payload.put("body", body);
        payload.put("issueId", ctx.getIssueId().toString());
        payload.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        payload.put("isHtml", true);
        payload.put("config", config);
        eventPublisher.publish(ctx.getIssueId(), "EMAIL_NOTIFICATION", payload);
    }

    private void unlinkIssue(WorkflowContext ctx, Map<String, Object> config) {
        Object linkId = config.get("linkId");
        if (linkId != null) {
            try {
                integrationClient.restTemplate().delete(
                        integrationClient.getIssueServiceUrl() + "/api/issues/links/" + linkId);
            } catch (Exception e) {
                log.warn("Failed to unlink issue: {}", e.getMessage());
            }
        }
    }

    private void cloneIssue(WorkflowContext ctx) {
        Map<String, Object> clonedIssue = integrationClient.cloneIssue(ctx.getIssueId());
        if (clonedIssue != null && clonedIssue.get("id") != null) {
            log.info("Cloned issue {} -> {} (key: {})", ctx.getIssueId(),
                    clonedIssue.get("id"), clonedIssue.getOrDefault("issueKey", "unknown"));
            // TODO: Create a "cloned from" issue link once a link-type-by-name lookup is available.
            // createIssueLink requires a UUID linkTypeId; resolving "Cloners" to UUID needs
            // an additional API call to issue-service that does not yet exist.
        } else {
            log.warn("CLONE_ISSUE post-function returned no cloned issue data for {}", ctx.getIssueId());
        }
    }

    private void moveIssue(WorkflowContext ctx, Map<String, Object> config) {
        Object targetProjectId = config.get("targetProjectId");
        if (targetProjectId != null) {
            integrationClient.moveIssue(ctx.getIssueId(), UUID.fromString(targetProjectId.toString()));
        }
    }

    private void addLabelPostFn(WorkflowContext ctx, Map<String, Object> config) {
        Object label = config.get("label");
        if (label != null) {
            integrationClient.addLabel(ctx.getIssueId(), label.toString());
        }
    }

    private void removeLabelPostFn(WorkflowContext ctx, Map<String, Object> config) {
        Object label = config.get("label");
        if (label != null) {
            integrationClient.removeLabel(ctx.getIssueId(), label.toString());
        }
    }

    private void logWork(WorkflowContext ctx, Map<String, Object> config) {
        Map<String, Object> screenInput = ctx.getScreenInput();
        Long timeSpent = null;
        String description = null;

        if (screenInput != null) {
            Object ts = screenInput.get("timeSpentSeconds");
            if (ts != null) {
                try { timeSpent = Long.parseLong(ts.toString()); } catch (NumberFormatException ignored) {}
            }
            Object desc = screenInput.get("workDescription");
            if (desc != null) description = desc.toString();
        }

        if (timeSpent == null) {
            Object ts = config.get("timeSpentSeconds");
            if (ts != null) {
                try { timeSpent = Long.parseLong(ts.toString()); } catch (NumberFormatException ignored) {}
            }
        }
        if (description == null) {
            description = stringVal(config.get("workDescription"), null);
        }

        if (timeSpent == null || timeSpent <= 0) {
            log.debug("LOG_WORK skipped: no timeSpentSeconds provided");
            return;
        }

        integrationClient.logWorkOnIssue(ctx.getIssueId(), ctx.getUserId(), timeSpent, description);
    }

    private void recordFieldChange(WorkflowContext ctx, String field, String oldVal, String newVal,
                                       String oldStr, String newStr) {
        try {
            String authorName = stringVal(ctx.getUserData().get("displayName"),
                    stringVal(ctx.getUserData().get("username"), "Workflow"));
            List<Map<String, Object>> changes = new ArrayList<>();
            changes.add(changeItem(field, oldVal, newVal, oldStr, newStr));
            integrationClient.recordChangeHistory(ctx.getIssueId(), ctx.getUserId(), authorName, changes);
        } catch (Exception e) {
            log.warn("Failed to record change history for field {}: {}", field, e.getMessage());
        }
    }

    private Map<String, Object> changeItem(String field, String oldVal, String newVal, String oldStr, String newStr) {
        Map<String, Object> item = new HashMap<>();
        item.put("fieldType", "avionics-systems");
        item.put("field", field);
        item.put("oldValue", oldVal);
        item.put("newValue", newVal);
        item.put("oldString", oldStr);
        item.put("newString", newStr);
        return item;
    }

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

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String stringVal(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }
}
