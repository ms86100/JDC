package com.jira.issue.controller;

import com.jira.issue.dto.*;
import com.jira.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Test Environment Controller
 */
@RestController
@RequestMapping("/api/test-environments")
@RequiredArgsConstructor
@Tag(name = "Test Environments", description = "Test environment management APIs")
public class TestEnvironmentController {

    private final TestManagementService testService;

    @PostMapping
    @Operation(summary = "Create a new test environment")
    public ResponseEntity<TestEnvironmentResponse> createEnvironment(
            @RequestParam UUID projectId,
            @RequestBody CreateEnvironmentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.createTestEnvironment(projectId, request, userId));
    }

    @GetMapping
    @Operation(summary = "Get all test environments for a project")
    public ResponseEntity<List<TestEnvironmentResponse>> getEnvironments(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getEnvironmentsByProject(projectId));
    }

    @GetMapping("/{envId}")
    @Operation(summary = "Get a test environment by ID")
    public ResponseEntity<TestEnvironmentResponse> getEnvironment(@PathVariable UUID envId) {
        return ResponseEntity.ok(testService.getEnvironment(envId));
    }

    @PutMapping("/{envId}")
    @Operation(summary = "Update a test environment")
    public ResponseEntity<TestEnvironmentResponse> updateEnvironment(
            @PathVariable UUID envId,
            @RequestBody CreateEnvironmentRequest request) {
        return ResponseEntity.ok(testService.updateEnvironment(envId, request));
    }

    @DeleteMapping("/{envId}")
    @Operation(summary = "Delete (deactivate) a test environment")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable UUID envId) {
        testService.deleteEnvironment(envId);
        return ResponseEntity.noContent().build();
    }
}