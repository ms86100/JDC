package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.entity.IssueType;
import com.jira.issue.entity.IssuePriority;
import com.jira.issue.entity.IssueStatus;
import com.jira.issue.service.IssueService;
import com.jira.issue.repository.IssueTypeRepository;
import com.jira.issue.repository.IssuePriorityRepository;
import com.jira.issue.repository.IssueStatusRepository;
import com.jira.issue.exception.PermissionDeniedException;
import com.jira.issue.security.PermissionCheckResult;
import com.jira.issue.service.ProjectPermissionClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Issue management endpoints")
@CrossOrigin(origins = "*")
public class IssueController {

    private final IssueService issueService;
    private final IssueTypeRepository issueTypeRepository;
    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ProjectPermissionClient projectPermissionClient;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    @Operation(summary = "Create a new issue", description = "Creates a new issue in the specified project")
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        UUID actor = resolveUserId(userId);
        requirePermission(actor, request.getProjectId(), "CREATE_ISSUES");

        IssueResponse response = issueService.createIssue(request, actor);
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

    @GetMapping("/by-key/{issueKey}")
    @Operation(summary = "Get issue by key", description = "Returns issue by issue key e.g. PROJ-42")
    public ResponseEntity<IssueResponse> getIssueByKey(
            @Parameter(description = "Issue key") @PathVariable String issueKey) {
        return ResponseEntity.ok(issueService.getIssueByKey(issueKey));
    }

    @GetMapping("/batch")
    @Operation(summary = "Get issues by IDs", description = "Returns multiple issues by their IDs")
    public ResponseEntity<java.util.List<IssueResponse>> getIssuesByIds(
            @Parameter(description = "Comma-separated issue IDs") @RequestParam String ids) {

        java.util.List<IssueResponse> issues = issueService.getIssuesByIds(ids);
        return ResponseEntity.ok(issues);
    }

    @PostMapping("/{id}/vote")
    @Operation(summary = "Vote for issue")
    public ResponseEntity<IssueResponse> vote(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(issueService.addVote(id, resolveUserId(userId)));
    }

    @DeleteMapping("/{id}/vote")
    @Operation(summary = "Remove vote")
    public ResponseEntity<IssueResponse> unvote(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(issueService.removeVote(id, resolveUserId(userId)));
    }

    @PostMapping("/{id}/watch")
    @Operation(summary = "Watch issue")
    public ResponseEntity<IssueResponse> watch(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(issueService.addWatcher(id, resolveUserId(userId)));
    }

    @DeleteMapping("/{id}/watch")
    @Operation(summary = "Stop watching issue")
    public ResponseEntity<IssueResponse> unwatch(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(issueService.removeWatcher(id, resolveUserId(userId)));
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
            @Valid @RequestBody UpdateIssueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Get issue to find project ID for permission check
        IssueResponse existingIssue = issueService.getIssue(id);

        UUID actor = resolveUserId(userId);
        requirePermission(actor, existingIssue.getProjectId(), "EDIT_ISSUES");

        IssueResponse response = issueService.updateIssue(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update issue status", description = "Transitions the issue to a new status (validates with workflow service)")
    public ResponseEntity<IssueResponse> updateIssueStatus(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateIssueStatusRequest request,
            @Parameter(description = "Project ID for workflow validation") @RequestParam UUID projectId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        UUID actor = resolveUserId(userId);
        requirePermission(actor, projectId, "RESOLVE_ISSUES");

        IssueResponse response = issueService.updateIssueStatus(id, request, projectId, actor);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status/internal")
    @Operation(summary = "Internal status update (workflow engine only)")
    public ResponseEntity<IssueResponse> updateIssueStatusInternal(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @RequestParam UUID projectId) {
        UUID statusId = UUID.fromString(String.valueOf(body.get("statusId")));
        return ResponseEntity.ok(issueService.updateIssueStatusInternal(id, statusId, projectId));
    }

    @PatchMapping("/{id}/workflow/internal")
    @Operation(summary = "Internal workflow post-function update")
    public ResponseEntity<IssueResponse> workflowInternalUpdate(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Workflow-Internal", required = false) String internal) {
        return ResponseEntity.ok(issueService.applyWorkflowInternalUpdate(id, body));
    }

    @GetMapping("/{id}/transitions")
    @Operation(summary = "Available workflow transitions for issue")
    public ResponseEntity<Object> getAvailableTransitions(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        String url = issueService.getWorkflowServiceUrl()
                + "/api/workflows/issues/" + id + "/available-transitions?projectId=" + projectId;
        HttpHeaders headers = new HttpHeaders();
        if (userId != null) {
            headers.set("X-User-Id", userId.toString());
        }
        Object response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                Object.class).getBody();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone issue")
    public ResponseEntity<IssueResponse> cloneIssue(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, UUID> body,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID targetProjectId = body != null ? body.get("projectId") : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueService.cloneIssue(id, targetProjectId, resolveUserId(userId)));
    }

    @PostMapping("/{id}/move")
    @Operation(summary = "Move issue to another project")
    public ResponseEntity<IssueResponse> moveIssue(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        UUID targetProjectId = body.get("projectId");
        if (targetProjectId == null) {
            throw new IllegalArgumentException("projectId is required");
        }
        return ResponseEntity.ok(issueService.moveIssue(id, targetProjectId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete issue", description = "Deletes an issue")
    public ResponseEntity<Void> deleteIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Get issue to find project ID for permission check
        IssueResponse existingIssue = issueService.getIssue(id);

        UUID actor = resolveUserId(userId);
        requirePermission(actor, existingIssue.getProjectId(), "DELETE_ISSUES");

        issueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/types")
    @Operation(summary = "Get issue types", description = "Returns all available issue types")
    public ResponseEntity<List<IssueType>> getIssueTypes() {
        return ResponseEntity.ok(issueTypeRepository.findAll());
    }

    @GetMapping("/priorities")
    @Operation(summary = "Get priorities", description = "Returns all available priorities")
    public ResponseEntity<List<IssuePriority>> getPriorities() {
        return ResponseEntity.ok(issuePriorityRepository.findAll());
    }

    @GetMapping("/statuses")
    @Operation(summary = "Get statuses", description = "Returns all available statuses")
    public ResponseEntity<List<IssueStatus>> getStatuses() {
        return ResponseEntity.ok(issueStatusRepository.findAll());
    }

    /**
     * Helper method to check permissions via project service REST API.
     * In a production system, this would use a shared permission service or local cache.
     */
    private static UUID resolveUserId(UUID userId) {
        return userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private void requirePermission(UUID userId, UUID projectId, String permission) {
        if (projectPermissionClient.check(userId, projectId, permission) != PermissionCheckResult.GRANTED) {
            throw new PermissionDeniedException(permission, "project " + projectId);
        }
    }
}