package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.service.AiTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Test Management Controller
 * Phase 16 - AI Features REST Endpoints
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiTestController {

    private final AiTestService aiTestService;

    @PostMapping("/analyze-duplicates")
    public ResponseEntity<AiTestService.DuplicateTestReport> analyzeDuplicates(
            @RequestBody Map<String, Object> request) {
        log.info("Analyzing test duplicates");
        List<AiTestService.TestSummary> tests = parseTestSummaries(request);
        return ResponseEntity.ok(aiTestService.findDuplicateTests(tests));
    }

    @GetMapping("/coverage-recommendations/{projectId}")
    public ResponseEntity<AiTestService.CoverageRecommendations> getCoverageRecommendations(
            @PathVariable UUID projectId,
            @RequestParam List<String> requirementKeys) {
        log.info("Getting coverage recommendations for project: {}", projectId);
        return ResponseEntity.ok(aiTestService.getCoverageRecommendations(projectId, requirementKeys));
    }

    @PostMapping("/cluster-failures")
    public ResponseEntity<AiTestService.FailureClusterReport> clusterFailures(
            @RequestBody Map<String, Object> request) {
        log.info("Clustering test failures");
        List<AiTestService.ExecutionHistory> failures = parseExecutionHistory(request);
        return ResponseEntity.ok(aiTestService.clusterFailures(failures));
    }

    @PostMapping("/suggest-tests")
    public ResponseEntity<List<AiTestService.TestSuggestion>> suggestTests(
            @RequestBody Map<String, String> request) {
        log.info("Generating test suggestions");
        String requirementDescription = request.get("requirementDescription");
        return ResponseEntity.ok(aiTestService.suggestTests(requirementDescription));
    }

    @PostMapping("/assess-risk/{testId}")
    public ResponseEntity<AiTestService.RiskAssessment> assessRisk(
            @PathVariable UUID testId,
            @RequestBody Map<String, Object> request) {
        log.info("Assessing risk for test: {}", testId);
        List<AiTestService.ExecutionHistory> history = parseExecutionHistory(request);
        return ResponseEntity.ok(aiTestService.assessRisk(testId, history));
    }

    private List<AiTestService.TestSummary> parseTestSummaries(Map<String, Object> request) {
        // Parse from request - simplified implementation
        return List.of();
    }

    private List<AiTestService.ExecutionHistory> parseExecutionHistory(Map<String, Object> request) {
        // Parse from request - simplified implementation
        return List.of();
    }
}