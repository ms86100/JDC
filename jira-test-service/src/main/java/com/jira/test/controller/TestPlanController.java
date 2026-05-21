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

import java.util.UUID;

@RestController
@RequestMapping("/api/test-plans")
@RequiredArgsConstructor
@Tag(name = "Test Plans", description = "APIs for managing test plans")
public class TestPlanController {

    private final TestService testService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new test plan")
    public ResponseEntity<TestPlanResponse> createTestPlan(@Valid @RequestBody CreateTestPlanRequest request) {
        TestPlanResponse testPlan = testService.createTestPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(testPlan);
    }

    @GetMapping("/{planId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get a test plan by ID")
    public ResponseEntity<TestPlanResponse> getTestPlan(@PathVariable UUID planId, @RequestParam UUID projectId) {
        TestPlanResponse testPlan = testService.getTestPlan(planId);
        return ResponseEntity.ok(testPlan);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all test plans for a project")
    public ResponseEntity<java.util.List<TestPlanResponse>> getTestPlansByProject(@PathVariable UUID projectId) {
        java.util.List<TestPlanResponse> testPlans = testService.getTestPlansByProject(projectId);
        return ResponseEntity.ok(testPlans);
    }

    @PostMapping("/{planId}/test-sets")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    @Operation(summary = "Add a test set to a test plan")
    public ResponseEntity<TestPlanResponse> addTestSetToPlan(
            @PathVariable UUID planId,
            @RequestParam UUID projectId,
            @RequestBody CreateTestSetItemRequest request) {
        TestPlanResponse testPlan = testService.addTestSetToPlan(planId, request.getTestId());
        return ResponseEntity.ok(testPlan);
    }
}