package com.jira.issue.controller;

import com.jira.issue.dto.ExternalPageLinkRequest;
import com.jira.issue.dto.ExternalPageLinkResponse;
import com.jira.issue.service.ExternalPageLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "External Page Links", description = "Confluence / external page linking for issues, epics, and sprints")
public class ExternalPageLinkController {

    private final ExternalPageLinkService externalPageLinkService;

    // --- Issue page links ---

    @PostMapping("/api/issues/{id}/page-links")
    @Operation(summary = "Add page link to issue")
    public ResponseEntity<ExternalPageLinkResponse> addIssuePageLink(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @Valid @RequestBody ExternalPageLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        ExternalPageLinkResponse response = externalPageLinkService.addPageLink("ISSUE", id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/issues/{id}/page-links")
    @Operation(summary = "Get page links for issue")
    public ResponseEntity<List<ExternalPageLinkResponse>> getIssuePageLinks(
            @Parameter(description = "Issue ID") @PathVariable UUID id) {
        return ResponseEntity.ok(externalPageLinkService.getPageLinks("ISSUE", id));
    }

    @DeleteMapping("/api/issues/{id}/page-links/{linkId}")
    @Operation(summary = "Remove page link from issue")
    public ResponseEntity<Void> removeIssuePageLink(
            @Parameter(description = "Issue ID") @PathVariable UUID id,
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {
        externalPageLinkService.removePageLink(linkId);
        return ResponseEntity.noContent().build();
    }

    // --- Epic page links ---

    @PostMapping("/api/epics/{id}/page-links")
    @Operation(summary = "Add page link to epic")
    public ResponseEntity<ExternalPageLinkResponse> addEpicPageLink(
            @Parameter(description = "Epic ID") @PathVariable UUID id,
            @Valid @RequestBody ExternalPageLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        ExternalPageLinkResponse response = externalPageLinkService.addPageLink("EPIC", id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/epics/{id}/page-links")
    @Operation(summary = "Get page links for epic")
    public ResponseEntity<List<ExternalPageLinkResponse>> getEpicPageLinks(
            @Parameter(description = "Epic ID") @PathVariable UUID id) {
        return ResponseEntity.ok(externalPageLinkService.getPageLinks("EPIC", id));
    }

    @DeleteMapping("/api/epics/{id}/page-links/{linkId}")
    @Operation(summary = "Remove page link from epic")
    public ResponseEntity<Void> removeEpicPageLink(
            @Parameter(description = "Epic ID") @PathVariable UUID id,
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {
        externalPageLinkService.removePageLink(linkId);
        return ResponseEntity.noContent().build();
    }

    // --- Sprint page links ---

    @PostMapping("/api/sprints/{id}/page-links")
    @Operation(summary = "Add page link to sprint")
    public ResponseEntity<ExternalPageLinkResponse> addSprintPageLink(
            @Parameter(description = "Sprint ID") @PathVariable UUID id,
            @Valid @RequestBody ExternalPageLinkRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        ExternalPageLinkResponse response = externalPageLinkService.addPageLink("SPRINT", id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/sprints/{id}/page-links")
    @Operation(summary = "Get page links for sprint")
    public ResponseEntity<List<ExternalPageLinkResponse>> getSprintPageLinks(
            @Parameter(description = "Sprint ID") @PathVariable UUID id) {
        return ResponseEntity.ok(externalPageLinkService.getPageLinks("SPRINT", id));
    }

    @DeleteMapping("/api/sprints/{id}/page-links/{linkId}")
    @Operation(summary = "Remove page link from sprint")
    public ResponseEntity<Void> removeSprintPageLink(
            @Parameter(description = "Sprint ID") @PathVariable UUID id,
            @Parameter(description = "Link ID") @PathVariable UUID linkId) {
        externalPageLinkService.removePageLink(linkId);
        return ResponseEntity.noContent().build();
    }
}
