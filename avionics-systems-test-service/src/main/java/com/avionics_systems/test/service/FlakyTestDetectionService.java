package com.avionics_systems.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.*;
import com.avionics_systems.test.exception.*;
import com.avionics_systems.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    @Value("${app.flaky.threshold:0.3}")
    private BigDecimal flakyThreshold;

    @Value("${app.flaky.high-threshold:0.5}")
    private BigDecimal highFlakyThreshold;

    @Value("${app.flaky.ml-confidence-threshold:0.75}")
    private BigDecimal mlConfidenceThreshold;

    @Value("${app.flaky.min-executions:5}")
    private int minExecutionsForAnalysis;

    @Value("${app.flaky.retention-days:30}")
    private int retentionDays;

    @Value("${app.flaky.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.flaky.retry-delay-ms:1000}")
    private int retryDelayMs;

    @Value("${app.flaky.intermittent-threshold:0.3}")
    private double intermittentThreshold;

    @Value("${app.flaky.timing-threshold:0.4}")
    private double timingThreshold;

    @Value("${app.flaky.data-dependency-threshold:0.35}")
    private double dataDependencyThreshold;

    @Value("${app.flaky.ml-coefficient.failure-rate:0.4}")
    private double mlCoefficientFailureRate;

    @Value("${app.flaky.ml-coefficient.recency-weight:0.25}")
    private double mlCoefficientRecencyWeight;

    @Value("${app.flaky.ml-coefficient.pattern-count:0.15}")
    private double mlCoefficientPatternCount;

    @Value("${app.flaky.ml-coefficient.env-diversity:0.1}")
    private double mlCoefficientEnvDiversity;

    @Value("${app.flaky.ml-coefficient.timing-variance:0.1}")
    private double mlCoefficientTimingVariance;

    // ==================== Execution Recording ====================

    @Transactional
    public void recordExecutionOutcome(UUID testId, String status, String failureReason,
                                        UUID executionId, UUID environmentId, Integer durationMs, int retryAttempt) {
        FlakyTestAnalysis analysis = getOrCreateAnalysis(testId);

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

        recalculateFlakyScore(analysis);
        updatePassRateTrend(analysis);
        updateTestStatus(analysis);

        flakyAnalysisRepository.save(analysis);

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

        identifyPatterns(testId);
        evaluateAutoQuarantine(analysis);

        log.info("Recorded execution for test {}: status={}, new flakyScore={}",
                testId, status, analysis.getFlakyScore());
    }

    // ==================== ML-Based Flakiness Prediction ====================

    public FlakinessPrediction predictFlakiness(UUID testId) {
        FlakyTestAnalysis analysis = flakyAnalysisRepository.findByTestId(testId).orElse(null);
        if (analysis == null) {
            return FlakinessPrediction.builder()
                    .testId(testId)
                    .predictionScore(BigDecimal.ZERO)
                    .confidence(BigDecimal.ZERO)
                    .predictedStatus("unknown")
                    .factors(Map.of())
                    .build();
        }

        Map<String, Double> factors = calculateMLFactors(analysis);
        double rawScore = calculateMLScore(factors);
        BigDecimal predictionScore = BigDecimal.valueOf(rawScore * 100).setScale(2, RoundingMode.HALF_UP);

        // Calculate confidence based on data quality
        BigDecimal confidence = calculatePredictionConfidence(analysis, factors);

        String predictedStatus = determinePredictedStatus(predictionScore, confidence);

        return FlakinessPrediction.builder()
                .testId(testId)
                .predictionScore(predictionScore)
                .confidence(confidence)
                .predictedStatus(predictedStatus)
                .factors(factors)
                .modelVersion("1.0")
                .predictedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Double> calculateMLFactors(FlakyTestAnalysis analysis) {
        Map<String, Double> factors = new HashMap<>();

        // Failure rate factor
        double failureRate = analysis.getTotalExecutions() > 0 ?
                (double) analysis.getTotalFailures() / analysis.getTotalExecutions() : 0.0;
        factors.put("failureRate", failureRate);

        // Recency weight - more recent failures matter more
        double recencyWeight = 0.0;
        if (analysis.getLastFlakyOccurrence() != null) {
            long daysSinceLastFlaky = java.time.temporal.ChronoUnit.DAYS.between(
                    analysis.getLastFlakyOccurrence(), LocalDateTime.now());
            recencyWeight = Math.max(0, 1.0 - (daysSinceLastFlaky / 14.0)); // Decay over 2 weeks
        }
        factors.put("recencyWeight", recencyWeight);

        // Pattern count factor
        List<FlakyTestPattern> patterns = patternRepository.findByTestId(analysis.getTestId());
        double patternFactor = Math.min(patterns.size() * 0.15, 0.5);
        factors.put("patternCount", patternFactor);

        // Environment diversity factor
        Set<UUID> uniqueEnvs = recordRepository.findByTestId(analysis.getTestId()).stream()
                .map(ExecutionFlakinessRecord::getEnvironmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        double envDiversity = Math.min(uniqueEnvs.size() * 0.1, 0.4);
        factors.put("envDiversity", envDiversity);

        // Timing variance factor
        List<ExecutionFlakinessRecord> records = recordRepository.findByTestId(analysis.getTestId());
        double timingVariance = calculateTimingVariance(records);
        factors.put("timingVariance", timingVariance);

        return factors;
    }

    private double calculateTimingVariance(List<ExecutionFlakinessRecord> records) {
        List<Integer> durations = records.stream()
                .filter(r -> r.getExecutionDurationMs() != null && r.getExecutionDurationMs() > 0)
                .map(ExecutionFlakinessRecord::getExecutionDurationMs)
                .collect(Collectors.toList());

        if (durations.size() < 3) return 0.0;

        double mean = durations.stream().mapToInt(i -> i).average().orElse(0.0);
        double variance = durations.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 0.0; // Coefficient of variation

        return Math.min(cv, 1.0); // Normalize to [0, 1]
    }

    private Map<String, Double> getMLCoefficients() {
        return Map.of(
                "failureRate", mlCoefficientFailureRate,
                "recencyWeight", mlCoefficientRecencyWeight,
                "patternCount", mlCoefficientPatternCount,
                "envDiversity", mlCoefficientEnvDiversity,
                "timingVariance", mlCoefficientTimingVariance
        );
    }

    private double calculateMLScore(Map<String, Double> factors) {
        double score = 0.0;

        for (Map.Entry<String, Double> entry : getMLCoefficients().entrySet()) {
            Double factorValue = factors.getOrDefault(entry.getKey(), 0.0);
            score += factorValue * entry.getValue();
        }

        return Math.min(score, 1.0);
    }

    private BigDecimal calculatePredictionConfidence(FlakyTestAnalysis analysis, Map<String, Double> factors) {
        // Confidence increases with more data and consistent patterns
        double dataSufficiency = Math.min(analysis.getTotalExecutions() / 20.0, 1.0);
        double patternConsistency = factors.getOrDefault("envDiversity", 0.0) > 0.2 ? 0.8 : 0.5;

        double confidence = (dataSufficiency * 0.6 + patternConsistency * 0.4);
        return BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }

    private String determinePredictedStatus(BigDecimal score, BigDecimal confidence) {
        if (confidence.compareTo(mlConfidenceThreshold) < 0) {
            return "insufficient_data";
        }

        if (score.compareTo(highFlakyThreshold) >= 0) {
            return "likely_flaky";
        } else if (score.compareTo(flakyThreshold) >= 0) {
            return "possibly_flaky";
        } else {
            return "likely_stable";
        }
    }

    // ==================== Automatic Retry Strategies ====================

    public RetryStrategy determineRetryStrategy(UUID testId) {
        FlakyTestAnalysis analysis = flakyAnalysisRepository.findByTestId(testId).orElse(null);

        if (analysis == null || analysis.getTotalExecutions() < minExecutionsForAnalysis) {
            return RetryStrategy.builder()
                    .testId(testId)
                    .maxAttempts(1)
                    .strategyType("NONE")
                    .delayMs(0)
                    .build();
        }

        double failureRate = (double) analysis.getTotalFailures() / analysis.getTotalExecutions();
        List<FlakyTestPattern> patterns = patternRepository.findByTestId(testId);

        // Determine retry strategy based on pattern type
        boolean hasIntermittent = patterns.stream()
                .anyMatch(p -> "intermittent".equals(p.getPatternType()));
        boolean hasTiming = patterns.stream()
                .anyMatch(p -> "timing".equals(p.getPatternType()));
        boolean hasDataDependency = patterns.stream()
                .anyMatch(p -> "data-dependent".equals(p.getPatternType()));

        String strategyType;
        int maxAttempts;
        int delayMs;

        if (failureRate > 0.6 && hasTiming) {
            strategyType = "EXPONENTIAL_BACKOFF";
            maxAttempts = maxRetryAttempts;
            delayMs = retryDelayMs;
        } else if (failureRate > 0.4 && hasIntermittent) {
            strategyType = "FIXED_DELAY";
            maxAttempts = Math.min(maxRetryAttempts, 2);
            delayMs = retryDelayMs * 2;
        } else if (failureRate > 0.2 && hasDataDependency) {
            strategyType = "SEQUENTIAL";
            maxAttempts = 2;
            delayMs = retryDelayMs;
        } else if (failureRate > 0.1) {
            strategyType = "SINGLE_RETRY";
            maxAttempts = 1;
            delayMs = retryDelayMs;
        } else {
            strategyType = "NONE";
            maxAttempts = 0;
            delayMs = 0;
        }

        // Adjust based on ML prediction
        FlakinessPrediction prediction = predictFlakiness(testId);
        if (prediction.getPredictionScore().compareTo(highFlakyThreshold) >= 0) {
            maxAttempts = Math.max(maxAttempts, 2);
        }

        return RetryStrategy.builder()
                .testId(testId)
                .strategyType(strategyType)
                .maxAttempts(maxAttempts)
                .delayMs(delayMs)
                .estimatedSuccessRate(calculateRetrySuccessRate(failureRate, strategyType))
                .build();
    }

    private double calculateRetrySuccessRate(double failureRate, String strategyType) {
        if ("NONE".equals(strategyType)) return 0.0;

        // Simplified success rate estimation
        switch (strategyType) {
            case "EXPONENTIAL_BACKOFF":
                return Math.min(failureRate * 1.5, 0.95);
            case "FIXED_DELAY":
                return Math.min(failureRate * 1.3, 0.90);
            case "SEQUENTIAL":
                return Math.min(failureRate * 1.2, 0.85);
            default:
                return Math.min(failureRate * 1.1, 0.80);
        }
    }

    public List<RetryRecommendation> getRetryRecommendations(UUID projectId) {
        List<FlakyTestAnalysis> flakyTests = flakyAnalysisRepository.findAll().stream()
                .filter(a -> a.getFlakyScore().compareTo(flakyThreshold) >= 0)
                .collect(Collectors.toList());

        List<RetryRecommendation> recommendations = new ArrayList<>();

        for (FlakyTestAnalysis analysis : flakyTests) {
            RetryStrategy strategy = determineRetryStrategy(analysis.getTestId());
            if (strategy.getMaxAttempts() > 0) {
                TestIssue test = testIssueRepository.findById(analysis.getTestId()).orElse(null);
                if (test != null) {
                    recommendations.add(RetryRecommendation.builder()
                            .testId(analysis.getTestId())
                            .testName(test.getName())
                            .currentFailureRate(analysis.getFlakyScore())
                            .recommendedStrategy(strategy)
                            .potentialImprovement(calculatePotentialImprovement(analysis, strategy))
                            .ciCdIntegrationAvailable(true)
                            .build());
                }
            }
        }

        return recommendations;
    }

    private BigDecimal calculatePotentialImprovement(FlakyTestAnalysis analysis, RetryStrategy strategy) {
        double currentFailureRate = analysis.getFlakyScore().doubleValue() / 100.0;
        double retrySuccessRate = strategy.getEstimatedSuccessRate();
        double improvedFailureRate = currentFailureRate * (1.0 - retrySuccessRate);

        BigDecimal improvement = BigDecimal.valueOf((currentFailureRate - improvedFailureRate) * 100)
                .setScale(2, RoundingMode.HALF_UP);
        return improvement;
    }

    // ==================== Root Cause Analysis ====================

    public RootCauseAnalysis performRootCauseAnalysis(UUID testId) {
        FlakyTestAnalysis analysis = flakyAnalysisRepository.findByTestId(testId).orElse(null);
        if (analysis == null) {
            throw new ResourceNotFoundException("FlakyTestAnalysis", "testId", testId);
        }

        List<FlakyTestPattern> patterns = patternRepository.findByTestId(testId);
        List<ExecutionFlakinessRecord> records = recordRepository.findByTestId(testId);

        // Analyze failure messages
        Map<String, Integer> failureCategories = categorizeFailures(records);

        // Identify primary cause
        String primaryCause = determinePrimaryCause(patterns, records, failureCategories);

        // Generate suggestions
        List<String> suggestions = generateRootCauseSuggestions(primaryCause, patterns, records);

        // Calculate confidence
        BigDecimal confidence = calculateRootCauseConfidence(patterns, records);

        // Environmental analysis
        Map<String, Long> environmentDistribution = analyzeEnvironmentDistribution(records);

        // Timing analysis
        TimingAnalysis timingAnalysis = analyzeTimingPatterns(records);

        return RootCauseAnalysis.builder()
                .testId(testId)
                .primaryCause(primaryCause)
                .secondaryCauses(identifySecondaryCauses(patterns, failureCategories))
                .suggestions(suggestions)
                .confidence(confidence)
                .failureCategories(failureCategories)
                .environmentDistribution(environmentDistribution)
                .timingAnalysis(timingAnalysis)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    private Map<String, Integer> categorizeFailures(List<ExecutionFlakinessRecord> records) {
        Map<String, Integer> categories = new HashMap<>();

        for (ExecutionFlakinessRecord record : records) {
            if (record.getFailureReason() == null) continue;

            String category = categorizeFailureMessage(record.getFailureReason());
            categories.merge(category, 1, Integer::sum);
        }

        return categories;
    }

    private String categorizeFailureMessage(String message) {
        if (message == null) return "UNKNOWN";

        String lower = message.toLowerCase();

        if (lower.contains("timeout") || lower.contains("timed out")) return "TIMEOUT";
        if (lower.contains("connection") || lower.contains("network")) return "NETWORK";
        if (lower.contains("assert") || lower.contains("expected")) return "ASSERTION";
        if (lower.contains("null") || lower.contains("undefined")) return "NULL_REFERENCE";
        if (lower.contains("database") || lower.contains("sql")) return "DATABASE";
        if (lower.contains("permission") || lower.contains("access denied")) return "PERMISSION";
        if (lower.contains("race") || lower.contains("concurrent")) return "CONCURRENCY";

        return "OTHER";
    }

    private String determinePrimaryCause(List<FlakyTestPattern> patterns,
                                         List<ExecutionFlakinessRecord> records,
                                         Map<String, Integer> failureCategories) {
        // Check patterns first
        if (!patterns.isEmpty()) {
            FlakyTestPattern topPattern = patterns.stream()
                    .max((a, b) -> a.getFrequencyScore().compareTo(b.getFrequencyScore()))
                    .orElse(null);

            if (topPattern != null && topPattern.getFrequencyScore().compareTo(BigDecimal.valueOf(0.5)) >= 0) {
                return classifyPatternCause(topPattern.getPatternType());
            }
        }

        // Check failure categories
        Optional<Map.Entry<String, Integer>> topCategory = failureCategories.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (topCategory.isPresent()) {
            return topCategory.get().getKey();
        }

        // Default to unknown
        return "UNKNOWN";
    }

    private String classifyPatternCause(String patternType) {
        switch (patternType) {
            case "environmental": return "ENVIRONMENT_CONFIGURATION";
            case "timing": return "TIMING_SENSITIVITY";
            case "data-dependent": return "DATA_DEPENDENCY";
            case "intermittent": return "INTERMITTENT_FAILURE";
            default: return "UNKNOWN";
        }
    }

    private List<String> identifySecondaryCauses(List<FlakyTestPattern> patterns,
                                                  Map<String, Integer> failureCategories) {
        List<String> secondary = new ArrayList<>();

        for (FlakyTestPattern pattern : patterns) {
            if (pattern.getFrequencyScore().compareTo(BigDecimal.valueOf(0.2)) >= 0 &&
                    pattern.getFrequencyScore().compareTo(BigDecimal.valueOf(0.5)) < 0) {
                secondary.add(classifyPatternCause(pattern.getPatternType()));
            }
        }

        // Add low-frequency failure categories
        failureCategories.entrySet().stream()
                .filter(e -> e.getValue() <= 3 && e.getValue() > 0)
                .map(Map.Entry::getKey)
                .forEach(secondary::add);

        return secondary.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private List<String> generateRootCauseSuggestions(String primaryCause,
                                                       List<FlakyTestPattern> patterns,
                                                       List<ExecutionFlakinessRecord> records) {
        List<String> suggestions = new ArrayList<>();

        switch (primaryCause) {
            case "TIMING_SENSITIVITY":
                suggestions.add("Add explicit wait conditions instead of fixed delays");
                suggestions.add("Implement retry with exponential backoff");
                suggestions.add("Review test for race conditions");
                break;
            case "ENVIRONMENT_CONFIGURATION":
                suggestions.add("Containerize test environment for consistency");
                suggestions.add("Use environment provisioning service");
                suggestions.add("Add environment health checks before test execution");
                break;
            case "DATA_DEPENDENCY":
                suggestions.add("Use test data factories instead of shared data");
                suggestions.add("Add data cleanup in test teardown");
                suggestions.add("Isolate tests from external data dependencies");
                break;
            case "NETWORK":
                suggestions.add("Add network timeout configuration");
                suggestions.add("Implement circuit breaker pattern");
                suggestions.add("Use mock services for external dependencies");
                break;
            case "DATABASE":
                suggestions.add("Use transaction rollback for test isolation");
                suggestions.add("Mock database interactions where possible");
                suggestions.add("Add database connection pooling");
                break;
            default:
                suggestions.add("Review test implementation for potential issues");
                suggestions.add("Enable detailed logging for failure investigation");
                suggestions.add("Consider test refactoring");
        }

        // Add pattern-specific suggestions
        for (FlakyTestPattern pattern : patterns) {
            if (pattern.getSuggestedFix() != null && !pattern.getSuggestedFix().isEmpty()) {
                suggestions.add(0, pattern.getSuggestedFix()); // Add at beginning
            }
        }

        return suggestions.stream().distinct().limit(10).collect(Collectors.toList());
    }

    private BigDecimal calculateRootCauseConfidence(List<FlakyTestPattern> patterns,
                                                    List<ExecutionFlakinessRecord> records) {
        if (records.size() < minExecutionsForAnalysis) {
            return BigDecimal.valueOf(0.3);
        }

        double patternConfidence = Math.min(patterns.size() * 0.15, 0.5);
        double dataConfidence = Math.min(records.size() / 30.0, 0.4);

        double confidence = patternConfidence + dataConfidence + 0.2; // Base confidence
        return BigDecimal.valueOf(Math.min(confidence, 1.0)).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Long> analyzeEnvironmentDistribution(List<ExecutionFlakinessRecord> records) {
        return records.stream()
                .filter(r -> r.getEnvironmentId() != null)
                .collect(Collectors.groupingBy(r -> r.getEnvironmentId().toString(), Collectors.counting()));
    }

    private TimingAnalysis analyzeTimingPatterns(List<ExecutionFlakinessRecord> records) {
        List<Integer> durations = records.stream()
                .filter(r -> r.getExecutionDurationMs() != null)
                .map(ExecutionFlakinessRecord::getExecutionDurationMs)
                .collect(Collectors.toList());

        if (durations.isEmpty()) {
            return TimingAnalysis.builder()
                    .averageDuration(0)
                    .minDuration(0)
                    .maxDuration(0)
                    .variance(0.0)
                    .hasTimingIssue(false)
                    .build();
        }

        double avg = durations.stream().mapToInt(i -> i).average().orElse(0.0);
        int min = durations.stream().mapToInt(i -> i).min().orElse(0);
        int max = durations.stream().mapToInt(i -> i).max().orElse(0);
        double variance = calculateVariance(durations, avg);
        boolean hasTimingIssue = variance > timingThreshold;

        return TimingAnalysis.builder()
                .averageDuration((int) avg)
                .minDuration(min)
                .maxDuration(max)
                .variance(variance)
                .hasTimingIssue(hasTimingIssue)
                .build();
    }

    private double calculateVariance(List<Integer> values, double mean) {
        if (values.size() < 2) return 0.0;

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return mean > 0 ? Math.sqrt(variance) / mean : 0.0;
    }

    // ==================== CI/CD Integration ====================

    public Map<String, Object> getCIDCIntegrationConfig(UUID testId) {
        RetryStrategy strategy = determineRetryStrategy(testId);
        FlakinessPrediction prediction = predictFlakiness(testId);

        Map<String, Object> config = new HashMap<>();
        config.put("testId", testId.toString());
        config.put("retryStrategy", strategy.getStrategyType());
        config.put("maxRetries", strategy.getMaxAttempts());
        config.put("retryDelayMs", strategy.getDelayMs());
        config.put("predictionScore", prediction.getPredictionScore());
        config.put("predictionConfidence", prediction.getConfidence());
        config.put("predictedStatus", prediction.getPredictedStatus());

        // Generate CI/CD pipeline snippet
        config.put("pipelineSnippet", generatePipelineSnippet(testId, strategy));

        return config;
    }

    private String generatePipelineSnippet(UUID testId, RetryStrategy strategy) {
        if (strategy.getMaxAttempts() == 0) {
            return "# No retry strategy needed - test is stable";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Retry configuration for test: ").append(testId).append("\n");
        sb.append("retry:\n");
        sb.append("  maxAttempts: ").append(strategy.getMaxAttempts()).append("\n");
        sb.append("  delay: ").append(strategy.getDelayMs()).append("ms\n");
        sb.append("  strategy: ").append(strategy.getStrategyType().toLowerCase().replace("_", "-")).append("\n");

        return sb.toString();
    }

    public List<Map<String, Object>> getCIStatusReport(UUID projectId) {
        List<FlakyTestAnalysis> analyses = flakyAnalysisRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (FlakyTestAnalysis analysis : analyses) {
            TestIssue test = testIssueRepository.findById(analysis.getTestId()).orElse(null);
            if (test == null) continue;

            RetryStrategy strategy = determineRetryStrategy(analysis.getTestId());
            FlakinessPrediction prediction = predictFlakiness(analysis.getTestId());

            Map<String, Object> entry = new HashMap<>();
            entry.put("testId", analysis.getTestId().toString());
            entry.put("testName", test.getName());
            entry.put("flakyScore", analysis.getFlakyScore());
            entry.put("predictionScore", prediction.getPredictionScore());
            entry.put("predictedStatus", prediction.getPredictedStatus());
            entry.put("retryStrategy", strategy.getStrategyType());
            entry.put("shouldQuarantine", "quarantine_candidate".equals(analysis.getCurrentStatus()));
            entry.put("lastExecution", analysis.getLastFlakyOccurrence());

            report.add(entry);
        }

        return report;
    }

    // ==================== Pattern Recognition ====================

    @Transactional
    public void identifyPatterns(UUID testId) {
        List<ExecutionFlakinessRecord> records = recordRepository.findByTestId(testId);

        if (records.size() < minExecutionsForAnalysis) {
            return;
        }

        // Analyze timing patterns
        double timingVariance = calculateTimingVariance(records);
        if (timingVariance > timingThreshold) {
            createOrUpdatePattern(testId, "timing", "Test execution time varies significantly",
                    BigDecimal.valueOf(timingVariance), null, null,
                    "Review async operations and add explicit waits");
        }

        // Detect environmental patterns
        Map<UUID, Long> envCounts = records.stream()
                .filter(r -> r.getEnvironmentId() != null)
                .collect(Collectors.groupingBy(ExecutionFlakinessRecord::getEnvironmentId, Collectors.counting()));

        Optional<Map.Entry<UUID, Long>> mostAffectedEnv = envCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (mostAffectedEnv.isPresent() && mostAffectedEnv.get().getValue() > records.size() * 0.5) {
            double envFrequency = (double) mostAffectedEnv.get().getValue() / records.size();
            createOrUpdatePattern(testId, "environmental", "Test fails more on specific environment",
                    BigDecimal.valueOf(envFrequency),
                    List.of(mostAffectedEnv.get().getKey().toString()),
                    null, "Review environment-specific configurations");
        }

        // Detect data dependency patterns
        Map<String, Long> failureMessages = records.stream()
                .filter(r -> r.getFailureReason() != null)
                .collect(Collectors.groupingBy(ExecutionFlakinessRecord::getFailureReason, Collectors.counting()));

        long dataRelatedFailures = failureMessages.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains("data") ||
                        e.getKey().toLowerCase().contains("null") ||
                        e.getKey().toLowerCase().contains("undefined"))
                .count();

        if (dataRelatedFailures > records.size() * dataDependencyThreshold) {
            createOrUpdatePattern(testId, "data-dependent", "Test depends on external data",
                    BigDecimal.valueOf((double) dataRelatedFailures / records.size()),
                    null, null, "Use test data factories, mock external data");
        }

        // Detect intermittent patterns
        long retryCount = records.stream()
                .filter(r -> r.getRetryAttempt() != null && r.getRetryAttempt() > 0)
                .count();
        if (retryCount > records.size() * intermittentThreshold) {
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

        // Enhanced pattern statistics
        Map<String, Integer> patternsByType = new HashMap<>();
        patternsByType.put("intermittent", (int) patternRepository.findByTestId(null).stream()
                .filter(p -> "intermittent".equals(p.getPatternType())).count());
        patternsByType.put("environmental", (int) patternRepository.findByTestId(null).stream()
                .filter(p -> "environmental".equals(p.getPatternType())).count());
        patternsByType.put("timing", (int) patternRepository.findByTestId(null).stream()
                .filter(p -> "timing".equals(p.getPatternType())).count());
        patternsByType.put("data-dependent", (int) patternRepository.findByTestId(null).stream()
                .filter(p -> "data-dependent".equals(p.getPatternType())).count());

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

    // ==================== Auto-Quarantine Evaluation ====================

    private void evaluateAutoQuarantine(FlakyTestAnalysis analysis) {
        if (analysis.getFlakyScore().compareTo(highFlakyThreshold) >= 0 &&
            analysis.getTotalExecutions() >= minExecutionsForAnalysis) {

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
               analysis.getFlakyScore().compareTo(highFlakyThreshold) >= 0;
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

        double score = (double) analysis.getTotalFailures() / analysis.getTotalExecutions();
        double confidence = Math.min(analysis.getTotalExecutions() / 20.0, 1.0);
        analysis.setConfidenceLevel(BigDecimal.valueOf(confidence));

        analysis.setFlakyScore(BigDecimal.valueOf(score * 100).setScale(2, RoundingMode.HALF_UP));
    }

    private void updatePassRateTrend(FlakyTestAnalysis analysis) {
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
        if (analysis.getFlakyScore().compareTo(highFlakyThreshold) >= 0) {
            analysis.setCurrentStatus("flaky");
        } else if (analysis.getFlakyScore().compareTo(flakyThreshold) >= 0) {
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

    // ==================== DTO Classes ====================

    @lombok.Data
    @lombok.Builder
    public static class FlakinessPrediction {
        private UUID testId;
        private BigDecimal predictionScore;
        private BigDecimal confidence;
        private String predictedStatus;
        private Map<String, Double> factors;
        private String modelVersion;
        private LocalDateTime predictedAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class RetryStrategy {
        private UUID testId;
        private String strategyType;
        private int maxAttempts;
        private int delayMs;
        private double estimatedSuccessRate;
    }

    @lombok.Data
    @lombok.Builder
    public static class RetryRecommendation {
        private UUID testId;
        private String testName;
        private BigDecimal currentFailureRate;
        private RetryStrategy recommendedStrategy;
        private BigDecimal potentialImprovement;
        private boolean ciCdIntegrationAvailable;
    }

    @lombok.Data
    @lombok.Builder
    public static class RootCauseAnalysis {
        private UUID testId;
        private String primaryCause;
        private List<String> secondaryCauses;
        private List<String> suggestions;
        private BigDecimal confidence;
        private Map<String, Integer> failureCategories;
        private Map<String, Long> environmentDistribution;
        private TimingAnalysis timingAnalysis;
        private LocalDateTime analyzedAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class TimingAnalysis {
        private int averageDuration;
        private int minDuration;
        private int maxDuration;
        private double variance;
        private boolean hasTimingIssue;
    }
}