package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.StepResult;
import com.avionics_systems.test.entity.TestExecution;
import com.avionics_systems.test.entity.TestIssue;
import com.avionics_systems.test.repository.StepResultRepository;
import com.avionics_systems.test.repository.TestExecutionRepository;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.service.CucumberImportService;
import com.avionics_systems.test.service.CiCdImportService;
import com.avionics_systems.test.service.TestNgImportService;
import com.avionics_systems.test.service.NUnitImportService;
import com.avionics_systems.test.service.RobotFrameworkImportService;
import com.avionics_systems.test.event.EventPublisherService;
import com.avionics_systems.test.event.TestImportedEvent;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Test Import", description = "APIs for importing tests from CI/CD pipelines")
public class ImportController {

    private final CucumberImportService cucumberImportService;
    private final CiCdImportService ciCdImportService;
    private final TestNgImportService testNgImportService;
    private final NUnitImportService nUnitImportService;
    private final RobotFrameworkImportService robotFrameworkImportService;
    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final EventPublisherService eventPublisher;

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

    // ─── TestNG endpoints ───────────────────────────────────────────────

    @PostMapping(value = "/testng", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from TestNG XML results (JSON with content)")
    public ResponseEntity<TestNgImportResponse> importTestNgJson(
            @Valid @RequestBody XmlImportRequest request) {
        log.info("Received TestNG import request (JSON) for project: {}", request.getProjectId());

        TestNgImportResponse response = testNgImportService.importTestNgXml(
                request.getProjectId(),
                request.getXmlContent(),
                request.getTestSetId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/testng/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #projectId)")
    @Operation(summary = "Import tests from TestNG XML file (multipart upload)")
    public ResponseEntity<TestNgImportResponse> importTestNgFile(
            @RequestParam UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID testSetId) {
        log.info("Received TestNG import request (multipart) for project: {}", projectId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return ResponseEntity.badRequest().build();
        }

        TestNgImportResponse response = testNgImportService.importTestNgXml(projectId, content, testSetId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── NUnit endpoints ────────────────────────────────────────────────

    @PostMapping(value = "/nunit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from NUnit XML results (JSON with content)")
    public ResponseEntity<NUnitImportResponse> importNUnitJson(
            @Valid @RequestBody XmlImportRequest request) {
        log.info("Received NUnit import request (JSON) for project: {}", request.getProjectId());

        NUnitImportResponse response = nUnitImportService.importNUnitXml(
                request.getProjectId(),
                request.getXmlContent(),
                request.getTestSetId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/nunit/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #projectId)")
    @Operation(summary = "Import tests from NUnit XML file (multipart upload)")
    public ResponseEntity<NUnitImportResponse> importNUnitFile(
            @RequestParam UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID testSetId) {
        log.info("Received NUnit import request (multipart) for project: {}", projectId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return ResponseEntity.badRequest().build();
        }

        NUnitImportResponse response = nUnitImportService.importNUnitXml(projectId, content, testSetId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Robot Framework endpoints ──────────────────────────────────────

    @PostMapping(value = "/robot", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from Robot Framework XML results (JSON with content)")
    public ResponseEntity<RobotImportResponse> importRobotJson(
            @Valid @RequestBody XmlImportRequest request) {
        log.info("Received Robot Framework import request (JSON) for project: {}", request.getProjectId());

        RobotImportResponse response = robotFrameworkImportService.importRobotXml(
                request.getProjectId(),
                request.getXmlContent(),
                request.getTestSetId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/robot/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #projectId)")
    @Operation(summary = "Import tests from Robot Framework XML file (multipart upload)")
    public ResponseEntity<RobotImportResponse> importRobotFile(
            @RequestParam UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID testSetId) {
        log.info("Received Robot Framework import request (multipart) for project: {}", projectId);

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file content", e);
            return ResponseEntity.badRequest().build();
        }

        RobotImportResponse response = robotFrameworkImportService.importRobotXml(projectId, content, testSetId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Generic import endpoint ────────────────────────────────────────

    @PostMapping(value = "/generic", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@projectSecurity.canImportTests(authentication, #request.projectId)")
    @Operation(summary = "Import tests from generic JSON results")
    public ResponseEntity<JunitImportResponse> importGeneric(
            @Valid @RequestBody GenericImportRequest request) {
        log.info("Received generic import request for project: {}", request.getProjectId());

        List<String> errors = new ArrayList<>();
        List<TestResponse> createdTests = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int successCount = 0;
        int failureCount = 0;
        UUID batchId = UUID.randomUUID();

        try {
            TestExecution execution = TestExecution.builder()
                    .testId(null)
                    .name("Generic Import: " + LocalDateTime.now())
                    .description("Auto-imported from generic JSON results")
                    .status("RUNNING")
                    .testEnv("CI")
                    .totalTests(request.getResults() != null ? request.getResults().size() : 0)
                    .passedTests(0)
                    .failedTests(0)
                    .blockedTests(0)
                    .notRunTests(0)
                    .startedAt(LocalDateTime.now())
                    .build();

            if (request.getTestSetId() != null) {
                execution.setTestSetId(request.getTestSetId());
            }

            execution = executionRepository.save(execution);

            if (request.getResults() != null) {
                for (GenericImportRequest.GenericTestResult result : request.getResults()) {
                    try {
                        TestIssue test = testIssueRepository.findByProjectIdAndName(
                                        request.getProjectId(), result.getName())
                                .orElse(null);

                        if (test == null) {
                            test = TestIssue.builder()
                                    .projectId(request.getProjectId())
                                    .name(result.getName())
                                    .description("Auto-created from generic import")
                                    .testType("AUTOMATED")
                                    .labels(List.of("automated", "generic-import"))
                                    .status(mapGenericStatus(result.getStatus()))
                                    .build();
                            test = testIssueRepository.save(test);
                            createdTests.add(mapToTestResponse(test));
                            successCount++;
                        }

                        String mappedStatus = mapGenericStatus(result.getStatus());

                        StepResult stepResult = StepResult.builder()
                                .executionId(execution.getId())
                                .stepId(test.getId())
                                .status(mappedStatus)
                                .executedAt(LocalDateTime.now())
                                .build();
                        stepResultRepository.save(stepResult);

                        if ("PASSED".equals(mappedStatus)) passed++;
                        else if ("FAILED".equals(mappedStatus)) failed++;
                        else skipped++;

                    } catch (Exception e) {
                        log.warn("Failed to process generic test '{}': {}", result.getName(), e.getMessage());
                        errors.add("Test '" + result.getName() + "': " + e.getMessage());
                        failureCount++;
                    }
                }
            }

            execution.setPassedTests(passed);
            execution.setFailedTests(failed);
            execution.setNotRunTests(skipped);
            execution.setStatus(failed == 0 ? "PASSED" : "FAILED");
            execution.setFinishedAt(LocalDateTime.now());
            executionRepository.save(execution);

            log.info("Generic import completed: {} passed, {} failed, {} skipped", passed, failed, skipped);

            publishGenericImportedEvent(request.getProjectId(), batchId,
                    request.getResults() != null ? request.getResults().size() : 0,
                    successCount, failureCount, errors, request.getTestSetId());

            return ResponseEntity.status(HttpStatus.CREATED).body(JunitImportResponse.builder()
                    .batchId(batchId)
                    .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                    .totalTests(request.getResults() != null ? request.getResults().size() : 0)
                    .passed(passed)
                    .failed(failed)
                    .skipped(skipped)
                    .message("Import completed successfully")
                    .createdTests(createdTests)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process generic import: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CREATED).body(JunitImportResponse.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .totalTests(0)
                    .passed(0)
                    .failed(0)
                    .skipped(0)
                    .message("Failed to process generic import: " + e.getMessage())
                    .createdTests(List.of())
                    .build());
        }
    }

    private String mapGenericStatus(String status) {
        if (status == null) return "SKIPPED";
        return switch (status.toUpperCase()) {
            case "PASS", "PASSED", "SUCCESS" -> "PASSED";
            case "FAIL", "FAILED", "ERROR" -> "FAILED";
            case "SKIP", "SKIPPED", "BLOCKED" -> "SKIPPED";
            default -> "SKIPPED";
        };
    }

    private void publishGenericImportedEvent(UUID projectId, UUID batchId, int totalImported,
                                             int successCount, int failureCount,
                                             List<String> errors, UUID testPlanId) {
        try {
            TestImportedEvent event = TestImportedEvent.builder()
                    .source(this)
                    .projectId(projectId)
                    .batchId(batchId)
                    .importSource("GENERIC")
                    .importType("GENERIC_JSON")
                    .totalImported(totalImported)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .errors(errors)
                    .testPlanId(testPlanId)
                    .build();
            eventPublisher.publish(event);
            log.info("Published TestImportedEvent for generic batch: {}", batchId);
        } catch (Exception e) {
            log.error("Failed to publish TestImportedEvent: {}", e.getMessage(), e);
        }
    }

    private TestResponse mapToTestResponse(TestIssue test) {
        return TestResponse.builder()
                .id(test.getId())
                .projectId(test.getProjectId())
                .name(test.getName())
                .description(test.getDescription())
                .testType(test.getTestType())
                .status(test.getStatus())
                .labels(test.getLabels())
                .priority(test.getPriority())
                .ownerId(test.getOwnerId())
                .archived(test.getArchived())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }
}