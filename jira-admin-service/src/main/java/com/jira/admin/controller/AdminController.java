package com.jira.admin.controller;

import com.jira.admin.entity.*;
import com.jira.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserEntity>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(adminService.getUsers(search, status, role));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserEntity> getUser(@PathVariable String userId) {
        return adminService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserEntity> createUser(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(adminService.createUser(data));
    }

    @PutMapping("/users/{userId}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserEntity> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(adminService.updateUser(userId, updates));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
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

    // ==================== System Health ====================

    @GetMapping("/health")
    @Operation(summary = "Get system health status")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(adminService.getSystemHealth());
    }
}