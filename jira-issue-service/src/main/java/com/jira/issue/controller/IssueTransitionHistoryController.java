package com.jira.issue.controller;

import com.jira.issue.dto.IssueTransitionHistoryResponse;
import com.jira.issue.dto.RecordIssueTransitionRequest;
import com.jira.issue.service.IssueTransitionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Issue Transition History", description = "Workflow transition audit per issue")
public class IssueTransitionHistoryController {

    private final IssueTransitionHistoryService transitionHistoryService;

    @PostMapping("/api/issues/{issueId}/transitions/history/internal")
    @Operation(summary = "Record transition history (workflow engine)")
    public ResponseEntity<IssueTransitionHistoryResponse> recordInternal(
            @PathVariable UUID issueId,
            @RequestBody RecordIssueTransitionRequest request) {
        return ResponseEntity.ok(transitionHistoryService.record(issueId, request));
    }

    @GetMapping("/api/issues/{issueId}/transitions/history")
    @Operation(summary = "List transition history for an issue")
    public ResponseEntity<List<IssueTransitionHistoryResponse>> list(
            @PathVariable UUID issueId) {
        return ResponseEntity.ok(transitionHistoryService.listByIssue(issueId));
    }
}
