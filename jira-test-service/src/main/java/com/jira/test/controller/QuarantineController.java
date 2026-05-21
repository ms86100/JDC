package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.entity.QuarantineTransition;
import com.jira.test.service.QuarantineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quarantine")
@RequiredArgsConstructor
@Tag(name = "Test Quarantine System", description = "APIs for managing quarantined tests")
public class QuarantineController {

    private final QuarantineService quarantineService;

    // ==================== Quarantine Operations ====================

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Quarantine a test")
    public ResponseEntity<QuarantineResponse> quarantineTest(@Valid @RequestBody QuarantineRequest request) {
        QuarantineResponse response = quarantineService.quarantineTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get quarantine status for a test")
    public ResponseEntity<QuarantineResponse> getQuarantine(@PathVariable UUID testId, @RequestParam UUID projectId) {
        QuarantineResponse response = quarantineService.getQuarantine(testId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all quarantined tests for a project")
    public ResponseEntity<List<QuarantineResponse>> getQuarantinedTests(@PathVariable UUID projectId) {
        List<QuarantineResponse> quarantines = quarantineService.getQuarantinedTests(projectId);
        return ResponseEntity.ok(quarantines);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get quarantined tests by status")
    public ResponseEntity<List<QuarantineResponse>> getByStatus(@PathVariable String status, @RequestParam UUID projectId) {
        List<QuarantineResponse> quarantines = quarantineService.getQuarantinedTestsByStatus(status);
        return ResponseEntity.ok(quarantines);
    }

    @PutMapping("/{quarantineId}/status")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update quarantine status")
    public ResponseEntity<QuarantineResponse> updateStatus(
            @PathVariable UUID quarantineId,
            @RequestParam String status,
            @RequestParam(required = false) String reason,
            @RequestParam UUID projectId) {
        QuarantineResponse response = quarantineService.updateStatus(quarantineId, status, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{quarantineId}/restore")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Restore a test from quarantine")
    public ResponseEntity<QuarantineResponse> restoreTest(
            @PathVariable UUID quarantineId,
            @RequestParam(required = false) String reason,
            @RequestParam UUID projectId) {
        QuarantineResponse response = quarantineService.restoreTest(quarantineId, reason, null);
        return ResponseEntity.ok(response);
    }

    // ==================== Dashboard ====================

    @GetMapping("/dashboard")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get quarantine dashboard")
    public ResponseEntity<QuarantineDashboardResponse> getDashboard(@RequestParam UUID projectId) {
        QuarantineDashboardResponse dashboard = quarantineService.getDashboard(projectId);
        return ResponseEntity.ok(dashboard);
    }

    // ==================== Transitions ====================

    @GetMapping("/{quarantineId}/transitions")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get status transition history")
    public ResponseEntity<List<QuarantineTransition>> getTransitions(@PathVariable UUID quarantineId, @RequestParam UUID projectId) {
        List<QuarantineTransition> transitions = quarantineService.getTransitions(quarantineId);
        return ResponseEntity.ok(transitions);
    }

    // ==================== Rules Management ====================

    @PostMapping("/rules")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Create a quarantine rule")
    public ResponseEntity<QuarantineRuleResponse> createRule(@Valid @RequestBody QuarantineRuleRequest request) {
        QuarantineRuleResponse rule = quarantineService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping("/rules/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get quarantine rules for a project")
    public ResponseEntity<List<QuarantineRuleResponse>> getRules(@PathVariable UUID projectId) {
        List<QuarantineRuleResponse> rules = quarantineService.getRules(projectId);
        return ResponseEntity.ok(rules);
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    @Operation(summary = "Delete a quarantine rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId, @RequestParam UUID projectId) {
        quarantineService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}