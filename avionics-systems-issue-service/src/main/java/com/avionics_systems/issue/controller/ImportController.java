package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Import Controller - Cucumber/Gherkin and JUnit XML import
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Import/Export", description = "Test import/export APIs")
public class ImportController {

    private final ImportService importService;

    // ==================== Cucumber Import ====================

    @PostMapping(value = "/cucumber", consumes = "multipart/form-data")
    @Operation(summary = "Import Cucumber/Gherkin feature file")
    public ResponseEntity<CucumberImportResponse> importCucumber(
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID testSetId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importService.importCucumberFeature(projectId, testSetId, file, userId));
    }

    @GetMapping("/cucumber/status/{batchId}")
    @Operation(summary = "Get Cucumber import status")
    public ResponseEntity<ImportBatchResponse> getCucumberImportStatus(@PathVariable UUID batchId) {
        return ResponseEntity.ok(importService.getImportBatchStatus(batchId));
    }

    // ==================== JUnit XML Import ====================

    @PostMapping(value = "/junit", consumes = "multipart/form-data")
    @Operation(summary = "Import JUnit XML results from CI/CD")
    public ResponseEntity<JunitImportResponse> importJunit(
            @RequestParam UUID projectId,
            @RequestParam(required = false) UUID testSetId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String ciSource,
            @RequestParam(required = false) String ciBuildUrl,
            @RequestHeader("X-User-Id") UUID userId) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importService.importJunitXml(projectId, testSetId, file, ciSource, ciBuildUrl, userId));
    }

    @GetMapping("/junit/status/{batchId}")
    @Operation(summary = "Get JUnit import status")
    public ResponseEntity<ImportBatchResponse> getJunitImportStatus(@PathVariable UUID batchId) {
        return ResponseEntity.ok(importService.getImportBatchStatus(batchId));
    }

    // ==================== Import History ====================

    @GetMapping("/history")
    @Operation(summary = "Get import history")
    public ResponseEntity<List<ImportBatchResponse>> getImportHistory(@RequestParam UUID projectId) {
        return ResponseEntity.ok(importService.getImportHistory(projectId));
    }
}