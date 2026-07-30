package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.LabelRequest;
import com.avionics_systems.issue.dto.LabelResponse;
import com.avionics_systems.issue.service.LabelService;
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
@RequestMapping("/api/issues/{issueId}/labels")
@RequiredArgsConstructor
@Tag(name = "Labels", description = "Issue label management API")
public class LabelController {

    private final LabelService labelService;

    @PostMapping
    @Operation(summary = "Add label", description = "Add a label to an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Label added"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<LabelResponse> addLabel(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Valid @RequestBody LabelRequest request) {
        request.setIssueId(issueId);
        return ResponseEntity.ok(labelService.addLabel(request));
    }

    @GetMapping
    @Operation(summary = "Get labels", description = "Get all labels for an issue")
    public ResponseEntity<List<LabelResponse>> getLabels(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(labelService.getLabelsByIssue(issueId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search labels", description = "Search for existing labels")
    public ResponseEntity<List<LabelResponse>> searchLabels(
            @Parameter(description = "Search query") @RequestParam String query) {
        return ResponseEntity.ok(labelService.searchLabels(query));
    }

    @DeleteMapping("/{labelName}")
    @Operation(summary = "Remove label", description = "Remove a label from an issue")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Label removed"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<Void> removeLabel(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "Label name") @PathVariable String labelName) {
        labelService.removeLabel(issueId, labelName);
        return ResponseEntity.noContent().build();
    }
}