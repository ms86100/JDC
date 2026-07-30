package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.*;
import com.avionics_systems.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final DefectLinkRepository defectLinkRepository;
    private final TestSetRepository testSetRepository;
    private final TestPlanRepository testPlanRepository;

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(UUID projectId) {
        log.info("Generating summary report for project: {}", projectId);

        List<TestIssue> tests = testIssueRepository.findByProjectIdAndArchivedFalse(projectId);
        List<TestExecution> executions = executionRepository.findAll().stream()
                .filter(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());

        long totalTests = tests.size();
        long totalTestSets = testSetRepository.findByProjectIdAndArchivedFalse(projectId).size();
        long totalTestPlans = testPlanRepository.findByProjectId(projectId).size();
        long totalExecutions = executions.size();

        int testsPassed = 0;
        int testsFailed = 0;
        int testsBlocked = 0;
        int testsNotRun = 0;

        double totalPassRate = 0;
        int passRateCount = 0;

        for (TestExecution execution : executions) {
            if (execution.getTotalTests() != null && execution.getTotalTests() > 0) {
                double passRate = (double) execution.getPassedTests() / execution.getTotalTests() * 100;
                totalPassRate += passRate;
                passRateCount++;

                testsPassed += execution.getPassedTests() != null ? execution.getPassedTests() : 0;
                testsFailed += execution.getFailedTests() != null ? execution.getFailedTests() : 0;
                testsBlocked += execution.getBlockedTests() != null ? execution.getBlockedTests() : 0;
                testsNotRun += execution.getNotRunTests() != null ? execution.getNotRunTests() : 0;
            }
        }

        double overallPassRate = passRateCount > 0 ? totalPassRate / passRateCount : 0;

        return ReportSummaryResponse.builder()
                .totalTests(totalTests)
                .totalTestSets(totalTestSets)
                .totalTestPlans(totalTestPlans)
                .totalExecutions(totalExecutions)
                .overallPassRate(Math.round(overallPassRate * 100.0) / 100.0)
                .testsPassed(testsPassed)
                .testsFailed(testsFailed)
                .testsBlocked(testsBlocked)
                .testsNotRun(testsNotRun)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReportSummaryResponse.TestTrendPoint> getTrend(UUID projectId, int days) {
        log.info("Generating trend report for project: {} over {} days", projectId, days);

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<TestExecution> executions = executionRepository.findAll().stream()
                .filter(e -> e.getCreatedAt().isAfter(startDate))
                .collect(Collectors.toList());

        Map<LocalDate, List<TestExecution>> byDate = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().toLocalDate()));

        List<ReportSummaryResponse.TestTrendPoint> trend = new ArrayList<>();

        LocalDate current = startDate.toLocalDate();
        LocalDate end = LocalDate.now();

        while (!current.isAfter(end)) {
            List<TestExecution> dayExecutions = byDate.getOrDefault(current, List.of());

            int passed = 0, failed = 0, blocked = 0;
            for (TestExecution execution : dayExecutions) {
                passed += execution.getPassedTests() != null ? execution.getPassedTests() : 0;
                failed += execution.getFailedTests() != null ? execution.getFailedTests() : 0;
                blocked += execution.getBlockedTests() != null ? execution.getBlockedTests() : 0;
            }

            trend.add(ReportSummaryResponse.TestTrendPoint.builder()
                    .date(current)
                    .passed(passed)
                    .failed(failed)
                    .blocked(blocked)
                    .build());

            current = current.plusDays(1);
        }

        return trend;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCoverage(UUID projectId) {
        log.info("Generating coverage report for project: {}", projectId);

        List<TestIssue> tests = testIssueRepository.findByProjectIdAndArchivedFalse(projectId);
        int totalTests = tests.size();
        int automatedTests = (int) tests.stream().filter(t -> "AUTOMATED".equals(t.getTestType())).count();
        int manualTests = (int) tests.stream().filter(t -> "MANUAL".equals(t.getTestType())).count();
        int cukeTests = (int) tests.stream().filter(t -> "CUKE".equals(t.getTestType())).count();

        int covered = 0;
        for (TestIssue test : tests) {
            List<TestExecution> testExecutions = executionRepository.findByTestId(test.getId());
            if (!testExecutions.isEmpty()) {
                covered++;
            }
        }

        double coveragePercent = totalTests > 0 ? (double) covered / totalTests * 100 : 0;

        Map<String, Object> coverage = new HashMap<>();
        coverage.put("totalTests", totalTests);
        coverage.put("automatedTests", automatedTests);
        coverage.put("manualTests", manualTests);
        coverage.put("cukeTests", cukeTests);
        coverage.put("coveredTests", covered);
        coverage.put("coveragePercent", Math.round(coveragePercent * 100.0) / 100.0);

        return coverage;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDefectDensity(UUID projectId) {
        log.info("Generating defect density report for project: {}", projectId);

        List<TestIssue> tests = testIssueRepository.findByProjectIdAndArchivedFalse(projectId);
        List<TestExecution> executions = executionRepository.findAll().stream()
                .filter(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());

        int totalExecutions = executions.size();
        int totalFailedTests = executions.stream()
                .mapToInt(e -> e.getFailedTests() != null ? e.getFailedTests() : 0)
                .sum();

        List<DefectLink> defects = defectLinkRepository.findAll().stream()
                .filter(d -> d.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());

        int totalDefects = defects.size();
        int criticalDefects = (int) defects.stream().filter(d -> "CRITICAL".equals(d.getSeverity())).count();
        int highDefects = (int) defects.stream().filter(d -> "HIGH".equals(d.getSeverity())).count();
        int mediumDefects = (int) defects.stream().filter(d -> "MEDIUM".equals(d.getSeverity())).count();
        int lowDefects = (int) defects.stream().filter(d -> "LOW".equals(d.getSeverity())).count();

        double defectDensity = totalExecutions > 0 ? (double) totalDefects / totalExecutions : 0;

        Map<String, Object> density = new HashMap<>();
        density.put("totalDefects", totalDefects);
        density.put("totalFailedTests", totalFailedTests);
        density.put("defectDensity", Math.round(defectDensity * 100.0) / 100.0);
        density.put("criticalDefects", criticalDefects);
        density.put("highDefects", highDefects);
        density.put("mediumDefects", mediumDefects);
        density.put("lowDefects", lowDefects);
        density.put("periodDays", 30);

        return density;
    }
}