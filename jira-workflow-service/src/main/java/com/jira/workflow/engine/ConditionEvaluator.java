package com.jira.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.engine.plugin.WorkflowPluginRegistry;
import com.jira.workflow.entity.WorkflowCondition;
import com.jira.workflow.repository.WorkflowConditionRepository;
import com.jira.workflow.service.ContextConverter;
import com.jira.workflow.service.ConditionEvaluationContext;
import com.jira.workflow.service.WorkflowConditionEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Evaluates workflow conditions for transitions.
 * This class delegates to WorkflowConditionEvaluationService when available,
 * providing a unified interface for condition evaluation.
 */
@Component
@Slf4j
public class ConditionEvaluator {

    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowIntegrationClient integrationClient;
    private final WorkflowPluginRegistry pluginRegistry;
    private final ProjectPermissionClient projectPermissionClient;
    private final WorkflowConditionEvaluationService evaluationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor for backward compatibility.
     * @deprecated Use the constructor with WorkflowConditionEvaluationService instead.
     */
    @Deprecated
    public ConditionEvaluator(
            WorkflowConditionRepository workflowConditionRepository,
            WorkflowIntegrationClient integrationClient,
            WorkflowPluginRegistry pluginRegistry,
            ProjectPermissionClient projectPermissionClient) {
        this.workflowConditionRepository = workflowConditionRepository;
        this.integrationClient = integrationClient;
        this.pluginRegistry = pluginRegistry;
        this.projectPermissionClient = projectPermissionClient;
        this.evaluationService = null;
    }

    /**
     * Full constructor with evaluation service.
     */
    @Autowired
    public ConditionEvaluator(
            WorkflowConditionRepository workflowConditionRepository,
            WorkflowIntegrationClient integrationClient,
            WorkflowPluginRegistry pluginRegistry,
            ProjectPermissionClient projectPermissionClient,
            WorkflowConditionEvaluationService evaluationService) {
        this.workflowConditionRepository = workflowConditionRepository;
        this.integrationClient = integrationClient;
        this.pluginRegistry = pluginRegistry;
        this.projectPermissionClient = projectPermissionClient;
        this.evaluationService = evaluationService;
    }

    public List<String> evaluateAll(UUID transitionId, WorkflowContext ctx) {
        List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transitionId);

        // Use the new evaluation service if available
        if (evaluationService != null) {
            ConditionEvaluationContext evalCtx = ContextConverter.toEvaluationContext(ctx);
            boolean passed = evaluationService.evaluateConditions(conditions, evalCtx);
            if (passed) {
                return List.of();
            }
            List<String> errors = new ArrayList<>();
            for (WorkflowCondition condition : conditions) {
                boolean result = evaluationService.evaluateCondition(condition, evalCtx);
                boolean pass = condition.getNegate() != null && condition.getNegate() ? !result : result;
                if (!pass) {
                    errors.add("Condition not met: " + condition.getConditionType());
                }
            }
            return errors;
        }

        // Fallback to legacy evaluation
        List<String> errors = new ArrayList<>();
        for (WorkflowCondition condition : conditions) {
            boolean result = evaluateLegacy(condition, ctx);
            boolean pass = condition.getNegate() != null && condition.getNegate() ? !result : result;
            if (!pass) {
                errors.add("Condition not met: " + condition.getConditionType());
            }
        }
        return errors;
    }

    public boolean evaluate(WorkflowCondition condition, WorkflowContext ctx) {
        // Use the new evaluation service if available
        if (evaluationService != null) {
            ConditionEvaluationContext evalCtx = ContextConverter.toEvaluationContext(ctx);
            return evaluationService.evaluateCondition(condition, evalCtx);
        }
        // Fallback to legacy evaluation
        return evaluateLegacy(condition, ctx);
    }

    /**
     * Legacy evaluation method for backward compatibility.
     * @deprecated Use evaluate() with the new service instead.
     */
    @Deprecated
    private boolean evaluateLegacy(WorkflowCondition condition, WorkflowContext ctx) {
        String type = condition.getConditionType();
        if (type == null) {
            return false;
        }
        return switch (type) {
            case WorkflowCondition.TYPE_AND -> evaluateGroup(condition, ctx, true);
            case WorkflowCondition.TYPE_OR -> evaluateGroup(condition, ctx, false);
            case WorkflowCondition.TYPE_NOT -> !evaluateChild(condition, ctx);
            case WorkflowCondition.TYPE_USER_IS_CURRENT_USER -> ctx.getUserId() != null;
            case WorkflowCondition.TYPE_USER_IS_REPORTER ->
                    Objects.equals(ctx.getUserId(), parseUuid(ctx.getIssueData().get("reporterId")));
            case WorkflowCondition.TYPE_USER_IS_ASSIGNEE ->
                    Objects.equals(ctx.getUserId(), parseUuid(ctx.getIssueData().get("assigneeId")));
            case WorkflowCondition.TYPE_USER_GROUP -> userInGroup(condition.getValue(), ctx.getUserData());
            case WorkflowCondition.TYPE_PERMISSION -> hasPermission(condition.getValue(), ctx);
            case WorkflowCondition.TYPE_FIELD_VALUE ->
                    evaluateField(ctx.getIssueData().get(condition.getFieldName()), condition.getOperator(), condition.getValue());
            case WorkflowCondition.TYPE_PREVIOUS_STATUS ->
                    condition.getValue() != null && condition.getValue().equalsIgnoreCase(
                            String.valueOf(ctx.getIssueData().getOrDefault("previousStatusId", "")));
            case WorkflowCondition.TYPE_SUBTASK_STATUS -> allSubtasksMatch(condition.getValue(), ctx);
            case WorkflowCondition.TYPE_SPRINT_STATUS ->
                    sprintMatches(condition.getValue(), ctx.getIssueData());
            case WorkflowCondition.TYPE_LINKED_ISSUE_STATUS -> evaluateLinkedIssueStatus(condition, ctx);
            case WorkflowCondition.TYPE_FIELD_CHANGED -> evaluateFieldChanged(condition, ctx);
            case WorkflowCondition.TYPE_SCRIPT -> evaluateScript(condition, ctx);
            default -> {
                log.warn("Unknown condition type {}, blocking", type);
                yield false;
            }
        };
    }

    private boolean evaluateGroup(WorkflowCondition group, WorkflowContext ctx, boolean andLogic) {
        List<WorkflowCondition> children = parseChildConditions(group.getConditionData());
        if (children.isEmpty()) {
            return true;
        }
        if (andLogic) {
            return children.stream().allMatch(c -> evaluateLegacy(c, ctx));
        }
        return children.stream().anyMatch(c -> evaluateLegacy(c, ctx));
    }

    private boolean evaluateChild(WorkflowCondition notCondition, WorkflowContext ctx) {
        List<WorkflowCondition> children = parseChildConditions(notCondition.getConditionData());
        if (children.isEmpty()) {
            return true;
        }
        return evaluateLegacy(children.get(0), ctx);
    }

    @SuppressWarnings("unchecked")
    private List<WorkflowCondition> parseChildConditions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, List.class);
            List<WorkflowCondition> result = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                result.add(WorkflowCondition.builder()
                        .conditionType(String.valueOf(m.get("conditionType")))
                        .fieldName(m.get("fieldName") != null ? String.valueOf(m.get("fieldName")) : null)
                        .operator(m.get("operator") != null ? String.valueOf(m.get("operator")) : null)
                        .value(m.get("value") != null ? String.valueOf(m.get("value")) : null)
                        .negate(Boolean.TRUE.equals(m.get("negate")))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Could not parse condition group JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean userInGroup(String requiredGroup, Map<String, Object> userData) {
        if (requiredGroup == null) {
            return true;
        }
        Object groups = userData.get("groups");
        if (groups instanceof List<?> list) {
            return list.stream().anyMatch(g -> requiredGroup.equalsIgnoreCase(String.valueOf(g)));
        }
        return false;
    }

    private boolean hasPermission(String permission, WorkflowContext ctx) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (ctx.getUserId() == null || ctx.getProjectId() == null) {
            return false;
        }
        Object perms = ctx.getUserData().get("permissions");
        if (perms instanceof List<?> list) {
            boolean cached = list.stream().anyMatch(p -> permission.equalsIgnoreCase(String.valueOf(p)));
            if (cached) {
                return true;
            }
        }
        return projectPermissionClient.hasPermission(ctx.getUserId(), ctx.getProjectId(), permission);
    }

    private boolean allSubtasksMatch(String requiredStatus, WorkflowContext ctx) {
        Object subtasks = ctx.getIssueData().get("subtasks");
        if (!(subtasks instanceof List<?> list) || list.isEmpty()) {
            return true;
        }
        for (Object st : list) {
            if (st instanceof Map<?, ?> m) {
                Object statusObj = m.containsKey("status") ? m.get("status") : m.get("statusName");
                String status = statusObj != null ? String.valueOf(statusObj) : "";
                if (requiredStatus != null && !requiredStatus.equalsIgnoreCase(status)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean sprintMatches(String required, Map<String, Object> issueData) {
        String sprint = String.valueOf(issueData.getOrDefault("sprintStatus", issueData.getOrDefault("sprintName", "")));
        return required == null || required.equalsIgnoreCase(sprint);
    }

    private boolean evaluateField(Object actual, String operator, String expected) {
        if (operator == null) {
            operator = WorkflowCondition.OP_EQUALS;
        }
        if (WorkflowCondition.OP_IS_EMPTY.equalsIgnoreCase(operator)) {
            return actual == null || actual.toString().isBlank();
        }
        if (WorkflowCondition.OP_IS_NOT_EMPTY.equalsIgnoreCase(operator)) {
            return actual != null && !actual.toString().isBlank();
        }
        if (actual == null) {
            return false;
        }
        String actualStr = actual.toString();
        return switch (operator.toUpperCase()) {
            case WorkflowCondition.OP_EQUALS, "=" -> actualStr.equalsIgnoreCase(expected);
            case WorkflowCondition.OP_NOT_EQUALS, "!=" -> !actualStr.equalsIgnoreCase(expected);
            case WorkflowCondition.OP_CONTAINS, "~" -> actualStr.toLowerCase().contains(expected.toLowerCase());
            case WorkflowCondition.OP_NOT_CONTAINS -> !actualStr.toLowerCase().contains(expected.toLowerCase());
            case WorkflowCondition.OP_IN -> Arrays.stream(expected.split(",")).map(String::trim)
                    .anyMatch(v -> actualStr.equalsIgnoreCase(v));
            default -> actualStr.equalsIgnoreCase(expected);
        };
    }

    private boolean evaluateLinkedIssueStatus(WorkflowCondition condition, WorkflowContext ctx) {
        Map<String, Object> config = parseConditionConfig(condition);
        String requiredLinkType = stringVal(config.get("linkType"), condition.getFieldName());
        String requiredStatusId = stringVal(config.get("statusId"), condition.getValue());
        String requiredStatusName = stringVal(config.get("statusName"), null);
        String direction = stringVal(config.get("direction"), "ANY").toUpperCase();
        boolean requireAll = Boolean.TRUE.equals(config.get("requireAll"));

        List<Map<String, Object>> links = integrationClient.fetchLinkedIssuesForWorkflow(ctx.getIssueId());
        if (links.isEmpty()) {
            return !requireAll;
        }

        List<Map<String, Object>> matchingLinks = links.stream()
                .filter(link -> matchesDirection(direction, link))
                .filter(link -> requiredLinkType == null || requiredLinkType.isBlank()
                        || requiredLinkType.equalsIgnoreCase(stringVal(link.get("linkType"), "")))
                .toList();

        if (matchingLinks.isEmpty()) {
            return !requireAll;
        }

        if (requireAll) {
            return matchingLinks.stream().allMatch(link -> linkedIssueMatchesStatus(link, requiredStatusId, requiredStatusName));
        }
        return matchingLinks.stream().anyMatch(link -> linkedIssueMatchesStatus(link, requiredStatusId, requiredStatusName));
    }

    private boolean matchesDirection(String direction, Map<String, Object> link) {
        if ("ANY".equals(direction)) {
            return true;
        }
        return direction.equalsIgnoreCase(stringVal(link.get("direction"), ""));
    }

    private boolean linkedIssueMatchesStatus(Map<String, Object> link, String statusId, String statusName) {
        if (statusId != null && !statusId.isBlank()) {
            return statusId.equalsIgnoreCase(stringVal(link.get("statusId"), ""));
        }
        if (statusName != null && !statusName.isBlank()) {
            return statusName.equalsIgnoreCase(stringVal(link.get("statusName"), ""));
        }
        return true;
    }

    private boolean evaluateFieldChanged(WorkflowCondition condition, WorkflowContext ctx) {
        String fieldName = condition.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        Map<String, Object> screen = ctx.getScreenInput() != null ? ctx.getScreenInput() : Map.of();
        if (!screen.containsKey(fieldName)) {
            return false;
        }
        Object oldVal = ctx.getIssueData().get(fieldName);
        Object newVal = screen.get(fieldName);
        if (oldVal == null && newVal == null) {
            return false;
        }
        if (oldVal == null || newVal == null) {
            return true;
        }
        return !oldVal.toString().equals(newVal.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConditionConfig(WorkflowCondition condition) {
        if (condition.getConditionData() != null && !condition.getConditionData().isBlank()) {
            try {
                return objectMapper.readValue(condition.getConditionData(), Map.class);
            } catch (Exception e) {
                log.warn("Could not parse condition config JSON: {}", e.getMessage());
            }
        }
        Map<String, Object> config = new HashMap<>();
        if (condition.getFieldName() != null) {
            config.put("linkType", condition.getFieldName());
        }
        if (condition.getValue() != null) {
            config.put("statusId", condition.getValue());
        }
        return config;
    }

    private static String stringVal(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }

    private boolean evaluateScript(WorkflowCondition condition, WorkflowContext ctx) {
        String key = condition.getValue();
        if (key == null || key.isBlank()) {
            return false;
        }
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
        return pluginRegistry.evaluateCondition(key, pluginCtx);
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
