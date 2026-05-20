package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlakyTestDetectionService {

    private final FlakyTestAnalysisRepository flakyAnalysisRepository;
    private final FlakyTestPatternRepository patternRepository;
    private final ExecutionFlakinessRecordRepository recordRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestQuarantineRepository quarantineRepository;
    private final ObjectMapper objectMapper;

    private static final BigDecimal FLaky_THRESHOLD = new BigDecimal("0.3"); // 30% failure rate
    private static final BigDecimal HIGH_FLAKY_THRESHOLD = new BigDecimal("0.5"); // 50% for quarantine
    private static final int MIN_EXECUTIONS_FOR_ANALYSIS = 5;

    // ==================== Execution Recording ====================

    @Transactional
    public void recordExecutionOutcome(UUID testId, String status, String failureReason,
                                        UUID executionId, UUID environmentId, Integer durationMs, int retryAttempt) {
        FlakyTestAnalysis analysis = getOrCreateAnalysis(testId);

        // Update totals
        analysis.setTotalExecutions(analysis.getTotalExecutions() + 1);
        if ("PASSED".equalsIgnoreCase(status)) {
            analysis.setTotalPasses(analysis.getTotalPasses() + 1);
        } else {
            analysis.setTotalFailures(analysis.getTotalFailures() + 1);
            if (analysis.getFirstFlakyOccurrence() == null) {
                analysis.setFirstFlakyOccurrence(LocalDateTime.now());
            }
            analysis.setLastFlakyOccurrence(LocalDateTime.now());
        }

        // Calculate flaky score
        recalculateFlakyScore(analysis);

        // Update pass rate trend
        updatePassRateTrend(analysis);

        // Update status
        updateTestStatus(analysis);

        flakyAnalysisRepository.save(analysis);

        // Record individual execution
        ExecutionFlakinessRecord record = ExecutionFlakinessRecord.builder()
                .testId(testId)
                .executionId(executionId)
                .isFlakyExecution(!"PASSED".equalsIgnoreCase(status))
                .failureReason(failureReason)
                .environmentId(environmentId)
                .executionDurationMs(durationMs)
                .retryAttempt(retryAttempt)
                .build();
        recordRepository.save(record);

        // Identify patterns
        identifyPatterns(testId);

        // Auto-quarantine if needed
        evaluateAutoQuarantine(analysis);

        log.info("Recorded execution for test {}: status={}, new flakyScore={}",
                testId, status, analysis.getFlakyScore());
    }

    // ==================== Analysis ====================

    @Transactional(readOnly = true)
    public FlakyTestResponse getFlakyTestDetails(UUID testId) {
        FlakyTestAnalysis analysis = flakyAnalysisRepository.findByTestId(testId)
                .orElseThrow(() -> new ResourceNotFoundException("FlakyTestAnalysis", "testId", testId));

        TestIssue test = testIssueRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", testId));

        List<FlakyTestPattern> patterns = patternRepository.findByTestId(testId);
        LocalDateTime since = LocalDateTime.now().minusDays(analysis.getAnalysisWindowDays());
        List<ExecutionFlakinessRecord> records = recordRepository.findRecentByTestId(testId, since);

        return mapToFlakyTestResponse(test, analysis, patterns, records);
    }

    @Transactional(readOnly = true)
    public List<FlakyTestResponse> getFlakyTests(int limit) {
        List<FlakyTestAnalysis> analyses = flakyAnalysisRepository.findAllOrderByFlakyScoreDesc();
        return analyses.stream().limit(limit).map(analysis -> {
            TestIssue test = testIssueRepository.findById(analysis.getTestId()).orElse(null);
            if (test == null) return null;
            List<FlakyTestPattern> patterns = patternRepository.findByTestId(analysis.getTestId());
            return mapToFlakyTestResponse(test, analysis, patterns, new ArrayList<>());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FlakyTestResponse> getQuarantineCandidates() {
        List<FlakyTestAnalysis> candidates = flakyAnalysisRepository.findQuarantineCandidates();
        return candidates.stream().map(analysis -> {
            TestIssue test = testIssueRepository.findById(analysis.getTestId()).orElse(null);
            if (test == null) return null;
            return mapToFlakyTestResponse(test, analysis, new ArrayList<>(), new ArrayList<>());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlakyDashboardResponse getDashboardSummary(UUID projectId) {
        List<FlakyTestAnalysis> allAnalysis = flakyAnalysisRepository.findAll();
        List<TestIssue> projectTests = testIssueRepository.findByProjectId(projectId);

        Set<UUID> projectTestIds = projectTests.stream().map(TestIssue::getId).collect(Collectors.toSet());
        List<FlakyTestAnalysis> projectAnalysis = allAnalysis.stream()
                .filter(a -> projectTestIds.contains(a.getTestId()))
                .collect(Collectors.toList());

        int stableCount = (int) projectAnalysis.stream().filter(a -> "stable".equals(a.getCurrentStatus())).count();
        int flakyCount = (int) projectAnalysis.stream().filter(a -> "flaky".equals(a.getCurrentStatus())).count();
        int candidateCount = (int) projectAnalysis.stream().filter(a -> "quarantine_candidate".equals(a.getCurrentStatus())).count();

        BigDecimal avgScore = projectAnalysis.stream()
                .map(FlakyTestAnalysis::getFlakyScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(projectAnalysis.size(), 1)), 2, RoundingMode.HALF_UP);

        List<FlakyTestResponse> topFlaky = getFlakyTests(10).stream()
                .filter(t -> projectTestIds.contains(t.getTestId()))
                .collect(Collectors.toList());

        Map<String, Integer> patternsByType = new HashMap<>();
        // Count patterns by type - simplified
        patternsByType.put("intermittent", 0);
        patternsByType.put("environmental", 0);
        patternsByType.put("timing", 0);
        patternsByType.put("data-dependent", 0);

        return FlakyDashboardResponse.builder()
                .totalTestsAnalyzed(projectAnalysis.size())
                .stableCount(stableCount)
                .flakyCount(flakyCount)
                .quarantineCandidateCount(candidateCount)
                .averageFlakyScore(avgScore)
                .topFlakyTests(topFlaky)
                .patternsByType(patternsByType)
                .build();
    }

    // ==================== Pattern Identification ====================

    @Transactional
    public void identifyPatterns(UUID testId) {
        List<ExecutionFlakinessRecord> records = recordRepository.findByTestId(testId);

        if (records.size() < MIN_EXECUTIONS_FOR_ANALYSIS) {
            return; // Not enough data
        }

        // Analyze timing patterns
        Optional<Integer> avgDuration = records.stream()
                .filter(r -> r.getExecutionDurationMs() != null)
                .map(ExecutionFlakinessRecord::getExecutionDurationMs)
                .reduce((a, b) -> a + b);

        // Detect environmental patterns
        Map<UUID, Long> envCounts = records.stream()
                .filter(r -> r.getEnvironmentId() != null)
                .collect(Collectors.groupingBy(ExecutionFlakinessRecord::getEnvironmentId, Collectors.counting()));

        Optional<Map.Entry<UUID, Long>> mostAffectedEnv = envCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        // Detect failure message patterns
        Map<String, Long> failurePatterns = records.stream()
                .filter(r -> r.getFailureReason() != null)
                .collect(Collectors.groupingBy(ExecutionFlakinessRecord::getFailureReason, Collectors.counting()));

        // Create pattern entries
        if (mostAffectedEnv.isPresent() && mostAffectedEnv.get().getValue() > records.size() * 0.5) {
            createOrUpdatePattern(testId, "environmental", "Test fails more on specific environment",
                    calculateFrequency(records, r -> r.getEnvironmentId() != null && r.getEnvironmentId().equals(mostAffectedEnv.get().getKey())),
                    Collections.singletonList(mostAffectedEnv.get().getKey().toString()),
                    null, "Review environment-specific configurations");
        }

        // Detect retry patterns
        long retryCount = records.stream().filter(r -> r.getRetryAttempt() != null && r.getRetryAttempt() > 0).count();
        if (retryCount > records.size() * 0.3) {
            createOrUpdatePattern(testId, "intermittent", "Test requires retries to pass",
                    BigDecimal.valueOf((double) retryCount / records.size()),
                    null, null, "Investigate race conditions or async operations");
        }
    }

    private void createOrUpdatePattern(UUID testId, String patternType, String description,
                                        BigDecimal frequency, List<String> environments, List<String> builds, String suggestedFix) {
        List<FlakyTestPattern> existing = patternRepository.findByTestId(testId).stream()
                .filter(p -> p.getPatternType().equals(patternType))
                .collect(Collectors.toList());

        if (existing.isEmpty()) {
            FlakyTestPattern pattern = FlakyTestPattern.builder()
                    .testId(testId)
                    .patternType(patternType)
                    .patternDescription(description)
                    .frequencyScore(frequency)
                    .affectedEnvironments(serializeList(environments))
                    .affectedBuilds(serializeList(builds))
                    .suggestedFix(suggestedFix)
                    .rootCauseCategory(classifyRootCause(patternType, description))
                    .confidenceScore(frequency.multiply(new BigDecimal("0.9")))
                    .build();
            patternRepository.save(pattern);
        } else {
            FlakyTestPattern pattern = existing.get(0);
            pattern.setFrequencyScore(frequency);
            if (environments != null) pattern.setAffectedEnvironments(serializeList(environments));
            patternRepository.save(pattern);
        }
    }

    // ==================== Auto-Quarantine Evaluation ====================

    private void evaluateAutoQuarantine(FlakyTestAnalysis analysis) {
        if (analysis.getFlakyScore().compareTo(HIGH_FLAKY_THRESHOLD) >= 0 &&
            analysis.getTotalExecutions() >= MIN_EXECUTIONS_FOR_ANALYSIS) {

            // Check if already quarantined
            if (quarantineRepository.findByTestId(analysis.getTestId()).isEmpty()) {
                analysis.setCurrentStatus("quarantine_candidate");
                log.info("Test {} is now a quarantine candidate with flakyScore {}",
                        analysis.getTestId(), analysis.getFlakyScore());
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean shouldQuarantine(UUID testId) {
        FlakyTestAnalysis analysis = flakyAnalysisRepository.findByTestId(testId).orElse(null);
        if (analysis == null) return false;
        return "quarantine_candidate".equals(analysis.getCurrentStatus()) ||
               analysis.getFlakyScore().compareTo(HIGH_FLAKY_THRESHOLD) >= 0;
    }

    // ==================== Helper Methods ====================

    private FlakyTestAnalysis getOrCreateAnalysis(UUID testId) {
        return flakyAnalysisRepository.findByTestId(testId)
                .orElseGet(() -> FlakyTestAnalysis.builder()
                        .testId(testId)
                        .totalExecutions(0)
                        .totalFailures(0)
                        .totalPasses(0)
                        .flakyScore(BigDecimal.ZERO)
                        .passRateTrend("stable")
                        .currentStatus("stable")
                        .confidenceLevel(BigDecimal.ZERO)
                        .build());
    }

    private void recalculateFlakyScore(FlakyTestAnalysis analysis) {
        if (analysis.getTotalExecutions() == 0) {
            analysis.setFlakyScore(BigDecimal.ZERO);
            return;
        }

        // Flaky score = failures / total executions, weighted by recency
        double score = (double) analysis.getTotalFailures() / analysis.getTotalExecutions();

        // Calculate confidence based on sample size
        double confidence = Math.min(analysis.getTotalExecutions() / 20.0, 1.0);
        analysis.setConfidenceLevel(BigDecimal.valueOf(confidence));

        analysis.setFlakyScore(BigDecimal.valueOf(score * 100).setScale(2, RoundingMode.HALF_UP));
    }

    private void updatePassRateTrend(FlakyTestAnalysis analysis) {
        // Simplified trend calculation based on recent executions
        if (analysis.getTotalExecutions() < 10) {
            analysis.setPassRateTrend("stable");
            return;
        }

        double recentFailRate = (double) analysis.getTotalFailures() / analysis.getTotalExecutions();
        if (recentFailRate > 0.5) {
            analysis.setPassRateTrend("degrading");
        } else if (recentFailRate < 0.1) {
            analysis.setPassRateTrend("improving");
        } else {
            analysis.setPassRateTrend("stable");
        }
    }

    private void updateTestStatus(FlakyTestAnalysis analysis) {
        if (analysis.getFlakyScore().compareTo(HIGH_FLAKY_THRESHOLD) >= 0) {
            analysis.setCurrentStatus("flaky");
        } else if (analysis.getFlakyScore().compareTo(FLaky_THRESHOLD) >= 0) {
            analysis.setCurrentStatus("flaky");
        } else {
            analysis.setCurrentStatus("stable");
        }
    }

    private BigDecimal calculateFrequency(List<ExecutionFlakinessRecord> records,
                                          java.util.function.Predicate<ExecutionFlakinessRecord> predicate) {
        long count = records.stream().filter(predicate).count();
        return BigDecimal.valueOf((double) count / records.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private String classifyRootCause(String patternType, String description) {
        switch (patternType) {
            case "environmental": return "Environment Configuration";
            case "timing": return "Concurrency/Race Conditions";
            case "data-dependent": return "Test Data Issues";
            case "intermittent": return "Flaky Infrastructure";
            default: return "Unknown";
        }
    }

    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private FlakyTestResponse mapToFlakyTestResponse(TestIssue test, FlakyTestAnalysis analysis,
                                                       List<FlakyTestPattern> patterns,
                                                       List<ExecutionFlakinessRecord> records) {
        return FlakyTestResponse.builder()
                .testId(analysis.getTestId())
                .testIssueKey(test.getName())
                .testName(test.getName())
                .totalExecutions(analysis.getTotalExecutions())
                .totalFailures(analysis.getTotalFailures())
                .totalPasses(analysis.getTotalPasses())
                .flakyScore(analysis.getFlakyScore())
                .passRateTrend(analysis.getPassRateTrend())
                .firstFlakyOccurrence(analysis.getFirstFlakyOccurrence())
                .lastFlakyOccurrence(analysis.getLastFlakyOccurrence())
                .currentStatus(analysis.getCurrentStatus())
                .confidenceLevel(analysis.getConfidenceLevel())
                .patterns(patterns.stream().map(this::mapToPatternResponse).collect(Collectors.toList()))
                .recentExecutions(records.stream().map(this::mapToRecordResponse).collect(Collectors.toList()))
                .build();
    }

    private FlakyPatternResponse mapToPatternResponse(FlakyTestPattern pattern) {
        return FlakyPatternResponse.builder()
                .id(pattern.getId())
                .testId(pattern.getTestId())
                .patternType(pattern.getPatternType())
                .patternDescription(pattern.getPatternDescription())
                .frequencyScore(pattern.getFrequencyScore())
                .affectedEnvironments(parseList(pattern.getAffectedEnvironments()))
                .affectedBuilds(parseList(pattern.getAffectedBuilds()))
                .rootCauseCategory(pattern.getRootCauseCategory())
                .suggestedFix(pattern.getSuggestedFix())
                .confidenceScore(pattern.getConfidenceScore())
                .createdAt(pattern.getCreatedAt())
                .build();
    }

    private ExecutionRecordResponse mapToRecordResponse(ExecutionFlakinessRecord record) {
        return ExecutionRecordResponse.builder()
                .id(record.getId())
                .executionId(record.getExecutionId())
                .testId(record.getTestId())
                .isFlakyExecution(record.getIsFlakyExecution())
                .failureReason(record.getFailureReason())
                .environmentId(record.getEnvironmentId())
                .executionDurationMs(record.getExecutionDurationMs())
                .retryAttempt(record.getRetryAttempt())
                .analyzedAt(record.getAnalyzedAt())
                .build();
    }
}