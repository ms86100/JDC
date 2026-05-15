package com.jira.issue.controller;

import com.jira.issue.dto.IssueLinkRequest;
import com.jira.issue.dto.IssueLinkResponse;
import com.jira.issue.service.IssueLinkService;
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
@RequestMapping("/api/issues/{issueId}/links")
@RequiredArgsConstructor
@Tag(name = "Issue Links", description = "Issue linking management API")
@CrossOrigin(origins = "*")
public class IssueLinkController {

    private final IssueLinkService issueLinkService;

    @PostMapping
    @Operation(summary = "Create link", description = "Link this issue to another issue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link created"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    public ResponseEntity<IssueLinkResponse> createLink(
            @Parameter(description = "Source Issue ID") @PathVariable UUID issueId,
            @Valid @RequestBody IssueLinkRequest request) {
        request.setSourceIssueId(issueId);
        return ResponseEntity.ok(issueLinkService.createIssueLink(request));
    }

    @GetMapping
    @Operation(summary = "Get all links", description = "Get all links for an issue (inward and outward)")
    public ResponseEntity<List<IssueLinkResponse>> getAllLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(issueLinkService.getLinksByIssue(issueId));
    }

    @GetMapping("/outward")
    @Operation(summary = "Get outward links", description = "Get links where this issue is the source")
    public ResponseEntity<List<IssueLinkResponse>> getOutwardLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(issueLinkService.getOutwardLinks(issueId));
    }

    @GetMapping("/inward")
    @Operation(summary = "Get inward links", description = "Get links where this issue is the destination")
    public ResponseEntity<List<IssueLinkResponse>> getInwardLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        return ResponseEntity.ok(issueLinkService.getInwardLinks(issueId));
    }

    @GetMapping("/types")
    @Operation(summary = "Get link types", description = "Get all available issue link types")
    public ResponseEntity<List<String>> getLinkTypes() {
        return ResponseEntity.ok(issueLinkService.getAvailableLinkTypes());
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Delete link", description = "Delete an issue link")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Link deleted"),
            @ApiResponse(responseCode = "404", description = "Link not found")
    })
    public ResponseEntity<Void> deleteLink(
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {
        issueLinkService.deleteIssueLink(linkId);
        return ResponseEntity.noContent().build();
    }
}