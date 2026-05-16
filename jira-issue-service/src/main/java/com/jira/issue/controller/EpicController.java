package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.service.EpicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/epics")
@RequiredArgsConstructor
@Tag(name = "Epics", description = "Epic management endpoints - Jira DC compliant")
public class EpicController {

    private final EpicService epicService;

    @PostMapping
    @Operation(summary = "Create a new epic", description = "Creates a new epic in the project")
    public ResponseEntity<EpicResponse> createEpic(@Valid @RequestBody CreateEpicRequest request) {
        EpicResponse response = epicService.createEpic(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all epics", description = "Returns list of all epics")
    public ResponseEntity<List<EpicResponse>> getAllEpics(
            @Parameter(description = "Filter by lead user ID") @RequestParam(required = false) String leadId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {

        List<EpicResponse> epics;
        if (leadId != null) {
            epics = epicService.getEpicsByLead(leadId);
        } else if (status != null) {
            epics = epicService.getEpicsByStatus(status);
        } else {
            epics = epicService.getAllEpics();
        }
        return ResponseEntity.ok(epics);
    }

    @GetMapping("/{epicId}")
    @Operation(summary = "Get epic by ID", description = "Returns a single epic with progress details")
    public ResponseEntity<EpicResponse> getEpic(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        EpicResponse response = epicService.getEpic(epicId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{epicId}")
    @Operation(summary = "Update epic", description = "Updates an existing epic")
    public ResponseEntity<EpicResponse> updateEpic(
            @Parameter(description = "Epic ID") @PathVariable String epicId,
            @Valid @RequestBody UpdateEpicRequest request) {
        EpicResponse response = epicService.updateEpic(epicId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{epicId}")
    @Operation(summary = "Delete epic", description = "Deletes an epic and unlinks all issues")
    public ResponseEntity<Void> deleteEpic(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        epicService.deleteEpic(epicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{epicId}/issues/{issueId}")
    @Operation(summary = "Add issue to epic", description = "Links an issue to this epic")
    public ResponseEntity<Void> addIssueToEpic(
            @Parameter(description = "Epic ID") @PathVariable String epicId,
            @Parameter(description = "Issue ID") @PathVariable String issueId) {
        epicService.addIssueToEpic(epicId, issueId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{epicId}/issues/{issueId}")
    @Operation(summary = "Remove issue from epic", description = "Unlinks an issue from this epic")
    public ResponseEntity<Void> removeIssueFromEpic(
            @Parameter(description = "Epic ID") @PathVariable String epicId,
            @Parameter(description = "Issue ID") @PathVariable String issueId) {
        epicService.removeIssueFromEpic(epicId, issueId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{epicId}/issues")
    @Operation(summary = "Get epic issues", description = "Returns all issue IDs linked to this epic")
    public ResponseEntity<List<String>> getEpicIssues(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        List<String> issueIds = epicService.getEpicIssueIds(epicId);
        return ResponseEntity.ok(issueIds);
    }

    @PostMapping("/{epicId}/progress/recalculate")
    @Operation(summary = "Recalculate epic progress", description = "Recalculates story points from linked issues")
    public ResponseEntity<EpicResponse> recalculateProgress(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        epicService.recalculateEpicProgress(epicId);
        EpicResponse response = epicService.getEpic(epicId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{epicId}/progress")
    @Operation(summary = "Get epic progress", description = "Returns current epic progress metrics")
    public ResponseEntity<EpicProgressResponse> getEpicProgress(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        EpicProgressResponse progress = epicService.getCurrentEpicProgress(epicId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/{epicId}/progress/history")
    @Operation(summary = "Get epic progress history", description = "Returns progress history over time")
    public ResponseEntity<List<EpicProgressResponse>> getProgressHistory(
            @Parameter(description = "Epic ID") @PathVariable String epicId) {
        List<EpicProgressResponse> history = epicService.getEpicProgressHistory(epicId);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{epicId}/status")
    @Operation(summary = "Update epic status", description = "Updates epic status (OPEN, IN_PROGRESS, COMPLETE)")
    public ResponseEntity<EpicResponse> updateEpicStatus(
            @Parameter(description = "Epic ID") @PathVariable String epicId,
            @RequestBody StatusUpdateRequest request) {
        epicService.updateEpicStatus(epicId, request.getStatus());
        EpicResponse response = epicService.getEpic(epicId);
        return ResponseEntity.ok(response);
    }

    @Data
    public static class StatusUpdateRequest {
        private String status;
    }
}