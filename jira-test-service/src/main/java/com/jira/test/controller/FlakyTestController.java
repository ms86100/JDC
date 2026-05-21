package com.jira.test.controller;

import com.jira.test.dto.*;
import com.jira.test.service.FlakyTestDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flaky-tests")
@RequiredArgsConstructor
@Tag(name = "Flaky Test Detection", description = "APIs for detecting and analyzing flaky tests")
public class FlakyTestController {

    private final FlakyTestDetectionService flakyTestDetectionService;

    // ==================== Basic Endpoints ====================

    @GetMapping
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all flaky tests ordered by score")
    public ResponseEntity<List<FlakyTestResponse>> getFlakyTests(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam UUID projectId) {
        List<FlakyTestResponse> flakyTests = flakyTestDetectionService.getFlakyTests(limit);
        return ResponseEntity.ok(flakyTests);
    }

    @GetMapping("/{testId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get flaky test details for a specific test")
    public ResponseEntity<FlakyTestResponse> getFlakyTestDetails(@PathVariable UUID testId, @RequestParam UUID projectId) {
        FlakyTestResponse details = flakyTestDetectionService.getFlakyTestDetails(testId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/quarantine-candidates")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get tests that are candidates for quarantine")
    public ResponseEntity<List<FlakyTestResponse>> getQuarantineCandidates(@RequestParam UUID projectId) {
        List<FlakyTestResponse> candidates = flakyTestDetectionService.getQuarantineCandidates();
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get flaky test dashboard summary")
    public ResponseEntity<FlakyDashboardResponse> getDashboard(@RequestParam UUID projectId) {
        FlakyDashboardResponse dashboard = flakyTestDetectionService.getDashboardSummary(projectId);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/{testId}/should-quarantine")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Check if a test should be quarantined")
    public ResponseEntity<Map<String, Boolean>> shouldQuarantine(@PathVariable UUID testId, @RequestParam UUID projectId) {
        boolean should = flakyTestDetectionService.shouldQuarantine(testId);
        return ResponseEntity.ok(Map.of("shouldQuarantine", should));
    }

    // ==================== ML-Based Prediction ====================

    @GetMapping("/{testId}/prediction")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get ML-based flakiness prediction for a test")
    public ResponseEntity<FlakyTestDetectionService.FlakinessPrediction> getFlakinessPrediction(
            @PathVariable UUID testId,
            @RequestParam UUID projectId) {
        FlakyTestDetectionService.FlakinessPrediction prediction = flakyTestDetectionService.predictFlakiness(testId);
        return ResponseEntity.ok(prediction);
    }

    @GetMapping("/predictions")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get flakiness predictions for all tests in a project")
    public ResponseEntity<List<FlakyTestDetectionService.FlakinessPrediction>> getAllPredictions(
            @RequestParam UUID projectId) {
        List<FlakyTestResponse> flakyTests = flakyTestDetectionService.getFlakyTests(100);
        List<FlakyTestDetectionService.FlakinessPrediction> predictions = flakyTests.stream()
                .map(test -> flakyTestDetectionService.predictFlakiness(test.getTestId()))
                .toList();
        return ResponseEntity.ok(predictions);
    }

    // ==================== Retry Strategies ====================

    @GetMapping("/{testId}/retry-strategy")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get recommended retry strategy for a test")
    public ResponseEntity<FlakyTestDetectionService.RetryStrategy> getRetryStrategy(
            @PathVariable UUID testId,
            @RequestParam UUID projectId) {
        FlakyTestDetectionService.RetryStrategy strategy = flakyTestDetectionService.determineRetryStrategy(testId);
        return ResponseEntity.ok(strategy);
    }

    @GetMapping("/retry-recommendations")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get retry strategy recommendations for all flaky tests")
    public ResponseEntity<List<FlakyTestDetectionService.RetryRecommendation>> getRetryRecommendations(
            @RequestParam UUID projectId) {
        List<FlakyTestDetectionService.RetryRecommendation> recommendations =
                flakyTestDetectionService.getRetryRecommendations(projectId);
        return ResponseEntity.ok(recommendations);
    }

    // ==================== Root Cause Analysis ====================

    @GetMapping("/{testId}/root-cause")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Perform root cause analysis for a flaky test")
    public ResponseEntity<FlakyTestDetectionService.RootCauseAnalysis> getRootCauseAnalysis(
            @PathVariable UUID testId,
            @RequestParam UUID projectId) {
        FlakyTestDetectionService.RootCauseAnalysis analysis =
                flakyTestDetectionService.performRootCauseAnalysis(testId);
        return ResponseEntity.ok(analysis);
    }

    // ==================== CI/CD Integration ====================

    @GetMapping("/{testId}/ci-config")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get CI/CD integration configuration for a test")
    public ResponseEntity<Map<String, Object>> getCIConfig(
            @PathVariable UUID testId,
            @RequestParam UUID projectId) {
        Map<String, Object> config = flakyTestDetectionService.getCIDCIntegrationConfig(testId);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/ci-status")
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    @Operation(summary = "Get CI/CD status report for all tests")
    public ResponseEntity<List<Map<String, Object>>> getCIStatusReport(@RequestParam UUID projectId) {
        List<Map<String, Object>> report = flakyTestDetectionService.getCIStatusReport(projectId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export/ci-config")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Export CI/CD retry configurations for all flaky tests")
    public ResponseEntity<String> exportCIConfig(
            @RequestParam UUID projectId,
            @RequestParam(defaultValue = "yaml") String format) {
        List<FlakyTestResponse> flakyTests = flakyTestDetectionService.getFlakyTests(100);
        StringBuilder sb = new StringBuilder();

        if ("yaml".equalsIgnoreCase(format)) {
            sb.append("# Auto-generated retry configurations\n");
            sb.append("# Generated by Jira Test Management Platform\n\n");
            for (FlakyTestResponse test : flakyTests) {
                FlakyTestDetectionService.RetryStrategy strategy =
                        flakyTestDetectionService.determineRetryStrategy(test.getTestId());
                sb.append("# Test: ").append(test.getTestName()).append("\n");
                sb.append("retry_").append(test.getTestId().toString().replace("-", "_")).append(":\n");
                sb.append("  maxAttempts: ").append(strategy.getMaxAttempts()).append("\n");
                sb.append("  delay: ").append(strategy.getDelayMs()).append("ms\n");
                sb.append("  strategy: ").append(strategy.getStrategyType().toLowerCase().replace("_", "-")).append("\n\n");
            }
        } else {
            sb.append("testId,testName,strategy,maxAttempts,delayMs\n");
            for (FlakyTestResponse test : flakyTests) {
                FlakyTestDetectionService.RetryStrategy strategy =
                        flakyTestDetectionService.determineRetryStrategy(test.getTestId());
                sb.append(test.getTestId()).append(",")
                        .append(test.getTestName()).append(",")
                        .append(strategy.getStrategyType()).append(",")
                        .append(strategy.getMaxAttempts()).append(",")
                        .append(strategy.getDelayMs()).append("\n");
            }
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"flaky-test-retry-config." + format + "\"")
                .body(sb.toString());
    }
}
