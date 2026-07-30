package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/test-executions")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "APIs for managing test executions")
public class TestExecutionController {

    private final TestExecutionService executionService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new test execution")
    public ResponseEntity<TestExecutionResponse> createExecution(@Valid @RequestBody CreateExecutionRequest request) {
        TestExecutionResponse execution = executionService.createExecution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(execution);
    }

    @GetMapping("/{executionId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test execution by ID")
    public ResponseEntity<TestExecutionResponse> getExecution(@PathVariable UUID executionId, @RequestParam UUID projectId) {
        TestExecutionResponse execution = executionService.getExecution(executionId);
        return ResponseEntity.ok(execution);
    }

    @GetMapping("/history")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get execution history")
    public ResponseEntity<List<TestExecutionResponse>> getExecutionHistory(
            @RequestParam(required = false) UUID testerId,
            @RequestParam(required = false) LocalDateTime since,
            @RequestParam UUID projectId) {
        List<TestExecutionResponse> executions = executionService.getExecutionHistory(testerId, since);
        return ResponseEntity.ok(executions);
    }

    @PutMapping("/{executionId}/steps/{stepId}")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #projectId)")
    @Operation(summary = "Update a step result")
    public ResponseEntity<StepResultResponse> updateStepResult(
            @PathVariable UUID executionId,
            @PathVariable UUID stepId,
            @RequestParam UUID projectId,
            @Valid @RequestBody StepResultUpdateRequest request) {
        StepResultResponse stepResult = executionService.updateStepResult(executionId, stepId, request);
        return ResponseEntity.ok(stepResult);
    }

    @PutMapping("/{executionId}/complete")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #projectId)")
    @Operation(summary = "Complete a test execution")
    public ResponseEntity<TestExecutionResponse> completeExecution(
            @PathVariable UUID executionId,
            @RequestParam(required = false) String status,
            @RequestParam UUID projectId) {
        TestExecutionResponse execution = executionService.completeExecution(executionId, status);
        return ResponseEntity.ok(execution);
    }

    @PostMapping("/{executionId}/evidence")
    @PreAuthorize("@projectSecurity.canExecuteTests(authentication, #projectId)")
    @Operation(summary = "Add evidence to a step result")
    public ResponseEntity<StepResultResponse> addEvidence(
            @PathVariable UUID executionId,
            @RequestParam UUID projectId,
            @RequestBody StepResultUpdateRequest request) {
        StepResultResponse stepResult = executionService.addEvidence(
                executionId, null, request.getEvidenceUrls() != null ? request.getEvidenceUrls() : List.of());
        return ResponseEntity.ok(stepResult);
    }

    @PostMapping("/{executionId}/defects")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Link a defect to an execution")
    public ResponseEntity<DefectLinkResponse> linkDefect(
            @PathVariable UUID executionId,
            @RequestParam UUID projectId,
            @Valid @RequestBody DefectLinkRequest request) {
        DefectLinkResponse defectLink = executionService.linkDefect(
                executionId, request.getStepResultId(), request.getDefectKey(), request.getSeverity());
        return ResponseEntity.status(HttpStatus.CREATED).body(defectLink);
    }
}