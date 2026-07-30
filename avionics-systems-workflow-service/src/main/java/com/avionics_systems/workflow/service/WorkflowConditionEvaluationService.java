package com.avionics_systems.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.engine.ProjectPermissionClient;
import com.avionics_systems.workflow.engine.WorkflowIntegrationClient;
import com.avionics_systems.workflow.engine.plugin.WorkflowPluginRegistry;
import com.avionics_systems.workflow.entity.WorkflowCondition;
import com.avionics_systems.workflow.repository.WorkflowConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Workflow Condition Evaluation Service.
 * Evaluates workflow conditions to determine if a transition can be executed.
 * Matches Avionics Systems DC's ConditionWorkflowPlugin and related infrastructure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowConditionEvaluationService {

    private final WorkflowConditionRepository conditionRepository;
    private final WorkflowIntegrationClient integrationClient;
    private final ProjectPermissionClient permissionClient;
    private final WorkflowPluginRegistry pluginRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Evaluate all conditions for a transition.
     * For AND logic: all conditions must pass.
     * For OR logic: at least one condition must pass.
     *
     * @param conditions List of conditions to evaluate
     * @param context    Evaluation context
     * @return true if all conditions are met
     */
    public boolean evaluateConditions(List<WorkflowCondition> conditions, ConditionEvaluationContext context) {
        if (conditions == null || conditions.isEmpty()) {
            log.debug("No conditions to evaluate, allowing transition");
            return true;
        }

        for (WorkflowCondition condition : conditions) {
            boolean result = evaluateCondition(condition, context);
            boolean pass = condition.getNegate() != null && condition.getNegate() ? !result : result;

            if (!pass) {
                log.debug("Condition {} failed for issue", condition.getConditionType());
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate a single condition.
     *
     * @param condition The condition to evaluate
     * @param context  Evaluation context
     * @return true if condition is met
     */
    public boolean evaluateCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        if (condition == null || condition.getConditionType() == null) {
            log.warn("Null condition or condition type, blocking");
            return false;
        }

        String type = condition.getConditionType();
        log.debug("Evaluating condition type: {}", type);

        return switch (type) {
            // Composite conditions
            case WorkflowCondition.TYPE_AND -> evaluateCompositeCondition(condition, context, "AND");
            case WorkflowCondition.TYPE_OR -> evaluateCompositeCondition(condition, context, "OR");
            case WorkflowCondition.TYPE_NOT -> !evaluateCompositeCondition(condition, context, "NOT");

            // User-based conditions
            case WorkflowCondition.TYPE_USER_IS_REPORTER -> evaluateUserReporterCondition(condition, context);
            case WorkflowCondition.TYPE_USER_IS_ASSIGNEE -> evaluateUserAssigneeCondition(condition, context);
            case WorkflowCondition.TYPE_USER_IS_CURRENT_USER -> context.getCurrentUserId() != null;

            // Group conditions
            case WorkflowCondition.TYPE_USER_GROUP -> evaluateGroupCondition(condition, context);

            // Permission conditions
            case WorkflowCondition.TYPE_PERMISSION -> evaluatePermissionCondition(condition, context);

            // Field-based conditions
            case WorkflowCondition.TYPE_FIELD_VALUE -> evaluateFieldValueCondition(condition, context);
            case WorkflowCondition.TYPE_FIELD_CHANGED -> evaluateFieldChangedCondition(condition, context);
            case WorkflowCondition.TYPE_FIELD_REQUIRED -> evaluateFieldRequiredCondition(condition, context);

            // Status conditions
            case WorkflowCondition.TYPE_PREVIOUS_STATUS -> evaluatePreviousStatusCondition(condition, context);
            case WorkflowCondition.TYPE_SPRINT_STATUS -> evaluateSprintStatusCondition(condition, context);
            case WorkflowCondition.TYPE_SUBTASK_STATUS -> evaluateSubtaskStatusCondition(condition, context);
            case WorkflowCondition.TYPE_LINKED_ISSUE_STATUS -> evaluateLinkedIssueStatusCondition(condition, context);

            // Script condition
            case WorkflowCondition.TYPE_SCRIPT -> evaluateScriptCondition(condition, context);

            default -> {
                log.warn("Unknown condition type {}, blocking", type);
                yield false;
            }
        };
    }

    /**
     * Evaluate permission condition.
     * Checks if the current user has a specific permission in the project.
     */
    public boolean evaluatePermissionCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String permission = condition.getValue();
        if (permission == null || permission.isBlank()) {
            log.debug("No permission specified, allowing");
            return true;
        }

        UUID userId = context.getCurrentUserId();
        UUID projectId = context.getProjectId();

        if (userId == null || projectId == null) {
            log.debug("Missing user or project for permission check");
            return false;
        }

        // Use custom permission checker if provided
        if (context.getPermissionChecker() != null) {
            Boolean result = context.getPermissionChecker().apply(userId, permission);
            return result != null && result;
        }

        return permissionClient.hasPermission(userId, projectId, permission);
    }

    /**
     * Evaluate user group condition.
     * Checks if the current user belongs to a required group.
     */
    public boolean evaluateGroupCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String requiredGroup = condition.getValue();
        if (requiredGroup == null || requiredGroup.isBlank()) {
            log.debug("No group specified, allowing");
            return true;
        }

        Set<String> userGroups = context.getCurrentUserGroups();
        if (userGroups == null || userGroups.isEmpty()) {
            log.debug("User has no groups");
            return false;
        }

        return userGroups.stream()
                .anyMatch(g -> g.equalsIgnoreCase(requiredGroup));
    }

    /**
     * Evaluate field value condition.
     * Compares a field value against an expected value using the specified operator.
     */
    public boolean evaluateFieldValueCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String fieldName = condition.getFieldName();
        String operator = condition.getOperator();
        String expectedValue = condition.getValue();

        if (fieldName == null || fieldName.isBlank()) {
            log.warn("Field name not specified for FIELD_VALUE condition");
            return false;
        }

        Object actualValue = context.getFieldValue(fieldName);
        return evaluateOperator(actualValue, operator, expectedValue);
    }

    /**
     * Evaluate field changed condition.
     * Checks if a specific field was modified in this transition.
     */
    public boolean evaluateFieldChangedCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String fieldName = condition.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            log.warn("Field name not specified for FIELD_CHANGED condition");
            return false;
        }

        Map<String, Object> screenInput = context.getScreenInput();
        if (screenInput == null || !screenInput.containsKey(fieldName)) {
            log.debug("Field {} not in screen input, not changed", fieldName);
            return false;
        }

        Object oldValue = context.getFieldValue(fieldName);
        Object newValue = screenInput.get(fieldName);

        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }

        return !oldValue.toString().equals(newValue.toString());
    }

    /**
     * Evaluate field required condition.
     * Checks if a field has a value (for required field validation in transitions).
     */
    public boolean evaluateFieldRequiredCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String fieldName = condition.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            log.warn("Field name not specified for FIELD_REQUIRED condition");
            return false;
        }

        Object value = context.getFieldValue(fieldName);
        return value != null && !value.toString().isBlank();
    }

    /**
     * Evaluate previous status condition.
     * Checks if the issue was in a specific status before this transition.
     */
    public boolean evaluatePreviousStatusCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String requiredStatusId = condition.getValue();
        if (requiredStatusId == null || requiredStatusId.isBlank()) {
            log.debug("No previous status specified, allowing");
            return true;
        }

        UUID previousStatus = context.getPreviousStatusId();
        if (previousStatus == null) {
            log.debug("No previous status in context");
            return false;
        }

        return previousStatus.toString().equalsIgnoreCase(requiredStatusId);
    }

    /**
     * Evaluate user is reporter condition.
     * Checks if the current user is the issue reporter.
     */
    public boolean evaluateUserReporterCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        UUID reporterId = context.getReporterId();
        if (reporterId == null) {
            // Try to extract from issue
            Object rawReporter = context.getFieldValue("reporterId");
            if (rawReporter != null) {
                reporterId = parseUuid(rawReporter);
            }
        }

        if (context.getCurrentUserId() == null || reporterId == null) {
            return false;
        }

        return context.getCurrentUserId().equals(reporterId);
    }

    /**
     * Evaluate user is assignee condition.
     * Checks if the current user is the issue assignee.
     */
    public boolean evaluateUserAssigneeCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        UUID assigneeId = context.getAssigneeId();
        if (assigneeId == null) {
            // Try to extract from issue
            Object rawAssignee = context.getFieldValue("assigneeId");
            if (rawAssignee != null) {
                assigneeId = parseUuid(rawAssignee);
            }
        }

        if (context.getCurrentUserId() == null || assigneeId == null) {
            return false;
        }

        return context.getCurrentUserId().equals(assigneeId);
    }

    /**
     * Evaluate script condition.
     * Executes a custom script/plugin to determine condition result.
     */
    public boolean evaluateScriptCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String scriptKey = condition.getValue();
        if (scriptKey == null || scriptKey.isBlank()) {
            log.warn("No script key specified for SCRIPT condition");
            return false;
        }

        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.put("issueId", context.getIssueId() != null ? context.getIssueId().toString()
                : (context.getIssue() != null ? context.getIssue().get("id") : null));
        scriptContext.put("projectId", context.getProjectId() != null ? context.getProjectId().toString() : null);
        scriptContext.put("userId", context.getCurrentUserId() != null ? context.getCurrentUserId().toString() : null);
        scriptContext.put("transitionId", context.getTransitionId() != null ? context.getTransitionId().toString() : null);
        scriptContext.put("currentStatusId", context.getCurrentStatusId() != null ? context.getCurrentStatusId().toString() : null);
        scriptContext.put("fromStatusId", context.getPreviousStatusId() != null ? context.getPreviousStatusId().toString() : null);
        scriptContext.put("previousStatusId", context.getPreviousStatusId() != null ? context.getPreviousStatusId().toString() : null);
        scriptContext.put("issueData", context.getIssue() != null ? context.getIssue() : (context.getIssueFields() != null ? context.getIssueFields() : Map.of()));
        scriptContext.put("userData", Map.of("groups", context.getCurrentUserGroups() != null ? context.getCurrentUserGroups() : Set.of()));
        scriptContext.put("screenInput", context.getScreenInput() != null ? context.getScreenInput() : Map.of());

        return pluginRegistry.evaluateCondition(scriptKey, scriptContext);
    }

    /**
     * Evaluate composite condition (AND, OR, NOT).
     * For AND: all child conditions must pass.
     * For OR: at least one child condition must pass.
     * For NOT: the single child condition must fail.
     */
    public boolean evaluateCompositeCondition(WorkflowCondition condition, ConditionEvaluationContext context, String type) {
        List<WorkflowCondition> children = parseChildConditions(condition.getConditionData());
        if (children.isEmpty()) {
            log.debug("No child conditions for composite {}, allowing", type);
            return true;
        }

        return switch (type) {
            case "AND" -> children.stream().allMatch(c -> evaluateCondition(c, context));
            case "OR" -> children.stream().anyMatch(c -> evaluateCondition(c, context));
            case "NOT" -> !children.stream().findFirst().map(c -> evaluateCondition(c, context)).orElse(true);
            default -> {
                log.warn("Unknown composite type: {}", type);
                yield false;
            }
        };
    }

    /**
     * Evaluate sprint status condition.
     * Checks if the sprint matches a required status.
     */
    private boolean evaluateSprintStatusCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String requiredStatus = condition.getValue();
        if (requiredStatus == null || requiredStatus.isBlank()) {
            return true;
        }

        Object sprintField = context.getFieldValue("sprintStatus");
        if (sprintField == null) {
            sprintField = context.getFieldValue("sprintName");
        }
        if (sprintField == null) {
            sprintField = context.getFieldValue("sprint");
        }

        String currentStatus = sprintField != null ? sprintField.toString() : "";
        return requiredStatus.equalsIgnoreCase(currentStatus);
    }

    /**
     * Evaluate subtask status condition.
     * Checks if all subtasks have a specific status.
     */
    private boolean evaluateSubtaskStatusCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        String requiredStatus = condition.getValue();
        if (requiredStatus == null || requiredStatus.isBlank()) {
            return true;
        }

        Object subtasksObj = context.getFieldValue("subtasks");
        if (!(subtasksObj instanceof List<?> subtasks) || subtasks.isEmpty()) {
            return true; // No subtasks means condition passes
        }

        for (Object st : subtasks) {
            if (st instanceof Map<?, ?> subtask) {
                Object status = subtask.get("status");
                if (status == null) {
                    status = subtask.get("statusName");
                }
                String statusStr = status != null ? status.toString() : "";
                if (!requiredStatus.equalsIgnoreCase(statusStr)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Evaluate linked issue status condition.
     * Checks if linked issues have specific statuses.
     */
    private boolean evaluateLinkedIssueStatusCondition(WorkflowCondition condition, ConditionEvaluationContext context) {
        Map<String, Object> config = parseConditionConfig(condition);
        String requiredLinkType = getStringVal(config.get("linkType"), condition.getFieldName());
        String requiredStatusId = getStringVal(config.get("statusId"), condition.getValue());
        String requiredStatusName = getStringVal(config.get("statusName"), null);
        String direction = getStringVal(config.get("direction"), "ANY").toUpperCase();
        boolean requireAll = Boolean.TRUE.equals(config.get("requireAll"));

        UUID issueId = parseUuid(context.getIssue() != null ? context.getIssue().get("id") : null);
        if (issueId == null) {
            return !requireAll;
        }

        List<Map<String, Object>> linkedIssues = integrationClient.fetchLinkedIssuesForWorkflow(issueId);
        if (linkedIssues.isEmpty()) {
            return !requireAll;
        }

        List<Map<String, Object>> matchingLinks = linkedIssues.stream()
                .filter(link -> matchesDirection(direction, link))
                .filter(link -> requiredLinkType == null || requiredLinkType.isBlank()
                        || requiredLinkType.equalsIgnoreCase(getStringVal(link.get("linkType"), "")))
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
        return direction.equalsIgnoreCase(getStringVal(link.get("direction"), ""));
    }

    private boolean linkedIssueMatchesStatus(Map<String, Object> link, String statusId, String statusName) {
        if (statusId != null && !statusId.isBlank()) {
            return statusId.equalsIgnoreCase(getStringVal(link.get("statusId"), ""));
        }
        if (statusName != null && !statusName.isBlank()) {
            return statusName.equalsIgnoreCase(getStringVal(link.get("statusName"), ""));
        }
        return true;
    }

    /**
     * Parse child conditions from JSON stored in conditionData.
     */
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

    /**
     * Parse condition configuration from JSON.
     */
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

    /**
     * Evaluate an operator against actual and expected values.
     */
    private boolean evaluateOperator(Object actual, String operator, String expected) {
        if (operator == null) {
            operator = WorkflowCondition.OP_EQUALS;
        }

        // Handle empty checks first
        if (WorkflowCondition.OP_IS_EMPTY.equalsIgnoreCase(operator)) {
            return actual == null || actual.toString().isBlank();
        }
        if (WorkflowCondition.OP_IS_NOT_EMPTY.equalsIgnoreCase(operator)) {
            return actual != null && !actual.toString().isBlank();
        }

        // For all other operators, null actual means failure
        if (actual == null) {
            return false;
        }

        String actualStr = actual.toString();
        return switch (operator.toUpperCase()) {
            case WorkflowCondition.OP_EQUALS, "=" -> expected != null && actualStr.equalsIgnoreCase(expected);
            case WorkflowCondition.OP_NOT_EQUALS, "!=" -> expected == null || !actualStr.equalsIgnoreCase(expected);
            case WorkflowCondition.OP_CONTAINS, "~" -> expected != null && actualStr.toLowerCase().contains(expected.toLowerCase());
            case WorkflowCondition.OP_NOT_CONTAINS -> expected == null || !actualStr.toLowerCase().contains(expected.toLowerCase());
            case WorkflowCondition.OP_GREATER_THAN -> compareNumeric(actualStr, expected) > 0;
            case WorkflowCondition.OP_LESS_THAN -> compareNumeric(actualStr, expected) < 0;
            case WorkflowCondition.OP_IN -> Arrays.stream(expected.split(","))
                    .map(String::trim)
                    .anyMatch(v -> actualStr.equalsIgnoreCase(v));
            case WorkflowCondition.OP_NOT_IN -> Arrays.stream(expected.split(","))
                    .map(String::trim)
                    .noneMatch(v -> actualStr.equalsIgnoreCase(v));
            default -> actualStr.equalsIgnoreCase(expected);
        };
    }

    private int compareNumeric(String actual, String expected) {
        try {
            double actualNum = Double.parseDouble(actual);
            double expectedNum = Double.parseDouble(expected);
            return Double.compare(actualNum, expectedNum);
        } catch (NumberFormatException e) {
            return actual.compareTo(expected);
        }
    }

    private static String getStringVal(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }

    private UUID parseUuid(Object value) {
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