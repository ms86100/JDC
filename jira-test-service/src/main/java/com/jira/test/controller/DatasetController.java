package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
@Tag(name = "Dataset Management", description = "APIs for managing test datasets")
public class DatasetController {

    private final DatasetService datasetService;

    // ==================== Dataset CRUD ====================

    @PostMapping
    @Operation(summary = "Create a new dataset")
    public ResponseEntity<DatasetResponse> createDataset(@Valid @RequestBody CreateDatasetRequest request) {
        DatasetResponse dataset = datasetService.createDataset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dataset);
    }

    @GetMapping("/{datasetId}")
    @Operation(summary = "Get a dataset by ID")
    public ResponseEntity<DatasetResponse> getDataset(@PathVariable UUID datasetId) {
        DatasetResponse dataset = datasetService.getDataset(datasetId);
        return ResponseEntity.ok(dataset);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all datasets for a project")
    public ResponseEntity<List<DatasetResponse>> getDatasetsByProject(@PathVariable UUID projectId) {
        List<DatasetResponse> datasets = datasetService.getDatasetsByProject(projectId);
        return ResponseEntity.ok(datasets);
    }

    @GetMapping("/search")
    @Operation(summary = "Search datasets by name")
    public ResponseEntity<List<DatasetResponse>> searchDatasets(
            @RequestParam UUID projectId,
            @RequestParam(required = false) String search) {
        List<DatasetResponse> datasets = datasetService.searchDatasets(projectId, search);
        return ResponseEntity.ok(datasets);
    }

    @PutMapping("/{datasetId}")
    @Operation(summary = "Update a dataset")
    public ResponseEntity<DatasetResponse> updateDataset(
            @PathVariable UUID datasetId,
            @Valid @RequestBody UpdateDatasetRequest request) {
        DatasetResponse dataset = datasetService.updateDataset(datasetId, request);
        return ResponseEntity.ok(dataset);
    }

    @DeleteMapping("/{datasetId}")
    @Operation(summary = "Archive a dataset")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID datasetId) {
        datasetService.deleteDataset(datasetId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Versioning ====================

    @PostMapping("/{datasetId}/versions")
    @Operation(summary = "Create a new version snapshot")
    public ResponseEntity<DatasetVersionResponse> createVersion(
            @PathVariable UUID datasetId,
            @RequestParam(required = false) String changeSummary) {
        DatasetVersionResponse version = datasetService.createVersion(datasetId, changeSummary, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @GetMapping("/{datasetId}/versions")
    @Operation(summary = "Get all versions of a dataset")
    public ResponseEntity<List<DatasetVersionResponse>> getVersions(@PathVariable UUID datasetId) {
        List<DatasetVersionResponse> versions = datasetService.getVersions(datasetId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{datasetId}/versions/{versionNumber}")
    @Operation(summary = "Get a specific version")
    public ResponseEntity<DatasetVersionResponse> getVersion(
            @PathVariable UUID datasetId,
            @PathVariable Integer versionNumber) {
        DatasetVersionResponse version = datasetService.getVersion(datasetId, versionNumber);
        return ResponseEntity.ok(version);
    }

    @GetMapping("/{datasetId}/snapshot")
    @Operation(summary = "Get immutable snapshot for execution")
    public ResponseEntity<DatasetResponse> getImmutableSnapshot(
            @PathVariable UUID datasetId,
            @RequestParam UUID executionId) {
        DatasetResponse snapshot = datasetService.getImmutableSnapshot(datasetId, executionId);
        return ResponseEntity.ok(snapshot);
    }

    // ==================== Import/Export ====================

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import dataset from CSV or JSON file")
    public ResponseEntity<DatasetResponse> importDataset(
            @RequestParam UUID projectId,
            @RequestParam String format, // CSV or JSON
            @RequestParam(required = false) String name,
            @RequestParam MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        DatasetResponse dataset;
        if ("CSV".equalsIgnoreCase(format)) {
            dataset = datasetService.importFromCSV(projectId, content, name != null ? name : file.getOriginalFilename());
        } else {
            dataset = datasetService.importFromJSON(projectId, content, name);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dataset);
    }

    @GetMapping("/{datasetId}/export")
    @Operation(summary = "Export dataset to CSV")
    public ResponseEntity<String> exportToCSV(@PathVariable UUID datasetId) {
        String csv = datasetService.exportToCSV(datasetId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dataset.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping(value = "/{datasetId}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Export dataset to JSON")
    public ResponseEntity<String> exportToJSON(@PathVariable UUID datasetId) {
        String json = datasetService.exportToJSON(datasetId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dataset.json")
                .body(json);
    }

    // ==================== Binding ====================

    @PostMapping("/bind")
    @Operation(summary = "Bind a dataset to a test")
    public ResponseEntity<DatasetBindingResponse> bindToTest(@Valid @RequestBody BindDatasetRequest request) {
        DatasetBindingResponse binding = datasetService.bindToTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(binding);
    }

    @DeleteMapping("/bind/{testId}/{datasetId}")
    @Operation(summary = "Unbind a dataset from a test")
    public ResponseEntity<Void> unbindFromTest(
            @PathVariable UUID testId,
            @PathVariable UUID datasetId) {
        datasetService.unbindFromTest(testId, datasetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bind/test/{testId}")
    @Operation(summary = "Get all dataset bindings for a test")
    public ResponseEntity<List<DatasetBindingResponse>> getBindingsForTest(@PathVariable UUID testId) {
        List<DatasetBindingResponse> bindings = datasetService.getBindingsForTest(testId);
        return ResponseEntity.ok(bindings);
    }

    @GetMapping("/bind/dataset/{datasetId}/tests")
    @Operation(summary = "Get all tests bound to a dataset")
    public ResponseEntity<List<DatasetBindingResponse>> getTestsBoundToDataset(@PathVariable UUID datasetId) {
        List<DatasetBindingResponse> bindings = datasetService.getTestsBoundToDataset(datasetId);
        return ResponseEntity.ok(bindings);
    }

    // ==================== Execution Expansion ====================

    @GetMapping("/{datasetId}/expand")
    @Operation(summary = "Get expanded parameters for test execution")
    public ResponseEntity<List<Map<String, String>>> expandParameters(
            @PathVariable UUID datasetId,
            @RequestParam UUID testId) {
        List<Map<String, String>> expanded = datasetService.expandParameters(testId, datasetId);
        return ResponseEntity.ok(expanded);
    }
}