package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.IssueLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Issue Link Controller - REST endpoints for managing issue links and link types.
 */
@RestController
@RequestMapping("/api/issues/links")
@RequiredArgsConstructor
@Tag(name = "Issue Links", description = "Issue linking management API")
public class IssueLinkController {

    private final IssueLinkService issueLinkService;

    // ========== Link Type Endpoints ==========

    @PostMapping("/types")
    @Operation(summary = "Create link type", description = "Creates a new issue link type")
    public ResponseEntity<IssueLinkTypeResponse> createLinkType(
            @Valid @RequestBody CreateLinkTypeRequest request) {

        IssueLinkTypeResponse response = issueLinkService.createLinkType(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/types")
    @Operation(summary = "Get all link types", description = "Returns all issue link types")
    public ResponseEntity<List<IssueLinkTypeResponse>> getAllLinkTypes(
            @Parameter(description = "Include inactive types") @RequestParam(defaultValue = "false") boolean includeInactive) {

        List<IssueLinkTypeResponse> types = includeInactive ?
                issueLinkService.getAllLinkTypes() :
                issueLinkService.getActiveLinkTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/types/{linkTypeId}")
    @Operation(summary = "Get link type by ID", description = "Returns a specific link type")
    public ResponseEntity<IssueLinkTypeResponse> getLinkTypeById(
            @Parameter(description = "Link type ID") @PathVariable UUID linkTypeId) {

        return ResponseEntity.ok(issueLinkService.getLinkTypeById(linkTypeId));
    }

    @PutMapping("/types/{linkTypeId}")
    @Operation(summary = "Update link type", description = "Updates a link type")
    public ResponseEntity<IssueLinkTypeResponse> updateLinkType(
            @Parameter(description = "Link type ID") @PathVariable UUID linkTypeId,
            @Valid @RequestBody UpdateLinkTypeRequest request) {

        return ResponseEntity.ok(issueLinkService.updateLinkType(linkTypeId, request));
    }

    @DeleteMapping("/types/{linkTypeId}")
    @Operation(summary = "Delete link type", description = "Deletes or deactivates a link type")
    public ResponseEntity<Void> deleteLinkType(
            @Parameter(description = "Link type ID") @PathVariable UUID linkTypeId) {

        issueLinkService.deleteLinkType(linkTypeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/types/seed")
    @Operation(summary = "Seed default link types", description = "Seeds the default Avionics Systems-style link types")
    public ResponseEntity<Void> seedDefaultLinkTypes() {
        issueLinkService.seedDefaultLinkTypes();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ========== Issue Link Endpoints ==========

    @PostMapping
    @Operation(summary = "Create issue link", description = "Creates a link between two issues")
    public ResponseEntity<IssueLinkResponse> createIssueLink(
            @Valid @RequestBody IssueLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        // Ensure source issue is set (could come from path parameter in future)
        if (request.getSourceIssueId() == null) {
            throw new IllegalArgumentException("sourceIssueId is required");
        }

        IssueLinkResponse response = issueLinkService.createIssueLink(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{linkId}")
    @Operation(summary = "Get link by ID", description = "Returns a specific issue link")
    public ResponseEntity<IssueLinkResponse> getLinkById(
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {

        return ResponseEntity.ok(issueLinkService.getLinkById(linkId));
    }

    @GetMapping("/issue/{issueId}")
    @Operation(summary = "Get all links for issue", description = "Returns all links (inward and outward) for an issue")
    public ResponseEntity<List<IssueLinkResponse>> getLinksByIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        return ResponseEntity.ok(issueLinkService.getLinksByIssue(issueId));
    }

    @GetMapping("/issue/{issueId}/outward")
    @Operation(summary = "Get outward links", description = "Returns outward links from an issue")
    public ResponseEntity<List<IssueLinkResponse>> getOutwardLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        return ResponseEntity.ok(issueLinkService.getOutwardLinks(issueId));
    }

    @GetMapping("/issue/{issueId}/inward")
    @Operation(summary = "Get inward links", description = "Returns inward links to an issue")
    public ResponseEntity<List<IssueLinkResponse>> getInwardLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        return ResponseEntity.ok(issueLinkService.getInwardLinks(issueId));
    }

    @GetMapping("/issue/{issueId}/workflow")
    @Operation(summary = "Get links for workflow", description = "Returns link context for workflow validation")
    public ResponseEntity<List<IssueLinkWorkflowContextResponse>> getLinksForWorkflow(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        return ResponseEntity.ok(issueLinkService.getLinksForWorkflow(issueId));
    }

    @DeleteMapping("/{linkId}")
    @Operation(summary = "Delete issue link", description = "Deletes an issue link")
    public ResponseEntity<Void> deleteIssueLink(
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {

        issueLinkService.deleteIssueLink(linkId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issue/{issueId}")
    @Operation(summary = "Delete all links for issue", description = "Deletes all links (inward and outward) for an issue")
    public ResponseEntity<Integer> deleteLinksByIssue(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {

        int count = issueLinkService.deleteLinksByIssue(issueId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/types/names")
    @Operation(summary = "Get link type names", description = "Returns list of active link type names")
    public ResponseEntity<List<String>> getAvailableLinkTypes() {
        return ResponseEntity.ok(issueLinkService.getAvailableLinkTypes());
    }
}