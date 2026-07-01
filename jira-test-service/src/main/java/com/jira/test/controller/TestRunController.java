package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.TestRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-runs")
@RequiredArgsConstructor
@Tag(name = "Test Runs", description = "APIs for managing individual test execution runs")
public class TestRunController {

    private final TestRunService testRunService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new test run")
    public ResponseEntity<TestRunResponse> createTestRun(@Valid @RequestBody CreateTestRunRequest request) {
        TestRunResponse response = testRunService.createTestRun(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{runId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #runId)")
    @Operation(summary = "Get a test run by ID")
    public ResponseEntity<TestRunResponse> getTestRun(@PathVariable UUID runId) {
        TestRunResponse response = testRunService.getTestRun(runId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Get all runs for a test")
    public ResponseEntity<List<TestRunResponse>> getTestRunsByTestId(@PathVariable UUID testId) {
        List<TestRunResponse> response = testRunService.getTestRunsByTestId(testId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/{testId}/latest")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Get the latest run for a test")
    public ResponseEntity<TestRunResponse> getLatestRun(@PathVariable UUID testId) {
        TestRunResponse response = testRunService.getLatestRun(testId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/{testId}/stats")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Get test statistics (pass rate, duration, flakiness)")
    public ResponseEntity<TestRunStatsResponse> getTestStats(@PathVariable UUID testId) {
        TestRunStatsResponse response = testRunService.getTestStats(testId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test/{testId}/history")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #testId)")
    @Operation(summary = "Get test run history")
    public ResponseEntity<List<TestRunResponse>> getTestHistory(
            @PathVariable UUID testId,
            @Parameter(description = "Number of days of history to retrieve")
            @RequestParam(defaultValue = "30") int days) {
        List<TestRunResponse> response = testRunService.getTestHistory(testId, days);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{runId}/start")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #runId)")
    @Operation(summary = "Start a test run")
    public ResponseEntity<TestRunResponse> startTestRun(@PathVariable UUID runId) {
        TestRunResponse response = testRunService.startTestRun(runId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{runId}/complete")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #runId)")
    @Operation(summary = "Complete a test run")
    public ResponseEntity<TestRunResponse> completeTestRun(
            @PathVariable UUID runId,
            @Valid @RequestBody CompleteTestRunRequest request) {
        TestRunResponse response = testRunService.completeTestRun(runId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{runId}/retry")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #runId)")
    @Operation(summary = "Retry a test run")
    public ResponseEntity<TestRunResponse> retryTestRun(@PathVariable UUID runId) {
        TestRunResponse response = testRunService.retryTestRun(runId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/execution/{executionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #executionId)")
    @Operation(summary = "Get all runs for an execution")
    public ResponseEntity<List<TestRunResponse>> getRunsByExecution(@PathVariable UUID executionId) {
        List<TestRunResponse> response = testRunService.getRunsByExecution(executionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all runs for a project")
    public ResponseEntity<List<TestRunResponse>> getRunsByProject(@PathVariable UUID projectId) {
        List<TestRunResponse> response = testRunService.getRunsByProject(projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}/environment/{environment}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get runs by environment")
    public ResponseEntity<List<TestRunResponse>> getRunsByEnvironment(
            @PathVariable UUID projectId,
            @PathVariable String environment) {
        List<TestRunResponse> response = testRunService.getRunsByEnvironment(projectId, environment);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #userId)")
    @Operation(summary = "Get runs executed by a user")
    public ResponseEntity<List<TestRunResponse>> getRunsByUser(@PathVariable UUID userId) {
        List<TestRunResponse> response = testRunService.getRunsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}/date-range")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get runs by project within date range")
    public ResponseEntity<List<TestRunResponse>> getRunsByDateRange(
            @PathVariable UUID projectId,
            @Parameter(description = "Start date/time (ISO format)")
            @RequestParam LocalDateTime start,
            @Parameter(description = "End date/time (ISO format)")
            @RequestParam LocalDateTime end) {
        List<TestRunResponse> response = testRunService.getRunsByProjectAndDateRange(projectId, start, end);
        return ResponseEntity.ok(response);
    }
}