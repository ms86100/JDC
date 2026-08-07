package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.RemainingEstimateStrategy;
import com.avionics_systems.issue.dto.WorklogRequest;
import com.avionics_systems.issue.dto.WorklogResponse;
import com.avionics_systems.issue.entity.Worklog;
import com.avionics_systems.issue.exception.PermissionDeniedException;
import com.avionics_systems.issue.security.PermissionCheckResult;
import com.avionics_systems.issue.service.IssueService;
import com.avionics_systems.issue.service.ProjectPermissionClient;
import com.avionics_systems.issue.service.WorklogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/worklogs")
@RequiredArgsConstructor
@Tag(name = "Worklogs", description = "Issue worklog management API")
public class WorklogController {

    private final WorklogService worklogService;
    private final IssueService issueService;
    private final ProjectPermissionClient projectPermissionClient;

    @PostMapping
    @Operation(summary = "Log work", description = "Log time worked on an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Worklog created"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<WorklogResponse> createWorklog(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody WorklogRequest request) {

        UUID actor = resolveUserId(userId);
        UUID projectId = issueService.getIssue(issueId).getProjectId();
        requirePermission(actor, projectId, "WORK_ON_ISSUES");

        request.setIssueId(issueId);
        if (request.getAuthorId() == null) {
            request.setAuthorId(actor);
        }
        return new ResponseEntity<>(worklogService.createWorklog(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get worklogs", description = "Get all worklogs for an issue (filtered by visibility)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklogs retrieved")
    })
    public ResponseEntity<List<WorklogResponse>> getWorklogs(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId != null) {
            return ResponseEntity.ok(worklogService.getVisibleWorklogsByIssue(issueId, userId));
        }
        return ResponseEntity.ok(worklogService.getWorklogsByIssue(issueId));
    }

    @GetMapping("/total")
    @Operation(summary = "Get total time", description = "Get total time worked on an issue")
    public ResponseEntity<Long> getTotalTimeWorked(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(worklogService.getTotalTimeWorked(issueId));
    }

    @GetMapping("/aggregate")
    @Operation(summary = "Get aggregate time", description = "Get aggregated time for issue and its sub-tasks")
    public ResponseEntity<AggregateTimeResponse> getAggregateTime(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        long[] agg = worklogService.getAggregateTimeForIssue(issueId);
        return ResponseEntity.ok(new AggregateTimeResponse(agg[0], agg[1], agg[2]));
    }

    @GetMapping("/{worklogId}")
    @Operation(summary = "Get worklog", description = "Get a specific worklog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklog found"),
            @ApiResponse(responseCode = "404", description = "Worklog not found")
    })
    public ResponseEntity<WorklogResponse> getWorklog(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId) {
        WorklogResponse response = worklogService.getWorklog(worklogId);
        if (!response.getIssueId().equals(issueId)) {
            throw new com.avionics_systems.issue.exception.ResourceNotFoundException(
                    "Worklog " + worklogId + " does not belong to issue " + issueId);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{worklogId}")
    @Operation(summary = "Update worklog", description = "Update a worklog entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklog updated"),
            @ApiResponse(responseCode = "403", description = "Permission denied")
    })
    public ResponseEntity<WorklogResponse> updateWorklog(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody WorklogRequest request) {

        UUID actor = resolveUserId(userId);
        UUID projectId = issueService.getIssue(issueId).getProjectId();
        Worklog existing = worklogService.getWorklogEntity(worklogId);

        if (Objects.equals(existing.getAuthorId(), actor)) {
            requirePermission(actor, projectId, "EDIT_OWN_WORKLOGS");
        } else {
            requirePermission(actor, projectId, "EDIT_ALL_WORKLOGS");
        }

        return ResponseEntity.ok(worklogService.updateWorklog(worklogId, issueId, request));
    }

    @DeleteMapping("/{worklogId}")
    @Operation(summary = "Delete worklog", description = "Delete a worklog entry with remaining estimate adjustment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Worklog deleted"),
            @ApiResponse(responseCode = "403", description = "Permission denied"),
            @ApiResponse(responseCode = "404", description = "Worklog not found")
    })
    public ResponseEntity<Void> deleteWorklog(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(value = "adjustEstimate", defaultValue = "AUTO") RemainingEstimateStrategy adjustEstimate,
            @RequestParam(value = "adjustmentSeconds", required = false) Long adjustmentSeconds) {

        UUID actor = resolveUserId(userId);
        UUID projectId = issueService.getIssue(issueId).getProjectId();
        Worklog existing = worklogService.getWorklogEntity(worklogId);

        if (Objects.equals(existing.getAuthorId(), actor)) {
            requirePermission(actor, projectId, "DELETE_OWN_WORKLOGS");
        } else {
            requirePermission(actor, projectId, "DELETE_ALL_WORKLOGS");
        }

        worklogService.deleteWorklog(worklogId, issueId, adjustEstimate, adjustmentSeconds);
        return ResponseEntity.noContent().build();
    }

    private static UUID resolveUserId(UUID userId) {
        if (userId == null) {
            throw new PermissionDeniedException("Authentication required (X-User-Id)");
        }
        return userId;
    }

    private void requirePermission(UUID userId, UUID projectId, String permission) {
        if (projectPermissionClient.check(userId, projectId, permission) != PermissionCheckResult.GRANTED) {
            throw new PermissionDeniedException(permission, "project " + projectId);
        }
    }

    public record AggregateTimeResponse(long aggregateEstimate, long aggregateTimeSpent, long aggregateRemaining) {}
}
