package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.TestManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Test Plan Controller
 */
@RestController
@RequestMapping("/api/test-plans")
@RequiredArgsConstructor
@Tag(name = "Test Plans", description = "Test plan management APIs")
public class TestPlanController {

    private final TestManagementService testService;

    @PostMapping
    @Operation(summary = "Create a new test plan")
    public ResponseEntity<TestPlanResponse> createTestPlan(
            @RequestParam UUID projectId,
            @RequestBody CreateTestPlanRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testService.createTestPlan(projectId, request, userId));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a test plan by ID")
    public ResponseEntity<TestPlanResponse> getTestPlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(testService.getTestPlan(planId));
    }

    @GetMapping
    @Operation(summary = "Get all test plans for a project")
    public ResponseEntity<java.util.List<TestPlanResponse>> getTestPlans(@RequestParam UUID projectId) {
        return ResponseEntity.ok(testService.getTestPlansByProject(projectId));
    }

    @PostMapping("/{planId}/test-sets")
    @Operation(summary = "Add a test set to a test plan")
    public ResponseEntity<TestPlanResponse> addTestSetToPlan(
            @PathVariable UUID planId,
            @RequestParam UUID testSetId) {
        return ResponseEntity.ok(testService.addTestSetToPlan(planId, testSetId));
    }
}