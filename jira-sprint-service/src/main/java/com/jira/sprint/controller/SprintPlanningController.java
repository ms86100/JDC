package com.jira.sprint.controller;

import com.jira.board.dto.BoardIssueResponse;
import com.jira.sprint.dto.CreateSprintRequest;
import com.jira.sprint.dto.SprintResponse;
import com.jira.sprint.service.SprintPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Sprint Planning Board operations.
 * Provides endpoints for managing sprints, backlogs, and sprint planning.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/sprints")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sprint Planning", description = "Sprint planning and backlog management")
public class SprintPlanningController {

    private final SprintPlanningService sprintPlanningService;

    @PostMapping
    @Operation(summary = "Create a new sprint for the board")
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable UUID boardId,
            @RequestParam UUID projectId,
            @Valid @RequestBody CreateSprintRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Creating sprint for board {}: {}", boardId, request.getName());
        SprintResponse response = sprintPlanningService.createSprint(boardId, projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all sprints for the board")
    public ResponseEntity<List<SprintPlanningService.SprintBoardResponse>> getBoardSprints(
            @PathVariable UUID boardId) {
        log.info("Getting sprints for board: {}", boardId);
        return ResponseEntity.ok(sprintPlanningService.getBoardSprints(boardId));
    }

    @GetMapping("/{sprintId}/planning")
    @Operation(summary = "Get sprint planning data including backlog context")
    public ResponseEntity<SprintPlanningService.SprintPlanningDataResponse> getSprintPlanningData(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId) {
        log.info("Getting planning data for sprint {} on board {}", sprintId, boardId);
        return ResponseEntity.ok(sprintPlanningService.getSprintPlanningData(boardId, sprintId));
    }

    @GetMapping("/{sprintId}/issues")
    @Operation(summary = "Get issues assigned to a sprint")
    public ResponseEntity<List<BoardIssueResponse>> getSprintIssues(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId) {
        log.info("Getting issues for sprint {} on board {}", sprintId, boardId);
        return ResponseEntity.ok(sprintPlanningService.getSprintIssues(sprintId));
    }

    @PostMapping("/{sprintId}/issues")
    @Operation(summary = "Add issues from backlog to sprint")
    public ResponseEntity<SprintPlanningService.SprintIssuesResponse> addIssuesToSprint(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId,
            @RequestBody AddIssuesRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        log.info("Adding {} issues to sprint {}", request.getIssueIds().size(), sprintId);
        return ResponseEntity.ok(
                sprintPlanningService.addIssuesToSprint(sprintId, request.getIssueIds(), userId));
    }

    @DeleteMapping("/{sprintId}/issues")
    @Operation(summary = "Remove issues from sprint (move to backlog)")
    public ResponseEntity<Void> removeIssuesFromSprint(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId,
            @RequestBody List<UUID> issueIds) {
        log.info("Removing {} issues from sprint {}", issueIds.size(), sprintId);
        sprintPlanningService.removeIssuesFromSprint(sprintId, issueIds);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sprintId}/issues/reorder")
    @Operation(summary = "Reorder issues within a sprint")
    public ResponseEntity<Void> reorderSprintIssues(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId,
            @RequestBody List<UUID> issueIds) {
        log.info("Reordering {} issues in sprint {}", issueIds.size(), sprintId);
        sprintPlanningService.reorderSprintIssues(sprintId, issueIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sprintId}/start")
    @Operation(summary = "Start a sprint (change from PLANNING to ACTIVE)")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId) {
        log.info("Starting sprint {} on board {}", sprintId, boardId);
        return ResponseEntity.ok(sprintPlanningService.startSprint(sprintId));
    }

    @PostMapping("/{sprintId}/complete")
    @Operation(summary = "Complete/close a sprint")
    public ResponseEntity<SprintResponse> completeSprint(
            @PathVariable UUID boardId,
            @PathVariable UUID sprintId) {
        log.info("Completing sprint {} on board {}", sprintId, boardId);
        return ResponseEntity.ok(sprintPlanningService.completeSprint(sprintId));
    }

    @GetMapping("/backlog")
    @Operation(summary = "Get backlog issues for the board")
    public ResponseEntity<SprintPlanningService.BacklogResponse> getBacklog(
            @PathVariable UUID boardId,
            @RequestParam UUID projectId,
            @RequestParam(required = false) String jql) {
        log.info("Getting backlog for board {} (project {})", boardId, projectId);
        return ResponseEntity.ok(sprintPlanningService.getBacklog(boardId, projectId, jql));
    }

    // Request DTOs
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AddIssuesRequest {
        private List<UUID> issueIds;
    }
}