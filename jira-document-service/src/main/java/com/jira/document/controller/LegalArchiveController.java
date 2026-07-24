package com.jira.document.controller;

import com.jira.document.dto.*;
import com.jira.document.service.DocumentService;
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
@RequestMapping("/api/legal-archives")
@RequiredArgsConstructor
@Tag(name = "Legal Archives", description = "Legal Archive management endpoints")
public class LegalArchiveController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create legal archive", description = "Creates a new legal archive for compliance")
    public ResponseEntity<LegalArchiveResponse> createLegalArchive(
            @Valid @RequestBody CreateLegalArchiveRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) { throw new IllegalArgumentException("X-User-Id header is required"); }
        UUID actor = userId;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createLegalArchive(request, actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get legal archive by ID", description = "Returns a specific legal archive")
    public ResponseEntity<LegalArchiveResponse> getLegalArchive(
            @Parameter(description = "Archive ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getLegalArchive(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project legal archives", description = "Returns all legal archives for a project")
    public ResponseEntity<List<LegalArchiveResponse>> getProjectArchives(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(documentService.getLegalArchivesByProject(projectId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update archive status", description = "Updates the status of a legal archive")
    public ResponseEntity<LegalArchiveResponse> updateStatus(
            @Parameter(description = "Archive ID") @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(documentService.updateLegalArchiveStatus(id, status));
    }
}