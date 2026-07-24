package com.jira.issue.controller;

import com.jira.issue.dto.IssueResponse;
import com.jira.issue.service.IssueHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Issue Hierarchy Controller - REST endpoints for parent-child relationships and subtasks.
 */
@RestController
@RequestMapping("/api/issues/hierarchy")
@RequiredArgsConstructor
@Tag(name = "Issue Hierarchy", description = "Issue parent-child hierarchy management")
public class IssueHierarchyController {

    private final IssueHierarchyService issueHierarchyService;

    @PostMapping("/{issueId}/parent")
    @Operation(summary = "Set parent issue", description = "Sets the parent issue for a given issue")
    public ResponseEntity<IssueResponse> setParentIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestBody SetParentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        IssueResponse response = issueHierarchyService.setParentIssue(
                issueId, request.getParentIssueId(), resolveUserId(userId));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{issueId}/parent")
    @Operation(summary = "Remove parent issue", description = "Removes the parent from an issue, making it standalone")
    public ResponseEntity<IssueResponse> removeParentIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        IssueResponse response = issueHierarchyService.removeParentIssue(issueId, resolveUserId(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{issueId}/subtasks")
    @Operation(summary = "Get subtasks", description = "Returns all direct subtasks of an issue")
    public ResponseEntity<List<IssueResponse>> getSubtasks(
            @Parameter(description = "Parent issue ID") @PathVariable UUID issueId) {

        List<IssueResponse> subtasks = issueHierarchyService.getSubtasks(issueId);
        return ResponseEntity.ok(subtasks);
    }

    @GetMapping("/{issueId}/subtasks/count")
    @Operation(summary = "Get subtask count", description = "Returns the count of direct subtasks")
    public ResponseEntity<Integer> getSubtaskCount(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        int count = issueHierarchyService.getSubtaskCount(issueId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{issueId}/parent")
    @Operation(summary = "Get parent issue", description = "Returns the parent issue of a given issue")
    public ResponseEntity<IssueResponse> getParentIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        IssueResponse parent = issueHierarchyService.getParentIssue(issueId);
        if (parent == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(parent);
    }

    @GetMapping("/{issueId}/path")
    @Operation(summary = "Get hierarchy path", description = "Returns the full hierarchy path from root to the issue")
    public ResponseEntity<List<IssueResponse>> getHierarchyPath(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        List<IssueResponse> path = issueHierarchyService.getHierarchyPath(issueId);
        return ResponseEntity.ok(path);
    }

    @GetMapping("/{issueId}/descendants")
    @Operation(summary = "Get all descendants", description = "Returns all descendants (recursive subtasks) of an issue")
    public ResponseEntity<List<IssueResponse>> getAllDescendants(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        List<IssueResponse> descendants = issueHierarchyService.getAllDescendants(issueId);
        return ResponseEntity.ok(descendants);
    }

    @PostMapping("/{issueId}/convert-to-subtask")
    @Operation(summary = "Convert to subtask", description = "Converts a regular issue to a subtask of a parent")
    public ResponseEntity<IssueResponse> convertToSubtask(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestBody SetParentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        IssueResponse response = issueHierarchyService.convertToSubtask(
                issueId, request.getParentIssueId(), resolveUserId(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{issueId}/convert-from-subtask")
    @Operation(summary = "Convert from subtask", description = "Converts a subtask back to a regular issue")
    public ResponseEntity<IssueResponse> convertFromSubtask(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        IssueResponse response = issueHierarchyService.convertFromSubtask(issueId, resolveUserId(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{issueId}/move-to-parent")
    @Operation(summary = "Move subtask to new parent", description = "Moves a subtask to a different parent")
    public ResponseEntity<IssueResponse> moveSubtaskToParent(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestBody SetParentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        IssueResponse response = issueHierarchyService.moveSubtaskToParent(
                issueId, request.getParentIssueId(), resolveUserId(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{issueId}/stats")
    @Operation(summary = "Get hierarchy statistics", description = "Returns hierarchy stats for an issue")
    public ResponseEntity<IssueHierarchyService.HierarchyStatsResponse> getHierarchyStats(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        IssueHierarchyService.HierarchyStatsResponse stats = issueHierarchyService.getHierarchyStats(issueId);
        return ResponseEntity.ok(stats);
    }

    private static UUID resolveUserId(UUID userId) {
        return userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @lombok.Data
    public static class SetParentRequest {
        private UUID parentIssueId;
    }
}