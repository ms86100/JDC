package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test-sets")
@RequiredArgsConstructor
@Tag(name = "Test Sets", description = "APIs for managing test sets")
public class TestSetController {

    private final TestService testService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new test set")
    public ResponseEntity<TestSetResponse> createTestSet(@Valid @RequestBody CreateTestSetRequest request) {
        TestSetResponse testSet = testService.createTestSet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(testSet);
    }

    @GetMapping("/{setId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test set by ID")
    public ResponseEntity<TestSetResponse> getTestSet(@PathVariable UUID setId, @RequestParam UUID projectId) {
        TestSetResponse testSet = testService.getTestSet(setId);
        return ResponseEntity.ok(testSet);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all test sets for a project")
    public ResponseEntity<java.util.List<TestSetResponse>> getTestSetsByProject(@PathVariable UUID projectId) {
        java.util.List<TestSetResponse> testSets = testService.getTestSetsByProject(projectId);
        return ResponseEntity.ok(testSets);
    }

    @PostMapping("/{setId}/tests")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Add a test to a test set")
    public ResponseEntity<TestSetResponse> addTestToSet(
            @PathVariable UUID setId,
            @RequestParam UUID projectId,
            @RequestBody CreateTestSetItemRequest request) {
        TestSetResponse testSet = testService.addTestToSet(setId, request.getTestId());
        return ResponseEntity.ok(testSet);
    }

    @DeleteMapping("/{setId}/tests/{testId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Remove a test from a test set")
    public ResponseEntity<Void> removeTestFromSet(
            @PathVariable UUID setId,
            @PathVariable UUID testId,
            @RequestParam UUID projectId) {
        testService.removeTestFromSet(setId, testId);
        return ResponseEntity.noContent().build();
    }
}