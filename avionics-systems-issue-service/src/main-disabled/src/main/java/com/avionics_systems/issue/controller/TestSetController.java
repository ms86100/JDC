package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Test Set Controller
 */
@RestController
@RequestMapping("/api/test-sets")
@RequiredArgsConstructor
@Tag(name = "Test Sets", description = "Test set management APIs")
public class TestSetController {

    private final TestManagementService testService;

    @PostMapping
    @Operation(summary = "Create a new test set")
    public ResponseEntity<TestSetResponse> createTestSet(
            @RequestParam UUID projectId,
            @RequestBody CreateTestSetRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.createTestSet(projectId, request, userId));
    }

    @GetMapping("/{testSetId}")
    @Operation(summary = "Get a test set by ID")
    public ResponseEntity<TestSetResponse> getTestSet(@PathVariable UUID testSetId) {
        return ResponseEntity.ok(testService.getTestSet(testSetId));
    }

    @GetMapping
    @Operation(summary = "Get all test sets for a project")
    public ResponseEntity<List<TestSetResponse>> getTestSets(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getTestSetsByProject(projectId));
    }

    @PostMapping("/{testSetId}/tests")
    @Operation(summary = "Add a test to a test set")
    public ResponseEntity<TestSetResponse> addTestToSet(
            @PathVariable UUID testSetId,
            @RequestParam UUID testId) {
        return ResponseEntity.ok(testService.addTestToSet(testSetId, testId));
    }
}