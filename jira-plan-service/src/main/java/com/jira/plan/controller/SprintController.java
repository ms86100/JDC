package com.jira.plan.controller;

import com.jira.plan.dto.request.CreateSprintRequest;
import com.jira.plan.dto.request.UpdateIssueColumnRequest;
import com.jira.plan.dto.response.SprintBurndownResponse;
import com.jira.plan.dto.response.SprintIssueResponse;
import com.jira.plan.dto.response.SprintResponse;
import com.jira.plan.exception.ForbiddenException;
import com.jira.plan.service.BoardPermissionService;
import com.jira.plan.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Sprint management.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;
    private final BoardPermissionService boardPermissionService;

    // Sprint CRUD

    @GetMapping("/boards/{boardId}/sprints")
    public ResponseEntity<List<SprintResponse>> getSprints(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getSprintsByBoardId(boardId));
    }

    @GetMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> getSprint(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprint);
    }

    @PostMapping("/boards/{boardId}/sprints")
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable UUID boardId,
            @RequestBody CreateSprintRequest request,
            @RequestParam UUID userId) {
        checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.createSprint(boardId, request));
    }

    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable UUID sprintId,
            @RequestBody CreateSprintRequest request,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.updateSprint(sprintId, request));
    }

    @DeleteMapping("/sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.noContent().build();
    }

    // Sprint Lifecycle

    @PostMapping("/sprints/{sprintId}/start")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.startSprint(sprintId, userId));
    }

    @PostMapping("/sprints/{sprintId}/close")
    public ResponseEntity<SprintResponse> closeSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.closeSprint(sprintId, userId));
    }

    @PostMapping("/sprints/{sprintId}/abandon")
    public ResponseEntity<SprintResponse> abandonSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.abandonSprint(sprintId, userId));
    }

    // Sprint Issues

    @GetMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<List<SprintIssueResponse>> getSprintIssues(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getSprintIssues(sprintId));
    }

    @PostMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<SprintIssueResponse> addIssueToSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID planItemId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.addIssueToSprint(sprintId, planItemId, userId));
    }

    @DeleteMapping("/sprints/{sprintId}/issues/{planItemId}")
    public ResponseEntity<Void> removeIssueFromSprint(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        sprintService.removeIssueFromSprint(sprintId, planItemId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sprints/{sprintId}/issues/{planItemId}/complete")
    public ResponseEntity<SprintIssueResponse> completeIssue(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        }
        return ResponseEntity.ok(sprintService.completeIssue(sprintId, planItemId));
    }

    @PutMapping("/sprints/{sprintId}/issues/{planItemId}/column")
    public ResponseEntity<SprintIssueResponse> updateIssueColumn(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestBody UpdateIssueColumnRequest request,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        }
        return ResponseEntity.ok(sprintService.updateIssueColumn(sprintId, planItemId, request.getColumnName()));
    }

    // Burndown

    @GetMapping("/sprints/{sprintId}/burndown")
    public ResponseEntity<SprintBurndownResponse> getSprintBurndown(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getSprintBurndown(sprintId));
    }

    @PostMapping("/sprints/{sprintId}/burndown/snapshot")
    public ResponseEntity<Void> takeBurndownSnapshot(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        sprintService.takeBurndownSnapshot(sprintId);
        return ResponseEntity.ok().build();
    }

    // Velocity

    @GetMapping("/boards/{boardId}/velocity")
    public ResponseEntity<Double> getAverageVelocity(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        Double velocity = sprintService.getAverageVelocity(boardId);
        return ResponseEntity.ok(velocity != null ? velocity : 0.0);
    }

    // ============ Legacy permission-protected endpoints (DEPRECATED - use standard endpoints with userId) ============

    /**
     * @deprecated Use POST /api/plans/boards/{boardId}/sprints with userId parameter instead.
     * This endpoint is kept for backwards compatibility.
     */
    @Deprecated
    @PostMapping("/boards/{boardId}/sprints/managed")
    public ResponseEntity<SprintResponse> createSprintManaged(
            @PathVariable UUID boardId,
            @RequestBody CreateSprintRequest request,
            @RequestParam UUID userId) {
        checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.createSprint(boardId, request));
    }

    /**
     * @deprecated Use POST /api/plans/sprints/{sprintId}/start with userId parameter instead.
     * This endpoint is kept for backwards compatibility.
     */
    @Deprecated
    @PostMapping("/sprints/{sprintId}/start/managed")
    public ResponseEntity<SprintResponse> startSprintManaged(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.startSprint(sprintId, userId));
    }

    /**
     * @deprecated Use POST /api/plans/sprints/{sprintId}/close with userId parameter instead.
     * This endpoint is kept for backwards compatibility.
     */
    @Deprecated
    @PostMapping("/sprints/{sprintId}/close/managed")
    public ResponseEntity<SprintResponse> closeSprintManaged(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.closeSprint(sprintId, userId));
    }

    /**
     * @deprecated Use POST /api/plans/sprints/{sprintId}/issues with userId parameter instead.
     * This endpoint is kept for backwards compatibility.
     */
    @Deprecated
    @PostMapping("/sprints/{sprintId}/issues/managed")
    public ResponseEntity<SprintIssueResponse> addIssueToSprintManaged(
            @PathVariable UUID sprintId,
            @RequestParam UUID planItemId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.addIssueToSprint(sprintId, planItemId, userId));
    }

    private void checkBoardPermission(UUID boardId, UUID userId, String permission) {
        if (!boardPermissionService.hasPermission(boardId, permission, userId)) {
            throw new ForbiddenException("User does not have " + permission + " permission on this board");
        }
    }
}