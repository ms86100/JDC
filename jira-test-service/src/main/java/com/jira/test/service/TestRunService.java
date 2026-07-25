package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.TestRun;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.exception.InvalidOperationException;
import com.jira.test.repository.TestRunRepository;
import com.jira.test.repository.TestIssueRepository;
import com.jira.test.repository.TestStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRunService {

    private final TestRunRepository testRunRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestStepRepository testStepRepository;

    @Value("${app.defaults.test-run-status.initial:PENDING}")
    private String defaultTestRunInitialStatus;

    @Value("${app.defaults.test-run-status.in-progress:IN_PROGRESS}")
    private String defaultTestRunInProgressStatus;

    @Value("${app.defaults.annotation-type:NOTE}")
    private String defaultAnnotationType;

    @Value("${app.defaults.annotation-author:Unknown}")
    private String defaultAnnotationAuthor;

    @Value("${app.defaults.flaky-score-threshold:0.3}")
    private double flakyScoreThreshold;

    @Value("${app.defaults.html-report-header-color:#4CAF50}")
    private String htmlReportHeaderColor;

    @Transactional
    public TestRunResponse createTestRun(CreateTestRunRequest request) {
        log.info("Creating test run for test: {}", request.getTestId());

        // Validate test exists
        if (!testIssueRepository.existsById(request.getTestId())) {
            throw new ResourceNotFoundException("TestIssue", "id", request.getTestId());
        }

        TestRun testRun = TestRun.builder()
                .testId(request.getTestId())
                .executionId(request.getExecutionId())
                .projectId(request.getProjectId())
                .executedBy(request.getExecutedBy())
                .status(defaultTestRunInitialStatus)
                .environment(request.getEnvironment())
                .browser(request.getBrowser())
                .platform(request.getPlatform())
                .testData(request.getTestData())
                .evidenceLinks(request.getEvidenceLinks() != null ? request.getEvidenceLinks() : new ArrayList<>())
                .passedSteps(0)
                .failedSteps(0)
                .blockedSteps(0)
                .isRetry(false)
                .build();

        testRun = testRunRepository.save(testRun);
        log.info("Test run created with id: {}", testRun.getId());

        return mapToResponse(testRun);
    }

    @Transactional
    public TestRunResponse startTestRun(UUID runId) {
        log.info("Starting test run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        if (!defaultTestRunInitialStatus.equals(testRun.getStatus())) {
            throw new InvalidOperationException("Test run is not in " + defaultTestRunInitialStatus + " status");
        }

        testRun.setStatus(defaultTestRunInProgressStatus);
        testRun.setStartedAt(LocalDateTime.now());
        testRun.setExecutedAt(LocalDateTime.now());
        testRun.setExecutedBy(testRun.getExecutedBy());

        testRun = testRunRepository.save(testRun);
        log.info("Test run started: {}", runId);

        return mapToResponse(testRun);
    }

    @Transactional
    public TestRunResponse completeTestRun(UUID runId, CompleteTestRunRequest request) {
        log.info("Completing test run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        if (!defaultTestRunInProgressStatus.equals(testRun.getStatus())) {
            throw new InvalidOperationException("Test run is not in " + defaultTestRunInProgressStatus + " status");
        }

        testRun.setStatus(request.getStatus());
        testRun.setComment(request.getComment());
        testRun.setDefectKeys(request.getDefectKeys());
        testRun.setCompletedAt(LocalDateTime.now());

        // Calculate duration
        if (testRun.getStartedAt() != null) {
            long durationSeconds = java.time.Duration.between(testRun.getStartedAt(), testRun.getCompletedAt()).getSeconds();
            testRun.setDuration((int) durationSeconds);
        }

        // Process step statuses
        if (request.getStepStatuses() != null) {
            testRun.setStepStatuses(request.getStepStatuses());
            testRun.setTotalSteps(request.getStepStatuses().size());

            long passed = request.getStepStatuses().stream().filter("PASSED"::equals).count();
            long failed = request.getStepStatuses().stream().filter("FAILED"::equals).count();
            long blocked = request.getStepStatuses().stream().filter("BLOCKED"::equals).count();

            testRun.setPassedSteps((int) passed);
            testRun.setFailedSteps((int) failed);
            testRun.setBlockedSteps((int) blocked);
        }

        if (request.getEvidenceLinks() != null && !request.getEvidenceLinks().isEmpty()) {
            List<String> existing = testRun.getEvidenceLinks() != null
                    ? new ArrayList<>(testRun.getEvidenceLinks())
                    : new ArrayList<>();
            existing.addAll(request.getEvidenceLinks());
            testRun.setEvidenceLinks(existing);
        }

        testRun.setLogs(request.getLogs());
        testRun.setErrorMessage(request.getErrorMessage());

        testRun = testRunRepository.save(testRun);

        // Update parent execution counts if linked
        if (testRun.getExecutionId() != null) {
            updateExecutionCounts(testRun);
        }

        log.info("Test run completed: {} with status: {}", runId, testRun.getStatus());
        return mapToResponse(testRun);
    }

    @Transactional(readOnly = true)
    public TestRunResponse getTestRun(UUID runId) {
        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));
        return mapToResponse(testRun);
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getTestRunsByTestId(UUID testId) {
        return testRunRepository.findByTestIdOrderByExecutedAtDesc(testId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestRunResponse getLatestRun(UUID testId) {
        return testRunRepository.findLatestByTestId(testId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "testId", testId));
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getTestHistory(UUID testId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return testRunRepository.findByTestIdSince(testId, since).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TestRunStatsResponse getTestStats(UUID testId) {
        long total = testRunRepository.countTotalByTestId(testId);
        long passed = testRunRepository.countPassedByTestId(testId);
        long failed = testRunRepository.countFailedByTestId(testId);
        long blocked = testRunRepository.findByTestIdAndStatus(testId, "BLOCKED").size();
        long pending = testRunRepository.findByTestIdAndStatus(testId, "PENDING").size() +
                       testRunRepository.findByTestIdAndStatus(testId, "IN_PROGRESS").size();

        Double passRate = total > 0 ? (double) passed / total * 100 : 0.0;
        Double avgDuration = testRunRepository.getAverageDurationByTestId(testId);

        // Flaky detection: check last 10 runs
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<TestRun> recentRuns = testRunRepository.findRecentByTestId(testId, since);
        Double flakyScore = calculateFlakyScore(recentRuns);

        TestRun latest = testRunRepository.findLatestByTestId(testId).orElse(null);

        return TestRunStatsResponse.builder()
                .testId(testId)
                .totalRuns(total)
                .passedRuns(passed)
                .failedRuns(failed)
                .blockedRuns(blocked)
                .pendingRuns(pending)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .averageDuration(avgDuration)
                .flakyScore(flakyScore)
                .lastRunAt(latest != null ? latest.getExecutedAt() : null)
                .lastRunStatus(latest != null ? latest.getStatus() : null)
                .isFlaky(flakyScore > flakyScoreThreshold)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByExecution(UUID executionId) {
        return testRunRepository.findByExecutionId(executionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestRunResponse retryTestRun(UUID parentRunId) {
        log.info("Retrying test run from parent: {}", parentRunId);

        TestRun parentRun = testRunRepository.findById(parentRunId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", parentRunId));

        TestRun newRun = TestRun.builder()
                .testId(parentRun.getTestId())
                .executionId(parentRun.getExecutionId())
                .projectId(parentRun.getProjectId())
                .executedBy(parentRun.getExecutedBy())
                .status("PENDING")
                .environment(parentRun.getEnvironment())
                .browser(parentRun.getBrowser())
                .platform(parentRun.getPlatform())
                .testData(parentRun.getTestData())
                .evidenceLinks(new ArrayList<>())
                .isRetry(true)
                .parentRunId(parentRunId)
                .status(defaultTestRunInitialStatus)
                .passedSteps(0)
                .failedSteps(0)
                .blockedSteps(0)
                .build();

        newRun = testRunRepository.save(newRun);
        log.info("Retry run created: {} (parent: {})", newRun.getId(), parentRunId);

        return mapToResponse(newRun);
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByEnvironment(UUID projectId, String environment) {
        return testRunRepository.findByEnvironment(environment, projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByProject(UUID projectId) {
        return testRunRepository.findByProjectIdOrderByExecutedAtDesc(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByUser(UUID userId) {
        return testRunRepository.findByExecutedBy(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByProjectAndDateRange(UUID projectId, LocalDateTime start, LocalDateTime end) {
        return testRunRepository.findByProjectIdAndExecutedAtBetween(projectId, start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== TEST RUN COMPARISON ==========

    @Transactional(readOnly = true)
    public TestRunComparisonResponse compareRuns(UUID run1Id, UUID run2Id) {
        log.info("Comparing test runs: {} vs {}", run1Id, run2Id);

        TestRun run1 = testRunRepository.findById(run1Id)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", run1Id));
        TestRun run2 = testRunRepository.findById(run2Id)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", run2Id));

        TestRunComparisonResponse.ComparisonSummary summary = buildComparisonSummary(run1, run2);
        List<TestRunComparisonResponse.StepComparison> stepComparisons = buildStepComparisons(run1, run2);
        TestRunComparisonResponse.DurationComparison durationComparison = buildDurationComparison(run1, run2);
        TestRunComparisonResponse.StatusComparison statusComparison = buildStatusComparison(run1, run2);

        return TestRunComparisonResponse.builder()
                .run1(mapToResponse(run1))
                .run2(mapToResponse(run2))
                .summary(summary)
                .stepComparisons(stepComparisons)
                .durationComparison(durationComparison)
                .statusComparison(statusComparison)
                .build();
    }

    private TestRunComparisonResponse.ComparisonSummary buildComparisonSummary(TestRun run1, TestRun run2) {
        int passedDiff = run2.getPassedSteps() - run1.getPassedSteps();
        int failedDiff = run2.getFailedSteps() - run1.getFailedSteps();

        Double passRate1 = run1.getTotalSteps() > 0 ? (double) run1.getPassedSteps() / run1.getTotalSteps() * 100 : 0.0;
        Double passRate2 = run2.getTotalSteps() > 0 ? (double) run2.getPassedSteps() / run2.getTotalSteps() * 100 : 0.0;
        Double passRateChange = passRate2 - passRate1;

        int durationChange = (run2.getDuration() != null ? run2.getDuration() : 0) -
                           (run1.getDuration() != null ? run1.getDuration() : 0);

        String verdict = determineComparisonVerdict(run1, run2, passedDiff, failedDiff);

        return TestRunComparisonResponse.ComparisonSummary.builder()
                .totalSteps1(run1.getTotalSteps())
                .totalSteps2(run2.getTotalSteps())
                .passedSteps1(run1.getPassedSteps())
                .passedSteps2(run2.getPassedSteps())
                .failedSteps1(run1.getFailedSteps())
                .failedSteps2(run2.getFailedSteps())
                .passedDiff(passedDiff)
                .failedDiff(failedDiff)
                .passRateChange(Math.round(passRateChange * 100.0) / 100.0)
                .durationChange(durationChange)
                .verdict(verdict)
                .build();
    }

    private String determineComparisonVerdict(TestRun run1, TestRun run2, int passedDiff, int failedDiff) {
        if ("PASSED".equals(run2.getStatus()) && !"PASSED".equals(run1.getStatus())) {
            return "FIXED";
        }
        if ("FAILED".equals(run2.getStatus()) && "PASSED".equals(run1.getStatus())) {
            return "REGRESSED";
        }
        if (failedDiff < 0) {
            return "IMPROVED";
        }
        if (failedDiff > 0) {
            return "NEW_FAILURES";
        }
        return "STABLE";
    }

    private List<TestRunComparisonResponse.StepComparison> buildStepComparisons(TestRun run1, TestRun run2) {
        List<TestRunComparisonResponse.StepComparison> comparisons = new ArrayList<>();
        List<String> steps1 = run1.getStepStatuses() != null ? run1.getStepStatuses() : List.of();
        List<String> steps2 = run2.getStepStatuses() != null ? run2.getStepStatuses() : List.of();

        int maxSteps = Math.max(steps1.size(), steps2.size());
        for (int i = 0; i < maxSteps; i++) {
            String status1 = i < steps1.size() ? steps1.get(i) : null;
            String status2 = i < steps2.size() ? steps2.get(i) : null;

            boolean changed = !Objects.equals(status1, status2);
            String changeType = null;

            if (changed) {
                if ("PASSED".equals(status2) && !"PASSED".equals(status1)) {
                    changeType = "FIXED";
                } else if ("FAILED".equals(status2) && !"FAILED".equals(status1)) {
                    changeType = "BROKE";
                } else {
                    changeType = "SAME";
                }
            }

            comparisons.add(TestRunComparisonResponse.StepComparison.builder()
                    .stepIndex(i + 1)
                    .status1(status1)
                    .status2(status2)
                    .changed(changed)
                    .changeType(changeType)
                    .build());
        }

        return comparisons;
    }

    private TestRunComparisonResponse.DurationComparison buildDurationComparison(TestRun run1, TestRun run2) {
        Integer d1 = run1.getDuration();
        Integer d2 = run2.getDuration();

        if (d1 == null || d2 == null) {
            return TestRunComparisonResponse.DurationComparison.builder()
                    .duration1(d1)
                    .duration2(d2)
                    .verdict("SAME")
                    .build();
        }

        int changeSeconds = d2 - d1;
        double changePercent = d1 > 0 ? (double) changeSeconds / d1 * 100 : 0.0;
        String verdict = changeSeconds < -10 ? "FASTER" : (changeSeconds > 10 ? "SLOWER" : "SAME");

        return TestRunComparisonResponse.DurationComparison.builder()
                .duration1(d1)
                .duration2(d2)
                .changeSeconds(changeSeconds)
                .changePercent(Math.round(changePercent * 100.0) / 100.0)
                .verdict(verdict)
                .build();
    }

    private TestRunComparisonResponse.StatusComparison buildStatusComparison(TestRun run1, TestRun run2) {
        boolean improved = "PASSED".equals(run2.getStatus()) || isBetterStatus(run2.getStatus(), run1.getStatus());
        boolean regressed = "FAILED".equals(run2.getStatus()) || isBetterStatus(run1.getStatus(), run2.getStatus());

        String verdict;
        if (improved && !regressed) {
            verdict = "IMPROVED";
        } else if (regressed && !improved) {
            verdict = "REGRESSED";
        } else {
            verdict = "SAME";
        }

        return TestRunComparisonResponse.StatusComparison.builder()
                .status1(run1.getStatus())
                .status2(run2.getStatus())
                .improved(improved)
                .regressed(regressed)
                .verdict(verdict)
                .build();
    }

    private boolean isBetterStatus(String newStatus, String oldStatus) {
        Map<String, Integer> statusOrder = Map.of(
                "PASSED", 5,
                "BLOCKED", 4,
                "SKIPPED", 3,
                "IN_PROGRESS", 2,
                "PENDING", 1,
                "FAILED", 0
        );
        return statusOrder.getOrDefault(newStatus, 0) > statusOrder.getOrDefault(oldStatus, 0);
    }

    // ========== TEST RUN TRENDS ==========

    @Transactional(readOnly = true)
    public TestRunTrendResponse getTestTrends(UUID testId, int days) {
        log.info("Getting trends for test: {} over {} days", testId, days);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<TestRun> runs = testRunRepository.findByTestIdSince(testId, since);

        List<TestRunTrendResponse.TrendDataPoint> dataPoints = runs.stream()
                .map(this::mapToTrendDataPoint)
                .collect(Collectors.toList());

        TestRunTrendResponse.TrendSummary summary = calculateTrendSummary(dataPoints);

        return TestRunTrendResponse.builder()
                .testId(testId)
                .days(days)
                .dataPoints(dataPoints)
                .summary(summary)
                .build();
    }

    private TestRunTrendResponse.TrendDataPoint mapToTrendDataPoint(TestRun run) {
        Double passRate = run.getTotalSteps() > 0
                ? (double) run.getPassedSteps() / run.getTotalSteps() * 100
                : 0.0;

        return TestRunTrendResponse.TrendDataPoint.builder()
                .timestamp(run.getExecutedAt())
                .status(run.getStatus())
                .duration(run.getDuration())
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .passedSteps(run.getPassedSteps())
                .failedSteps(run.getFailedSteps())
                .totalSteps(run.getTotalSteps())
                .environment(run.getEnvironment())
                .runId(run.getId())
                .build();
    }

    private TestRunTrendResponse.TrendSummary calculateTrendSummary(List<TestRunTrendResponse.TrendDataPoint> dataPoints) {
        if (dataPoints.isEmpty()) {
            return TestRunTrendResponse.TrendSummary.builder()
                    .totalRuns(0)
                    .trendDirection("STABLE")
                    .build();
        }

        int passed = (int) dataPoints.stream().filter(dp -> "PASSED".equals(dp.getStatus())).count();
        int failed = (int) dataPoints.stream().filter(dp -> "FAILED".equals(dp.getStatus())).count();
        int flaky = (int) dataPoints.stream().filter(dp -> "FLAKY".equals(dp.getStatus()) || "BLOCKED".equals(dp.getStatus())).count();

        Double avgPassRate = dataPoints.stream()
                .mapToDouble(TestRunTrendResponse.TrendDataPoint::getPassRate)
                .average().orElse(0.0);

        Double avgDuration = dataPoints.stream()
                .filter(dp -> dp.getDuration() != null)
                .mapToInt(TestRunTrendResponse.TrendDataPoint::getDuration)
                .average().orElse(0.0);

        Double flakinessScore = dataPoints.size() >= 3 ? calculateTrendFlakiness(dataPoints) : 0.0;
        String trendDirection = calculateTrendDirection(dataPoints);
        Double trendSlope = calculateTrendSlope(dataPoints);

        TestRunTrendResponse.DurationTrend durationTrend = calculateDurationTrend(dataPoints);

        return TestRunTrendResponse.TrendSummary.builder()
                .averagePassRate(Math.round(avgPassRate * 100.0) / 100.0)
                .averageDuration(Math.round(avgDuration * 100.0) / 100.0)
                .totalRuns(dataPoints.size())
                .passedRuns(passed)
                .failedRuns(failed)
                .flakyRuns(flaky)
                .flakinessScore(Math.round(flakinessScore * 100.0) / 100.0)
                .trendDirection(trendDirection)
                .trendSlope(Math.round(trendSlope * 100.0) / 100.0)
                .durationTrend(durationTrend)
                .build();
    }

    private Double calculateTrendFlakiness(List<TestRunTrendResponse.TrendDataPoint> dataPoints) {
        int switches = 0;
        String lastStatus = null;
        for (TestRunTrendResponse.TrendDataPoint dp : dataPoints) {
            if (lastStatus != null && !lastStatus.equals(dp.getStatus())) {
                if (("PASSED".equals(lastStatus) || "FAILED".equals(lastStatus)) &&
                    ("PASSED".equals(dp.getStatus()) || "FAILED".equals(dp.getStatus()))) {
                    switches++;
                }
            }
            lastStatus = dp.getStatus();
        }
        return Math.min(1.0, (double) switches / Math.max(1, dataPoints.size() - 1));
    }

    private String calculateTrendDirection(List<TestRunTrendResponse.TrendDataPoint> dataPoints) {
        if (dataPoints.size() < 3) return "STABLE";

        double slope = calculateTrendSlope(dataPoints);
        if (slope > 5) return "IMPROVING";
        if (slope < -5) return "DECLINING";

        int switches = 0;
        for (int i = 1; i < dataPoints.size(); i++) {
            if (!dataPoints.get(i).getStatus().equals(dataPoints.get(i - 1).getStatus())) {
                switches++;
            }
        }
        return switches > dataPoints.size() / 3 ? "FLUCTUATING" : "STABLE";
    }

    private Double calculateTrendSlope(List<TestRunTrendResponse.TrendDataPoint> dataPoints) {
        if (dataPoints.size() < 2) return 0.0;

        int n = dataPoints.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = dataPoints.get(i).getPassRate();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0.0;

        return (n * sumXY - sumX * sumY) / denominator;
    }

    private TestRunTrendResponse.DurationTrend calculateDurationTrend(List<TestRunTrendResponse.TrendDataPoint> dataPoints) {
        List<Integer> durations = dataPoints.stream()
                .filter(dp -> dp.getDuration() != null)
                .map(TestRunTrendResponse.TrendDataPoint::getDuration)
                .collect(Collectors.toList());

        if (durations.isEmpty()) {
            return TestRunTrendResponse.DurationTrend.builder().direction("STABLE").build();
        }

        double avg = durations.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double min = durations.stream().mapToInt(Integer::intValue).min().orElse(0);
        double max = durations.stream().mapToInt(Integer::intValue).max().orElse(0);

        return TestRunTrendResponse.DurationTrend.builder()
                .averageDuration(Math.round(avg * 100.0) / 100.0)
                .minDuration((double) min)
                .maxDuration((double) max)
                .direction("STABLE")
                .changePercent(0.0)
                .build();
    }

    // ========== ANNOTATIONS ==========

    @Transactional
    public AnnotationResponse addAnnotation(UUID runId, AnnotationRequest request) {
        log.info("Adding annotation to run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        String existing = testRun.getAnnotations();
        String newAnnotation = String.format("[%s] %s (%s): %s",
                request.getType() != null ? request.getType() : defaultAnnotationType,
                request.getAuthorName() != null ? request.getAuthorName() : defaultAnnotationAuthor,
                LocalDateTime.now().toLocalDate(),
                request.getContent());

        String updatedAnnotations = existing != null && !existing.isEmpty()
                ? existing + "\n---\n" + newAnnotation
                : newAnnotation;

        testRun.setAnnotations(updatedAnnotations);
        testRunRepository.save(testRun);

        log.info("Annotation added to run: {}", runId);
        return AnnotationResponse.builder()
                .id(UUID.randomUUID())
                .runId(runId)
                .content(request.getContent())
                .type(request.getType())
                .authorId(request.getAuthorId())
                .authorName(request.getAuthorName())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> getAnnotations(UUID runId) {
        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        return parseAnnotations(testRun.getAnnotations(), runId);
    }

    private List<AnnotationResponse> parseAnnotations(String annotations, UUID runId) {
        List<AnnotationResponse> responses = new ArrayList<>();

        if (annotations == null || annotations.isEmpty()) {
            return responses;
        }

        String[] parts = annotations.split("\n---\n");
        for (String part : parts) {
            try {
                String[] content = part.trim().split(": ", 2);
                if (content.length >= 2) {
                    responses.add(AnnotationResponse.builder()
                            .id(UUID.randomUUID())
                            .runId(runId)
                            .content(content[1])
                            .type(content[0])
                            .createdAt(LocalDateTime.now())
                            .build());
                }
            } catch (Exception e) {
                log.warn("Failed to parse annotation: {}", part);
            }
        }

        return responses;
    }

    // ========== TAGS ==========

    @Transactional
    public TestRunResponse addTags(UUID runId, TagRequest request) {
        log.info("Adding tags to run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        List<String> currentTags = testRun.getTags() != null
                ? new ArrayList<>(testRun.getTags())
                : new ArrayList<>();

        for (String tag : request.getTags()) {
            if (!currentTags.contains(tag)) {
                currentTags.add(tag);
            }
        }

        testRun.setTags(currentTags);
        testRun = testRunRepository.save(testRun);

        log.info("Tags added to run: {}, total tags: {}", runId, currentTags.size());
        return mapToResponse(testRun);
    }

    @Transactional
    public TestRunResponse removeTag(UUID runId, String tag) {
        log.info("Removing tag '{}' from run: {}", tag, runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        List<String> currentTags = testRun.getTags();
        if (currentTags != null) {
            currentTags.remove(tag);
            testRun.setTags(currentTags);
            testRun = testRunRepository.save(testRun);
        }

        return mapToResponse(testRun);
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getRunsByTag(UUID projectId, String tag) {
        return testRunRepository.findByProjectIdAndTag(projectId, tag).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== BASELINE ==========

    @Transactional
    public BaselineResponse setBaseline(UUID runId, UUID userId, String userName) {
        log.info("Setting run {} as baseline", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        // Unset any existing baseline for this test
        testRunRepository.findBaselineByTestId(testRun.getTestId())
                .ifPresent(existing -> {
                    existing.setIsBaseline(false);
                    testRunRepository.save(existing);
                });

        // Set this run as baseline
        testRun.setIsBaseline(true);
        testRun.setBaselineId(runId);
        testRunRepository.save(testRun);

        return BaselineResponse.builder()
                .runId(runId)
                .testId(testRun.getTestId())
                .projectId(testRun.getProjectId())
                .status(testRun.getStatus())
                .setAt(LocalDateTime.now())
                .setBy(userId)
                .setByName(userName)
                .passedSteps(testRun.getPassedSteps())
                .failedSteps(testRun.getFailedSteps())
                .totalSteps(testRun.getTotalSteps())
                .duration(testRun.getDuration())
                .environment(testRun.getEnvironment())
                .build();
    }

    @Transactional(readOnly = true)
    public TestRunComparisonResponse compareToBaseline(UUID runId) {
        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        TestRun baseline = testRunRepository.findBaselineByTestId(testRun.getTestId())
                .orElseThrow(() -> new InvalidOperationException("No baseline set for this test"));

        return compareRuns(runId, baseline.getId());
    }

    @Transactional(readOnly = true)
    public BaselineResponse getBaseline(UUID testId) {
        TestRun baseline = testRunRepository.findBaselineByTestId(testId)
                .orElseThrow(() -> new InvalidOperationException("No baseline set for this test"));

        return BaselineResponse.builder()
                .runId(baseline.getId())
                .testId(baseline.getTestId())
                .projectId(baseline.getProjectId())
                .status(baseline.getStatus())
                .passedSteps(baseline.getPassedSteps())
                .failedSteps(baseline.getFailedSteps())
                .totalSteps(baseline.getTotalSteps())
                .duration(baseline.getDuration())
                .environment(baseline.getEnvironment())
                .build();
    }

    // ========== ARCHIVE ==========

    @Transactional
    public TestRunResponse archiveRun(UUID runId) {
        log.info("Archiving test run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        testRun.setIsArchived(true);
        testRun.setArchivedAt(LocalDateTime.now());
        testRun = testRunRepository.save(testRun);

        log.info("Test run archived: {}", runId);
        return mapToResponse(testRun);
    }

    @Transactional
    public TestRunResponse unarchiveRun(UUID runId) {
        log.info("Unarchiving test run: {}", runId);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        testRun.setIsArchived(false);
        testRun.setArchivedAt(null);
        testRun = testRunRepository.save(testRun);

        return mapToResponse(testRun);
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getArchivedRuns(UUID projectId) {
        return testRunRepository.findArchivedByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> getActiveRuns(UUID projectId) {
        return testRunRepository.findActiveByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== EXPORT ==========

    @Transactional(readOnly = true)
    public String exportRun(UUID runId, String format) {
        log.info("Exporting run {} in format: {}", runId, format);

        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        return switch (format.toUpperCase()) {
            case "JSON" -> exportToJson(testRun);
            case "CSV" -> exportToCsv(testRun);
            case "XML" -> exportToXml(testRun);
            case "HTML" -> exportToHtml(testRun);
            default -> throw new InvalidOperationException("Unsupported export format: " + format);
        };
    }

    private String exportToJson(TestRun testRun) {
        TestRunResponse response = mapToResponse(testRun);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export to JSON", e);
        }
    }

    private String exportToCsv(TestRun testRun) {
        StringBuilder csv = new StringBuilder();
        csv.append("Field,Value\n");
        csv.append("ID,").append(testRun.getId()).append("\n");
        csv.append("TestID,").append(testRun.getTestId()).append("\n");
        csv.append("Status,").append(testRun.getStatus()).append("\n");
        csv.append("ExecutedAt,").append(testRun.getExecutedAt()).append("\n");
        csv.append("Duration,").append(testRun.getDuration()).append("\n");
        csv.append("PassedSteps,").append(testRun.getPassedSteps()).append("\n");
        csv.append("FailedSteps,").append(testRun.getFailedSteps()).append("\n");
        csv.append("BlockedSteps,").append(testRun.getBlockedSteps()).append("\n");
        csv.append("TotalSteps,").append(testRun.getTotalSteps()).append("\n");
        csv.append("Environment,").append(testRun.getEnvironment()).append("\n");
        csv.append("Browser,").append(testRun.getBrowser()).append("\n");
        csv.append("Platform,").append(testRun.getPlatform()).append("\n");
        if (testRun.getStepStatuses() != null) {
            csv.append("StepStatuses,").append(String.join(";", testRun.getStepStatuses())).append("\n");
        }
        return csv.toString();
    }

    private String exportToXml(TestRun testRun) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<TestRun>\n");
        xml.append("  <ID>").append(testRun.getId()).append("</ID>\n");
        xml.append("  <TestID>").append(testRun.getTestId()).append("</TestID>\n");
        xml.append("  <Status>").append(testRun.getStatus()).append("</Status>\n");
        xml.append("  <ExecutedAt>").append(testRun.getExecutedAt()).append("</ExecutedAt>\n");
        xml.append("  <Duration>").append(testRun.getDuration()).append("</Duration>\n");
        xml.append("  <PassedSteps>").append(testRun.getPassedSteps()).append("</PassedSteps>\n");
        xml.append("  <FailedSteps>").append(testRun.getFailedSteps()).append("</FailedSteps>\n");
        xml.append("  <BlockedSteps>").append(testRun.getBlockedSteps()).append("</BlockedSteps>\n");
        xml.append("  <TotalSteps>").append(testRun.getTotalSteps()).append("</TotalSteps>\n");
        xml.append("  <Environment>").append(testRun.getEnvironment()).append("</Environment>\n");
        xml.append("  <Browser>").append(testRun.getBrowser()).append("</Browser>\n");
        xml.append("  <Platform>").append(testRun.getPlatform()).append("</Platform>\n");
        if (testRun.getStepStatuses() != null) {
            xml.append("  <StepStatuses>\n");
            for (String status : testRun.getStepStatuses()) {
                xml.append("    <Status>").append(status).append("</Status>\n");
            }
            xml.append("  </StepStatuses>\n");
        }
        xml.append("</TestRun>");
        return xml.toString();
    }

    private String exportToHtml(TestRun testRun) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<title>Test Run Report</title>\n");
        html.append("<style>\nbody { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: ").append(htmlReportHeaderColor).append("; color: white; }\n");
        html.append(".passed { color: green; }\n");
        html.append(".failed { color: red; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<h1>Test Run Report</h1>\n");
        html.append("<table>\n");
        html.append("<tr><th>Field</th><th>Value</th></tr>\n");
        html.append("<tr><td>ID</td><td>").append(testRun.getId()).append("</td></tr>\n");
        html.append("<tr><td>Test ID</td><td>").append(testRun.getTestId()).append("</td></tr>\n");
        html.append("<tr><td>Status</td><td class=\"")
                .append("PASSED".equals(testRun.getStatus()) ? "passed" : "failed")
                .append("\">").append(testRun.getStatus()).append("</td></tr>\n");
        html.append("<tr><td>Executed At</td><td>").append(testRun.getExecutedAt()).append("</td></tr>\n");
        html.append("<tr><td>Duration</td><td>").append(testRun.getDuration()).append(" seconds</td></tr>\n");
        html.append("<tr><td>Passed Steps</td><td class=\"passed\">").append(testRun.getPassedSteps()).append("</td></tr>\n");
        html.append("<tr><td>Failed Steps</td><td class=\"failed\">").append(testRun.getFailedSteps()).append("</td></tr>\n");
        html.append("<tr><td>Total Steps</td><td>").append(testRun.getTotalSteps()).append("</td></tr>\n");
        html.append("<tr><td>Environment</td><td>").append(testRun.getEnvironment()).append("</td></tr>\n");
        html.append("</table>\n</body>\n</html>");
        return html.toString();
    }

    // ========== FLAKINESS DETECTION ==========

    @Transactional(readOnly = true)
    public Double detectFlakiness(UUID runId) {
        TestRun testRun = testRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRun", "id", runId));

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<TestRun> recentRuns = testRunRepository.findRecentByTestId(testRun.getTestId(), since);

        Double score = calculateFlakyScore(recentRuns);

        // Save flakiness score to the run
        testRun.setFlakinessScore(score);
        testRunRepository.save(testRun);

        return score;
    }

    @Transactional(readOnly = true)
    public List<TestRunResponse> findFlakyRuns(UUID projectId, Double threshold) {
        return testRunRepository.findFlakyByProjectId(projectId, threshold).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== HELPER METHODS ==========

    private Double calculateFlakyScore(List<TestRun> runs) {
        if (runs.size() < 3) {
            return 0.0;
        }

        int switches = 0;
        String lastStatus = null;
        for (TestRun run : runs) {
            if (lastStatus != null && !lastStatus.equals(run.getStatus())) {
                // Count only PASSED <-> FAILED switches as flakiness
                if (("PASSED".equals(lastStatus) || "FAILED".equals(lastStatus)) &&
                    ("PASSED".equals(run.getStatus()) || "FAILED".equals(run.getStatus()))) {
                    switches++;
                }
            }
            lastStatus = run.getStatus();
        }

        // Score = switches / (runs - 1), normalized to 0-1
        return Math.min(1.0, (double) switches / (runs.size() - 1));
    }

    private void updateExecutionCounts(TestRun testRun) {
        // This method can be extended to update parent TestExecution if needed
        // For now, TestExecution tracks aggregate counts separately
        log.debug("Updated counts for execution: {}", testRun.getExecutionId());
    }

    private TestRunResponse mapToResponse(TestRun testRun) {
        return TestRunResponse.builder()
                .id(testRun.getId())
                .testId(testRun.getTestId())
                .executionId(testRun.getExecutionId())
                .projectId(testRun.getProjectId())
                .status(testRun.getStatus())
                .executedBy(testRun.getExecutedBy())
                .executedAt(testRun.getExecutedAt())
                .startedAt(testRun.getStartedAt())
                .completedAt(testRun.getCompletedAt())
                .duration(testRun.getDuration())
                .comment(testRun.getComment())
                .defectKeys(testRun.getDefectKeys())
                .stepStatuses(testRun.getStepStatuses())
                .passedSteps(testRun.getPassedSteps())
                .failedSteps(testRun.getFailedSteps())
                .blockedSteps(testRun.getBlockedSteps())
                .totalSteps(testRun.getTotalSteps())
                .environment(testRun.getEnvironment())
                .browser(testRun.getBrowser())
                .platform(testRun.getPlatform())
                .testData(testRun.getTestData())
                .evidenceLinks(testRun.getEvidenceLinks())
                .logs(testRun.getLogs())
                .errorMessage(testRun.getErrorMessage())
                .isRetry(testRun.getIsRetry())
                .parentRunId(testRun.getParentRunId())
                .createdAt(testRun.getCreatedAt())
                .updatedAt(testRun.getUpdatedAt())
                .build();
    }
}