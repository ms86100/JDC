package com.avionics_systems.component.controller;

import com.avionics_systems.component.dto.*;
import com.avionics_systems.component.entity.ComponentAssignmentRule;
import com.avionics_systems.component.entity.ComponentAuditLog;
import com.avionics_systems.component.service.ComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Tag(name = "Component Management", description = "Enterprise-grade component management API")
public class ComponentController {

    private final ComponentService componentService;

    // ========== COMPONENT CRUD ==========

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all components for a project")
    public ResponseEntity<List<ComponentResponse>> getComponentsByProject(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(componentService.getComponentsByProject(projectId, includeArchived));
    }

    @GetMapping("/{componentId}")
    @Operation(summary = "Get component by ID")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.getComponentById(componentId));
    }

    @PostMapping
    @Operation(summary = "Create a new component")
    public ResponseEntity<ComponentResponse> createComponent(@Valid @RequestBody CreateComponentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(componentService.createComponent(request));
    }

    @PutMapping("/{componentId}")
    @Operation(summary = "Update a component")
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable UUID componentId,
            @Valid @RequestBody UpdateComponentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(componentService.updateComponent(componentId, request, userId));
    }

    @DeleteMapping("/{componentId}")
    @Operation(summary = "Delete a component")
    public ResponseEntity<Void> deleteComponent(@PathVariable UUID componentId) {
        componentService.deleteComponent(componentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{componentId}/restore")
    @Operation(summary = "Restore a deleted component")
    public ResponseEntity<ComponentResponse> restoreComponent(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.restoreComponent(componentId));
    }

    // ========== ARCHIVE OPERATIONS ==========

    @PostMapping("/{componentId}/archive")
    @Operation(summary = "Archive a component")
    public ResponseEntity<ComponentResponse> archiveComponent(
            @PathVariable UUID componentId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(componentService.archiveComponent(componentId, userId));
    }

    @PostMapping("/{componentId}/unarchive")
    @Operation(summary = "Unarchive a component")
    public ResponseEntity<ComponentResponse> unarchiveComponent(
            @PathVariable UUID componentId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(componentService.unarchiveComponent(componentId, userId));
    }

    // ========== ISSUE COMPONENT LINKING ==========

    @PostMapping("/issue")
    @Operation(summary = "Assign component to an issue")
    public ResponseEntity<Void> assignComponent(
            @RequestParam UUID issueId,
            @RequestParam UUID componentId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        componentService.assignComponent(issueId, componentId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/issue")
    @Operation(summary = "Remove component from an issue")
    public ResponseEntity<Void> removeComponent(
            @RequestParam UUID issueId,
            @RequestParam UUID componentId) {
        componentService.removeComponent(issueId, componentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/issue/{issueId}")
    @Operation(summary = "Get components for an issue")
    public ResponseEntity<List<UUID>> getIssueComponents(@PathVariable UUID issueId) {
        return ResponseEntity.ok(componentService.getIssueComponents(issueId));
    }

    // ========== BULK OPERATIONS ==========

    @PostMapping("/bulk-assign")
    @Operation(summary = "Bulk assign issues to a component")
    public ResponseEntity<Integer> bulkAssignComponent(@RequestBody BulkAssignComponentRequest request) {
        int count = componentService.bulkAssignComponent(request.getIssueIds(), request.getComponentId(), null);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/bulk-remove")
    @Operation(summary = "Bulk remove issues from a component")
    public ResponseEntity<Integer> bulkRemoveComponent(@RequestBody BulkAssignComponentRequest request) {
        int count = componentService.bulkRemoveComponent(request.getIssueIds(), request.getComponentId());
        return ResponseEntity.ok(count);
    }

    // ========== OWNERSHIP TRANSFER ==========

    @PostMapping("/{componentId}/transfer-ownership")
    @Operation(summary = "Transfer component ownership")
    public ResponseEntity<ComponentResponse> transferOwnership(
            @PathVariable UUID componentId,
            @Valid @RequestBody TransferOwnershipRequest request) {
        return ResponseEntity.ok(componentService.transferOwnership(componentId, request));
    }

    @GetMapping("/{componentId}/ownership-history")
    @Operation(summary = "Get component ownership history")
    public ResponseEntity<List<OwnershipTransferResponse>> getOwnershipHistory(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.getOwnershipHistory(componentId));
    }

    // ========== METRICS ==========

    @GetMapping("/{componentId}/metrics")
    @Operation(summary = "Get component metrics history")
    public ResponseEntity<List<ComponentMetricsResponse>> getComponentMetrics(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.getComponentMetrics(componentId));
    }

    @PostMapping("/{componentId}/metrics/snapshot")
    @Operation(summary = "Record a metrics snapshot")
    public ResponseEntity<ComponentMetricsResponse> recordMetricsSnapshot(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.recordMetricsSnapshot(componentId));
    }

    // ========== ASSIGNMENT RULES ==========

    @GetMapping("/{componentId}/assignment-rules")
    @Operation(summary = "Get component assignment rules")
    public ResponseEntity<List<ComponentAssignmentRuleResponse>> getAssignmentRules(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.getAssignmentRules(componentId));
    }

    @PostMapping("/{componentId}/assignment-rules")
    @Operation(summary = "Create an assignment rule")
    public ResponseEntity<ComponentAssignmentRuleResponse> createAssignmentRule(
            @PathVariable UUID componentId,
            @RequestBody ComponentAssignmentRule rule,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(componentService.createAssignmentRule(componentId, rule, userId));
    }

    @DeleteMapping("/assignment-rules/{ruleId}")
    @Operation(summary = "Delete an assignment rule")
    public ResponseEntity<Void> deleteAssignmentRule(@PathVariable UUID ruleId) {
        componentService.deleteAssignmentRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    // ========== AUDIT ==========

    @GetMapping("/{componentId}/audit")
    @Operation(summary = "Get component audit logs")
    public ResponseEntity<List<ComponentAuditLog>> getComponentAuditLogs(@PathVariable UUID componentId) {
        return ResponseEntity.ok(componentService.getComponentAuditLogs(componentId));
    }
}
