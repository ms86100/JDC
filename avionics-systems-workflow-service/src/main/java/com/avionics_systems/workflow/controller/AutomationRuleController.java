package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.*;
import com.avionics_systems.workflow.service.AutomationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing automation rules.
 * Provides CRUD operations, manual triggering, and execution log retrieval.
 */
@RestController
@RequestMapping("/api/automation/rules")
@RequiredArgsConstructor
@Tag(name = "Automation Rules", description = "Manage event-driven automation rules (Automation for Avionics Systems DC 9.0+)")
public class AutomationRuleController {

    private final AutomationRuleService automationRuleService;

    @GetMapping
    @Operation(summary = "List all automation rules",
               description = "Returns all automation rules across all projects")
    public ResponseEntity<List<AutomationRuleResponse>> listAllRules() {
        return ResponseEntity.ok(automationRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get automation rule by ID",
               description = "Returns a specific automation rule by its ID")
    public ResponseEntity<AutomationRuleResponse> getRuleById(
            @Parameter(description = "Rule ID") @PathVariable UUID id) {
        return ResponseEntity.ok(automationRuleService.getRule(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List rules by project",
               description = "Returns all automation rules for a specific project")
    public ResponseEntity<List<AutomationRuleResponse>> getRulesByProject(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(automationRuleService.getRulesByProject(projectId));
    }

    @PostMapping
    @Operation(summary = "Create automation rule",
               description = "Creates a new automation rule with trigger, conditions, and actions")
    public ResponseEntity<AutomationRuleResponse> createRule(
            @Valid @RequestBody CreateAutomationRuleRequest request) {
        AutomationRuleResponse response = automationRuleService.createRule(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update automation rule",
               description = "Updates an existing automation rule. Only provided fields will be updated.")
    public ResponseEntity<AutomationRuleResponse> updateRule(
            @Parameter(description = "Rule ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateAutomationRuleRequest request) {
        return ResponseEntity.ok(automationRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete automation rule",
               description = "Deletes an automation rule and all its execution logs")
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Rule ID") @PathVariable UUID id) {
        automationRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable an automation rule",
               description = "Toggles the enabled state of an automation rule")
    public ResponseEntity<Void> toggleRule(
            @Parameter(description = "Rule ID") @PathVariable UUID id,
            @RequestParam boolean enabled) {
        automationRuleService.toggleRule(id, enabled);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/trigger/{issueId}")
    @Operation(summary = "Manually trigger an automation rule",
               description = "Manually triggers the execution of an automation rule against a specific issue")
    public ResponseEntity<Void> triggerManually(
            @Parameter(description = "Rule ID") @PathVariable UUID id,
            @Parameter(description = "Issue ID to run the rule against") @PathVariable UUID issueId) {
        automationRuleService.triggerManually(id, issueId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "Get execution log for a rule",
               description = "Returns the execution history for a specific automation rule, ordered by most recent first")
    public ResponseEntity<List<AutomationExecutionLogResponse>> getExecutionLog(
            @Parameter(description = "Rule ID") @PathVariable UUID id) {
        return ResponseEntity.ok(automationRuleService.getExecutionLog(id));
    }
}
