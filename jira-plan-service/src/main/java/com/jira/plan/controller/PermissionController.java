package com.jira.plan.controller;

import com.jira.plan.dto.request.CreateBoardPermissionRequest;
import com.jira.plan.dto.request.CreateProjectSprintPermissionRequest;
import com.jira.plan.dto.response.BoardPermissionResponse;
import com.jira.plan.dto.response.ProjectSprintPermissionResponse;
import com.jira.plan.service.BoardPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Board and Project-level Permissions.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PermissionController {

    private final BoardPermissionService permissionService;

    // Board Permissions

    @GetMapping("/boards/{boardId}/permissions")
    public ResponseEntity<List<BoardPermissionResponse>> getBoardPermissions(@PathVariable UUID boardId) {
        return ResponseEntity.ok(permissionService.getBoardPermissions(boardId));
    }

    @PostMapping("/boards/{boardId}/permissions")
    public ResponseEntity<BoardPermissionResponse> grantBoardPermission(
            @PathVariable UUID boardId,
            @RequestBody CreateBoardPermissionRequest request,
            @RequestParam(required = false) UUID grantedBy) {
        return ResponseEntity.ok(permissionService.grantBoardPermission(boardId, request, grantedBy));
    }

    @DeleteMapping("/boards/permissions/{permissionId}")
    public ResponseEntity<Void> revokeBoardPermission(@PathVariable UUID permissionId) {
        permissionService.revokeBoardPermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/boards/{boardId}/permissions/check")
    public ResponseEntity<Boolean> checkBoardPermission(
            @PathVariable UUID boardId,
            @RequestParam String permission,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.hasPermission(boardId, permission, userId));
    }

    @GetMapping("/boards/{boardId}/permissions/effective")
    public ResponseEntity<List<String>> getEffectivePermissions(
            @PathVariable UUID boardId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.getEffectivePermissions(boardId, userId));
    }

    @GetMapping("/boards/{boardId}/access")
    public ResponseEntity<Boolean> checkBoardAccess(
            @PathVariable UUID boardId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.hasBoardAccess(boardId, userId));
    }

    // Project Sprint Permissions

    @GetMapping("/projects/{projectId}/sprint-permissions")
    public ResponseEntity<List<ProjectSprintPermissionResponse>> getProjectSprintPermissions(
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(permissionService.getProjectSprintPermissions(projectId));
    }

    @PostMapping("/projects/{projectId}/sprint-permissions")
    public ResponseEntity<ProjectSprintPermissionResponse> grantProjectSprintPermission(
            @PathVariable UUID projectId,
            @RequestBody CreateProjectSprintPermissionRequest request,
            @RequestParam(required = false) UUID grantedBy) {
        return ResponseEntity.ok(permissionService.grantProjectSprintPermission(projectId, request, grantedBy));
    }

    @DeleteMapping("/projects/sprint-permissions/{permissionId}")
    public ResponseEntity<Void> revokeProjectSprintPermission(@PathVariable UUID permissionId) {
        permissionService.revokeProjectSprintPermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/sprint-permissions/check")
    public ResponseEntity<Boolean> checkProjectSprintPermission(
            @PathVariable UUID projectId,
            @RequestParam String permissionKey,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.hasProjectSprintPermission(projectId, permissionKey, userId));
    }

    // Specific permission checks

    @GetMapping("/projects/{projectId}/can-manage-sprints")
    public ResponseEntity<Boolean> canManageSprints(
            @PathVariable UUID projectId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.canManageSprints(projectId, userId));
    }

    @GetMapping("/projects/{projectId}/can-start-stop-sprints")
    public ResponseEntity<Boolean> canStartStopSprints(
            @PathVariable UUID projectId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.canStartStopSprints(projectId, userId));
    }

    @GetMapping("/projects/{projectId}/can-edit-sprint-name-goal")
    public ResponseEntity<Boolean> canEditSprintNameAndGoal(
            @PathVariable UUID projectId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(permissionService.canEditSprintNameAndGoal(projectId, userId));
    }
}