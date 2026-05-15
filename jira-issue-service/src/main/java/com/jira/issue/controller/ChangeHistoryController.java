package com.jira.issue.controller;

import com.jira.issue.dto.ChangeHistoryResponse;
import com.jira.issue.service.ChangeHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/history")
@RequiredArgsConstructor
@Tag(name = "Change History", description = "Issue change history API")
@CrossOrigin(origins = "*")
public class ChangeHistoryController {

    private final ChangeHistoryService changeHistoryService;

    @GetMapping
    @Operation(summary = "Get change history", description = "Get all change history for an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved")
    })
    public ResponseEntity<List<ChangeHistoryResponse>> getChangeHistory(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(changeHistoryService.getChangeHistoryByIssue(issueId));
    }

    @GetMapping("/{changeGroupId}")
    @Operation(summary = "Get change group", description = "Get a specific change group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Change group found"),
            @ApiResponse(responseCode = "404", description = "Change group not found")
    })
    public ResponseEntity<ChangeHistoryResponse> getChangeGroup(
            @Parameter(description = "Change Group ID") @PathVariable UUID changeGroupId) {
        return ResponseEntity.ok(changeHistoryService.getChangeHistory(changeGroupId));
    }
}