package com.jira.admin.controller;

import com.jira.admin.dto.*;
import com.jira.admin.entity.*;
import com.jira.admin.service.AdminService;
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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "System Administration API")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final IssueAdministrationService issueAdminService;

    // ==================== System Settings ====================

    @GetMapping("/settings")
    @Operation(summary = "Get all system settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(adminService.getSystemSettings());
    }

    @GetMapping("/settings/{category}")
    @Operation(summary = "Get settings by category")
    public ResponseEntity<Map<String, Object>> getSettingsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(adminService.getSystemSettingsByCategory(category));
    }

    @PutMapping("/settings/{key}")
    @Operation(summary = "Update a single setting")
    public ResponseEntity<SystemSettingsEntity> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.updateSetting(key, body.get("value")));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update multiple settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(adminService.updateSettings(updates));
    }

    // ==================== User Management ====================

    // NOTE: User management endpoints are in UserManagementController at /api/admin/users

    @GetMapping("/statistics")
    @Operation(summary = "Get platform statistics")
    public ResponseEntity<Map<String, Object>> getPlatformStatistics() {
        return ResponseEntity.ok(adminService.getUserStatistics());
    }

    // ==================== Project Settings ====================

    @GetMapping("/projects")
    @Operation(summary = "Get all projects")
    public ResponseEntity<List<ProjectEntity>> getProjects() {
        return ResponseEntity.ok(adminService.getProjects());
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectEntity> getProject(@PathVariable String projectId) {
        return adminService.getProject(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/projects/{projectId}")
    @Operation(summary = "Update project settings")
    public ResponseEntity<ProjectEntity> updateProject(
            @PathVariable String projectId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(adminService.updateProject(projectId, updates));
    }

    // ==================== Appearance ====================

    @GetMapping("/appearance")
    @Operation(summary = "Get appearance settings")
    public ResponseEntity<AppearanceEntity> getAppearance() {
        return ResponseEntity.ok(adminService.getAppearance());
    }

    @PutMapping("/appearance")
    @Operation(summary = "Update appearance settings")
    public ResponseEntity<AppearanceEntity> updateAppearance(@RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(adminService.updateAppearance(updates));
    }

    // ==================== Licensing ====================

    @GetMapping("/license")
    @Operation(summary = "Get license information")
    public ResponseEntity<LicenseEntity> getLicense() {
        return ResponseEntity.ok(adminService.getLicense());
    }

    // ==================== System Health (use /api/admin/health for full health) ====================

    @GetMapping("/system-health")
    @Operation(summary = "Get system health status")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(adminService.getSystemHealth());
    }

    // ==================== Statuses (Issue Administration) ====================

    @GetMapping("/statuses")
    @Operation(summary = "List all statuses", description = "Returns all non-archived statuses sorted by sequence")
    public ResponseEntity<List<StatusResponse>> listStatuses(
            @RequestParam(required = false) String category) {
        List<StatusResponse> statuses;
        if (category != null && !category.isEmpty()) {
            statuses = issueAdminService.getStatusesByCategory(category).stream()
                    .map(StatusResponse::fromEntity)
                    .toList();
        } else {
            statuses = issueAdminService.getStatuses().stream()
                    .map(StatusResponse::fromEntity)
                    .toList();
        }
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/statuses/{statusId}")
    @Operation(summary = "Get status by ID")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable String statusId) {
        StatusResponse response = StatusResponse.fromEntity(issueAdminService.getStatus(statusId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/statuses")
    @Operation(summary = "Create a new status")
    public ResponseEntity<StatusResponse> createStatus(
            @Valid @RequestBody CreateStatusRequest request) {
        StatusResponse response = StatusResponse.fromEntity(issueAdminService.createStatus(request));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/statuses/{statusId}")
    @Operation(summary = "Update an existing status")
    public ResponseEntity<StatusResponse> updateStatus(
            @PathVariable String statusId,
            @Valid @RequestBody UpdateStatusRequest request) {
        StatusResponse response = StatusResponse.fromEntity(issueAdminService.updateStatus(statusId, request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/statuses/{statusId}/archive")
    @Operation(summary = "Archive a status (soft delete)")
    public ResponseEntity<Void> archiveStatus(@PathVariable String statusId) {
        issueAdminService.archiveStatus(statusId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/statuses/{statusId}/restore")
    @Operation(summary = "Restore an archived status")
    public ResponseEntity<Void> restoreStatus(@PathVariable String statusId) {
        issueAdminService.restoreStatus(statusId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/statuses/{statusId}")
    @Operation(summary = "Delete a status permanently")
    public ResponseEntity<Void> deleteStatus(@PathVariable String statusId) {
        issueAdminService.deleteStatus(statusId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Workflow Admin Proxies (fallback for /api/admin/workflows/* when routed here) ====================

    @GetMapping("/workflows/conditions/definitions")
    @Operation(summary = "Get condition definitions (proxied to workflow-service)")
    public ResponseEntity<List<Map<String, Object>>> getConditionDefinitions() {
        return ResponseEntity.ok(issueAdminService.getConditionDefinitions());
    }

    @GetMapping("/workflows/validators/definitions")
    @Operation(summary = "Get validator definitions (proxied to workflow-service)")
    public ResponseEntity<List<Map<String, Object>>> getValidatorDefinitions() {
        return ResponseEntity.ok(issueAdminService.getValidatorDefinitions());
    }

    @GetMapping("/workflows/post-functions/definitions")
    @Operation(summary = "Get post-function definitions (proxied to workflow-service)")
    public ResponseEntity<List<Map<String, Object>>> getPostFunctionDefinitions() {
        return ResponseEntity.ok(issueAdminService.getPostFunctionDefinitions());
    }

    @GetMapping("/workflows/screens")
    @Operation(summary = "List workflow screens (proxied to workflow-service)")
    public ResponseEntity<List<Map<String, Object>>> listWorkflowScreens(
            @RequestParam(required = false) String screenType) {
        return ResponseEntity.ok(issueAdminService.listWorkflowScreens(screenType));
    }
}