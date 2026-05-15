package com.jira.plan.controller;

import com.jira.plan.dto.request.CreateSprintRequest;
import com.jira.plan.dto.request.UpdateIssueColumnRequest;
import com.jira.plan.dto.response.SprintBurndownResponse;
import com.jira.plan.dto.response.SprintIssueResponse;
import com.jira.plan.dto.response.SprintResponse;
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

    // Sprint CRUD

    @GetMapping("/boards/{boardId}/sprints")
    public ResponseEntity<List<SprintResponse>> getSprints(@PathVariable UUID boardId) {
        return ResponseEntity.ok(sprintService.getSprintsByBoardId(boardId));
    }

    @GetMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> getSprint(@PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.getSprintById(sprintId));
    }

    @PostMapping("/boards/{boardId}/sprints")
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable UUID boardId,
            @RequestBody CreateSprintRequest request) {
        return ResponseEntity.ok(sprintService.createSprint(boardId, request));
    }

    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable UUID sprintId,
            @RequestBody CreateSprintRequest request) {
        return ResponseEntity.ok(sprintService.updateSprint(sprintId, request));
    }

    @DeleteMapping("/sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable UUID sprintId) {
        // Could implement soft delete or proper deletion logic
        return ResponseEntity.noContent().build();
    }

    // Sprint Lifecycle

    @PostMapping("/sprints/{sprintId}/start")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(sprintService.startSprint(sprintId, userId));
    }

    @PostMapping("/sprints/{sprintId}/close")
    public ResponseEntity<SprintResponse> closeSprint(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(sprintService.closeSprint(sprintId, userId));
    }

    @PostMapping("/sprints/{sprintId}/abandon")
    public ResponseEntity<SprintResponse> abandonSprint(
            @PathVariable UUID sprintId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(sprintService.abandonSprint(sprintId, userId));
    }

    // Sprint Issues

    @GetMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<List<SprintIssueResponse>> getSprintIssues(@PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.getSprintIssues(sprintId));
    }

    @PostMapping("/sprints/{sprintId}/issues")
    public ResponseEntity<SprintIssueResponse> addIssueToSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID planItemId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(sprintService.addIssueToSprint(sprintId, planItemId, userId));
    }

    @DeleteMapping("/sprints/{sprintId}/issues/{planItemId}")
    public ResponseEntity<Void> removeIssueFromSprint(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestParam(required = false) UUID userId) {
        sprintService.removeIssueFromSprint(sprintId, planItemId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sprints/{sprintId}/issues/{planItemId}/complete")
    public ResponseEntity<SprintIssueResponse> completeIssue(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId) {
        return ResponseEntity.ok(sprintService.completeIssue(sprintId, planItemId));
    }

    @PutMapping("/sprints/{sprintId}/issues/{planItemId}/column")
    public ResponseEntity<SprintIssueResponse> updateIssueColumn(
            @PathVariable UUID sprintId,
            @PathVariable UUID planItemId,
            @RequestBody UpdateIssueColumnRequest request) {
        return ResponseEntity.ok(sprintService.updateIssueColumn(sprintId, planItemId, request.getColumnName()));
    }

    // Burndown

    @GetMapping("/sprints/{sprintId}/burndown")
    public ResponseEntity<SprintBurndownResponse> getSprintBurndown(@PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.getSprintBurndown(sprintId));
    }

    @PostMapping("/sprints/{sprintId}/burndown/snapshot")
    public ResponseEntity<Void> takeBurndownSnapshot(@PathVariable UUID sprintId) {
        sprintService.takeBurndownSnapshot(sprintId);
        return ResponseEntity.ok().build();
    }

    // Velocity

    @GetMapping("/boards/{boardId}/velocity")
    public ResponseEntity<Double> getAverageVelocity(@PathVariable UUID boardId) {
        Double velocity = sprintService.getAverageVelocity(boardId);
        return ResponseEntity.ok(velocity != null ? velocity : 0.0);
    }
}