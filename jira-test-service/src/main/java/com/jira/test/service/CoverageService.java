package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.event.CoverageRecalculatedEvent;
import com.jira.test.event.EventPublisherService;
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
import java.util.stream.Stream;

/**
 * Service for calculating and managing test coverage metrics.
 * This service is invoked by event listeners when test runs are updated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverageService {

    private final EventPublisherService eventPublisher;
    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final TestPlanRepository testPlanRepository;
    private final CoverageRuleRepository coverageRuleRepository;
    private final CoverageThresholdRepository coverageThresholdRepository;
    private final CoverageDriftRecordRepository coverageDriftRecordRepository;
    private final TestSetRepository testSetRepository;

    // ========== CORE COVERAGE METHODS ==========

    @Transactional
    public void recalculateCoverage(UUID projectId, UUID testId) {
        log.info("Recalculating coverage for project: {}, test: {}", projectId, testId);

        try {
            List<String> requirementKeys = requirementLinkRepository.findByTestId(testId)
                    .stream()
                    .map(r -> r.getRequirementKey())
                    .toList();

            double coveragePercentage = calculateCoverage(projectId, testId, requirementKeys);

            CoverageRecalculatedEvent event = CoverageRecalculatedEvent.builder()
                    .source(this)
                    .projectId(projectId)
                    .requirementId(null)
                    .testPlanId(null)
                    .coveragePercentage(coveragePercentage)
                    .totalTests(1)
                    .coveredTests(coveragePercentage > 0 ? 1 : 0)
                    .impactedRequirementIds(new ArrayList<>())
                    .build();

            eventPublisher.publish(event);
            log.info("Coverage recalculation completed: {}%", coveragePercentage);

        } catch (Exception e) {
            log.error("Failed to recalculate coverage: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void recalculateRequirementCoverage(UUID projectId, UUID requirementId) {
        log.info("Recalculating requirement coverage for requirement: {}", requirementId);

        try {
            requirementLinkRepository.findById(requirementId).ifPresent(link -> {
                String requirementKey = link.getRequirementKey();
                recalculateCoverageForRequirementKey(projectId, requirementKey);
            });

        } catch (Exception e) {
            log.error("Failed to recalculate requirement coverage: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public double getCoveragePercentage(UUID projectId, UUID requirementId) {
        return requirementLinkRepository.findById(requirementId)
                .map(link -> {
                    String requirementKey = link.getRequirementKey();
                    return calculateCoverage(projectId, null, List.of(requirementKey));
                })
                .orElse(0.0);
    }

    private void recalculateCoverageForRequirementKey(UUID projectId, String requirementKey) {
        List<String> testKeys = requirementLinkRepository.findByRequirementKey(requirementKey)
                .stream()
                .map(r -> r.getTestId().toString())
                .toList();

        int totalTests = testKeys.size();
        int coveredTests = countCoveredTests(testKeys);
        double coveragePercentage = totalTests > 0 ? (coveredTests * 100.0 / totalTests) : 0.0;

        CoverageRecalculatedEvent event = CoverageRecalculatedEvent.builder()
                .source(this)
                .projectId(projectId)
                .requirementId(null)
                .testPlanId(null)
                .coveragePercentage(coveragePercentage)
                .totalTests(totalTests)
                .coveredTests(coveredTests)
                .impactedRequirementIds(new ArrayList<>())
                .build();

        eventPublisher.publish(event);
        log.info("Requirement coverage recalculated for {}: {}%", requirementKey, coveragePercentage);
    }

    private double calculateCoverage(UUID projectId, UUID testId, List<String> requirementKeys) {
        int totalTests = 0;
        int coveredTests = 0;

        if (testId != null) {
            totalTests = 1;
            coveredTests = executionRepository.findByTestId(testId).stream()
                    .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus())) ? 1 : 0;
        } else if (!requirementKeys.isEmpty()) {
            for (String reqKey : requirementKeys) {
                List<UUID> tests = requirementLinkRepository.findByRequirementKey(reqKey)
                        .stream()
                        .map(r -> r.getTestId())
                        .toList();
                totalTests += tests.size();
                for (UUID tid : tests) {
                    boolean hasExecution = executionRepository.findByTestId(tid).stream()
                            .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
                    if (hasExecution) coveredTests++;
                }
            }
        }

        return totalTests > 0 ? (coveredTests * 100.0 / totalTests) : 0.0;
    }

    private int countCoveredTests(List<String> testIdStrings) {
        int count = 0;
        for (String testIdStr : testIdStrings) {
            UUID testId = UUID.fromString(testIdStr);
            boolean hasExecution = executionRepository.findByTestId(testId).stream()
                    .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
            if (hasExecution) count++;
        }
        return count;
    }

    // ========== COVERAGE RULE ENGINE ==========

    @Transactional
    public CoverageRuleResponse createRule(CoverageRuleRequest request) {
        CoverageRule rule = CoverageRule.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .ruleType(request.getRuleType())
                .threshold(request.getThreshold())
                .scope(request.getScope())
                .scopeId(request.getScopeId())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        rule = coverageRuleRepository.save(rule);
        log.info("Created coverage rule: {} for project: {}", rule.getName(), rule.getProjectId());
        return CoverageRuleResponse.from(rule);
    }

    @Transactional
    public CoverageRuleResponse updateRule(UUID ruleId, CoverageRuleRequest request) {
        CoverageRule rule = coverageRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Coverage rule not found: " + ruleId));

        if (request.getName() != null) rule.setName(request.getName());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
        if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
        if (request.getScope() != null) rule.setScope(request.getScope());
        if (request.getScopeId() != null) rule.setScopeId(request.getScopeId());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());

        rule = coverageRuleRepository.save(rule);
        log.info("Updated coverage rule: {}", ruleId);
        return CoverageRuleResponse.from(rule);
    }

    @Transactional(readOnly = true)
    public List<CoverageRuleResponse> getRulesByProject(UUID projectId) {
        return coverageRuleRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(CoverageRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoverageRuleResponse> getEnabledRules(UUID projectId) {
        return coverageRuleRepository.findByProjectIdAndEnabledTrue(projectId)
                .stream()
                .map(CoverageRuleResponse::from)
                .toList();
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        coverageRuleRepository.deleteById(ruleId);
        log.info("Deleted coverage rule: {}", ruleId);
    }

    /**
     * Evaluates all enabled coverage rules for a project and returns violations.
     */
    @Transactional(readOnly = true)
    public List<CoverageRuleViolation> evaluateRules(UUID projectId) {
        List<CoverageRule> enabledRules = coverageRuleRepository.findByProjectIdAndEnabledTrue(projectId);
        List<CoverageRuleViolation> violations = new ArrayList<>();

        for (CoverageRule rule : enabledRules) {
            BigDecimal currentCoverage = getScopeCoverage(projectId, rule);
            BigDecimal threshold = rule.getThreshold();

            boolean violated = switch (rule.getRuleType()) {
                case MINIMUM_COVERAGE -> currentCoverage.compareTo(threshold) < 0;
                case TEST_DIVERSITY -> evaluateTestDiversity(projectId, rule);
                case EXECUTION_FREQUENCY -> evaluateExecutionFrequency(projectId, rule);
            };

            if (violated) {
                violations.add(CoverageRuleViolation.builder()
                        .ruleId(rule.getId())
                        .ruleName(rule.getName())
                        .ruleType(rule.getRuleType())
                        .threshold(threshold)
                        .currentValue(currentCoverage)
                        .scope(rule.getScope())
                        .scopeId(rule.getScopeId())
                        .build());
            }
        }

        return violations;
    }

    private BigDecimal getScopeCoverage(UUID projectId, CoverageRule rule) {
        double coverage;
        if (rule.getScope() == CoverageRule.Scope.GLOBAL) {
            coverage = calculateProjectCoverage(projectId);
        } else if (rule.getScopeId() != null) {
            coverage = switch (rule.getScope()) {
                case REQUIREMENT -> getCoverageForRequirement(rule.getScopeId());
                case TEST_SET -> getCoverageForTestSet(rule.getScopeId());
                default -> calculateProjectCoverage(projectId);
            };
        } else {
            coverage = calculateProjectCoverage(projectId);
        }
        return BigDecimal.valueOf(coverage).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean evaluateTestDiversity(UUID projectId, CoverageRule rule) {
        // Check if there are sufficient diverse test types covering requirements
        int distinctTestTypes = getDistinctTestTypes(projectId);
        return distinctTestTypes < rule.getThreshold().intValue();
    }

    private boolean evaluateExecutionFrequency(UUID projectId, CoverageRule rule) {
        // Check if tests are executed frequently enough
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long recentExecutions = executionRepository.findByExecutedAtAfter(since).stream()
                .filter(e -> executionRepository.existsByProjectId(projectId))
                .count();
        return recentExecutions < rule.getThreshold().intValue();
    }

    private int getDistinctTestTypes(UUID projectId) {
        return testIssueRepository.findByProjectId(projectId).stream()
                .map(TestIssue::getTestType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
    }

    private double calculateProjectCoverage(UUID projectId) {
        List<TestIssue> tests = testIssueRepository.findByProjectId(projectId);
        if (tests.isEmpty()) return 0.0;

        int covered = 0;
        for (TestIssue test : tests) {
            boolean hasExecution = executionRepository.findByTestId(test.getId()).stream()
                    .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
            if (hasExecution) covered++;
        }

        return (covered * 100.0) / tests.size();
    }

    private double getCoverageForRequirement(UUID requirementId) {
        return requirementLinkRepository.findById(requirementId)
                .map(link -> calculateCoverage(null, null, List.of(link.getRequirementKey())))
                .orElse(0.0);
    }

    private double getCoverageForTestSet(UUID testSetId) {
        List<TestIssue> tests = testSetRepository.findById(testSetId)
                .map(ts -> testSetRepository.findTestsByTestSetId(testSetId))
                .orElse(List.of());

        if (tests.isEmpty()) return 0.0;

        int covered = 0;
        for (TestIssue test : tests) {
            boolean hasExecution = executionRepository.findByTestId(test.getId()).stream()
                    .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
            if (hasExecution) covered++;
        }

        return (covered * 100.0) / tests.size();
    }

    // ========== THRESHOLD MONITORING ==========

    @Transactional
    public CoverageThresholdResponse updateThreshold(UUID requirementId, CoverageThresholdRequest request) {
        CoverageThreshold threshold = coverageThresholdRepository.findByRequirementId(requirementId)
                .orElse(new CoverageThreshold());

        threshold.setRequirementId(requirementId);
        if (request.getProjectId() != null) threshold.setProjectId(request.getProjectId());
        if (request.getRequirementKey() != null) threshold.setRequirementKey(request.getRequirementKey());
        if (request.getMinimumCoverage() != null) threshold.setMinimumCoverage(request.getMinimumCoverage());
        if (request.getWarningThreshold() != null) threshold.setWarningThreshold(request.getWarningThreshold());
        if (request.getAlertEnabled() != null) threshold.setAlertEnabled(request.getAlertEnabled());

        // Calculate current coverage
        double currentCoverage = getCoveragePercentage(threshold.getProjectId(), requirementId);
        threshold.setCurrentCoverage(BigDecimal.valueOf(currentCoverage).setScale(2, RoundingMode.HALF_UP));

        threshold = coverageThresholdRepository.save(threshold);
        log.info("Updated threshold for requirement: {}", requirementId);
        return CoverageThresholdResponse.from(threshold);
    }

    @Transactional(readOnly = true)
    public List<CoverageThresholdResponse> getThresholdsByProject(UUID projectId) {
        return coverageThresholdRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(CoverageThresholdResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoverageAlertResponse> getAlerts(UUID projectId) {
        List<CoverageThreshold> thresholds = coverageThresholdRepository.findByProjectIdAndAlertEnabledTrue(projectId);
        List<CoverageAlertResponse> alerts = new ArrayList<>();

        for (CoverageThreshold threshold : thresholds) {
            CoverageThreshold.AlertLevel level = threshold.getAlertLevel();
            if (level != CoverageThreshold.AlertLevel.OK) {
                alerts.add(CoverageAlertResponse.builder()
                        .requirementId(threshold.getRequirementId())
                        .requirementKey(threshold.getRequirementKey())
                        .alertLevel(level)
                        .currentCoverage(threshold.getCurrentCoverage())
                        .minimumCoverage(threshold.getMinimumCoverage())
                        .warningThreshold(threshold.getWarningThreshold())
                        .message(generateAlertMessage(threshold))
                        .build());
            }
        }

        // Check rule violations
        List<CoverageRuleViolation> violations = evaluateRules(projectId);
        for (CoverageRuleViolation violation : violations) {
            alerts.add(CoverageAlertResponse.builder()
                    .ruleId(violation.getRuleId())
                    .alertLevel(CoverageThreshold.AlertLevel.CRITICAL)
                    .currentCoverage(violation.getCurrentValue())
                    .minimumCoverage(violation.getThreshold())
                    .message(String.format("Coverage rule violation: %s", violation.getRuleName()))
                    .build());
        }

        return alerts;
    }

    private String generateAlertMessage(CoverageThreshold threshold) {
        return switch (threshold.getAlertLevel()) {
            case CRITICAL -> String.format("Coverage %s%% is below minimum threshold %s%%",
                    threshold.getCurrentCoverage(), threshold.getMinimumCoverage());
            case WARNING -> String.format("Coverage %s%% is approaching minimum threshold %s%%",
                    threshold.getCurrentCoverage(), threshold.getMinimumCoverage());
            default -> "Coverage is healthy";
        };
    }

    // ========== COVERAGE TREND ANALYSIS ==========

    @Transactional(readOnly = true)
    public CoverageTrendResponse getCoverageTrends(UUID projectId, int days) {
        List<CoverageDriftRecord> driftRecords = coverageDriftRecordRepository.findAll().stream()
                .filter(r -> isInDateRange(r.getAnalysisTimestamp(), days))
                .toList();

        List<CoverageTrendResponse.TrendDataPoint> trendPoints = new ArrayList<>();
        Map<LocalDateTime, List<CoverageDriftRecord>> byDate = driftRecords.stream()
                .collect(Collectors.groupingBy(r -> r.getAnalysisTimestamp().toLocalDate().atStartOfDay()));

        byDate.forEach((date, records) -> {
            BigDecimal avgCoverage = records.stream()
                    .map(CoverageDriftRecord::getCurrentCoverageScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);

            trendPoints.add(CoverageTrendResponse.TrendDataPoint.builder()
                    .date(date)
                    .overallCoverage(avgCoverage)
                    .build());
        });

        trendPoints.sort(Comparator.comparing(CoverageTrendResponse.TrendDataPoint::getDate));

        CoverageTrendResponse.TrendSummary summary = calculateTrendSummary(trendPoints, days);

        return CoverageTrendResponse.builder()
                .projectId(projectId)
                .periodDays(days)
                .trendPoints(trendPoints)
                .summary(summary)
                .build();
    }

    private boolean isInDateRange(LocalDateTime timestamp, int days) {
        return timestamp.isAfter(LocalDateTime.now().minusDays(days));
    }

    private CoverageTrendResponse.TrendSummary calculateTrendSummary(List<CoverageTrendResponse.TrendDataPoint> points, int days) {
        if (points.isEmpty()) {
            return CoverageTrendResponse.TrendSummary.builder()
                    .currentCoverage(BigDecimal.ZERO)
                    .trendDirection("STABLE")
                    .build();
        }

        BigDecimal current = points.get(points.size() - 1).getOverallCoverage();
        BigDecimal sevenDayAvg = calculateAverage(points, 7);
        BigDecimal thirtyDayAvg = calculateAverage(points, 30);
        BigDecimal ninetyDayAvg = calculateAverage(points, 90);

        BigDecimal changeRate7d = calculateChangeRate(points, 7);
        BigDecimal changeRate30d = calculateChangeRate(points, 30);
        BigDecimal changeRate90d = calculateChangeRate(points, 90);

        String trendDirection = determineTrendDirection(changeRate7d);

        return CoverageTrendResponse.TrendSummary.builder()
                .currentCoverage(current)
                .sevenDayAverage(sevenDayAvg)
                .thirtyDayAverage(thirtyDayAvg)
                .ninetyDayAverage(ninetyDayAvg)
                .changeRate7d(changeRate7d)
                .changeRate30d(changeRate30d)
                .changeRate90d(changeRate90d)
                .trendDirection(trendDirection)
                .build();
    }

    private BigDecimal calculateAverage(List<CoverageTrendResponse.TrendDataPoint> points, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return points.stream()
                .filter(p -> p.getDate().isAfter(since))
                .map(CoverageTrendResponse.TrendDataPoint::getOverallCoverage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, (int) points.stream().filter(p -> p.getDate().isAfter(since)).count())), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChangeRate(List<CoverageTrendResponse.TrendDataPoint> points, int days) {
        if (points.size() < 2) return BigDecimal.ZERO;

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<CoverageTrendResponse.TrendDataPoint> filtered = points.stream()
                .filter(p -> p.getDate().isAfter(since))
                .toList();

        if (filtered.size() < 2) return BigDecimal.ZERO;

        BigDecimal oldest = filtered.get(0).getOverallCoverage();
        BigDecimal newest = filtered.get(filtered.size() - 1).getOverallCoverage();

        if (oldest.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return newest.subtract(oldest).divide(oldest, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private String determineTrendDirection(BigDecimal changeRate) {
        if (changeRate.compareTo(BigDecimal.valueOf(5)) > 0) return "IMPROVING";
        if (changeRate.compareTo(BigDecimal.valueOf(-5)) < 0) return "DECLINING";
        return "STABLE";
    }

    // ========== PROJECT COVERAGE ==========

    @Transactional(readOnly = true)
    public ProjectCoverageResponse getProjectCoverage(UUID projectId) {
        List<TestIssue> tests = testIssueRepository.findByProjectId(projectId);
        Map<String, List<RequirementLink>> linksByRequirement = requirementLinkRepository.findAll().stream()
                .collect(Collectors.groupingBy(RequirementLink::getRequirementKey));

        int totalTests = tests.size();
        int coveredTests = 0;
        Map<String, ProjectCoverageResponse.RequirementCoverage> byRequirement = new HashMap<>();

        for (TestIssue test : tests) {
            boolean hasExecution = !executionRepository.findByTestId(test.getId()).isEmpty() &&
                    executionRepository.findByTestId(test.getId()).stream()
                            .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
            if (hasExecution) coveredTests++;
        }

        // Calculate coverage by requirement
        for (Map.Entry<String, List<RequirementLink>> entry : linksByRequirement.entrySet()) {
            String reqKey = entry.getKey();
            List<RequirementLink> links = entry.getValue();
            List<UUID> testIds = links.stream().map(RequirementLink::getTestId).toList();

            int reqTotal = testIds.size();
            int reqCovered = 0;
            for (UUID testId : testIds) {
                boolean hasExecution = !executionRepository.findByTestId(testId).isEmpty() &&
                        executionRepository.findByTestId(testId).stream()
                                .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus()));
                if (hasExecution) reqCovered++;
            }

            BigDecimal coverage = reqTotal > 0 ?
                    BigDecimal.valueOf(reqCovered * 100.0 / reqTotal).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            // Check threshold
            boolean meetsThreshold = coverageThresholdRepository.findByRequirementId(links.get(0).getId())
                    .map(t -> coverage.compareTo(t.getMinimumCoverage()) >= 0)
                    .orElse(true);

            byRequirement.put(reqKey, ProjectCoverageResponse.RequirementCoverage.builder()
                    .requirementKey(reqKey)
                    .requirementId(links.get(0).getId())
                    .coverage(coverage)
                    .totalTests(reqTotal)
                    .coveredTests(reqCovered)
                    .uncoveredTests(reqTotal - reqCovered)
                    .meetsThreshold(meetsThreshold)
                    .build());
        }

        BigDecimal overallCoverage = totalTests > 0 ?
                BigDecimal.valueOf(coveredTests * 100.0 / totalTests).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        List<CoverageAlert> alerts = getAlerts(projectId).stream()
                .map(a -> CoverageAlert.builder()
                        .alertType("THRESHOLD")
                        .severity(a.getAlertLevel().name())
                        .message(a.getMessage())
                        .requirementKey(a.getRequirementKey())
                        .build())
                .toList();

        return ProjectCoverageResponse.builder()
                .projectId(projectId)
                .overallCoverage(overallCoverage)
                .totalRequirements(byRequirement.size())
                .totalTests(totalTests)
                .coveredTests(coveredTests)
                .uncoveredTests(totalTests - coveredTests)
                .byRequirement(byRequirement)
                .lastUpdated(LocalDateTime.now())
                .alerts(alerts)
                .build();
    }

    // ========== COVERAGE MATRIX ==========

    @Transactional(readOnly = true)
    public CoverageMatrixResponse getCoverageMatrix(UUID projectId) {
        Map<String, List<RequirementLink>> linksByRequirement = requirementLinkRepository.findAll().stream()
                .collect(Collectors.groupingBy(RequirementLink::getRequirementKey));
        List<String> requirementKeys = new ArrayList<>(linksByRequirement.keySet());

        List<TestSet> testSets = testSetRepository.findAll();
        List<String> testSetNames = testSets.stream().map(TestSet::getName).toList();

        List<List<CoverageMatrixResponse.MatrixCell>> matrix = new ArrayList<>();

        for (String reqKey : requirementKeys) {
            List<CoverageMatrixResponse.MatrixCell> row = new ArrayList<>();
            for (TestSet testSet : testSets) {
                List<RequirementLink> links = linksByRequirement.get(reqKey);
                int totalTests = links.size();
                int coveredTests = (int) links.stream()
                        .filter(l -> !executionRepository.findByTestId(l.getTestId()).isEmpty() &&
                                executionRepository.findByTestId(l.getTestId()).stream()
                                        .anyMatch(e -> "PASSED".equals(e.getStatus()) || "FAILED".equals(e.getStatus())))
                        .count();

                BigDecimal coverage = totalTests > 0 ?
                        BigDecimal.valueOf(coveredTests * 100.0 / totalTests).setScale(2, RoundingMode.HALF_UP) :
                        BigDecimal.ZERO;

                String status = coverage.compareTo(BigDecimal.valueOf(80)) >= 0 ? "COVERED" :
                        coverage.compareTo(BigDecimal.valueOf(50)) >= 0 ? "PARTIAL" : "UNCOVERED";

                row.add(CoverageMatrixResponse.MatrixCell.builder()
                        .requirementKey(reqKey)
                        .testSetName(testSet.getName())
                        .coverage(coverage)
                        .testsCovered(coveredTests)
                        .totalTests(totalTests)
                        .status(status)
                        .build());
            }
            matrix.add(row);
        }

        int fullyCovered = (int) matrix.stream()
                .flatMap(List::stream)
                .filter(c -> "COVERED".equals(c.getStatus()))
                .count();
        int partiallyCovered = (int) matrix.stream()
                .flatMap(List::stream)
                .filter(c -> "PARTIAL".equals(c.getStatus()))
                .count();

        BigDecimal overallCoverage = matrix.stream()
                .flatMap(List::stream)
                .mapToDouble(c -> c.getCoverage().doubleValue())
                .average()
                .orElse(0.0);

        return CoverageMatrixResponse.builder()
                .projectId(projectId)
                .requirementKeys(requirementKeys)
                .testSetNames(testSetNames)
                .matrix(matrix)
                .summary(CoverageMatrixResponse.MatrixSummary.builder()
                        .overallCoverage(BigDecimal.valueOf(overallCoverage).setScale(2, RoundingMode.HALF_UP))
                        .totalRequirements(requirementKeys.size())
                        .totalTestSets(testSets.size())
                        .fullyCoveredRequirements(fullyCovered)
                        .partiallyCoveredRequirements(partiallyCovered)
                        .uncoveredRequirements((int) matrix.stream()
                                .flatMap(List::stream)
                                .filter(c -> "UNCOVERED".equals(c.getStatus()))
                                .count())
                        .build())
                .build();
    }

    // ========== AUTOMATED SUGGESTIONS & PRIORITIZATION ==========

    @Transactional(readOnly = true)
    public CoverageSuggestionResponse getSuggestions(UUID projectId) {
        List<TestIssue> tests = testIssueRepository.findByProjectId(projectId);
        List<CoverageSuggestionResponse.CoverageSuggestion> suggestions = new ArrayList<>();
        List<CoverageSuggestionResponse.PrioritizedTest> prioritizedTests = new ArrayList<>();

        // Find uncovered requirements
        Map<String, List<RequirementLink>> linksByRequirement = requirementLinkRepository.findAll().stream()
                .collect(Collectors.groupingBy(RequirementLink::getRequirementKey));

        for (Map.Entry<String, List<RequirementLink>> entry : linksByRequirement.entrySet()) {
            String reqKey = entry.getKey();
            List<RequirementLink> links = entry.getValue();

            int totalTests = links.size();
            int coveredTests = (int) links.stream()
                    .filter(l -> !executionRepository.findByTestId(l.getTestId()).isEmpty())
                    .count();

            if (coveredTests < totalTests) {
                suggestions.add(CoverageSuggestionResponse.CoverageSuggestion.builder()
                        .type("EXPAND_COVERAGE")
                        .priority(totalTests - coveredTests > 3 ? "HIGH" : "MEDIUM")
                        .requirementKey(reqKey)
                        .description(String.format("Add %d more tests for requirement %s", totalTests - coveredTests, reqKey))
                        .estimatedImpact(totalTests - coveredTests)
                        .build());
            }
        }

        // Prioritize tests by coverage gaps
        for (TestIssue test : tests) {
            boolean hasExecution = !executionRepository.findByTestId(test.getId()).isEmpty();
            if (!hasExecution) {
                int priorityScore = calculateTestPriority(test, linksByRequirement);
                prioritizedTests.add(CoverageSuggestionResponse.PrioritizedTest.builder()
                        .testId(test.getId())
                        .testKey(test.getKey())
                        .requirementKey(test.getKey())
                        .priorityScore(priorityScore)
                        .reason("Test not yet executed - contributes to coverage gaps")
                        .build());
            }
        }

        prioritizedTests.sort(Comparator.comparing(CoverageSuggestionResponse.PrioritizedTest::getPriorityScore).reversed());

        int highPriority = (int) suggestions.stream().filter(s -> "HIGH".equals(s.getPriority())).count();
        int mediumPriority = (int) suggestions.stream().filter(s -> "MEDIUM".equals(s.getPriority())).count();
        int lowPriority = suggestions.size() - highPriority - mediumPriority;

        return CoverageSuggestionResponse.builder()
                .projectId(projectId)
                .suggestions(suggestions)
                .prioritizedTests(prioritizedTests.stream().limit(50).toList())
                .actionSummary(CoverageSuggestionResponse.AutomatedActionSummary.builder()
                        .totalSuggestions(suggestions.size())
                        .highPriorityCount(highPriority)
                        .mediumPriorityCount(mediumPriority)
                        .lowPriorityCount(lowPriority)
                        .potentialCoverageGain(BigDecimal.valueOf(suggestions.stream().mapToInt(CoverageSuggestionResponse.CoverageSuggestion::getEstimatedImpact).sum())))
                        .build())
                .build();
    }

    private int calculateTestPriority(TestIssue test, Map<String, List<RequirementLink>> linksByRequirement) {
        int score = 50; // Base score

        // Higher priority for tests linked to critical requirements
        for (Map.Entry<String, List<RequirementLink>> entry : linksByRequirement.entrySet()) {
            boolean isLinked = entry.getValue().stream()
                    .anyMatch(l -> l.getTestId().equals(test.getId()));
            if (isLinked) {
                score += 10;
                // Check if requirement has low coverage
                CoverageThreshold threshold = coverageThresholdRepository.findByRequirementId(entry.getValue().get(0).getId())
                        .orElse(null);
                if (threshold != null && threshold.getCurrentCoverage() != null &&
                        threshold.getCurrentCoverage().compareTo(threshold.getMinimumCoverage()) < 0) {
                    score += 30;
                }
            }
        }

        return score;
    }

    // ========== HELPER CLASSES ==========

    @Data
    @Builder
    public static class CoverageRuleViolation {
        private UUID ruleId;
        private String ruleName;
        private CoverageRule.RuleType ruleType;
        private BigDecimal threshold;
        private BigDecimal currentValue;
        private CoverageRule.Scope scope;
        private UUID scopeId;
    }

    @Data
    @Builder
    public static class CoverageAlertResponse {
        private UUID requirementId;
        private UUID ruleId;
        private String requirementKey;
        private CoverageThreshold.AlertLevel alertLevel;
        private BigDecimal currentCoverage;
        private BigDecimal minimumCoverage;
        private BigDecimal warningThreshold;
        private String message;
    }

    // ========== ENHANCED TREND ANALYSIS (7/30/90 days) ==========

    /**
     * Get multi-period trend analysis for coverage metrics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMultiPeriodTrendAnalysis(UUID projectId) {
        Map<String, Object> analysis = new HashMap<>();

        CoverageTrendResponse trend7d = getCoverageTrends(projectId, 7);
        CoverageTrendResponse trend30d = getCoverageTrends(projectId, 30);
        CoverageTrendResponse trend90d = getCoverageTrends(projectId, 90);

        analysis.put("projectId", projectId);
        analysis.put("trend7Days", trend7d);
        analysis.put("trend30Days", trend30d);
        analysis.put("trend90Days", trend90d);
        analysis.put("summary", buildMultiPeriodSummary(trend7d, trend30d, trend90d));

        return analysis;
    }

    private Map<String, Object> buildMultiPeriodSummary(CoverageTrendResponse trend7d,
                                                         CoverageTrendResponse trend30d,
                                                         CoverageTrendResponse trend90d) {
        Map<String, Object> summary = new HashMap<>();

        if (trend7d.getSummary() != null) {
            summary.put("currentCoverage7d", trend7d.getSummary().getCurrentCoverage());
            summary.put("changeRate7d", trend7d.getSummary().getChangeRate7d());
            summary.put("trendDirection7d", trend7d.getSummary().getTrendDirection());
        }

        if (trend30d.getSummary() != null) {
            summary.put("currentCoverage30d", trend30d.getSummary().getCurrentCoverage());
            summary.put("changeRate30d", trend30d.getSummary().getChangeRate30d());
            summary.put("trendDirection30d", trend30d.getSummary().getTrendDirection());
            summary.put("thirtyDayAverage", trend30d.getSummary().getThirtyDayAverage());
        }

        if (trend90d.getSummary() != null) {
            summary.put("currentCoverage90d", trend90d.getSummary().getCurrentCoverage());
            summary.put("changeRate90d", trend90d.getSummary().getChangeRate90d());
            summary.put("trendDirection90d", trend90d.getSummary().getTrendDirection());
            summary.put("ninetyDayAverage", trend90d.getSummary().getNinetyDayAverage());
        }

        summary.put("overallTrend", determineOverallTrend(summary));
        return summary;
    }

    private String determineOverallTrend(Map<String, Object> summary) {
        String dir7d = (String) summary.getOrDefault("trendDirection7d", "STABLE");
        String dir30d = (String) summary.getOrDefault("trendDirection30d", "STABLE");
        String dir90d = (String) summary.getOrDefault("trendDirection90d", "STABLE");

        long improving = Stream.of(dir7d, dir30d, dir90d).filter("IMPROVING"::equals).count();
        long declining = Stream.of(dir7d, dir30d, dir90d).filter("DECLINING"::equals).count();

        if (improving >= 2) return "IMPROVING";
        if (declining >= 2) return "DECLINING";
        return "STABLE";
    }

    // ========== COVERAGE AUTOMATED SUGGESTIONS ==========

    /**
     * Get comprehensive automated suggestions for improving coverage.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComprehensiveSuggestions(UUID projectId) {
        Map<String, Object> suggestions = new HashMap<>();

        CoverageSuggestionResponse basicSuggestions = getSuggestions(projectId);
        suggestions.put("basicSuggestions", basicSuggestions);

        // Add advanced suggestions based on rule violations
        List<CoverageRuleViolation> violations = evaluateRules(projectId);
        List<Map<String, Object>> ruleBasedSuggestions = new ArrayList<>();

        for (CoverageRuleViolation violation : violations) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("ruleId", violation.getRuleId());
            suggestion.put("ruleName", violation.getRuleName());
            suggestion.put("currentValue", violation.getCurrentValue());
            suggestion.put("threshold", violation.getThreshold());
            suggestion.put("priority", determineSuggestionPriority(violation));
            suggestion.put("action", generateActionForViolation(violation));
            ruleBasedSuggestions.add(suggestion);
        }
        suggestions.put("ruleBasedSuggestions", ruleBasedSuggestions);

        // Calculate potential improvement
        BigDecimal potentialImprovement = calculatePotentialImprovement(projectId);
        suggestions.put("potentialCoverageImprovement", potentialImprovement);

        return suggestions;
    }

    private String determineSuggestionPriority(CoverageRuleViolation violation) {
        BigDecimal gap = violation.getThreshold().subtract(violation.getCurrentValue());
        BigDecimal threshold50 = violation.getThreshold().multiply(BigDecimal.valueOf(0.5));

        if (gap.compareTo(threshold50) > 0) return "CRITICAL";
        if (gap.compareTo(BigDecimal.valueOf(20)) > 0) return "HIGH";
        if (gap.compareTo(BigDecimal.valueOf(10)) > 0) return "MEDIUM";
        return "LOW";
    }

    private String generateActionForViolation(CoverageRuleViolation violation) {
        return switch (violation.getRuleType()) {
            case MINIMUM_COVERAGE -> String.format("Increase coverage by %.2f%% to meet minimum threshold of %.2f%%",
                    violation.getThreshold().subtract(violation.getCurrentValue()), violation.getThreshold());
            case TEST_DIVERSITY -> "Add more diverse test types to improve test coverage diversity";
            case EXECUTION_FREQUENCY -> "Schedule more frequent test executions to meet coverage requirements";
        };
    }

    private BigDecimal calculatePotentialImprovement(UUID projectId) {
        List<TestIssue> uncoveredTests = testIssueRepository.findByProjectId(projectId).stream()
                .filter(t -> executionRepository.findByTestId(t.getId()).isEmpty())
                .toList();

        int potentialGain = uncoveredTests.size();
        return BigDecimal.valueOf(potentialGain * 2.5).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== EXPORT DATA METHODS ==========

    /**
     * Export coverage data as a structured map for reporting.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportCoverageData(UUID projectId, int periodDays) {
        Map<String, Object> exportData = new HashMap<>();

        ProjectCoverageResponse coverage = getProjectCoverage(projectId);
        CoverageTrendResponse trends = getCoverageTrends(projectId, periodDays);
        CoverageMatrixResponse matrix = getCoverageMatrix(projectId);
        List<CoverageAlertResponse> alerts = getAlerts(projectId);
        List<CoverageRuleViolation> violations = evaluateRules(projectId);

        exportData.put("projectId", projectId);
        exportData.put("exportedAt", LocalDateTime.now());
        exportData.put("periodDays", periodDays);
        exportData.put("projectCoverage", coverage);
        exportData.put("trends", trends);
        exportData.put("matrix", matrix);
        exportData.put("alerts", alerts);
        exportData.put("ruleViolations", violations);

        return exportData;
    }

    /**
     * Generate coverage snapshot for point-in-time reporting.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateCoverageSnapshot(UUID projectId) {
        Map<String, Object> snapshot = new HashMap<>();

        List<TestIssue> tests = testIssueRepository.findByProjectId(projectId);
        int totalTests = tests.size();
        int executedTests = (int) tests.stream()
                .filter(t -> !executionRepository.findByTestId(t.getId()).isEmpty())
                .count();

        Map<TestIssue.TestType, Integer> byType = tests.stream()
                .filter(t -> t.getTestType() != null)
                .collect(Collectors.groupingBy(TestIssue.TestType, Collectors.collectingAndThen(
                        Collectors.counting(), Long::intValue)));

        Map<String, Object> coverageSnapshot = new HashMap<>();
        coverageSnapshot.put("timestamp", LocalDateTime.now());
        coverageSnapshot.put("totalTests", totalTests);
        coverageSnapshot.put("executedTests", executedTests);
        coverageSnapshot.put("unexecutedTests", totalTests - executedTests);
        coverageSnapshot.put("executionRate", totalTests > 0 ?
                BigDecimal.valueOf(executedTests * 100.0 / totalTests).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
        coverageSnapshot.put("testsByType", byType);

        List<CoverageThreshold> thresholds = coverageThresholdRepository.findByProjectIdAndAlertEnabledTrue(projectId);
        coverageSnapshot.put("activeThresholds", thresholds.size());
        coverageSnapshot.put("thresholdsAtRisk", thresholds.stream()
                .filter(t -> t.getAlertLevel() != CoverageThreshold.AlertLevel.OK)
                .count());

        snapshot.put("projectId", projectId);
        snapshot.put("snapshot", coverageSnapshot);

        return snapshot;
    }

    private static <T> Stream<T> Stream(T... items) {
        return java.util.Arrays.stream(items);
    }
}