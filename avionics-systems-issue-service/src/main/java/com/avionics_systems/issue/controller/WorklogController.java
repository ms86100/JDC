package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.WorklogRequest;
import com.avionics_systems.issue.dto.WorklogResponse;
import com.avionics_systems.issue.service.WorklogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/worklogs")
@RequiredArgsConstructor
@Tag(name = "Worklogs", description = "Issue worklog management API")
public class WorklogController {

    private final WorklogService worklogService;

    @PostMapping
    @Operation(summary = "Log work", description = "Log time worked on an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklog created"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<WorklogResponse> createWorklog(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody WorklogRequest request) {
        request.setIssueId(issueId);
        if (request.getAuthorId() == null) {
            request.setAuthorId(userId != null ? userId : UUID.fromString("5ba38176-421f-431c-87f9-3836e4147a8c"));
        }
        return ResponseEntity.ok(worklogService.createWorklog(request));
    }

    @GetMapping
    @Operation(summary = "Get worklogs", description = "Get all worklogs for an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklogs retrieved")
    })
    public ResponseEntity<List<WorklogResponse>> getWorklogs(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(worklogService.getWorklogsByIssue(issueId));
    }

    @GetMapping("/total")
    @Operation(summary = "Get total time", description = "Get total time worked on an issue")
    public ResponseEntity<Long> getTotalTimeWorked(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(worklogService.getTotalTimeWorked(issueId));
    }

    @GetMapping("/{worklogId}")
    @Operation(summary = "Get worklog", description = "Get a specific worklog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Worklog found"),
            @ApiResponse(responseCode = "404", description = "Worklog not found")
    })
    public ResponseEntity<WorklogResponse> getWorklog(
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId) {
        return ResponseEntity.ok(worklogService.getWorklog(worklogId));
    }

    @PutMapping("/{worklogId}")
    @Operation(summary = "Update worklog", description = "Update a worklog")
    public ResponseEntity<WorklogResponse> updateWorklog(
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId,
            @Valid @RequestBody WorklogRequest request) {
        return ResponseEntity.ok(worklogService.updateWorklog(worklogId, request));
    }

    @DeleteMapping("/{worklogId}")
    @Operation(summary = "Delete worklog", description = "Delete a worklog")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Worklog deleted"),
            @ApiResponse(responseCode = "404", description = "Worklog not found")
    })
    public ResponseEntity<Void> deleteWorklog(
            @Parameter(description = "Worklog ID") @PathVariable UUID worklogId) {
        worklogService.deleteWorklog(worklogId);
        return ResponseEntity.noContent().build();
    }
}