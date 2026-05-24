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
@RequestMapping("/api/legal")
@RequiredArgsConstructor
@Tag(name = "Legal", description = "Legal Archive and Legal Holds endpoints")
@CrossOrigin(origins = "*")
public class LegalController {

    private final DocumentService documentService;

    // Legal Archive Endpoints
    @PostMapping("/archives")
    @Operation(summary = "Create legal archive", description = "Creates a new legal archive")
    public ResponseEntity<LegalArchiveResponse> createLegalArchive(
            @Valid @RequestBody CreateLegalArchiveRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createLegalArchive(request, actor));
    }

    @GetMapping("/archives/{id}")
    @Operation(summary = "Get legal archive by ID", description = "Returns a specific legal archive")
    public ResponseEntity<LegalArchiveResponse> getLegalArchive(
            @Parameter(description = "Archive ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getLegalArchive(id));
    }

    @GetMapping("/archives/project/{projectId}")
    @Operation(summary = "Get project legal archives", description = "Returns all legal archives for a project")
    public ResponseEntity<List<LegalArchiveResponse>> getProjectLegalArchives(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(documentService.getLegalArchivesByProject(projectId));
    }

    @PutMapping("/archives/{id}/status")
    @Operation(summary = "Update legal archive status", description = "Updates the status of a legal archive")
    public ResponseEntity<LegalArchiveResponse> updateArchiveStatus(
            @Parameter(description = "Archive ID") @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(documentService.updateLegalArchiveStatus(id, status));
    }

    // Legal Hold Endpoints
    @PostMapping("/holds")
    @Operation(summary = "Create legal hold", description = "Creates a new legal hold")
    public ResponseEntity<LegalHoldResponse> createLegalHold(
            @Valid @RequestBody CreateLegalHoldRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createLegalHold(request, actor));
    }

    @GetMapping("/holds/{id}")
    @Operation(summary = "Get legal hold by ID", description = "Returns a specific legal hold")
    public ResponseEntity<LegalHoldResponse> getLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getLegalHold(id));
    }

    @GetMapping("/holds/active")
    @Operation(summary = "Get active legal holds", description = "Returns all active legal holds")
    public ResponseEntity<List<LegalHoldResponse>> getActiveLegalHolds() {
        return ResponseEntity.ok(documentService.getActiveLegalHolds());
    }

    @GetMapping("/holds/custodian/{custodianId}")
    @Operation(summary = "Get legal holds by custodian", description = "Returns legal holds for a specific custodian")
    public ResponseEntity<List<LegalHoldResponse>> getLegalHoldsByCustodian(
            @Parameter(description = "Custodian ID") @PathVariable UUID custodianId) {
        return ResponseEntity.ok(documentService.getLegalHoldsByCustodian(custodianId));
    }

    @PostMapping("/holds/{id}/activate")
    @Operation(summary = "Activate legal hold", description = "Activates a pending legal hold")
    public ResponseEntity<LegalHoldResponse> activateLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.activateLegalHold(id));
    }

    @PostMapping("/holds/{id}/release")
    @Operation(summary = "Release legal hold", description = "Releases an active legal hold")
    public ResponseEntity<LegalHoldResponse> releaseLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id,
            @RequestParam String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(documentService.releaseLegalHold(id, actor, reason));
    }
}