package com.jira.admin.controller;

import com.jira.admin.dto.*;
import com.jira.admin.entity.*;
import com.jira.admin.service.IssueAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Issue Administration Controller - Issue types, priorities, statuses, workflows, screens
 */
@RestController
@RequestMapping("/api/admin/issues")
@RequiredArgsConstructor
@Tag(name = "Issue Administration", description = "Issue Configuration API")
@CrossOrigin(origins = "*")
public class IssueAdministrationController {

    private final IssueAdministrationService issueAdministrationService;

    // ==================== Issue Types ====================

    @GetMapping("/issue-types")
    @Operation(summary = "Get all issue types")
    public ResponseEntity<List<IssueTypeEntity>> getIssueTypes() {
        return ResponseEntity.ok(issueAdministrationService.getIssueTypes());
    }

    @PostMapping("/issue-types")
    @Operation(summary = "Create issue type")
    public ResponseEntity<IssueTypeEntity> createIssueType(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createIssueType(data));
    }

    @PutMapping("/issue-types/{issueTypeId}")
    @Operation(summary = "Update issue type")
    public ResponseEntity<IssueTypeEntity> updateIssueType(
            @PathVariable String issueTypeId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(issueAdministrationService.updateIssueType(issueTypeId, updates));
    }

    // ==================== Priorities ====================

    @GetMapping("/priorities")
    @Operation(summary = "Get all priorities")
    public ResponseEntity<List<PriorityEntity>> getPriorities() {
        return ResponseEntity.ok(issueAdministrationService.getPriorities());
    }

    @PostMapping("/priorities")
    @Operation(summary = "Create priority")
    public ResponseEntity<PriorityEntity> createPriority(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createPriority(data));
    }

    @PutMapping("/priorities/{priorityId}")
    @Operation(summary = "Update priority")
    public ResponseEntity<PriorityEntity> updatePriority(
            @PathVariable String priorityId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(issueAdministrationService.updatePriority(priorityId, updates));
    }

    @DeleteMapping("/priorities/{priorityId}")
    @Operation(summary = "Delete priority")
    public ResponseEntity<Void> deletePriority(@PathVariable String priorityId) {
        issueAdministrationService.deletePriority(priorityId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Resolutions ====================

    @GetMapping("/resolutions")
    @Operation(summary = "Get all resolutions")
    public ResponseEntity<List<ResolutionEntity>> getResolutions() {
        return ResponseEntity.ok(issueAdministrationService.getResolutions());
    }

    @PostMapping("/resolutions")
    @Operation(summary = "Create resolution")
    public ResponseEntity<ResolutionEntity> createResolution(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createResolution(data));
    }

    // ==================== Statuses ====================

    @GetMapping("/statuses")
    @Operation(summary = "Get all statuses")
    public ResponseEntity<List<StatusResponse>> getStatuses() {
        List<StatusResponse> statuses = issueAdministrationService.getStatuses().stream()
                .map(StatusResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses/{statusId}")
    @Operation(summary = "Get status by ID")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable String statusId) {
        return ResponseEntity.ok(StatusResponse.fromEntity(issueAdministrationService.getStatus(statusId)));
    }

    @PostMapping("/statuses")
    @Operation(summary = "Create status")
    public ResponseEntity<StatusResponse> createStatus(@Valid @RequestBody CreateStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StatusResponse.fromEntity(issueAdministrationService.createStatus(request)));
    }

    @PutMapping("/statuses/{statusId}")
    @Operation(summary = "Update status")
    public ResponseEntity<StatusResponse> updateStatus(
            @PathVariable String statusId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(StatusResponse.fromEntity(issueAdministrationService.updateStatus(statusId, request)));
    }

    @DeleteMapping("/statuses/{statusId}")
    @Operation(summary = "Delete status")
    public ResponseEntity<Void> deleteStatus(@PathVariable String statusId) {
        issueAdministrationService.deleteStatus(statusId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Issue Type Schemes ====================

    @GetMapping("/issue-type-schemes")
    @Operation(summary = "Get all issue type schemes")
    public ResponseEntity<List<IssueTypeSchemeEntity>> getIssueTypeSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getIssueTypeSchemes());
    }

    @PostMapping("/issue-type-schemes")
    @Operation(summary = "Create issue type scheme")
    public ResponseEntity<IssueTypeSchemeEntity> createIssueTypeScheme(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createIssueTypeScheme(data));
    }

    // ==================== Workflows ====================

    @GetMapping("/workflows")
    @Operation(summary = "Get all workflows")
    public ResponseEntity<List<WorkflowEntity>> getWorkflows() {
        return ResponseEntity.ok(issueAdministrationService.getWorkflows());
    }

    @GetMapping("/workflows/{workflowId}")
    @Operation(summary = "Get workflow by ID")
    public ResponseEntity<WorkflowEntity> getWorkflow(@PathVariable String workflowId) {
        return issueAdministrationService.getWorkflowById(workflowId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/workflows")
    @Operation(summary = "Create workflow")
    public ResponseEntity<WorkflowEntity> createWorkflow(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createWorkflow(data));
    }

    @PutMapping("/workflows/{workflowId}")
    @Operation(summary = "Update workflow")
    public ResponseEntity<WorkflowEntity> updateWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(issueAdministrationService.updateWorkflow(workflowId, updates));
    }

    @PostMapping("/workflows/{workflowId}/publish")
    @Operation(summary = "Publish workflow")
    public ResponseEntity<WorkflowEntity> publishWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(issueAdministrationService.publishWorkflow(workflowId));
    }

    @PostMapping("/workflows/{workflowId}/draft")
    @Operation(summary = "Create draft from workflow")
    public ResponseEntity<WorkflowEntity> createDraftFromWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(issueAdministrationService.createDraftFromWorkflow(workflowId));
    }

    // ==================== Workflow Schemes ====================

    @GetMapping("/workflow-schemes")
    @Operation(summary = "Get all workflow schemes")
    public ResponseEntity<List<WorkflowSchemeEntity>> getWorkflowSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getWorkflowSchemes());
    }

    @PostMapping("/workflow-schemes")
    @Operation(summary = "Create workflow scheme")
    public ResponseEntity<WorkflowSchemeEntity> createWorkflowScheme(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createWorkflowScheme(data));
    }

    // ==================== Screens ====================

    @GetMapping("/screens")
    @Operation(summary = "Get all screens")
    public ResponseEntity<List<ScreenEntity>> getScreens() {
        return ResponseEntity.ok(issueAdministrationService.getScreens());
    }

    @PostMapping("/screens")
    @Operation(summary = "Create screen")
    public ResponseEntity<ScreenEntity> createScreen(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createScreen(data));
    }

    @PostMapping("/screens/{screenId}/tabs")
    @Operation(summary = "Add tab to screen")
    public ResponseEntity<ScreenEntity> addScreenTab(
            @PathVariable String screenId,
            @RequestParam String tabName) {
        return ResponseEntity.ok(issueAdministrationService.addScreenTab(screenId, tabName));
    }

    @PostMapping("/screens/{screenId}/tabs/{tabIndex}/fields")
    @Operation(summary = "Add field to screen tab")
    public ResponseEntity<ScreenEntity> addFieldToTab(
            @PathVariable String screenId,
            @PathVariable int tabIndex,
            @RequestParam String fieldId) {
        return ResponseEntity.ok(issueAdministrationService.addFieldToTab(screenId, tabIndex, fieldId));
    }

    // ==================== Screen Schemes ====================

    @GetMapping("/screen-schemes")
    @Operation(summary = "Get all screen schemes")
    public ResponseEntity<List<ScreenSchemeEntity>> getScreenSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getScreenSchemes());
    }

    @PostMapping("/screen-schemes")
    @Operation(summary = "Create screen scheme")
    public ResponseEntity<ScreenSchemeEntity> createScreenScheme(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createScreenScheme(data));
    }

    // ==================== Issue Type Screen Schemes ====================

    @GetMapping("/issue-type-screen-schemes")
    @Operation(summary = "Get all issue type screen schemes")
    public ResponseEntity<List<IssueTypeScreenSchemeEntity>> getIssueTypeScreenSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getIssueTypeScreenSchemes());
    }

    // ==================== Permission Schemes ====================

    @GetMapping("/permission-schemes")
    @Operation(summary = "Get all permission schemes")
    public ResponseEntity<List<PermissionSchemeEntity>> getPermissionSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getPermissionSchemes());
    }

    @PostMapping("/permission-schemes")
    @Operation(summary = "Create permission scheme")
    public ResponseEntity<PermissionSchemeEntity> createPermissionScheme(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createPermissionScheme(data));
    }

    // ==================== Notification Schemes ====================

    @GetMapping("/notification-schemes")
    @Operation(summary = "Get all notification schemes")
    public ResponseEntity<List<NotificationSchemeEntity>> getNotificationSchemes() {
        return ResponseEntity.ok(issueAdministrationService.getNotificationSchemes());
    }

    @PostMapping("/notification-schemes")
    @Operation(summary = "Create notification scheme")
    public ResponseEntity<NotificationSchemeEntity> createNotificationScheme(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(issueAdministrationService.createNotificationScheme(data));
    }
}
