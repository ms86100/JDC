package com.jira.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.*;
import com.jira.workflow.entity.AutomationExecutionLog;
import com.jira.workflow.entity.AutomationRule;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.engine.WorkflowIntegrationClient;
import com.jira.workflow.repository.AutomationExecutionLogRepository;
import com.jira.workflow.repository.AutomationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and executing automation rules.
 * Automation rules fire on issue events (create, update, field change, etc.)
 * independently of workflow transitions, matching "Automation for Jira" in JDC 9.0+.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationRuleService {

    private final AutomationRuleRepository ruleRepo;
    private final AutomationExecutionLogRepository logRepo;
    private final WorkflowIntegrationClient integrationClient;
    private final ObjectMapper objectMapper;

    // ===== CRUD =====

    @Transactional
    public AutomationRuleResponse createRule(CreateAutomationRuleRequest request) {
        log.info("Creating automation rule: {}", request.getName());

        AutomationRule rule = AutomationRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .triggerType(request.getTriggerType())
                .triggerConfig(defaultIfBlank(request.getTriggerConfig(), "{}"))
                .conditions(defaultIfBlank(request.getConditions(), "[]"))
                .actions(defaultIfBlank(request.getActions(), "[]"))
                .branchType(request.getBranchType())
                .branchLinkType(request.getBranchLinkType())
                .branchActions(defaultIfBlank(request.getBranchActions(), "[]"))
                .build();

        rule = ruleRepo.save(rule);
        log.info("Automation rule created: {} (id={})", rule.getName(), rule.getId());
        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public AutomationRuleResponse getRule(UUID id) {
        log.debug("Fetching automation rule: {}", id);
        AutomationRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", id));
        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> getRulesByProject(UUID projectId) {
        log.debug("Fetching automation rules for project: {}", projectId);
        return ruleRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> getAllRules() {
        log.debug("Fetching all automation rules");
        return ruleRepo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AutomationRuleResponse updateRule(UUID id, UpdateAutomationRuleRequest request) {
        log.info("Updating automation rule: {}", id);

        AutomationRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", id));

        if (request.getName() != null) {
            rule.setName(request.getName());
        }
        if (request.getDescription() != null) {
            rule.setDescription(request.getDescription());
        }
        if (request.getProjectId() != null) {
            rule.setProjectId(request.getProjectId());
        }
        if (request.getTriggerType() != null) {
            rule.setTriggerType(request.getTriggerType());
        }
        if (request.getTriggerConfig() != null) {
            rule.setTriggerConfig(request.getTriggerConfig());
        }
        if (request.getConditions() != null) {
            rule.setConditions(request.getConditions());
        }
        if (request.getActions() != null) {
            rule.setActions(request.getActions());
        }
        if (request.getBranchType() != null) {
            rule.setBranchType(request.getBranchType());
        }
        if (request.getBranchLinkType() != null) {
            rule.setBranchLinkType(request.getBranchLinkType());
        }
        if (request.getBranchActions() != null) {
            rule.setBranchActions(request.getBranchActions());
        }

        rule = ruleRepo.save(rule);
        log.info("Automation rule updated: {}", id);
        return mapToResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID id) {
        log.info("Deleting automation rule: {}", id);
        AutomationRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", id));
        ruleRepo.delete(rule);
        log.info("Automation rule deleted: {}", id);
    }

    @Transactional
    public void toggleRule(UUID id, boolean enabled) {
        log.info("Toggling automation rule {} to enabled={}", id, enabled);
        AutomationRule rule = ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", id));
        rule.setIsEnabled(enabled);
        ruleRepo.save(rule);
        log.info("Automation rule {} is now {}", id, enabled ? "enabled" : "disabled");
    }

    // ===== Execution =====

    /**
     * Evaluate all enabled automation rules matching the given trigger type and project.
     * For each matching rule, conditions are checked, and if satisfied, actions are executed.
     * If a branch is configured, linked/sub issues are iterated and branch actions applied.
     *
     * @param triggerType the event trigger (e.g. ISSUE_CREATED, STATUS_CHANGED)
     * @param issueId     the issue that triggered the event
     * @param projectId   the project the issue belongs to
     * @param eventData   contextual data about the event (changed fields, status info, etc.)
     */
    @Transactional
    public void evaluateRules(String triggerType, UUID issueId, UUID projectId, Map<String, Object> eventData) {
        log.debug("Evaluating automation rules for trigger={}, issue={}, project={}", triggerType, issueId, projectId);

        List<AutomationRule> matchingRules = ruleRepo.findEnabledRulesForTrigger(triggerType, projectId);
        if (matchingRules.isEmpty()) {
            log.debug("No enabled automation rules found for trigger={} project={}", triggerType, projectId);
            return;
        }

        // Fetch issue data for condition evaluation
        Map<String, Object> issueData = integrationClient.fetchIssue(issueId);
        // Merge event data into issue data for richer context
        Map<String, Object> context = new HashMap<>(issueData);
        if (eventData != null) {
            context.putAll(eventData);
        }

        for (AutomationRule rule : matchingRules) {
            long startMs = System.currentTimeMillis();
            try {
                // Check trigger config match (e.g. specific field for FIELD_CHANGED)
                if (!matchesTriggerConfig(rule, eventData)) {
                    logExecution(rule, issueId, triggerType, AutomationExecutionLog.STATUS_SKIPPED,
                            0, null, (int) (System.currentTimeMillis() - startMs));
                    continue;
                }

                // Evaluate conditions
                List<Map<String, Object>> conditionsList = parseJsonArray(rule.getConditions());
                if (!evaluateConditions(conditionsList, context)) {
                    logExecution(rule, issueId, triggerType, AutomationExecutionLog.STATUS_SKIPPED,
                            0, "Conditions not met", (int) (System.currentTimeMillis() - startMs));
                    continue;
                }

                // Execute primary actions
                List<Map<String, Object>> actionsList = parseJsonArray(rule.getActions());
                int actionsExecuted = executeActions(actionsList, issueId, context);

                // Execute branch if configured
                if (rule.getBranchType() != null && !rule.getBranchType().isBlank()) {
                    actionsExecuted += executeBranch(rule, issueId, context);
                }

                // Update rule stats
                rule.setExecutionCount(rule.getExecutionCount() + 1);
                rule.setLastExecutedAt(LocalDateTime.now());
                rule.setLastError(null);
                ruleRepo.save(rule);

                logExecution(rule, issueId, triggerType, AutomationExecutionLog.STATUS_SUCCESS,
                        actionsExecuted, null, (int) (System.currentTimeMillis() - startMs));

                log.info("Automation rule '{}' executed successfully ({} actions) for issue {}",
                        rule.getName(), actionsExecuted, issueId);

            } catch (Exception e) {
                log.error("Automation rule '{}' failed for issue {}: {}", rule.getName(), issueId, e.getMessage(), e);

                rule.setExecutionCount(rule.getExecutionCount() + 1);
                rule.setLastExecutedAt(LocalDateTime.now());
                rule.setLastError(e.getMessage());
                ruleRepo.save(rule);

                logExecution(rule, issueId, triggerType, AutomationExecutionLog.STATUS_FAILED,
                        0, e.getMessage(), (int) (System.currentTimeMillis() - startMs));
            }
        }
    }

    /**
     * Check whether the rule's trigger config matches the event data.
     * For FIELD_CHANGED triggers, verifies that the configured fieldName matches what actually changed.
     */
    private boolean matchesTriggerConfig(AutomationRule rule, Map<String, Object> eventData) {
        if (eventData == null || rule.getTriggerConfig() == null) {
            return true;
        }

        Map<String, Object> config = parseJsonObject(rule.getTriggerConfig());
        if (config.isEmpty()) {
            return true;
        }

        // For FIELD_CHANGED: check that the specific field actually changed
        if (AutomationRule.TRIGGER_FIELD_CHANGED.equals(rule.getTriggerType())) {
            String requiredField = (String) config.get("fieldName");
            if (requiredField != null) {
                String changedField = (String) eventData.get("fieldName");
                return requiredField.equalsIgnoreCase(changedField);
            }
        }

        return true;
    }

    /**
     * Evaluate a list of conditions against the current issue/event context.
     * All conditions must pass (AND logic). An empty conditions list returns true.
     *
     * Supported operators: EQUALS, NOT_EQUALS, CONTAINS, IS_EMPTY, IS_NOT_EMPTY, IN, CHANGED
     */
    boolean evaluateConditions(List<Map<String, Object>> conditions, Map<String, Object> issueData) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Map<String, Object> condition : conditions) {
            String field = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object expectedValue = condition.get("value");

            if (field == null || operator == null) {
                log.warn("Skipping malformed condition: missing field or operator");
                continue;
            }

            Object actualValue = issueData.get(field);
            String actualStr = actualValue != null ? actualValue.toString() : "";
            String expectedStr = expectedValue != null ? expectedValue.toString() : "";

            boolean result = switch (operator.toUpperCase()) {
                case "EQUALS" -> expectedStr.equalsIgnoreCase(actualStr);
                case "NOT_EQUALS" -> !expectedStr.equalsIgnoreCase(actualStr);
                case "CONTAINS" -> actualStr.toLowerCase().contains(expectedStr.toLowerCase());
                case "IS_EMPTY" -> actualValue == null || actualStr.isBlank();
                case "IS_NOT_EMPTY" -> actualValue != null && !actualStr.isBlank();
                case "IN" -> {
                    // expectedValue should be a comma-separated list or a JSON array
                    if (expectedValue instanceof List<?> list) {
                        yield list.stream().anyMatch(v -> v != null && v.toString().equalsIgnoreCase(actualStr));
                    }
                    yield Arrays.stream(expectedStr.split(","))
                            .map(String::trim)
                            .anyMatch(v -> v.equalsIgnoreCase(actualStr));
                }
                case "CHANGED" -> {
                    // The event data must contain the field, meaning it changed
                    yield issueData.containsKey(field);
                }
                default -> {
                    log.warn("Unknown condition operator: {}", operator);
                    yield true;
                }
            };

            if (!result) {
                log.debug("Condition failed: field={}, operator={}, expected={}, actual={}", field, operator, expectedValue, actualValue);
                return false;
            }
        }

        return true;
    }

    /**
     * Execute a list of actions on the target issue.
     * Supports: UPDATE_FIELD, CREATE_ISSUE, TRANSITION_STATUS, ADD_COMMENT,
     *           LINK_ISSUE, CLONE_ISSUE, ASSIGN_ISSUE, ADD_LABEL, REMOVE_LABEL
     *
     * @return the number of actions successfully executed
     */
    private int executeActions(List<Map<String, Object>> actions, UUID issueId, Map<String, Object> context) {
        if (actions == null || actions.isEmpty()) {
            return 0;
        }

        int executed = 0;
        for (Map<String, Object> action : actions) {
            String actionType = (String) action.get("type");
            if (actionType == null) {
                log.warn("Skipping action with no type");
                continue;
            }

            try {
                switch (actionType.toUpperCase()) {
                    case "UPDATE_FIELD" -> {
                        String field = (String) action.get("field");
                        Object value = resolveTemplateValue(action.get("value"), context);
                        if (field != null) {
                            integrationClient.patchIssueFields(issueId, Map.of(field, value));
                            executed++;
                        }
                    }
                    case "CREATE_ISSUE" -> {
                        Map<String, Object> issueData = new HashMap<>();
                        if (action.get("projectId") != null) {
                            issueData.put("projectId", action.get("projectId").toString());
                        } else if (context.get("projectId") != null) {
                            issueData.put("projectId", context.get("projectId").toString());
                        }
                        issueData.put("title", resolveTemplateValue(action.getOrDefault("summary", "Auto-created issue"), context));
                        if (action.get("issueTypeId") != null) {
                            issueData.put("issueTypeId", action.get("issueTypeId").toString());
                        }
                        if (action.get("parentIssueId") != null) {
                            issueData.put("parentIssueId", action.get("parentIssueId").toString());
                        } else {
                            issueData.put("parentIssueId", issueId.toString());
                        }
                        integrationClient.createIssue(issueData, null);
                        executed++;
                    }
                    case "TRANSITION_STATUS" -> {
                        String transitionId = (String) action.get("transitionId");
                        UUID projectId = context.get("projectId") != null
                                ? UUID.fromString(context.get("projectId").toString()) : null;
                        if (transitionId != null && projectId != null) {
                            integrationClient.transitionIssue(issueId, projectId, transitionId);
                            executed++;
                        }
                    }
                    case "ADD_COMMENT" -> {
                        String content = (String) resolveTemplateValue(action.get("content"), context);
                        UUID userId = action.get("userId") != null
                                ? UUID.fromString(action.get("userId").toString()) : null;
                        integrationClient.addComment(issueId, content, userId);
                        executed++;
                    }
                    case "LINK_ISSUE" -> {
                        UUID targetIssueId = action.get("targetIssueId") != null
                                ? UUID.fromString(action.get("targetIssueId").toString()) : null;
                        UUID linkTypeId = action.get("linkTypeId") != null
                                ? UUID.fromString(action.get("linkTypeId").toString()) : null;
                        if (targetIssueId != null && linkTypeId != null) {
                            integrationClient.createIssueLink(issueId, targetIssueId, linkTypeId);
                            executed++;
                        }
                    }
                    case "CLONE_ISSUE" -> {
                        integrationClient.cloneIssue(issueId);
                        executed++;
                    }
                    case "ASSIGN_ISSUE" -> {
                        Object assigneeId = resolveTemplateValue(action.get("assigneeId"), context);
                        if (assigneeId != null) {
                            integrationClient.patchIssueFields(issueId, Map.of("assigneeId", assigneeId.toString()));
                            executed++;
                        }
                    }
                    case "ADD_LABEL" -> {
                        String label = (String) resolveTemplateValue(action.get("label"), context);
                        if (label != null && !label.isBlank()) {
                            integrationClient.addLabel(issueId, label);
                            executed++;
                        }
                    }
                    case "REMOVE_LABEL" -> {
                        String label = (String) resolveTemplateValue(action.get("label"), context);
                        if (label != null && !label.isBlank()) {
                            integrationClient.removeLabel(issueId, label);
                            executed++;
                        }
                    }
                    default -> log.warn("Unknown automation action type: {}", actionType);
                }
            } catch (Exception e) {
                log.error("Failed to execute action type={} on issue {}: {}", actionType, issueId, e.getMessage());
            }
        }
        return executed;
    }

    /**
     * Execute branch actions: iterate over linked issues or subtasks and apply branch actions to each.
     *
     * @return total number of branch actions executed across all target issues
     */
    private int executeBranch(AutomationRule rule, UUID triggerIssueId, Map<String, Object> context) {
        List<Map<String, Object>> branchActionsList = parseJsonArray(rule.getBranchActions());
        if (branchActionsList.isEmpty()) {
            return 0;
        }

        List<Map<String, Object>> targetIssues;

        if (AutomationRule.BRANCH_FOR_EACH_SUBTASK.equals(rule.getBranchType())) {
            targetIssues = integrationClient.fetchSubtasks(triggerIssueId);
        } else if (AutomationRule.BRANCH_FOR_EACH_LINKED_ISSUE.equals(rule.getBranchType())) {
            List<Map<String, Object>> allLinked = integrationClient.fetchLinkedIssuesForWorkflow(triggerIssueId);
            // Optionally filter by link type
            if (rule.getBranchLinkType() != null && !rule.getBranchLinkType().isBlank()) {
                targetIssues = allLinked.stream()
                        .filter(link -> {
                            String linkType = (String) link.get("linkTypeName");
                            return rule.getBranchLinkType().equalsIgnoreCase(linkType);
                        })
                        .collect(Collectors.toList());
            } else {
                targetIssues = allLinked;
            }
        } else {
            log.warn("Unknown branch type: {}", rule.getBranchType());
            return 0;
        }

        int totalExecuted = 0;
        for (Map<String, Object> targetIssue : targetIssues) {
            UUID targetIssueId = extractUuid(targetIssue, "id");
            if (targetIssueId == null) {
                targetIssueId = extractUuid(targetIssue, "issueId");
            }
            if (targetIssueId == null) {
                log.warn("Could not extract issue ID from branch target: {}", targetIssue);
                continue;
            }

            // Merge target issue data into context for template resolution
            Map<String, Object> branchContext = new HashMap<>(context);
            branchContext.putAll(targetIssue);

            totalExecuted += executeActions(branchActionsList, targetIssueId, branchContext);
        }

        log.debug("Branch actions executed: {} across {} target issues",
                totalExecuted, targetIssues.size());
        return totalExecuted;
    }

    // ===== Audit =====

    @Transactional(readOnly = true)
    public List<AutomationExecutionLogResponse> getExecutionLog(UUID ruleId) {
        log.debug("Fetching execution log for rule: {}", ruleId);
        return logRepo.findByRuleIdOrderByExecutedAtDesc(ruleId).stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    // ===== Manual Trigger =====

    @Transactional
    public void triggerManually(UUID ruleId, UUID issueId) {
        log.info("Manually triggering automation rule {} for issue {}", ruleId, issueId);

        AutomationRule rule = ruleRepo.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", ruleId));

        Map<String, Object> issueData = integrationClient.fetchIssue(issueId);
        UUID projectId = extractUuid(issueData, "projectId");

        evaluateRulesForSingleRule(rule, issueId, projectId, issueData);
    }

    /**
     * Evaluate and execute a single specific rule (used by manual trigger).
     */
    private void evaluateRulesForSingleRule(AutomationRule rule, UUID issueId, UUID projectId, Map<String, Object> context) {
        long startMs = System.currentTimeMillis();
        try {
            List<Map<String, Object>> conditionsList = parseJsonArray(rule.getConditions());
            if (!evaluateConditions(conditionsList, context)) {
                logExecution(rule, issueId, AutomationRule.TRIGGER_MANUAL,
                        AutomationExecutionLog.STATUS_SKIPPED, 0, "Conditions not met",
                        (int) (System.currentTimeMillis() - startMs));
                return;
            }

            List<Map<String, Object>> actionsList = parseJsonArray(rule.getActions());
            int actionsExecuted = executeActions(actionsList, issueId, context);

            if (rule.getBranchType() != null && !rule.getBranchType().isBlank()) {
                actionsExecuted += executeBranch(rule, issueId, context);
            }

            rule.setExecutionCount(rule.getExecutionCount() + 1);
            rule.setLastExecutedAt(LocalDateTime.now());
            rule.setLastError(null);
            ruleRepo.save(rule);

            logExecution(rule, issueId, AutomationRule.TRIGGER_MANUAL,
                    AutomationExecutionLog.STATUS_SUCCESS, actionsExecuted, null,
                    (int) (System.currentTimeMillis() - startMs));

        } catch (Exception e) {
            log.error("Manual trigger of rule '{}' failed: {}", rule.getName(), e.getMessage(), e);

            rule.setExecutionCount(rule.getExecutionCount() + 1);
            rule.setLastExecutedAt(LocalDateTime.now());
            rule.setLastError(e.getMessage());
            ruleRepo.save(rule);

            logExecution(rule, issueId, AutomationRule.TRIGGER_MANUAL,
                    AutomationExecutionLog.STATUS_FAILED, 0, e.getMessage(),
                    (int) (System.currentTimeMillis() - startMs));
        }
    }

    // ===== Helpers =====

    private void logExecution(AutomationRule rule, UUID issueId, String triggerEvent,
                              String status, int actionsExecuted, String errorMessage, int durationMs) {
        AutomationExecutionLog logEntry = AutomationExecutionLog.builder()
                .ruleId(rule.getId())
                .triggerIssueId(issueId)
                .triggerEvent(triggerEvent)
                .status(status)
                .actionsExecuted(actionsExecuted)
                .errorMessage(errorMessage)
                .executionDurationMs(durationMs)
                .build();
        logRepo.save(logEntry);
    }

    /**
     * Resolve template values like {{trigger.newValue}} or {{issueData.assigneeId}}.
     * Falls back to the raw value if no template pattern is found.
     */
    private Object resolveTemplateValue(Object value, Map<String, Object> context) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        if (str.startsWith("{{") && str.endsWith("}}")) {
            String key = str.substring(2, str.length() - 2).trim();
            // Support dot-notation: e.g. "trigger.newValue" -> look for "newValue"
            // or "issueData.assigneeId" -> look for "assigneeId"
            if (key.contains(".")) {
                String[] parts = key.split("\\.", 2);
                // Try the full key first, then the suffix
                Object resolved = context.get(key);
                if (resolved == null) {
                    resolved = context.get(parts[1]);
                }
                return resolved;
            }
            return context.getOrDefault(key, value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON object: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private UUID extractUuid(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) {
            return null;
        }
        if (val instanceof UUID uuid) {
            return uuid;
        }
        try {
            return UUID.fromString(val.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    // ===== Mappers =====

    private AutomationRuleResponse mapToResponse(AutomationRule rule) {
        return AutomationRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .projectId(rule.getProjectId())
                .isEnabled(rule.getIsEnabled())
                .triggerType(rule.getTriggerType())
                .triggerConfig(rule.getTriggerConfig())
                .conditions(rule.getConditions())
                .actions(rule.getActions())
                .branchType(rule.getBranchType())
                .branchLinkType(rule.getBranchLinkType())
                .branchActions(rule.getBranchActions())
                .executionCount(rule.getExecutionCount())
                .lastExecutedAt(rule.getLastExecutedAt())
                .lastError(rule.getLastError())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private AutomationExecutionLogResponse mapToLogResponse(AutomationExecutionLog logEntry) {
        return AutomationExecutionLogResponse.builder()
                .id(logEntry.getId())
                .ruleId(logEntry.getRuleId())
                .triggerIssueId(logEntry.getTriggerIssueId())
                .triggerEvent(logEntry.getTriggerEvent())
                .status(logEntry.getStatus())
                .actionsExecuted(logEntry.getActionsExecuted())
                .errorMessage(logEntry.getErrorMessage())
                .executionDurationMs(logEntry.getExecutionDurationMs())
                .executedAt(logEntry.getExecutedAt())
                .build();
    }
}
