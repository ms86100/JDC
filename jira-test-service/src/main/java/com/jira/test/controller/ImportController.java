package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.CucumberImportService;
import com.jira.test.service.CiCdImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Test Import", description = "APIs for importing tests from CI/CD pipelines")
public class ImportController {

    private final CucumberImportService cucumberImportService;
    private final CiCdImportService ciCdImportService;

    @PostMapping(value = "/cucumber", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from Cucumber/Gherkin feature file")
    public ResponseEntity<CucumberImportResponse> importCucumberFeature(
            @Valid @RequestBody CucumberImportRequest request) {
        log.info("Received Cucumber import request for project: {}", request.getProjectId());

        CucumberImportResponse response = cucumberImportService.importFeatureFile(
                request.getProjectId(),
                request.getFeatureContent(),
                request.getFeatureFileName(),
                request.getTags(),
                request.getTestSetId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/cucumber/status/{jobId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get status of a Cucumber import job")
    public ResponseEntity<String> getCucumberImportStatus(@PathVariable UUID jobId, @RequestParam UUID projectId) {
        return ResponseEntity.ok("Import job status: COMPLETED");
    }

    @PostMapping(value = "/junit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from JUnit XML results")
    public ResponseEntity<JunitImportResponse> importJUnitResults(
            @Valid @RequestBody JunitImportRequest request) {
        log.info("Received JUnit import request for project: {}", request.getProjectId());

        JunitImportResponse response = ciCdImportService.importJUnitXml(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/junit/history")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get JUnit import history")
    public ResponseEntity<String> getJUnitImportHistory(@RequestParam UUID projectId) {
        return ResponseEntity.ok("JUnit import history endpoint");
    }

    @GetMapping("/ci-source")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Detect CI source from build URL")
    public ResponseEntity<String> detectCiSource(@RequestParam String buildUrl, @RequestParam UUID projectId) {
        String source = ciCdImportService.detectCiSource(buildUrl);
        return ResponseEntity.ok(source);
    }
}