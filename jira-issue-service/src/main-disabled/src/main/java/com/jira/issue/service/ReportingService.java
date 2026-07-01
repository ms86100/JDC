package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportingService - Test execution analytics and reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final ProjectRepository projectRepository;
    private final TestSetRepository testSetRepository;
    private final TestPlanRepository testPlanRepository;
    private final TestExecutionRepository executionRepository;
    private final TestExecutionHistoryRepository historyRepository;
    private final StepResultRepository stepResultRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final DefectLinkRepository defectLinkRepository;
    private final TestImportBatchRepository importBatchRepository;

    // ==================== Summary Reports ====================

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummaryReport(UUID projectId, UUID sprintId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating summary report for project: {}", projectId);

        // Get all tests for project
        List<Issue> tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test");
        int totalTests = tests.size();

        // Count by status
        long draft = tests.stream().filter(t -> "DRAFT".equals(t.getTestStatus())).count();
        long ready = tests.stream().filter(t -> "READY".equals(t.getTestStatus())).count();
        long approved = tests.stream().filter(t -> "APPROVED".equals(t.getTestStatus())).count();
        long deprecated = tests.stream().filter(t -> "DEPRECATED".equals(t.getTestStatus())).count();

        // Count by type
        long manual = tests.stream().filter(t -> "MANUAL".equals(t.getTestType())).count();
        long automated = tests.stream().filter(t -> "AUTOMATED".equals(t.getTestType())).count();
        long bdd = tests.stream().filter(t -> "BDD".equals(t.getTestType())).count();

        // Get executions in date range
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        List<TestExecution> executions = executionRepository.findByProjectIdAndDateRange(projectId, start, end);

        int totalExecutions = executions.size();
        long passed = executions.stream().filter(e -> "PASSED".equals(e.getStatus())).count();
        long failed = executions.stream().filter(e -> "FAILED".equals(e.getStatus())).count();
        long blocked = executions.stream().filter(e -> "BLOCKED".equals(e.getStatus())).count();
        long running = executions.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();

        int totalPassedTests = executions.stream().mapToInt(TestExecution::getPassedTests).sum();
        int totalFailedTests = executions.stream().mapToInt(TestExecution::getFailedTests).sum();
        int totalTestsRun = totalPassedTests + totalFailedTests;

        double passRate = totalTestsRun > 0 ? (double) totalPassedTests / totalTestsRun * 100 : 0.0;

        // Get test sets
        List<TestSet> testSets = testSetRepository.findByProjectIdAndArchivedFalseOrderByNameAsc(projectId);

        // Get requirement coverage
        List<String> requirements = tests.stream()
                .filter(t -> t.getRequirementKeys() != null)
                .flatMap(t -> Arrays.stream(t.getRequirementKeys()))
                .distinct()
                .collect(Collectors.toList());

        long coveredRequirements = requirements.stream()
                .map(req -> requirementLinkRepository.findByRequirementKey(req))
                .filter(links -> !links.isEmpty())
                .count();

        // Calculate defect metrics
        List<TestExecution> recentFailed = executions.stream()
                .filter(e -> "FAILED".equals(e.getStatus()))
                .limit(10)
                .collect(Collectors.toList());

        List<DefectLink> defects = new ArrayList<>();
        for (TestExecution exec : recentFailed) {
            defects.addAll(defectLinkRepository.findByTestExecutionId(exec.getId()));
        }

        long criticalDefects = defects.stream().filter(d -> "CRITICAL".equals(d.getSeverity())).count();
        long majorDefects = defects.stream().filter(d -> "MAJOR".equals(d.getSeverity())).count();
        long openDefects = defects.stream().filter(d -> "OPEN".equals(d.getStatus())).count();

        return ReportSummaryResponse.builder()
                .projectId(projectId)
                .sprintId(sprintId)
                .startDate(startDate)
                .endDate(endDate)
                // Test counts
                .totalTests(totalTests)
                .testsDraft((int) draft)
                .testsReady((int) ready)
                .testsApproved((int) approved)
                .testsDeprecated((int) deprecated)
                .testsManual((int) manual)
                .testsAutomated((int) automated)
                .testsBdd((int) bdd)
                // Execution counts
                .totalExecutions(totalExecutions)
                .executionsPassed((int) passed)
                .executionsFailed((int) failed)
                .executionsBlocked((int) blocked)
                .executionsRunning((int) running)
                // Test results
                .totalTestsRun(totalTestsRun)
                .testsPassed(totalPassedTests)
                .testsFailed(totalFailedTests)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                // Test sets
                .totalTestSets(testSets.size())
                // Coverage
                .totalRequirements(requirements.size())
                .coveredRequirements((int) coveredRequirements)
                .requirementCoverage(totalTests > 0 ? (double) coveredRequirements / requirements.size() * 100 : 0)
                // Defects
                .totalDefects(defects.size())
                .criticalDefects((int) criticalDefects)
                .majorDefects((int) majorDefects)
                .openDefects((int) openDefects)
                // Timestamps
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Test Trends ====================

    @Transactional(readOnly = true)
    public List<TestTrendResponse> getTestTrends(UUID projectId, UUID testId, int days) {
        log.info("Getting test trends for test: {}", testId);

        LocalDateTime start = LocalDateTime.now().minusDays(days);

        List<TestExecutionHistory> history = historyRepository.findByTestIssueIdAndDateRange(testId, start, LocalDateTime.now());

        return history.stream()
                .map(h -> TestTrendResponse.builder()
                        .date(h.getExecutedAt())
                        .status(h.getStatus())
                        .testEnv(h.getTestEnv())
                        .durationMs(h.getDurationMs())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== Requirement Coverage Report ====================

    @Transactional(readOnly = true)
    public RequirementCoverageResponse getRequirementCoverageReport(UUID projectId) {
        List<Issue> tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test");

        // Group tests by requirement
        Map<String, List<RequirementLink>> byRequirement = new HashMap<>();
        for (Issue test : tests) {
            if (test.getRequirementKeys() != null) {
                for (String reqKey : test.getRequirementKeys()) {
                    List<RequirementLink> links = requirementLinkRepository.findByRequirementKey(reqKey);
                    byRequirement.computeIfAbsent(reqKey, k -> new ArrayList<>()).addAll(links);
                }
            }
        }

        List<RequirementCoverageResponse.RequirementCoverageRow> rows = byRequirement.entrySet().stream()
                .map(entry -> {
                    String reqKey = entry.getKey();
                    List<RequirementLink> links = entry.getValue();

                    int testCount = links.size();
                    long passed = links.stream()
                            .filter(l -> "PASSED".equals(l.getLastExecutionStatus()))
                            .count();
                    long failed = links.stream()
                            .filter(l -> "FAILED".equals(l.getLastExecutionStatus()))
                            .count();
                    double coverage = testCount > 0 ? (double) passed / testCount * 100 : 0;

                    String status;
                    if (testCount == 0) status = "NOT_COVERED";
                    else if (failed > 0) status = "FAILING";
                    else if (passed == testCount) status = "COVERED";
                    else status = "PARTIAL";

                    return RequirementCoverageResponse.RequirementCoverageRow.builder()
                            .requirementKey(reqKey)
                            .testCount(testCount)
                            .testsPassed((int) passed)
                            .testsFailed((int) failed)
                            .coveragePercent(Math.round(coverage * 100.0) / 100.0)
                            .status(status)
                            .build();
                })
                .sorted(Comparator.comparing(RequirementCoverageResponse.RequirementCoverageRow::getCoveragePercent))
                .collect(Collectors.toList());

        int totalReqs = rows.size();
        long coveredReqs = rows.stream().filter(r -> "COVERED".equals(r.getStatus())).count();
        long partialReqs = rows.stream().filter(r -> "PARTIAL".equals(r.getStatus())).count();
        long failingReqs = rows.stream().filter(r -> "FAILING".equals(r.getStatus())).count();

        return RequirementCoverageResponse.builder()
                .projectId(projectId)
                .totalRequirements(totalReqs)
                .fullyCovered((int) coveredReqs)
                .partiallyCovered((int) partialReqs)
                .failing((int) failingReqs)
                .overallCoverage(totalReqs > 0 ? (double) coveredReqs / totalReqs * 100 : 0)
                .requirements(rows)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Defect Density Report ====================

    @Transactional(readOnly = true)
    public DefectDensityResponse getDefectDensityReport(UUID projectId) {
        List<DefectLink> allDefects = defectLinkRepository.findAll().stream()
                .filter(d -> {
                    // Get related issue to check project
                    return true; // Simplified
                })
                .collect(Collectors.toList());

        // Group by requirement
        Map<String, Long> defectsByRequirement = new HashMap<>();
        for (DefectLink defect : allDefects) {
            String reqKey = "UNKNOWN"; // Would need to join with requirement links
            defectsByRequirement.merge(reqKey, 1L, Long::sum);
        }

        List<DefectDensityResponse.DefectDensityRow> rows = defectsByRequirement.entrySet().stream()
                .map(entry -> DefectDensityResponse.DefectDensityRow.builder()
                        .requirementKey(entry.getKey())
                        .defectCount(entry.getValue().intValue())
                        .severity("MIXED")
                        .riskLevel(entry.getValue() > 5 ? "HIGH" : entry.getValue() > 2 ? "MEDIUM" : "LOW")
                        .build())
                .sorted(Comparator.comparing(DefectDensityResponse.DefectDensityRow::getDefectCount).reversed())
                .collect(Collectors.toList());

        return DefectDensityResponse.builder()
                .projectId(projectId)
                .totalDefects(rows.size())
                .requirements(rows)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Sprint Quality Report ====================

    @Transactional(readOnly = true)
    public SprintQualityResponse getSprintQualityReport(UUID projectId, UUID sprintId) {
        List<TestExecution> executions = executionRepository.findByProjectIdOrderByStartedAtDesc(projectId);

        // Get recent executions
        LocalDateTime sprintStart = LocalDateTime.now().minusWeeks(2);
        List<TestExecution> sprintExecs = executions.stream()
                .filter(e -> e.getStartedAt() != null && e.getStartedAt().isAfter(sprintStart))
                .collect(Collectors.toList());

        int totalTests = sprintExecs.stream().mapToInt(TestExecution::getTotalTests).sum();
        int passedTests = sprintExecs.stream().mapToInt(TestExecution::getPassedTests).sum();
        int failedTests = sprintExecs.stream().mapToInt(TestExecution::getFailedTests).sum();

        double passRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;

        // Get blocked and skipped
        int blockedTests = sprintExecs.stream().mapToInt(TestExecution::getBlockedTests).sum();
        int skippedTests = sprintExecs.stream().mapToInt(TestExecution::getSkippedTests).sum();

        // Calculate execution frequency
        double avgExecPerDay = sprintExecs.size() / 14.0;

        // Get defects found
        List<DefectLink> defects = new ArrayList<>();
        for (TestExecution exec : sprintExecs) {
            defects.addAll(defectLinkRepository.findByTestExecutionId(exec.getId()));
        }

        // Compare to previous sprint
        LocalDateTime prevSprintStart = LocalDateTime.now().minusWeeks(4);
        LocalDateTime prevSprintEnd = LocalDateTime.now().minusWeeks(2);

        List<TestExecution> prevExecs = executions.stream()
                .filter(e -> e.getStartedAt() != null &&
                        e.getStartedAt().isAfter(prevSprintStart) &&
                        e.getStartedAt().isBefore(prevSprintEnd))
                .collect(Collectors.toList());

        int prevTotalTests = prevExecs.stream().mapToInt(TestExecution::getTotalTests).sum();
        int prevPassedTests = prevExecs.stream().mapToInt(TestExecution::getPassedTests).sum();
        double prevPassRate = prevTotalTests > 0 ? (double) prevPassedTests / prevTotalTests * 100 : 0;

        double passRateChange = passRate - prevPassRate;

        // Determine status
        String status;
        if (passRate >= 95) status = "EXCELLENT";
        else if (passRate >= 85) status = "GOOD";
        else if (passRate >= 70) status = "NEEDS_IMPROVEMENT";
        else status = "CRITICAL";

        return SprintQualityResponse.builder()
                .projectId(projectId)
                .sprintId(sprintId)
                .totalExecutions(sprintExecs.size())
                .totalTestsRun(totalTests)
                .testsPassed(passedTests)
                .testsFailed(failedTests)
                .testsBlocked(blockedTests)
                .testsSkipped(skippedTests)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .passRateChange(Math.round(passRateChange * 100.0) / 100.0)
                .avgExecutionsPerDay(Math.round(avgExecPerDay * 100.0) / 100.0)
                .defectsFound(defects.size())
                .status(status)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Automation Coverage Report ====================

    @Transactional(readOnly = true)
    public AutomationCoverageResponse getAutomationCoverageReport(UUID projectId) {
        List<Issue> tests = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test");

        int totalTests = tests.size();
        int automatedTests = (int) tests.stream().filter(t -> "AUTOMATED".equals(t.getTestType())).count();
        int manualTests = (int) tests.stream().filter(t -> "MANUAL".equals(t.getTestType())).count();
        int bddTests = (int) tests.stream().filter(t -> "BDD".equals(t.getTestType())).count();

        // Count tests with recent CI imports
        List<TestImportBatch> recentImports = importBatchRepository.findByDateRange(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        int recentlyAutomated = recentImports.stream()
                .filter(b -> "AUTOMATED".equals(b.getImportType()))
                .mapToInt(TestImportBatch::getTestsCreated)
                .sum();

        double automationPercent = totalTests > 0 ? (double) automatedTests / totalTests * 100 : 0;

        // Get requirements without automated tests
        List<String> uncoveredReqs = new ArrayList<>();
        for (Issue test : tests) {
            if (test.getRequirementKeys() != null && !"AUTOMATED".equals(test.getTestType())) {
                for (String reqKey : test.getRequirementKeys()) {
                    boolean hasAutomated = tests.stream()
                            .filter(t -> "AUTOMATED".equals(t.getTestType()))
                            .anyMatch(t -> Arrays.asList(t.getRequirementKeys()).contains(reqKey));
                    if (!hasAutomated && !uncoveredReqs.contains(reqKey)) {
                        uncoveredReqs.add(reqKey);
                    }
                }
            }
        }

        return AutomationCoverageResponse.builder()
                .projectId(projectId)
                .totalTests(totalTests)
                .automatedTests(automatedTests)
                .manualTests(manualTests)
                .bddTests(bddTests)
                .automationPercent(Math.round(automationPercent * 100.0) / 100.0)
                .recentlyAutomated(recentlyAutomated)
                .requirementsWithoutAutomation(uncoveredReqs.size())
                .uncoveredRequirementKeys(uncoveredReqs)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}