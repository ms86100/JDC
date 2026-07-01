package com.jira.notification.controller;

import com.jira.notification.dto.*;
import com.jira.notification.service.AutomationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@Slf4j
public class AutomationController {

    private final AutomationService automationService;

    // Rule endpoints
    @PostMapping("/rules")
    public ResponseEntity<AutomationRuleResponse> createRule(
            @Valid @RequestBody CreateAutomationRuleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("POST /api/automation/rules - Creating automation rule: {}", request.getName());
        UUID createdBy = userId != null ? userId : UUID.randomUUID();
        AutomationRuleResponse response = automationService.createRule(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules/{id}")
    public ResponseEntity<AutomationRuleResponse> getRule(@PathVariable UUID id) {
        log.info("GET /api/automation/rules/{} - Fetching automation rule", id);
        AutomationRuleResponse response = automationService.getRule(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AutomationRuleResponse>> getAllRules() {
        log.info("GET /api/automation/rules - Fetching all automation rules");
        List<AutomationRuleResponse> response = automationService.getAllRules();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/project/{projectId}")
    public ResponseEntity<List<AutomationRuleResponse>> getRulesByProject(@PathVariable UUID projectId) {
        log.info("GET /api/automation/rules/project/{} - Fetching rules for project", projectId);
        List<AutomationRuleResponse> response = automationService.getRulesByProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/enabled")
    public ResponseEntity<List<AutomationRuleResponse>> getEnabledRules() {
        log.info("GET /api/automation/rules/enabled - Fetching enabled rules");
        List<AutomationRuleResponse> response = automationService.getEnabledRules();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AutomationRuleResponse> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAutomationRuleRequest request) {
        log.info("PUT /api/automation/rules/{} - Updating automation rule", id);
        AutomationRuleResponse response = automationService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        log.info("DELETE /api/automation/rules/{} - Deleting automation rule", id);
        automationService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<AutomationRuleResponse> toggleRule(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        log.info("PATCH /api/automation/rules/{}/toggle - Toggling rule to enabled={}", id, enabled);
        AutomationRuleResponse response = automationService.toggleRule(id, enabled);
        return ResponseEntity.ok(response);
    }

    // Trigger endpoints
    @PostMapping("/rules/{ruleId}/triggers")
    public ResponseEntity<AutomationTriggerResponse> addTrigger(
            @PathVariable UUID ruleId,
            @Valid @RequestBody CreateAutomationTriggerRequest request) {
        log.info("POST /api/automation/rules/{}/triggers - Adding trigger", ruleId);
        AutomationTriggerResponse response = automationService.addTrigger(ruleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules/{ruleId}/triggers")
    public ResponseEntity<List<AutomationTriggerResponse>> getTriggersByRule(@PathVariable UUID ruleId) {
        log.info("GET /api/automation/rules/{}/triggers - Fetching triggers", ruleId);
        List<AutomationTriggerResponse> response = automationService.getTriggersByRule(ruleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{ruleId}/triggers/{triggerId}")
    public ResponseEntity<Void> deleteTrigger(
            @PathVariable UUID ruleId,
            @PathVariable UUID triggerId) {
        log.info("DELETE /api/automation/rules/{}/triggers/{} - Deleting trigger", ruleId, triggerId);
        automationService.deleteTrigger(ruleId, triggerId);
        return ResponseEntity.noContent().build();
    }

    // Condition endpoints
    @PostMapping("/rules/{ruleId}/conditions")
    public ResponseEntity<AutomationConditionResponse> addCondition(
            @PathVariable UUID ruleId,
            @Valid @RequestBody CreateAutomationConditionRequest request) {
        log.info("POST /api/automation/rules/{}/conditions - Adding condition", ruleId);
        AutomationConditionResponse response = automationService.addCondition(ruleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules/{ruleId}/conditions")
    public ResponseEntity<List<AutomationConditionResponse>> getConditionsByRule(@PathVariable UUID ruleId) {
        log.info("GET /api/automation/rules/{}/conditions - Fetching conditions", ruleId);
        List<AutomationConditionResponse> response = automationService.getConditionsByRule(ruleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{ruleId}/conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(
            @PathVariable UUID ruleId,
            @PathVariable UUID conditionId) {
        log.info("DELETE /api/automation/rules/{}/conditions/{} - Deleting condition", ruleId, conditionId);
        automationService.deleteCondition(ruleId, conditionId);
        return ResponseEntity.noContent().build();
    }

    // Action endpoints
    @PostMapping("/rules/{ruleId}/actions")
    public ResponseEntity<AutomationActionResponse> addAction(
            @PathVariable UUID ruleId,
            @Valid @RequestBody CreateAutomationActionRequest request) {
        log.info("POST /api/automation/rules/{}/actions - Adding action", ruleId);
        AutomationActionResponse response = automationService.addAction(ruleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules/{ruleId}/actions")
    public ResponseEntity<List<AutomationActionResponse>> getActionsByRule(@PathVariable UUID ruleId) {
        log.info("GET /api/automation/rules/{}/actions - Fetching actions", ruleId);
        List<AutomationActionResponse> response = automationService.getActionsByRule(ruleId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{ruleId}/actions/{actionId}")
    public ResponseEntity<Void> deleteAction(
            @PathVariable UUID ruleId,
            @PathVariable UUID actionId) {
        log.info("DELETE /api/automation/rules/{}/actions/{} - Deleting action", ruleId, actionId);
        automationService.deleteAction(ruleId, actionId);
        return ResponseEntity.noContent().build();
    }

    // Log endpoints
    @PostMapping("/logs")
    public ResponseEntity<AutomationLogResponse> createLog(@Valid @RequestBody CreateAutomationLogRequest request) {
        log.info("POST /api/automation/logs - Creating automation log");
        AutomationLogResponse response = automationService.createLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/logs/rule/{ruleId}")
    public ResponseEntity<List<AutomationLogResponse>> getLogsByRule(
            @PathVariable UUID ruleId,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("GET /api/automation/logs/rule/{} - Fetching logs for rule", ruleId);
        List<AutomationLogResponse> response = automationService.getLogsByRule(ruleId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/status/{status}")
    public ResponseEntity<List<AutomationLogResponse>> getLogsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("GET /api/automation/logs/status/{} - Fetching logs by status", status);
        List<AutomationLogResponse> response = automationService.getLogsByStatus(status, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/{ruleId}/stats")
    public ResponseEntity<AutomationRuleStatsResponse> getRuleStats(@PathVariable UUID ruleId) {
        log.info("GET /api/automation/rules/{}/stats - Fetching rule stats", ruleId);
        long successCount = automationService.getRuleSuccessCount(ruleId);
        long failureCount = automationService.getRuleFailureCount(ruleId);
        AutomationRuleStatsResponse stats = new AutomationRuleStatsResponse(ruleId, successCount, failureCount, successCount + failureCount);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/logs/cleanup")
    public ResponseEntity<CleanupResponse> cleanupOldLogs(@RequestParam(defaultValue = "30") int daysToKeep) {
        log.info("DELETE /api/automation/logs/cleanup - Cleaning up logs older than {} days", daysToKeep);
        int deleted = automationService.cleanupOldLogs(daysToKeep);
        return ResponseEntity.ok(new CleanupResponse(deleted));
    }

    // DTO classes for responses
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AutomationRuleStatsResponse {
        private UUID ruleId;
        private long successCount;
        private long failureCount;
        private long totalCount;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CleanupResponse {
        private int deletedCount;
    }
}