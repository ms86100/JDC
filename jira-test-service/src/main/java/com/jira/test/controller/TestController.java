package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Tag(name = "Test Management", description = "APIs for managing test issues")
public class TestController {

    private final TestService testService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new test")
    public ResponseEntity<TestResponse> createTest(@Valid @RequestBody CreateTestRequest request) {
        TestResponse test = testService.createTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(test);
    }

    @GetMapping("/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test by ID")
    public ResponseEntity<TestResponse> getTest(@PathVariable UUID testId, @RequestParam UUID projectId) {
        TestResponse test = testService.getTest(testId);
        return ResponseEntity.ok(test);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all tests for a project")
    public ResponseEntity<List<TestResponse>> getTestsByProject(@PathVariable UUID projectId) {
        List<TestResponse> tests = testService.getTestsByProject(projectId);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/set/{testSetId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all tests in a test set")
    public ResponseEntity<List<TestResponse>> getTestsBySet(@PathVariable UUID testSetId, @RequestParam UUID projectId) {
        List<TestResponse> tests = testService.getTestsBySet(testSetId);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/requirement/{requirementKey}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all tests linked to a requirement")
    public ResponseEntity<List<TestResponse>> getTestsByRequirement(@PathVariable String requirementKey, @RequestParam UUID projectId) {
        List<TestResponse> tests = testService.getTestsByRequirement(requirementKey);
        return ResponseEntity.ok(tests);
    }

    @PutMapping("/{testId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Update a test")
    public ResponseEntity<TestResponse> updateTest(
            @PathVariable UUID testId,
            @RequestParam UUID projectId,
            @Valid @RequestBody CreateTestRequest request) {
        TestResponse test = testService.updateTest(testId, request);
        return ResponseEntity.ok(test);
    }

    @DeleteMapping("/{testId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Archive a test")
    public ResponseEntity<Void> deleteTest(@PathVariable UUID testId, @RequestParam UUID projectId) {
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }
}