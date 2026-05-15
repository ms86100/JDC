package com.jira.sprint.controller;

import com.jira.sprint.dto.CreateSprintRequest;
import com.jira.sprint.dto.SprintResponse;
import com.jira.sprint.dto.UpdateSprintRequest;
import com.jira.sprint.service.SprintService;
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
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Sprint management API")
@CrossOrigin(origins = "*")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @Operation(summary = "Create sprint", description = "Create a new sprint")
    public ResponseEntity<SprintResponse> createSprint(
            @Valid @RequestBody CreateSprintRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        SprintResponse response = sprintService.createSprint(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get sprints", description = "Get all sprints for a project")
    public ResponseEntity<List<SprintResponse>> getSprints(
            @RequestParam(required = false) UUID projectId) {
        List<SprintResponse> sprints = projectId != null
                ? sprintService.getSprintsByProject(projectId)
                : sprintService.getSprintsByProject(null);
        return ResponseEntity.ok(sprints);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active sprint", description = "Get the active sprint for a project")
    public ResponseEntity<SprintResponse> getActiveSprint(
            @RequestParam UUID projectId) {
        SprintResponse sprint = sprintService.getActiveSprint(projectId);
        return sprint != null ? ResponseEntity.ok(sprint) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{sprintId}")
    @Operation(summary = "Get sprint", description = "Get a sprint by ID")
    public ResponseEntity<SprintResponse> getSprint(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.getSprint(sprintId));
    }

    @PutMapping("/{sprintId}")
    @Operation(summary = "Update sprint", description = "Update a sprint")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable UUID sprintId,
            @Valid @RequestBody UpdateSprintRequest request) {
        return ResponseEntity.ok(sprintService.updateSprint(sprintId, request));
    }

    @PostMapping("/{sprintId}/start")
    @Operation(summary = "Start sprint", description = "Start a sprint")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.startSprint(sprintId));
    }

    @PostMapping("/{sprintId}/complete")
    @Operation(summary = "Complete sprint", description = "Complete a sprint")
    public ResponseEntity<SprintResponse> completeSprint(
            @PathVariable UUID sprintId) {
        return ResponseEntity.ok(sprintService.completeSprint(sprintId));
    }

    @DeleteMapping("/{sprintId}")
    @Operation(summary = "Delete sprint", description = "Delete a sprint")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable UUID sprintId) {
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/issues")
    @Operation(summary = "Add issue to sprint", description = "Add an issue to a sprint")
    public ResponseEntity<Void> addIssueToSprint(
            @PathVariable UUID sprintId,
            @RequestParam UUID issueId) {
        sprintService.addIssueToSprint(sprintId, issueId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{sprintId}/issues/{issueId}")
    @Operation(summary = "Remove issue from sprint", description = "Remove an issue from a sprint")
    public ResponseEntity<Void> removeIssueFromSprint(
            @PathVariable UUID sprintId,
            @PathVariable UUID issueId) {
        sprintService.removeIssueFromSprint(sprintId, issueId);
        return ResponseEntity.noContent().build();
    }
}