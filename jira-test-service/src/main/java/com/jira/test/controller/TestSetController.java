package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test-sets")
@RequiredArgsConstructor
@Tag(name = "Test Sets", description = "APIs for managing test sets")
public class TestSetController {

    private final TestService testService;

    @PostMapping
    @Operation(summary = "Create a new test set")
    public ResponseEntity<TestSetResponse> createTestSet(@Valid @RequestBody CreateTestSetRequest request) {
        TestSetResponse testSet = testService.createTestSet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(testSet);
    }

    @GetMapping("/{setId}")
    @Operation(summary = "Get a test set by ID")
    public ResponseEntity<TestSetResponse> getTestSet(@PathVariable UUID setId) {
        TestSetResponse testSet = testService.getTestSet(setId);
        return ResponseEntity.ok(testSet);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all test sets for a project")
    public ResponseEntity<java.util.List<TestSetResponse>> getTestSetsByProject(@PathVariable UUID projectId) {
        java.util.List<TestSetResponse> testSets = testService.getTestSetsByProject(projectId);
        return ResponseEntity.ok(testSets);
    }

    @PostMapping("/{setId}/tests")
    @Operation(summary = "Add a test to a test set")
    public ResponseEntity<TestSetResponse> addTestToSet(
            @PathVariable UUID setId,
            @RequestBody CreateTestSetItemRequest request) {
        TestSetResponse testSet = testService.addTestToSet(setId, request.getTestId());
        return ResponseEntity.ok(testSet);
    }

    @DeleteMapping("/{setId}/tests/{testId}")
    @Operation(summary = "Remove a test from a test set")
    public ResponseEntity<Void> removeTestFromSet(
            @PathVariable UUID setId,
            @PathVariable UUID testId) {
        testService.removeTestFromSet(setId, testId);
        return ResponseEntity.noContent().build();
    }
}