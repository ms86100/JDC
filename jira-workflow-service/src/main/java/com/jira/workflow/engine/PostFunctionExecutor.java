package com.jira.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.ExecuteTransitionRequest;
import com.jira.workflow.engine.plugin.WorkflowPluginRegistry;
import com.jira.workflow.entity.WorkflowPostFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Jira DC post-function implementations invoked by {@link PostFunctionPipeline}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostFunctionExecutor {

    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowEventPublisher eventPublisher;
    private final WorkflowPluginRegistry pluginRegistry;
    @Lazy
    private final WorkflowExecutionEngine executionEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            case WorkflowPostFunction.TYPE_GENERATE_CHANGE_HISTORY -> recordChangeHistory(ctx);
            case WorkflowPostFunction.TYPE_REINDEX_ISSUE -> reindexIssue(ctx);
            case WorkflowPostFunction.TYPE_STORE_ISSUE -> applyStatus(ctx);
            case WorkflowPostFunction.TYPE_CREATE_SUBTASK -> integrationClient.createSubtask(ctx, config);
            case WorkflowPostFunction.TYPE_LINK_ISSUE -> linkIssue(ctx, config);
            case WorkflowPostFunction.TYPE_UNLINK_ISSUE -> log.debug("Unlink issue not yet implemented");
            case WorkflowPostFunction.TYPE_AUTO_TRANSITION -> autoTransition(ctx, config);
            case WorkflowPostFunction.TYPE_SEND_EMAIL -> log.debug("Send email post-function skipped (notification outbox handles alerts)");
            case WorkflowPostFunction.TYPE_SCRIPT_POST_FUNCTION -> executeScript(ctx, config);
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
        integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("assigneeId", userId.toString()));
    }

    private void assignToRole(WorkflowContext ctx, Map<String, Object> config) {
        Object roleId = config.get("roleId");
        if (roleId == null) {
            log.warn("ASSIGN_TO_ROLE missing roleId in functionData");
            return;
        }
        log.debug("ASSIGN_TO_ROLE {} — assignee resolution requires project-role service", roleId);
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
            integrationClient.patchIssueFields(ctx.getIssueId(), Map.of("priorityId", priorityId.toString()));
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
        integrationClient.patchIssueFields(ctx.getIssueId(), Map.of(field, value));
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
        ExecuteTransitionRequest req = new ExecuteTransitionRequest();
        req.setIssueId(ctx.getIssueId());
        req.setProjectId(ctx.getProjectId());
        req.setUserId(ctx.getUserId());
        req.setTransitionId(UUID.fromString(transitionId.toString()));
        executionEngine.execute(req);
    }

    private void executeScript(WorkflowContext ctx, Map<String, Object> config) {
        String pluginKey = stringVal(config.get("pluginKey"), stringVal(config.get("scriptKey"), null));
        if (pluginKey == null) {
            return;
        }
        Map<String, Object> pluginCtx = new HashMap<>();
        pluginCtx.put("issueId", ctx.getIssueId().toString());
        pluginCtx.put("projectId", ctx.getProjectId() != null ? ctx.getProjectId().toString() : null);
        pluginCtx.put("userId", ctx.getUserId() != null ? ctx.getUserId().toString() : null);
        pluginRegistry.evaluateCondition(pluginKey, pluginCtx);
    }

    private Map<String, Object> changeItem(String field, String oldVal, String newVal, String oldStr, String newStr) {
        Map<String, Object> item = new HashMap<>();
        item.put("fieldType", "jira");
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
