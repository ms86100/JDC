package com.avionics_systems.notification.service;

import com.avionics_systems.notification.dto.*;
import com.avionics_systems.notification.entity.*;
import com.avionics_systems.notification.exception.ResourceNotFoundException;
import com.avionics_systems.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final AutomationRuleRepository ruleRepository;
    private final AutomationTriggerRepository triggerRepository;
    private final AutomationConditionRepository conditionRepository;
    private final AutomationActionRepository actionRepository;
    private final AutomationLogRepository logRepository;

    @Value("${app.automation.defaults.logical-group:ALL}")
    private String defaultLogicalGroup;

    @Value("${app.automation.defaults.failure-handling:CONTINUE}")
    private String defaultFailureHandling;

    @Value("${app.automation.status.success:SUCCESS}")
    private String statusSuccess;

    @Value("${app.automation.status.failed:FAILED}")
    private String statusFailed;

    @Transactional
    public AutomationRuleResponse createRule(CreateAutomationRuleRequest request, UUID createdBy) {
        log.info("Creating automation rule: {}", request.getName());

        AutomationRule rule = AutomationRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .createdBy(createdBy)
                .triggerType(request.getTriggerType())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .isSystemRule(request.getIsSystemRule() != null ? request.getIsSystemRule() : false)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created automation rule with id: {}", rule.getId());

        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public AutomationRuleResponse getRule(UUID ruleId) {
        log.debug("Fetching automation rule: {}", ruleId);

        AutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + ruleId));

        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> getAllRules() {
        log.debug("Fetching all automation rules");
        return ruleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> getRulesByProject(UUID projectId) {
        log.debug("Fetching automation rules for project: {}", projectId);
        return ruleRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> getEnabledRules() {
        log.debug("Fetching enabled automation rules");
        return ruleRepository.findByEnabled(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AutomationRuleResponse updateRule(UUID ruleId, CreateAutomationRuleRequest request) {
        log.info("Updating automation rule: {}", ruleId);

        AutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + ruleId));

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        if (request.getProjectId() != null) {
            rule.setProjectId(request.getProjectId());
        }
        rule.setTriggerType(request.getTriggerType());
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getOrderIndex() != null) {
            rule.setOrderIndex(request.getOrderIndex());
        }

        rule = ruleRepository.save(rule);
        log.info("Updated automation rule: {}", ruleId);

        return mapToResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        log.info("Deleting automation rule: {}", ruleId);

        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Automation rule not found: " + ruleId);
        }

        triggerRepository.deleteAllByRuleId(ruleId);
        conditionRepository.deleteAllByRuleId(ruleId);
        actionRepository.deleteAllByRuleId(ruleId);
        logRepository.deleteAllByRuleId(ruleId);
        ruleRepository.deleteById(ruleId);

        log.info("Deleted automation rule: {}", ruleId);
    }

    @Transactional
    public AutomationRuleResponse toggleRule(UUID ruleId, boolean enabled) {
        log.info("Toggling automation rule {} to enabled={}", ruleId, enabled);

        AutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + ruleId));

        rule.setEnabled(enabled);
        rule = ruleRepository.save(rule);

        return mapToResponse(rule);
    }

    // Trigger Management
    @Transactional
    public AutomationTriggerResponse addTrigger(UUID ruleId, CreateAutomationTriggerRequest request) {
        log.info("Adding trigger to rule: {}", ruleId);

        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Automation rule not found: " + ruleId);
        }

        AutomationTrigger trigger = AutomationTrigger.builder()
                .ruleId(ruleId)
                .triggerType(request.getTriggerType())
                .triggerConfig(request.getTriggerConfig())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        trigger = triggerRepository.save(trigger);
        log.info("Added trigger to rule: {} - triggerId: {}", ruleId, trigger.getId());

        return mapToTriggerResponse(trigger);
    }

    @Transactional(readOnly = true)
    public List<AutomationTriggerResponse> getTriggersByRule(UUID ruleId) {
        log.debug("Fetching triggers for rule: {}", ruleId);
        return triggerRepository.findByRuleId(ruleId).stream()
                .map(this::mapToTriggerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTrigger(UUID ruleId, UUID triggerId) {
        log.info("Deleting trigger {} from rule: {}", triggerId, ruleId);

        AutomationTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trigger not found: " + triggerId));

        if (!trigger.getRuleId().equals(ruleId)) {
            throw new IllegalArgumentException("Trigger does not belong to this rule");
        }

        triggerRepository.deleteById(triggerId);
        log.info("Deleted trigger: {} from rule: {}", triggerId, ruleId);
    }

    // Condition Management
    @Transactional
    public AutomationConditionResponse addCondition(UUID ruleId, CreateAutomationConditionRequest request) {
        log.info("Adding condition to rule: {}", ruleId);

        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Automation rule not found: " + ruleId);
        }

        AutomationCondition condition = AutomationCondition.builder()
                .ruleId(ruleId)
                .conditionType(request.getConditionType())
                .fieldName(request.getFieldName())
                .operator(request.getOperator())
                .conditionValue(request.getConditionValue())
                .conditionConfig(request.getConditionConfig())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .logicalGroup(request.getLogicalGroup() != null ? request.getLogicalGroup() : defaultLogicalGroup)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .build();

        condition = conditionRepository.save(condition);
        log.info("Added condition to rule: {} - conditionId: {}", ruleId, condition.getId());

        return mapToConditionResponse(condition);
    }

    @Transactional(readOnly = true)
    public List<AutomationConditionResponse> getConditionsByRule(UUID ruleId) {
        log.debug("Fetching conditions for rule: {}", ruleId);
        return conditionRepository.findByRuleId(ruleId).stream()
                .map(this::mapToConditionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCondition(UUID ruleId, UUID conditionId) {
        log.info("Deleting condition {} from rule: {}", conditionId, ruleId);

        AutomationCondition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found: " + conditionId));

        if (!condition.getRuleId().equals(ruleId)) {
            throw new IllegalArgumentException("Condition does not belong to this rule");
        }

        conditionRepository.deleteById(conditionId);
        log.info("Deleted condition: {} from rule: {}", conditionId, ruleId);
    }

    // Action Management
    @Transactional
    public AutomationActionResponse addAction(UUID ruleId, CreateAutomationActionRequest request) {
        log.info("Adding action to rule: {}", ruleId);

        if (!ruleRepository.existsById(ruleId)) {
            throw new ResourceNotFoundException("Automation rule not found: " + ruleId);
        }

        AutomationAction action = AutomationAction.builder()
                .ruleId(ruleId)
                .actionType(request.getActionType())
                .actionConfig(request.getActionConfig())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .failureHandling(request.getFailureHandling() != null ? request.getFailureHandling() : defaultFailureHandling)
                .build();

        action = actionRepository.save(action);
        log.info("Added action to rule: {} - actionId: {}", ruleId, action.getId());

        return mapToActionResponse(action);
    }

    @Transactional(readOnly = true)
    public List<AutomationActionResponse> getActionsByRule(UUID ruleId) {
        log.debug("Fetching actions for rule: {}", ruleId);
        return actionRepository.findByRuleId(ruleId).stream()
                .map(this::mapToActionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAction(UUID ruleId, UUID actionId) {
        log.info("Deleting action {} from rule: {}", actionId, ruleId);

        AutomationAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Action not found: " + actionId));

        if (!action.getRuleId().equals(ruleId)) {
            throw new IllegalArgumentException("Action does not belong to this rule");
        }

        actionRepository.deleteById(actionId);
        log.info("Deleted action: {} from rule: {}", actionId, ruleId);
    }

    // Log Management
    @Transactional
    public AutomationLogResponse createLog(CreateAutomationLogRequest request) {
        log.debug("Creating automation log for rule: {}", request.getRuleId());

        AutomationLog logEntry = AutomationLog.builder()
                .ruleId(request.getRuleId())
                .triggerType(request.getTriggerType())
                .triggerEventId(request.getTriggerEventId())
                .status(request.getStatus())
                .message(request.getMessage())
                .conditionsEvaluated(request.getConditionsEvaluated())
                .conditionsPassed(request.getConditionsPassed())
                .actionsExecuted(request.getActionsExecuted())
                .actionsFailed(request.getActionsFailed())
                .executionTimeMs(request.getExecutionTimeMs())
                .errorDetails(request.getErrorDetails())
                .contextData(request.getContextData())
                .build();

        logEntry = logRepository.save(logEntry);

        // Update rule execution stats
        if (request.getRuleId() != null) {
            ruleRepository.updateExecutionStats(request.getRuleId(), request.getStatus());
        }

        return mapToLogResponse(logEntry);
    }

    @Transactional(readOnly = true)
    public List<AutomationLogResponse> getLogsByRule(UUID ruleId, int limit) {
        log.debug("Fetching logs for rule: {}", ruleId);
        return logRepository.findRecentByRuleId(ruleId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AutomationLogResponse> getLogsByStatus(String status, int limit) {
        log.debug("Fetching logs by status: {}", status);
        return logRepository.findByStatus(status, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getRuleSuccessCount(UUID ruleId) {
        return logRepository.countByRuleIdAndStatus(ruleId, statusSuccess);
    }

    @Transactional(readOnly = true)
    public long getRuleFailureCount(UUID ruleId) {
        return logRepository.countByRuleIdAndStatus(ruleId, statusFailed);
    }

    @Transactional
    public int cleanupOldLogs(int daysToKeep) {
        log.info("Cleaning up automation logs older than {} days", daysToKeep);
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(daysToKeep);
        int deleted = logRepository.deleteOlderThan(cutoff);
        log.info("Deleted {} old automation logs", deleted);
        return deleted;
    }

    // Mapping methods
    private AutomationRuleResponse mapToResponse(AutomationRule rule) {
        List<AutomationTriggerResponse> triggers = triggerRepository.findByRuleId(rule.getId())
                .stream().map(this::mapToTriggerResponse).collect(Collectors.toList());

        List<AutomationConditionResponse> conditions = conditionRepository.findByRuleId(rule.getId())
                .stream().map(this::mapToConditionResponse).collect(Collectors.toList());

        List<AutomationActionResponse> actions = actionRepository.findByRuleId(rule.getId())
                .stream().map(this::mapToActionResponse).collect(Collectors.toList());

        return AutomationRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .projectId(rule.getProjectId())
                .createdBy(rule.getCreatedBy())
                .triggerType(rule.getTriggerType())
                .enabled(rule.getEnabled())
                .isSystemRule(rule.getIsSystemRule())
                .executionCount(rule.getExecutionCount())
                .lastExecutedAt(rule.getLastExecutedAt())
                .lastStatus(rule.getLastStatus())
                .orderIndex(rule.getOrderIndex())
                .triggers(triggers)
                .conditions(conditions)
                .actions(actions)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private AutomationTriggerResponse mapToTriggerResponse(AutomationTrigger trigger) {
        return AutomationTriggerResponse.builder()
                .id(trigger.getId())
                .ruleId(trigger.getRuleId())
                .triggerType(trigger.getTriggerType())
                .triggerConfig(trigger.getTriggerConfig())
                .enabled(trigger.getEnabled())
                .orderIndex(trigger.getOrderIndex())
                .createdAt(trigger.getCreatedAt())
                .build();
    }

    private AutomationConditionResponse mapToConditionResponse(AutomationCondition condition) {
        return AutomationConditionResponse.builder()
                .id(condition.getId())
                .ruleId(condition.getRuleId())
                .conditionType(condition.getConditionType())
                .fieldName(condition.getFieldName())
                .operator(condition.getOperator())
                .conditionValue(condition.getConditionValue())
                .conditionConfig(condition.getConditionConfig())
                .enabled(condition.getEnabled())
                .logicalGroup(condition.getLogicalGroup())
                .orderIndex(condition.getOrderIndex())
                .createdAt(condition.getCreatedAt())
                .build();
    }

    private AutomationActionResponse mapToActionResponse(AutomationAction action) {
        return AutomationActionResponse.builder()
                .id(action.getId())
                .ruleId(action.getRuleId())
                .actionType(action.getActionType())
                .actionConfig(action.getActionConfig())
                .enabled(action.getEnabled())
                .orderIndex(action.getOrderIndex())
                .failureHandling(action.getFailureHandling())
                .createdAt(action.getCreatedAt())
                .build();
    }

    private AutomationLogResponse mapToLogResponse(AutomationLog logEntry) {
        return AutomationLogResponse.builder()
                .id(logEntry.getId())
                .ruleId(logEntry.getRuleId())
                .triggerType(logEntry.getTriggerType())
                .triggerEventId(logEntry.getTriggerEventId())
                .status(logEntry.getStatus())
                .message(logEntry.getMessage())
                .conditionsEvaluated(logEntry.getConditionsEvaluated())
                .conditionsPassed(logEntry.getConditionsPassed())
                .actionsExecuted(logEntry.getActionsExecuted())
                .actionsFailed(logEntry.getActionsFailed())
                .executionTimeMs(logEntry.getExecutionTimeMs())
                .errorDetails(logEntry.getErrorDetails())
                .contextData(logEntry.getContextData())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}