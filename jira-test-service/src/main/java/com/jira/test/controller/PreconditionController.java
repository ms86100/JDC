package com.jira.test.controller;

import com.jira.test.entity.Precondition;
import com.jira.test.entity.TestPreconditionLink;
import com.jira.test.entity.TestIssue;
import com.jira.test.service.PreconditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/preconditions")
@RequiredArgsConstructor
@Tag(name = "Preconditions", description = "APIs for managing test preconditions")
public class PreconditionController {

    private final PreconditionService preconditionService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #projectId)")
    @Operation(summary = "Create a new precondition")
    public ResponseEntity<Precondition> createPrecondition(
            @RequestParam UUID projectId,
            @RequestBody PreconditionService.CreatePreconditionRequest request) {
        return ResponseEntity.ok(preconditionService.createPrecondition(projectId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #id)")
    @Operation(summary = "Get precondition by ID")
    public ResponseEntity<Precondition> getPrecondition(@PathVariable UUID id) {
        return ResponseEntity.ok(preconditionService.getPreconditionById(id));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all preconditions by project")
    public ResponseEntity<List<Precondition>> getPreconditionsByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(preconditionService.getPreconditionsByProject(projectId));
    }

    @GetMapping("/project/{projectId}/type/{type}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get preconditions by type")
    public ResponseEntity<List<Precondition>> getPreconditionsByType(
            @PathVariable UUID projectId, @PathVariable String type) {
        return ResponseEntity.ok(preconditionService.getPreconditionsByType(projectId, type));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #id)")
    @Operation(summary = "Update a precondition")
    public ResponseEntity<Precondition> updatePrecondition(
            @PathVariable UUID id,
            @RequestBody PreconditionService.UpdatePreconditionRequest request) {
        return ResponseEntity.ok(preconditionService.updatePrecondition(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #id)")
    @Operation(summary = "Delete a precondition")
    public ResponseEntity<Void> deletePrecondition(@PathVariable UUID id) {
        preconditionService.deletePrecondition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{preconditionId}/link/test/{testId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #testId)")
    @Operation(summary = "Link precondition to test")
    public ResponseEntity<TestPreconditionLink> linkToTest(
            @PathVariable UUID preconditionId,
            @PathVariable UUID testId,
            @RequestParam(required = false) Integer stepOrder) {
        return ResponseEntity.ok(preconditionService.linkPreconditionToTest(testId, preconditionId, stepOrder));
    }

    @DeleteMapping("/{preconditionId}/unlink/test/{testId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #testId)")
    @Operation(summary = "Unlink precondition from test")
    public ResponseEntity<Void> unlinkFromTest(
            @PathVariable UUID preconditionId,
            @PathVariable UUID testId) {
        preconditionService.unlinkPreconditionFromTest(testId, preconditionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Get preconditions for a test")
    public ResponseEntity<List<Precondition>> getPreconditionsForTest(@PathVariable UUID testId) {
        return ResponseEntity.ok(preconditionService.getPreconditionsForTest(testId));
    }

    @GetMapping("/{preconditionId}/tests")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #preconditionId)")
    @Operation(summary = "Get tests using a precondition")
    public ResponseEntity<List<TestIssue>> getTestsUsingPrecondition(@PathVariable UUID preconditionId) {
        return ResponseEntity.ok(preconditionService.getTestsUsingPrecondition(preconditionId));
    }

    @PutMapping("/{preconditionId}/link/test/{testId}/notes")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #testId)")
    @Operation(summary = "Update link notes")
    public ResponseEntity<TestPreconditionLink> updateLinkNotes(
            @PathVariable UUID preconditionId,
            @PathVariable UUID testId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                preconditionService.updateLinkNotes(testId, preconditionId, body.get("notes")));
    }

    @PostMapping("/evaluate/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Evaluate preconditions for a test")
    public ResponseEntity<PreconditionService.PreconditionEvaluationResult> evaluatePreconditions(
            @PathVariable UUID testId,
            @RequestBody PreconditionService.EvaluationContext context) {
        return ResponseEntity.ok(preconditionService.evaluatePreconditions(testId, context));
    }

    @GetMapping("/search/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Search preconditions by name")
    public ResponseEntity<List<Precondition>> searchPreconditions(
            @PathVariable UUID projectId,
            @RequestParam String query) {
        return ResponseEntity.ok(preconditionService.searchPreconditions(projectId, query));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #id)")
    @Operation(summary = "Duplicate a precondition")
    public ResponseEntity<Precondition> duplicatePrecondition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(preconditionService.duplicatePrecondition(id, body.get("newName")));
    }
}