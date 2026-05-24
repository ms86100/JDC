package com.jira.document.controller;

import com.jira.document.dto.*;
import com.jira.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management endpoints")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create a document", description = "Creates a new document")
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(request, actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Returns a specific document")
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Document ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my documents", description = "Returns paginated list of user's documents")
    public ResponseEntity<Page<DocumentResponse>> getMyDocuments(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(documentService.getDocumentsByOwner(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project documents", description = "Returns all documents for a project")
    public ResponseEntity<List<DocumentResponse>> getProjectDocuments(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        return ResponseEntity.ok(documentService.getDocumentsByProject(projectId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document", description = "Updates an existing document")
    public ResponseEntity<DocumentResponse> updateDocument(
            @Parameter(description = "Document ID") @PathVariable UUID id,
            @Valid @RequestBody CreateDocumentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.ok(documentService.updateDocument(id, request, actor));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive document", description = "Archives a document")
    public ResponseEntity<Void> archiveDocument(
            @Parameter(description = "Document ID") @PathVariable UUID id) {
        documentService.archiveDocument(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Permanently deletes a document")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Document ID") @PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get document versions", description = "Returns all versions of a document")
    public ResponseEntity<List<DocumentVersionResponse>> getDocumentVersions(
            @Parameter(description = "Document ID") @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocumentVersions(id));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Create document version", description = "Creates a new version of a document")
    public ResponseEntity<DocumentVersionResponse> createVersion(
            @Parameter(description = "Document ID") @PathVariable UUID id,
            @RequestBody com.jira.document.dto.CreateDocumentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createVersion(id, request.getContent(), "Updated", actor));
    }
}