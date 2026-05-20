package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.FlakyTestDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flaky-tests")
@RequiredArgsConstructor
@Tag(name = "Flaky Test Detection", description = "APIs for detecting and analyzing flaky tests")
public class FlakyTestController {

    private final FlakyTestDetectionService flakyTestDetectionService;

    @GetMapping
    @Operation(summary = "Get all flaky tests ordered by score")
    public ResponseEntity<List<FlakyTestResponse>> getFlakyTests(
            @RequestParam(defaultValue = "50") int limit) {
        List<FlakyTestResponse> flakyTests = flakyTestDetectionService.getFlakyTests(limit);
        return ResponseEntity.ok(flakyTests);
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Get flaky test details for a specific test")
    public ResponseEntity<FlakyTestResponse> getFlakyTestDetails(@PathVariable UUID testId) {
        FlakyTestResponse details = flakyTestDetectionService.getFlakyTestDetails(testId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/quarantine-candidates")
    @Operation(summary = "Get tests that are candidates for quarantine")
    public ResponseEntity<List<FlakyTestResponse>> getQuarantineCandidates() {
        List<FlakyTestResponse> candidates = flakyTestDetectionService.getQuarantineCandidates();
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get flaky test dashboard summary")
    public ResponseEntity<FlakyDashboardResponse> getDashboard(@RequestParam UUID projectId) {
        FlakyDashboardResponse dashboard = flakyTestDetectionService.getDashboardSummary(projectId);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{testId}/should-quarantine")
    @Operation(summary = "Check if a test should be quarantined")
    public ResponseEntity<java.util.Map<String, Boolean>> shouldQuarantine(@PathVariable UUID testId) {
        boolean should = flakyTestDetectionService.shouldQuarantine(testId);
        return ResponseEntity.ok(java.util.Map.of("shouldQuarantine", should));
    }
}