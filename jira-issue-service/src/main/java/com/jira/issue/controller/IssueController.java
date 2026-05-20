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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    @Operation(summary = "Create a new issue", description = "Creates a new issue in the specified project")
    public ResponseEntity<IssueResponse> createIssue(
            @Valid @RequestBody CreateIssueRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        // Check CREATE_ISSUES permission before allowing creation
        if (userId != null && !hasPermission(userId, request.getProjectId(), "CREATE_ISSUES")) {
            throw new PermissionDeniedException("CREATE_ISSUES", "project " + request.getProjectId());
        }

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
            @Valid @RequestBody UpdateIssueRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Get issue to find project ID for permission check
        IssueResponse existingIssue = issueService.getIssue(id);

        // Check EDIT_ISSUES permission
        if (userId != null && !hasPermission(userId, existingIssue.getProjectId(), "EDIT_ISSUES")) {
            throw new PermissionDeniedException("EDIT_ISSUES", "issue " + id);
        }

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

        // Check RESOLVE_ISSUES permission (status transitions typically require this)
        if (userId != null && !hasPermission(userId, projectId, "RESOLVE_ISSUES")) {
            throw new PermissionDeniedException("RESOLVE_ISSUES", "project " + projectId);
        }

        IssueResponse response = issueService.updateIssueStatus(id, request, projectId, userId);
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete issue", description = "Deletes an issue")
    public ResponseEntity<Void> deleteIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Get issue to find project ID for permission check
        IssueResponse existingIssue = issueService.getIssue(id);

        // Check DELETE_ISSUES permission
        if (userId != null && !hasPermission(userId, existingIssue.getProjectId(), "DELETE_ISSUES")) {
            throw new PermissionDeniedException("DELETE_ISSUES", "issue " + id);
        }

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
    private boolean hasPermission(UUID userId, UUID projectId, String permission) {
        if (userId == null) {
            return false; // No user = no permission
        }
        try {
            String url = String.format("%s/api/projects/%s/permissions/check?userId=%s&permission=%s",
                    projectServiceUrl, projectId, userId, permission);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return Boolean.TRUE.equals(response.get("hasPermission"));
        } catch (Exception e) {
            // If permission service is unavailable, deny access by default (fail-safe)
            return false;
        }
    }
}