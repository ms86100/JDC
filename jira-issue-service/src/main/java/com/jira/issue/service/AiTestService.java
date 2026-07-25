package com.jira.issue.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Test Management Service
 * Phase 16 - AI Features
 *
 * Provides intelligent test management capabilities including:
 * - Duplicate test detection
 * - Test coverage recommendations
 * - Test prioritization
 * - Failure clustering
 * - Test generation suggestions
 *
 * This implementation provides the framework. For production:
 * - Integrate with OpenAI, Anthropic, or local LLM
 * - Use embeddings for semantic search
 * - Implement caching for AI responses
 */
@Service
@Slf4j
public class AiTestService {

    @Value("${app.ai.duplicate-similarity-threshold:0.8}")
    private double duplicateSimilarityThreshold;

    @Value("${app.ai.risk-threshold-high:0.7}")
    private double riskThresholdHigh;

    @Value("${app.ai.risk-threshold-medium:0.4}")
    private double riskThresholdMedium;

    @Value("${app.ai.stability-warning-threshold:0.7}")
    private double stabilityWarningThreshold;

    @Value("${app.ai.coverage-warning-threshold:0.5}")
    private double coverageWarningThreshold;

    /**
     * Analyze test suite for potential duplicates
     */
    public DuplicateTestReport findDuplicateTests(List<TestSummary> tests) {
        log.info("Analyzing {} tests for duplicates", tests.size());

        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        Set<UUID> processedIds = new HashSet<>();

        for (int i = 0; i < tests.size(); i++) {
            if (processedIds.contains(tests.get(i).getId())) continue;

            List<TestSummary> similarTests = new ArrayList<>();
            for (int j = i + 1; j < tests.size(); j++) {
                if (processedIds.contains(tests.get(j).getId())) continue;

                double similarity = calculateSimilarity(tests.get(i), tests.get(j));
                if (similarity > duplicateSimilarityThreshold) {
                    similarTests.add(tests.get(j));
                    processedIds.add(tests.get(j).getId());
                }
            }

            if (!similarTests.isEmpty()) {
                final TestSummary currentTest = tests.get(i);
                List<TestSummary> group = new ArrayList<>();
                group.add(currentTest);
                group.addAll(similarTests);
                duplicateGroups.add(DuplicateGroup.builder()
                        .tests(group)
                        .averageSimilarity(similarTests.stream()
                                .mapToDouble(t -> calculateSimilarity(currentTest, t))
                                .average().orElse(0.8))
                        .recommendation(generateMergeRecommendation(group))
                        .build());
                processedIds.add(currentTest.getId());
            }
        }

        return DuplicateTestReport.builder()
                .totalTestsAnalyzed(tests.size())
                .duplicateGroupsFound(duplicateGroups.size())
                .potentialDuplicates(duplicateGroups)
                .build();
    }

    /**
     * Get AI-powered test coverage recommendations
     */
    public CoverageRecommendations getCoverageRecommendations(UUID projectId, List<String> requirementKeys) {
        log.info("Generating coverage recommendations for {} requirements", requirementKeys.size());

        List<CoverageRecommendation> recommendations = new ArrayList<>();

        for (String reqKey : requirementKeys) {
            recommendations.add(CoverageRecommendation.builder()
                    .requirementKey(reqKey)
                    .recommendationType("ADD_TEST_COVERAGE")
                    .priority(calculatePriority(reqKey))
                    .suggestedTestTypes(List.of("SMOKE", "REGRESSION"))
                    .reasoning("Requirement lacks test coverage. Add tests to ensure quality.")
                    .estimatedEffortHours(2.0)
                    .build());
        }

        // Sort by priority
        recommendations.sort((a, b) -> {
            if (a.getPriority() == null || b.getPriority() == null) return 0;
            return a.getPriority().compareTo(b.getPriority());
        });

        return CoverageRecommendations.builder()
                .projectId(projectId)
                .totalRequirements(requirementKeys.size())
                .recommendations(recommendations)
                .build();
    }

    /**
     * Cluster failing tests for root cause analysis
     */
    public FailureClusterReport clusterFailures(List<ExecutionHistory> recentFailures) {
        log.info("Clustering {} recent failures", recentFailures.size());

        Map<String, List<ExecutionHistory>> clusters = new HashMap<>();
        Map<String, String> clusterReasons = Map.of(
                "ASSERTION_FAILURE", "Tests failing due to assertion mismatches - likely code change",
                "TIMEOUT", "Tests timing out - possible performance issues",
                "AUTHENTICATION", "Auth-related failures - check user sessions or tokens",
                "DATA_SETUP", "Tests failing in setup - check test data",
                "NETWORK", "Network-dependent tests failing - flaky infrastructure",
                "UNKNOWN", "Unable to classify - manual investigation needed"
        );

        for (ExecutionHistory failure : recentFailures) {
            String cluster = classifyFailure(failure);
            clusters.computeIfAbsent(cluster, k -> new ArrayList<>()).add(failure);
        }

        List<FailureCluster> failureClusters = clusters.entrySet().stream()
                .map(e -> FailureCluster.builder()
                        .clusterType(e.getKey())
                        .reason(clusterReasons.getOrDefault(e.getKey(), "Unknown"))
                        .failingTests(e.getValue().stream()
                                .map(ExecutionHistory::getTestId)
                                .toList())
                        .occurrenceCount(e.getValue().size())
                        .percentage((e.getValue().size() * 100.0) / recentFailures.size())
                        .build())
                .sorted((a, b) -> Integer.compare(b.getOccurrenceCount(), a.getOccurrenceCount()))
                .toList();

        return FailureClusterReport.builder()
                .totalFailures(recentFailures.size())
                .clusterCount(failureClusters.size())
                .clusters(failureClusters)
                .build();
    }

    /**
     * Generate test case suggestions based on requirements
     */
    public List<TestSuggestion> suggestTests(String requirementDescription) {
        log.info("Generating test suggestions for requirement");

        // Generate structured test suggestions
        return List.of(
                TestSuggestion.builder()
                        .title("Positive path test")
                        .description("Test the happy path scenario")
                        .testType("SMOKE")
                        .estimatedSteps(5)
                        .confidence(0.95)
                        .tags(List.of("positive", "critical"))
                        .build(),
                TestSuggestion.builder()
                        .title("Negative path test")
                        .description("Test error handling with invalid inputs")
                        .testType("NEGATIVE")
                        .estimatedSteps(3)
                        .confidence(0.88)
                        .tags(List.of("negative", "validation"))
                        .build(),
                TestSuggestion.builder()
                        .title("Boundary value test")
                        .description("Test edge cases and boundary conditions")
                        .testType("BOUNDARY")
                        .estimatedSteps(4)
                        .confidence(0.85)
                        .tags(List.of("boundary", "edge-case"))
                        .build(),
                TestSuggestion.builder()
                        .title("Performance test")
                        .description("Verify response times under load")
                        .testType("PERFORMANCE")
                        .estimatedSteps(2)
                        .confidence(0.80)
                        .tags(List.of("performance", "load"))
                        .build()
        );
    }

    /**
     * Calculate risk score for test execution
     */
    public RiskAssessment assessRisk(UUID testId, List<ExecutionHistory> history) {
        double stabilityScore = calculateStabilityScore(history);
        double coverageScore = calculateCoverageScore(testId);
        double changeScore = calculateChangeScore(testId);

        double overallRisk = (1 - stabilityScore) * 0.5 + (1 - coverageScore) * 0.3 + changeScore * 0.2;

        String riskLevel;
        if (overallRisk > riskThresholdHigh) riskLevel = "HIGH";
        else if (overallRisk > riskThresholdMedium) riskLevel = "MEDIUM";
        else riskLevel = "LOW";

        return RiskAssessment.builder()
                .testId(testId)
                .overallRisk(overallRisk)
                .riskLevel(riskLevel)
                .stabilityScore(stabilityScore)
                .coverageScore(coverageScore)
                .changeScore(changeScore)
                .recommendations(generateRiskRecommendations(riskLevel, stabilityScore, coverageScore))
                .build();
    }

    // Helper methods

    private double calculateSimilarity(TestSummary t1, TestSummary t2) {
        double titleSimilarity = calculateJaccardSimilarity(
                tokenize(t1.getTitle()), tokenize(t2.getTitle()));
        double labelSimilarity = calculateJaccardSimilarity(
                new HashSet<>(t1.getLabels()), new HashSet<>(t2.getLabels()));
        return titleSimilarity * 0.7 + labelSimilarity * 0.3;
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase().split("\\s+"))
                .filter(w -> w.length() > 2)
                .collect(java.util.stream.Collectors.toSet());
    }

    private double calculateJaccardSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private String generateMergeRecommendation(List<TestSummary> tests) {
        return "Consider merging these " + tests.size() +
                " similar tests. They test the same functionality with minor variations.";
    }

    private String calculatePriority(String reqKey) {
        // In production, analyze requirement complexity
        int hash = Math.abs(reqKey.hashCode());
        if (hash % 5 == 0) return "CRITICAL";
        if (hash % 3 == 0) return "HIGH";
        if (hash % 2 == 0) return "MEDIUM";
        return "LOW";
    }

    private String classifyFailure(ExecutionHistory failure) {
        String errorType = failure.getErrorType();
        if (errorType == null) return "UNKNOWN";

        if (errorType.contains("assert") || errorType.contains("expected")) {
            return "ASSERTION_FAILURE";
        }
        if (errorType.contains("timeout") || errorType.contains("timed out")) {
            return "TIMEOUT";
        }
        if (errorType.contains("auth") || errorType.contains("401") || errorType.contains("403")) {
            return "AUTHENTICATION";
        }
        if (errorType.contains("setup") || errorType.contains("null")) {
            return "DATA_SETUP";
        }
        if (errorType.contains("connection") || errorType.contains("network")) {
            return "NETWORK";
        }
        return "UNKNOWN";
    }

    private double calculateStabilityScore(List<ExecutionHistory> history) {
        if (history.isEmpty()) return 0.5;
        long passCount = history.stream().filter(h -> "PASSED".equals(h.getStatus())).count();
        return (double) passCount / history.size();
    }

    private double calculateCoverageScore(UUID testId) {
        // Simplified - in production, analyze actual coverage
        return 0.7;
    }

    private double calculateChangeScore(UUID testId) {
        // Simplified - in production, check recent changes
        return 0.3;
    }

    private List<String> generateRiskRecommendations(String riskLevel, double stability, double coverage) {
        List<String> recommendations = new ArrayList<>();
        if (stability < stabilityWarningThreshold) {
            recommendations.add("This test is unstable. Review recent changes and fix flakiness.");
        }
        if (coverage < coverageWarningThreshold) {
            recommendations.add("This test has low code coverage. Consider increasing assertions.");
        }
        if ("HIGH".equals(riskLevel)) {
            recommendations.add("Execute this test in isolation before merging.");
        }
        return recommendations;
    }

    // DTO classes

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestSummary {
        private UUID id;
        private String title;
        private String description;
        private String testType;
        private List<String> labels;
    }

    @Data @Builder
    public static class DuplicateTestReport {
        private int totalTestsAnalyzed;
        private int duplicateGroupsFound;
        private List<DuplicateGroup> potentialDuplicates;
    }

    @Data @Builder
    public static class DuplicateGroup {
        private List<TestSummary> tests;
        private double averageSimilarity;
        private String recommendation;
    }

    @Data @Builder
    public static class CoverageRecommendations {
        private UUID projectId;
        private int totalRequirements;
        private List<CoverageRecommendation> recommendations;
    }

    @Data @Builder
    public static class CoverageRecommendation {
        private String requirementKey;
        private String recommendationType;
        private String priority;
        private List<String> suggestedTestTypes;
        private String reasoning;
        private double estimatedEffortHours;
    }

    @Data @Builder
    public static class FailureClusterReport {
        private int totalFailures;
        private int clusterCount;
        private List<FailureCluster> clusters;
    }

    @Data @Builder
    public static class FailureCluster {
        private String clusterType;
        private String reason;
        private List<UUID> failingTests;
        private int occurrenceCount;
        private double percentage;
    }

    @Data @Builder
    public static class ExecutionHistory {
        private UUID testId;
        private String status;
        private String errorType;
        private LocalDateTime executedAt;
    }

    @Data @Builder
    public static class TestSuggestion {
        private String title;
        private String description;
        private String testType;
        private int estimatedSteps;
        private double confidence;
        private List<String> tags;
    }

    @Data @Builder
    public static class RiskAssessment {
        private UUID testId;
        private double overallRisk;
        private String riskLevel;
        private double stabilityScore;
        private double coverageScore;
        private double changeScore;
        private List<String> recommendations;
    }
}