package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-executions")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "APIs for managing test executions")
public class TestExecutionController {

    private final TestExecutionService executionService;

    @PostMapping
    @Operation(summary = "Create a new test execution")
    public ResponseEntity<TestExecutionResponse> createExecution(@Valid @RequestBody CreateExecutionRequest request) {
        TestExecutionResponse execution = executionService.createExecution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(execution);
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "Get a test execution by ID")
    public ResponseEntity<TestExecutionResponse> getExecution(@PathVariable UUID executionId) {
        TestExecutionResponse execution = executionService.getExecution(executionId);
        return ResponseEntity.ok(execution);
    }

    @GetMapping("/history")
    @Operation(summary = "Get execution history")
    public ResponseEntity<List<TestExecutionResponse>> getExecutionHistory(
            @RequestParam(required = false) UUID testerId,
            @RequestParam(required = false) LocalDateTime since) {
        List<TestExecutionResponse> executions = executionService.getExecutionHistory(testerId, since);
        return ResponseEntity.ok(executions);
    }

    @PutMapping("/{executionId}/steps/{stepId}")
    @Operation(summary = "Update a step result")
    public ResponseEntity<StepResultResponse> updateStepResult(
            @PathVariable UUID executionId,
            @PathVariable UUID stepId,
            @Valid @RequestBody StepResultUpdateRequest request) {
        StepResultResponse stepResult = executionService.updateStepResult(executionId, stepId, request);
        return ResponseEntity.ok(stepResult);
    }

    @PutMapping("/{executionId}/complete")
    @Operation(summary = "Complete a test execution")
    public ResponseEntity<TestExecutionResponse> completeExecution(
            @PathVariable UUID executionId,
            @RequestParam(required = false) String status) {
        TestExecutionResponse execution = executionService.completeExecution(executionId, status);
        return ResponseEntity.ok(execution);
    }

    @PostMapping("/{executionId}/evidence")
    @Operation(summary = "Add evidence to a step result")
    public ResponseEntity<StepResultResponse> addEvidence(
            @PathVariable UUID executionId,
            @RequestBody StepResultUpdateRequest request) {
        StepResultResponse stepResult = executionService.addEvidence(
                executionId, null, request.getEvidenceUrls() != null ? request.getEvidenceUrls() : List.of());
        return ResponseEntity.ok(stepResult);
    }

    @PostMapping("/{executionId}/defects")
    @Operation(summary = "Link a defect to an execution")
    public ResponseEntity<DefectLinkResponse> linkDefect(
            @PathVariable UUID executionId,
            @Valid @RequestBody DefectLinkRequest request) {
        DefectLinkResponse defectLink = executionService.linkDefect(
                executionId, request.getStepResultId(), request.getDefectKey(), request.getSeverity());
        return ResponseEntity.status(HttpStatus.CREATED).body(defectLink);
    }
}