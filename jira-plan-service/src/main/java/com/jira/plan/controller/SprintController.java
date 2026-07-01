package com.jira.plan.controller;

import com.jira.plan.dto.request.*;
import com.jira.plan.dto.response.*;
import com.jira.plan.exception.ForbiddenException;
import com.jira.plan.service.BoardPermissionService;
import com.jira.plan.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;
    private final BoardPermissionService boardPermissionService;

    // ==================== SPRINT CRUD ====================

    // Gap 8: Added pagination and state filter support
    @GetMapping("/boards/{boardId}/sprints")
    public ResponseEntity<?> getSprints(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer startAt,
            @RequestParam(required = false) Integer maxResults) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        if (startAt != null || maxResults != null || state != null) {
            int start = startAt != null ? startAt : 0;
            int max = maxResults != null ? maxResults : 50;
            return ResponseEntity.ok(sprintService.getSprintsByBoardIdPaginated(boardId, state, start, max));
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

    // Gap 1: Partial update
    @PostMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> partialUpdateSprint(
            @PathVariable UUID sprintId,
            @RequestBody PartialUpdateSprintRequest request,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.partialUpdateSprint(sprintId, request));
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

    // ==================== SPRINT LIFECYCLE ====================

    @PostMapping("/sprints/{sprintId}/start")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.startSprint(sprintId, userId));
    }

    // Gap 20: Accept optional body for incomplete issue move
    @PostMapping("/sprints/{sprintId}/close")
    public ResponseEntity<SprintResponse> closeSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId,
            @RequestBody(required = false) CloseSprintRequest request) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.closeSprint(sprintId, userId, request));
    }

    @PostMapping("/sprints/{sprintId}/abandon")
    public ResponseEntity<SprintResponse> abandonSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.abandonSprint(sprintId, userId));
    }

    // Gap 16: Reopen sprint
    @PostMapping("/sprints/{sprintId}/reopen")
    public ResponseEntity<SprintResponse> reopenSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.reopenSprint(sprintId, userId));
    }

    // Gap 2: Sprint swap/reorder
    @PostMapping("/sprints/{sprintId}/swap")
    public ResponseEntity<Void> swapSprintOrder(
            @PathVariable UUID sprintId,
            @RequestBody @Valid SwapSprintRequest request,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        sprintService.swapSprintOrder(sprintId, request);
        return ResponseEntity.noContent().build();
    }

    // ==================== SPRINT ISSUES ====================

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

    // Gap 7: Board-scoped sprint issues
    @GetMapping("/boards/{boardId}/sprints/{sprintId}/issues")
    public ResponseEntity<List<SprintIssueResponse>> getBoardSprintIssues(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getBoardSprintIssues(boardId, sprintId));
    }

    // Gap 9: JQL filtering and pagination
    @GetMapping("/sprints/{sprintId}/issues/search")
    public ResponseEntity<PaginatedResponse<SprintIssueResponse>> searchSprintIssues(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) String jql,
            @RequestParam(defaultValue = "0") int startAt,
            @RequestParam(defaultValue = "50") int maxResults,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getSprintIssuesFiltered(sprintId, jql, startAt, maxResults));
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

    // Gap 4: Bulk move issues
    @PostMapping("/sprints/{sprintId}/issues/bulk-move")
    public ResponseEntity<BulkMoveIssuesResponse> bulkMoveIssues(
            @PathVariable UUID sprintId,
            @RequestBody @Valid BulkMoveIssuesRequest request,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.bulkMoveIssuesToSprint(sprintId, request, userId));
    }

    // Gap 5: Move issues to backlog
    @PostMapping("/backlog/issues")
    public ResponseEntity<Void> moveIssuesToBacklog(
            @RequestBody MoveToBacklogRequest request,
            @RequestParam UUID userId) {
        sprintService.moveIssuesToBacklog(request, userId);
        return ResponseEntity.ok().build();
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

    // Gap 6: Rank issue
    @PutMapping("/sprints/issues/{planItemId}/rank")
    public ResponseEntity<SprintIssueResponse> rankIssue(
            @PathVariable UUID planItemId,
            @RequestBody RankIssueRequest request,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(request.getSprintId());
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.rankIssue(planItemId, request));
    }

    // Gap 19: Toggle flag on issue
    @PostMapping("/sprints/{sprintId}/issues/{planItemId}/flag")
    public ResponseEntity<SprintIssueResponse> toggleFlag(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestBody Map<String, Object> body,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        boolean flagged = body.containsKey("flagged") ? (Boolean) body.get("flagged") : true;
        String reason = body.containsKey("reason") ? (String) body.get("reason") : null;
        return ResponseEntity.ok(sprintService.toggleFlag(sprintId, planItemId, flagged, reason));
    }

    // Gap 19: Get closed sprints for an issue
    @GetMapping("/issues/{planItemId}/closed-sprints")
    public ResponseEntity<List<SprintResponse>> getClosedSprintsForIssue(
            @PathVariable UUID planItemId) {
        return ResponseEntity.ok(sprintService.getClosedSprintsForIssue(planItemId));
    }

    // Gap 17: Issue estimation
    @GetMapping("/boards/{boardId}/issues/{planItemId}/estimation")
    public ResponseEntity<IssueEstimationResponse> getEstimation(
            @PathVariable UUID boardId,
            @PathVariable UUID planItemId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getIssueEstimation(boardId, planItemId));
    }

    @PutMapping("/boards/{boardId}/issues/{planItemId}/estimation")
    public ResponseEntity<IssueEstimationResponse> updateEstimation(
            @PathVariable UUID boardId,
            @PathVariable UUID planItemId,
            @RequestBody Map<String, Integer> body,
            @RequestParam UUID userId) {
        checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.updateIssueEstimation(boardId, planItemId, body.get("value"), userId));
    }

    // ==================== SPRINT PROPERTIES (Gap 3) ====================

    @GetMapping("/sprints/{sprintId}/properties")
    public ResponseEntity<List<SprintPropertyResponse>> getSprintProperties(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.getSprintProperties(sprintId));
    }

    @GetMapping("/sprints/{sprintId}/properties/{propertyKey}")
    public ResponseEntity<SprintPropertyResponse> getSprintProperty(
            @PathVariable UUID sprintId,
            @PathVariable String propertyKey) {
        return ResponseEntity.ok(sprintService.getSprintProperty(sprintId, propertyKey));
    }

    @PutMapping("/sprints/{sprintId}/properties/{propertyKey}")
    public ResponseEntity<SprintPropertyResponse> setSprintProperty(
            @PathVariable UUID sprintId,
            @PathVariable String propertyKey,
            @RequestBody String value,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        return ResponseEntity.ok(sprintService.setSprintProperty(sprintId, propertyKey, value));
    }

    @DeleteMapping("/sprints/{sprintId}/properties/{propertyKey}")
    public ResponseEntity<Void> deleteSprintProperty(
            @PathVariable UUID sprintId,
            @PathVariable String propertyKey,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_EDIT);
        sprintService.deleteSprintProperty(sprintId, propertyKey);
        return ResponseEntity.noContent().build();
    }

    // ==================== BURNDOWN & VELOCITY ====================

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

    // Gap 11: Event-based burndown
    @GetMapping("/sprints/{sprintId}/burndown/events")
    public ResponseEntity<EventBurndownResponse> getEventBasedBurndown(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getEventBasedBurndown(sprintId));
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

    // Gap 12: Velocity chart
    @GetMapping("/boards/{boardId}/velocity/chart")
    public ResponseEntity<VelocityChartResponse> getVelocityChart(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getVelocityChart(boardId));
    }

    // ==================== REPORTS (Gap 10) ====================

    @GetMapping("/sprints/{sprintId}/report")
    public ResponseEntity<SprintReportResponse> getSprintReport(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        if (userId != null && sprint.getBoardId() != null) {
            checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getSprintReport(sprintId));
    }

    // ==================== ANALYTICS ====================

    // Gap 13: Cumulative flow diagram
    @GetMapping("/boards/{boardId}/cfd")
    public ResponseEntity<CumulativeFlowResponse> getCumulativeFlowDiagram(
            @PathVariable UUID boardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getCumulativeFlowDiagram(boardId, from, to));
    }

    // Gap 14: Control chart
    @GetMapping("/boards/{boardId}/control-chart")
    public ResponseEntity<ControlChartResponse> getControlChart(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getControlChart(boardId));
    }

    // Gap 15: Epic burndown
    @GetMapping("/epics/{epicPlanItemId}/burndown")
    public ResponseEntity<EpicBurndownResponse> getEpicBurndown(
            @PathVariable UUID epicPlanItemId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(sprintService.getEpicBurndown(epicPlanItemId));
    }

    // ==================== BOARD FEATURES (Gap 18) ====================

    @GetMapping("/boards/{boardId}/features")
    public ResponseEntity<BoardFeaturesResponse> getBoardFeatures(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getBoardFeatures(boardId));
    }

    @PutMapping("/boards/{boardId}/features")
    public ResponseEntity<BoardFeaturesResponse> updateBoardFeatures(
            @PathVariable UUID boardId,
            @RequestBody BoardFeaturesRequest request,
            @RequestParam UUID userId) {
        checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_ADMIN);
        return ResponseEntity.ok(sprintService.updateBoardFeatures(boardId, request));
    }

    // ==================== BACKLOG PLANNING (Gap 22) ====================

    @GetMapping("/boards/{boardId}/backlog-planning")
    public ResponseEntity<BacklogPlanningResponse> getBacklogPlanningView(
            @PathVariable UUID boardId,
            @RequestParam(required = false) UUID userId) {
        if (userId != null) {
            checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_VIEW);
        }
        return ResponseEntity.ok(sprintService.getBacklogPlanningView(boardId));
    }

    // ==================== LEGACY (DEPRECATED) ====================

    @Deprecated
    @PostMapping("/boards/{boardId}/sprints/managed")
    public ResponseEntity<SprintResponse> createSprintManaged(
            @PathVariable UUID boardId,
            @RequestBody CreateSprintRequest request,
            @RequestParam UUID userId) {
        checkBoardPermission(boardId, userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.createSprint(boardId, request));
    }

    @Deprecated
    @PostMapping("/sprints/{sprintId}/start/managed")
    public ResponseEntity<SprintResponse> startSprintManaged(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.startSprint(sprintId, userId));
    }

    @Deprecated
    @PostMapping("/sprints/{sprintId}/close/managed")
    public ResponseEntity<SprintResponse> closeSprintManaged(
            @PathVariable UUID sprintId,
            @RequestParam UUID userId) {
        var sprint = sprintService.getSprintById(sprintId);
        checkBoardPermission(sprint.getBoardId(), userId, BoardPermissionService.PERMISSION_MANAGE_SPRINTS);
        return ResponseEntity.ok(sprintService.closeSprint(sprintId, userId));
    }

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
