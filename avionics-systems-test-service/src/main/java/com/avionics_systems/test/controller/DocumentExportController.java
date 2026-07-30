package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.CreateExportTemplateRequest;
import com.avionics_systems.test.dto.ExportTemplateResponse;
import com.avionics_systems.test.service.DocumentExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/export-templates")
@RequiredArgsConstructor
@Tag(name = "Document Export (XPorter)", description = "Template-based document export for LTRA, VVO coverage, and defect reports")
public class DocumentExportController {

    private final DocumentExportService exportService;

    // ── Template CRUD ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List templates, optionally filtered by source type")
    public ResponseEntity<List<ExportTemplateResponse>> getAllTemplates(
            @RequestParam(required = false) String sourceType) {
        List<ExportTemplateResponse> templates;
        if (sourceType != null && !sourceType.isBlank()) {
            templates = exportService.getTemplatesBySourceType(sourceType);
        } else {
            templates = exportService.getSystemTemplates();
        }
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/system")
    @Operation(summary = "List all system (built-in) templates")
    public ResponseEntity<List<ExportTemplateResponse>> getSystemTemplates() {
        return ResponseEntity.ok(exportService.getSystemTemplates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<ExportTemplateResponse> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(exportService.getTemplate(id));
    }

    @PostMapping
    @Operation(summary = "Create a new export template")
    public ResponseEntity<ExportTemplateResponse> createTemplate(
            @Valid @RequestBody CreateExportTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exportService.createTemplate(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an export template")
    public ResponseEntity<ExportTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExportTemplateRequest request) {
        return ResponseEntity.ok(exportService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an export template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        exportService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Document Generation ────────────────────────────────────────────────

    @PostMapping("/{templateId}/generate")
    @Operation(summary = "Generate document from template")
    public ResponseEntity<String> generateDocument(
            @PathVariable UUID templateId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID fixVersionId,
            @RequestParam(required = false) UUID testPlanId) {
        String content = exportService.generateDocument(templateId, projectId, fixVersionId, testPlanId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=export.csv")
                .body(content);
    }
}
