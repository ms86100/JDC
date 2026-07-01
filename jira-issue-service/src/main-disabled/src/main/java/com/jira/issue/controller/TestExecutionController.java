package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Test Execution Controller
 */
@RestController
@RequestMapping("/api/test-executions")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "Test execution management APIs")
public class TestExecutionController {

    private final TestManagementService testService;

    @PostMapping
    @Operation(summary = "Start a new test execution")
    public ResponseEntity<TestExecutionResponse> startExecution(
            @RequestBody CreateExecutionRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.startExecution(request.getProjectId(), request, userId));
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get a test execution by ID")
    public ResponseEntity<TestExecutionResponse> getExecution(@PathVariable UUID executionId) {
        return ResponseEntity.ok(testService.getExecution(executionId));
    }

    @GetMapping
    @Operation(summary = "Get all test executions for a project")
    public ResponseEntity<java.util.List<TestExecutionResponse>> getExecutions(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getExecutionsByProject(projectId));
    }

    @PostMapping("/{executionId}/steps/{testId}/{stepOrder}")
    @Operation(summary = "Record a step result")
    public ResponseEntity<StepResultResponse> recordStepResult(
            @PathVariable UUID executionId,
            @PathVariable UUID testId,
            @PathVariable Integer stepOrder,
            @RequestBody StepResultUpdateRequest request) {
        return ResponseEntity.ok(testService.recordStepResult(executionId, testId, stepOrder, request));
    }

    @PutMapping("/{executionId}/complete")
    @Operation(summary = "Complete a test execution")
    public ResponseEntity<TestExecutionResponse> completeExecution(@PathVariable UUID executionId) {
        return ResponseEntity.ok(testService.completeExecution(executionId));
    }

    @GetMapping("/{executionId}/steps")
    @Operation(summary = "Get all step results for an execution")
    public ResponseEntity<java.util.List<StepResultResponse>> getStepResults(@PathVariable UUID executionId) {
        return ResponseEntity.ok(testService.getStepResultsByExecution(executionId));
    }
}