package com.jira.issue.controller;

import com.jira.issue.service.IssueLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Standalone controller for issue link operations that don't require an issue ID.
 * Provides endpoints for getting link types and other global link-related data.
 */
@RestController
@RequestMapping("/api/issues/links")
@RequiredArgsConstructor
@Tag(name = "Issue Links", description = "Issue linking management API")
@CrossOrigin(origins = "*")
public class IssueLinksController {

    private final IssueLinkService issueLinkService;

    @GetMapping("/types")
    @Operation(summary = "Get link types", description = "Get all available issue link types")
    public ResponseEntity<List<String>> getLinkTypes() {
        return ResponseEntity.ok(issueLinkService.getAvailableLinkTypes());
    }
}