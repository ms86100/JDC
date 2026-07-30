package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.service.AiTestService;
import com.avionics_systems.issue.service.TestManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Test Controller
 * Phase 16 - AI Features
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiTestService aiTestService;
    private final TestManagementService testService;

    @GetMapping("/duplicates")
    public List<TestResponse> findDuplicates(@RequestParam UUID testId, @RequestParam UUID projectId) {
        TestResponse test = testService.getTest(testId);
        if (test == null) return List.of();
        List<TestResponse> allTests = testService.getTestsByProject(projectId, null, null, null);
        return aiTestService.findDuplicateTests(test, allTests);
    }

    @GetMapping("/coverage/recommendations")
    public List<TestResponse> getCoverageRecommendations(
            @RequestParam String requirementKey, @RequestParam UUID projectId) {
        return aiTestService.getCoverageRecommendations(requirementKey, projectId);
    }

    @PostMapping("/failures/cluster")
    public Map<String, Object> clusterFailures(@RequestBody List<TestExecutionResponse> failures) {
        return aiTestService.clusterFailures(failures);
    }

    @GetMapping("/suggestions")
    public List<TestResponse> suggestTests(@RequestParam String keywords, @RequestParam UUID projectId) {
        return aiTestService.suggestTests(keywords, projectId);
    }

    @GetMapping("/risk/{testId}")
    public Map<String, Object> assessRisk(@PathVariable UUID testId) {
        return aiTestService.assessRisk(testId);
    }
}