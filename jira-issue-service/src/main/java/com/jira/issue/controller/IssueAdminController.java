package com.jira.issue.controller;

import com.jira.issue.dto.IssueTypeRequest;
import com.jira.issue.dto.IssueTypeResponse;
import com.jira.issue.service.IssueTypeService;
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

@RestController
@RequestMapping("/api/admin/issues")
@RequiredArgsConstructor
@Tag(name = "Issue Administration", description = "Admin endpoints for issue types")
@CrossOrigin(origins = "*")
public class IssueAdminController {

    private final IssueTypeService issueTypeService;

    // ==================== Issue Types ====================

    @GetMapping("/issue-types")
    @Operation(summary = "Get all issue types", description = "Returns all available issue types")
    public ResponseEntity<List<IssueTypeResponse>> getIssueTypes() {
        List<IssueTypeResponse> issueTypes = issueTypeService.getAllIssueTypes();
        return ResponseEntity.ok(issueTypes);
    }

    @GetMapping("/issue-types/{id}")
    @Operation(summary = "Get issue type by ID", description = "Returns issue type by ID")
    public ResponseEntity<IssueTypeResponse> getIssueType(
            @Parameter(description = "Issue Type ID") @PathVariable UUID id) {
        IssueTypeResponse issueType = issueTypeService.getIssueType(id);
        return ResponseEntity.ok(issueType);
    }

    @PostMapping("/issue-types")
    @Operation(summary = "Create issue type", description = "Creates a new issue type")
    public ResponseEntity<IssueTypeResponse> createIssueType(
            @Valid @RequestBody IssueTypeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        IssueTypeResponse response = issueTypeService.createIssueType(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/issue-types/{id}")
    @Operation(summary = "Update issue type", description = "Updates an existing issue type")
    public ResponseEntity<IssueTypeResponse> updateIssueType(
            @Parameter(description = "Issue Type ID") @PathVariable UUID id,
            @Valid @RequestBody IssueTypeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        IssueTypeResponse response = issueTypeService.updateIssueType(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/issue-types/{id}")
    @Operation(summary = "Delete issue type", description = "Deletes an issue type")
    public ResponseEntity<Void> deleteIssueType(
            @Parameter(description = "Issue Type ID") @PathVariable UUID id) {
        issueTypeService.deleteIssueType(id);
        return ResponseEntity.noContent().build();
    }
}