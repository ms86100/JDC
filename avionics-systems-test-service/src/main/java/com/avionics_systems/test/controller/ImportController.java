package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.CucumberImportService;
import com.avionics_systems.test.service.CiCdImportService;
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
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    @Operation(summary = "Import tests from Cucumber/Gherkin feature file (JSON with content)")
    public ResponseEntity<CucumberImportResponse> importCucumberJson(
            @Valid @RequestBody CucumberImportRequest request) {
        log.info("Received Cucumber import request (JSON) for project: {}", request.getProjectId());

        CucumberImportResponse response = cucumberImportService.importFeatureFile(
                request.getProjectId(),
                request.getFeatureContent(),
                request.getFeatureFileName(),
                request.getTags(),
                request.getTestSetId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/cucumber/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #projectId)")
    @Operation(summary = "Import tests from Cucumber/Gherkin feature file (multipart upload)")
    public ResponseEntity<CucumberImportResponse> importCucumberFile(
            @RequestParam UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) UUID testSetId) {
        log.info("Received Cucumber import request (multipart) for project: {}", projectId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return ResponseEntity.badRequest().build();
        }

        java.util.List<String> tagList = tags != null ? Arrays.asList(tags.split(",")) : null;

        CucumberImportResponse response = cucumberImportService.importFeatureFile(
                projectId,
                content,
                file.getOriginalFilename(),
                tagList,
                testSetId);

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
    @Operation(summary = "Import tests from JUnit XML results (JSON with content)")
    public ResponseEntity<JunitImportResponse> importJUnitJson(
            @Valid @RequestBody JunitImportRequest request) {
        log.info("Received JUnit import request (JSON) for project: {}", request.getProjectId());

        JunitImportResponse response = ciCdImportService.importJUnitXml(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/junit/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #projectId)")
    @Operation(summary = "Import tests from JUnit XML file (multipart upload)")
    public ResponseEntity<JunitImportResponse> importJUnitFile(
            @RequestParam UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String ciSource,
            @RequestParam(required = false) String ciBuildUrl,
            @RequestParam(required = false) UUID testSetId) {
        log.info("Received JUnit import request (multipart) for project: {}", projectId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return ResponseEntity.badRequest().build();
        }

        JunitImportRequest request = JunitImportRequest.builder()
                .projectId(projectId)
                .xmlContent(content)
                .ciSource(ciSource)
                .ciBuildUrl(ciBuildUrl)
                .testSetId(testSetId)
                .build();

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