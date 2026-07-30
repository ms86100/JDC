package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.CloneIssueResponse;
import com.avionics_systems.issue.dto.IssueResponse;
import com.avionics_systems.issue.service.CloneIssueService;
import com.avionics_systems.issue.service.MoveIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for cloning and moving issues
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Clone & Move", description = "Clone and move issue operations")
public class CloneMoveController {

    private final CloneIssueService cloneIssueService;
    private final MoveIssueService moveIssueService;

    @PostMapping("/{issueId}/clone")
    @Operation(summary = "Clone an issue", description = "Create a copy of an existing issue")
    public ResponseEntity<CloneIssueResponse> cloneIssue(
            @Parameter(description = "Issue ID to clone") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "Include comments") @RequestParam(defaultValue = "false") boolean includeComments,
            @Parameter(description = "Include attachments") @RequestParam(defaultValue = "false") boolean includeAttachments) {
        log.info("Cloning issue {} by user {}", issueId, userId);
        CloneIssueResponse response = cloneIssueService.cloneIssue(issueId, userId, includeComments, includeAttachments);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{issueId}/clone-to-project")
    @Operation(summary = "Clone issue to another project", description = "Create a copy of an issue in a different project")
    public ResponseEntity<IssueResponse> cloneIssueToProject(
            @Parameter(description = "Issue ID to clone") @PathVariable UUID issueId,
            @Parameter(description = "Target project ID") @RequestParam UUID targetProjectId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Cloning issue {} to project {} by user {}", issueId, targetProjectId, userId);
        IssueResponse response = cloneIssueService.cloneIssueToProject(issueId, targetProjectId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{issueId}/move")
    @Operation(summary = "Move issue to another project", description = "Move an issue from one project to another")
    public ResponseEntity<IssueResponse> moveIssue(
            @Parameter(description = "Issue ID to move") @PathVariable UUID issueId,
            @RequestBody java.util.Map<String, String> request,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        UUID targetProjectId = UUID.fromString(request.get("targetProjectId"));
        log.info("Moving issue {} to project {} by user {}", issueId, targetProjectId, userId);
        IssueResponse response = moveIssueService.moveIssue(issueId, targetProjectId, userId);
        return ResponseEntity.ok(response);
    }
}