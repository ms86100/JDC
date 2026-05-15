package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Issue management endpoints")
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    @Operation(summary = "Create a new issue", description = "Creates a new issue in the specified project")
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        IssueResponse response = issueService.createIssue(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Search/list issues", description = "Returns paginated list of issues with optional filters")
    public ResponseEntity<Page<IssueResponse>> searchIssues(
            @Parameter(description = "Project ID filter") @RequestParam(required = false) UUID projectId,
            @Parameter(description = "Status ID filter") @RequestParam(required = false) UUID status,
            @Parameter(description = "Assignee ID filter") @RequestParam(required = false) UUID assigneeId,
            @Parameter(description = "Reporter ID filter") @RequestParam(required = false) UUID reporterId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        IssueSearchRequest searchRequest = IssueSearchRequest.builder()
                .projectId(projectId)
                .status(status)
                .assigneeId(assigneeId)
                .reporterId(reporterId)
                .page(page)
                .size(size)
                .build();

        Page<IssueResponse> issues = issueService.searchIssues(searchRequest);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/search")
    @Operation(summary = "JQL Search", description = "Search issues using JQL query")
    public ResponseEntity<Map<String, Object>> jqlSearch(
            @Parameter(description = "JQL query string") @RequestParam(required = false) String jql,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int pageSize) {

        Map<String, Object> result = issueService.searchByJql(jql, page, pageSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/batch")
    @Operation(summary = "Get issues by IDs", description = "Returns multiple issues by their IDs")
    public ResponseEntity<java.util.List<IssueResponse>> getIssuesByIds(
            @Parameter(description = "Comma-separated issue IDs") @RequestParam String ids) {

        java.util.List<IssueResponse> issues = issueService.getIssuesByIds(ids);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get issue by ID", description = "Returns issue details by ID")
    public ResponseEntity<IssueResponse> getIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID id) {

        IssueResponse response = issueService.getIssue(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update issue", description = "Updates issue details")
    public ResponseEntity<IssueResponse> updateIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateIssueRequest request) {

        IssueResponse response = issueService.updateIssue(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update issue status", description = "Transitions the issue to a new status (validates with workflow service)")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateIssueStatusRequest request,
            @Parameter(description = "Project ID for workflow validation") @RequestParam UUID projectId) {

        IssueResponse response = issueService.updateIssueStatus(id, request.getStatusId(), projectId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete issue", description = "Deletes an issue")
    public ResponseEntity<Void> deleteIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID id) {

        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }
}