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
@RequestMapping("/api/legal-holds")
@RequiredArgsConstructor
@Tag(name = "Legal Holds", description = "Legal Hold management endpoints")
@CrossOrigin(origins = "*")
public class LegalHoldController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create legal hold", description = "Creates a new legal hold for data preservation")
    public ResponseEntity<LegalHoldResponse> createLegalHold(
            @Valid @RequestBody CreateLegalHoldRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createLegalHold(request, actor));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate legal hold", description = "Activates a pending legal hold")
    public ResponseEntity<LegalHoldResponse> activateLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.activateLegalHold(id));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release legal hold", description = "Releases an active legal hold")
    public ResponseEntity<LegalHoldResponse> releaseLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(documentService.releaseLegalHold(id, actor, reason));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get legal hold by ID", description = "Returns a specific legal hold")
    public ResponseEntity<LegalHoldResponse> getLegalHold(
            @Parameter(description = "Hold ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getLegalHold(id));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active legal holds", description = "Returns all active legal holds")
    public ResponseEntity<List<LegalHoldResponse>> getActiveLegalHolds() {
        return ResponseEntity.ok(documentService.getActiveLegalHolds());
    }

    @GetMapping("/custodian/{custodianId}")
    @Operation(summary = "Get legal holds by custodian", description = "Returns all legal holds for a custodian")
    public ResponseEntity<List<LegalHoldResponse>> getLegalHoldsByCustodian(
            @Parameter(description = "Custodian User ID") @PathVariable UUID custodianId) {
        return ResponseEntity.ok(documentService.getLegalHoldsByCustodian(custodianId));
    }
}