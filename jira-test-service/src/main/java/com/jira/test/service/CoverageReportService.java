package com.jira.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating and exporting coverage reports.
 * Supports multiple formats (JSON, CSV, PDF, Excel) and report types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoverageReportService {

    private final CoverageService coverageService;
    private final CoverageRuleRepository coverageRuleRepository;
    private final CoverageThresholdRepository coverageThresholdRepository;
    private final CoverageDriftRecordRepository coverageDriftRecordRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final TestSetRepository testSetRepository;
    private final RequirementLinkRepository requirementLinkRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ========== REPORT GENERATION ==========

    /**
     * Generate a comprehensive coverage report based on request parameters.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateReport(CoverageExportRequest request) {
        long startTime = System.currentTimeMillis();
        UUID reportId = UUID.randomUUID();

        log.info("Generating {} report for project: {}", request.getReportType(), request.getProjectId());

        CoverageExportResponse response = switch (request.getReportType()) {
            case EXECUTIVE_SUMMARY -> generateExecutiveSummary(request);
            case DETAILED_ANALYSIS -> generateDetailedAnalysis(request);
            case TREND_REPORT -> generateTrendReport(request);
            case COMPLIANCE_REPORT -> generateComplianceReport(request);
            case TEST_DIVERSITY_REPORT -> generateTestDiversityReport(request);
        };

        response.setReportId(reportId);
        response.setGeneratedAt(LocalDateTime.now());
        response.setFileName(generateFileName(request));
        response.setContentType(getContentType(request.getFormat()));

        long generationTime = System.currentTimeMillis() - startTime;
        response.getMetadata().setGenerationTimeMs(generationTime);
        response.getMetadata().setGeneratedBy(LocalDateTime.now());

        log.info("Report generated in {}ms: {}", generationTime, reportId);
        return response;
    }

    /**
     * Generate executive summary report for stakeholders.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateExecutiveSummary(CoverageExportRequest request) {
        ProjectCoverageResponse projectCoverage = coverageService.getProjectCoverage(request.getProjectId());
        List<CoverageService.CoverageAlertResponse> alerts = coverageService.getAlerts(request.getProjectId());
        List<CoverageService.CoverageRuleViolation> violations = coverageService.evaluateRules(request.getProjectId());
        CoverageTrendResponse trends = coverageService.getCoverageTrends(request.getProjectId(), request.getPeriodDays());

        CoverageExportResponse.ExecutiveSummary summary = CoverageExportResponse.ExecutiveSummary.builder()
                .overallCoverage(projectCoverage.getOverallCoverage())
                .coverageChange(trends.getSummary() != null ? trends.getSummary().getChangeRate7d() : BigDecimal.ZERO)
                .trendDirection(trends.getSummary() != null ? trends.getSummary().getTrendDirection() : "STABLE")
                .totalRequirements(projectCoverage.getTotalRequirements())
                .requirementsMet((int) projectCoverage.getByRequirement().values().stream().filter(ProjectCoverageResponse.RequirementCoverage::isMeetsThreshold).count())
                .requirementsAtRisk((int) projectCoverage.getByRequirement().values().stream().filter(r -> !r.isMeetsThreshold()).count())
                .totalRules(violations.size())
                .rulesViolated((int) violations.stream().filter(v -> v.getCurrentValue().compareTo(v.getThreshold()) < 0).count())
                .criticalAlerts((int) alerts.stream().filter(a -> a.getAlertLevel() == CoverageThreshold.AlertLevel.CRITICAL).count())
                .warnings((int) alerts.stream().filter(a -> a.getAlertLevel() == CoverageThreshold.AlertLevel.WARNING).count())
                .build();

        CoverageExportResponse.ReportMetadata metadata = CoverageExportResponse.ReportMetadata.builder()
                .overallCoverage(projectCoverage.getOverallCoverage())
                .totalRequirements(projectCoverage.getTotalRequirements())
                .totalTests(projectCoverage.getTotalTests())
                .coveredTests(projectCoverage.getCoveredTests())
                .uncoveredTests(projectCoverage.getUncoveredTests())
                .totalRules(coverageRuleRepository.findByProjectIdOrderByCreatedAtDesc(request.getProjectId()).size())
                .violatedRules((int) violations.stream().filter(v -> v.getCurrentValue().compareTo(v.getThreshold()) < 0).count())
                .highPrioritySuggestions(0)
                .build();

        return CoverageExportResponse.builder()
                .projectId(request.getProjectId())
                .reportName("Executive Coverage Summary")
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(LocalDateTime.now().minusDays(request.getPeriodDays()))
                .periodEnd(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Generate detailed analysis report with per-requirement and per-test-set breakdown.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateDetailedAnalysis(CoverageExportRequest request) {
        ProjectCoverageResponse projectCoverage = coverageService.getProjectCoverage(request.getProjectId());
        CoverageMatrixResponse matrix = coverageService.getCoverageMatrix(request.getProjectId());
        List<CoverageService.CoverageRuleViolation> violations = coverageService.evaluateRules(request.getProjectId());

        // Build requirement analysis
        List<CoverageExportResponse.RequirementAnalysis> requirementAnalysis = new ArrayList<>();
        projectCoverage.getByRequirement().forEach((reqKey, reqCoverage) -> {
            BigDecimal coverageChange = BigDecimal.ZERO;
            String status = reqCoverage.getCoverage().compareTo(BigDecimal.valueOf(80)) >= 0 ? "COVERED" :
                    reqCoverage.getCoverage().compareTo(BigDecimal.valueOf(50)) >= 0 ? "PARTIAL" : "UNCOVERED";

            requirementAnalysis.add(CoverageExportResponse.RequirementAnalysis.builder()
                    .requirementKey(reqKey)
                    .requirementId(reqCoverage.getRequirementId())
                    .currentCoverage(reqCoverage.getCoverage())
                    .previousCoverage(reqCoverage.getCoverage())
                    .coverageChange(coverageChange)
                    .totalTests(reqCoverage.getTotalTests())
                    .coveredTests(reqCoverage.getCoveredTests())
                    .missingTests(reqCoverage.getUncoveredTests())
                    .status(status)
                    .build());
        });

        // Build test set analysis
        List<CoverageExportResponse.TestSetAnalysis> testSetAnalysis = new ArrayList<>();
        List<TestSet> testSets = testSetRepository.findAll();
        for (TestSet ts : testSets) {
            List<TestIssue> tests = testSetRepository.findTestsByTestSetId(ts.getId());
            int totalTests = tests.size();
            int coveredTests = 0;
            int flakyTests = 0;
            BigDecimal passRate = BigDecimal.ZERO;

            for (TestIssue test : tests) {
                List<TestExecution> executions = executionRepository.findByTestId(test.getId());
                if (!executions.isEmpty()) {
                    coveredTests++;
                    long passed = executions.stream().filter(e -> "PASSED".equals(e.getStatus())).count();
                    long failed = executions.stream().filter(e -> "FAILED".equals(e.getStatus())).count();
                    if (failed > 0 && passed > 0) flakyTests++;
                    if (passed + failed > 0) {
                        passRate = BigDecimal.valueOf(passed * 100.0 / (passed + failed))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }

            BigDecimal coverage = totalTests > 0 ?
                    BigDecimal.valueOf(coveredTests * 100.0 / totalTests).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            testSetAnalysis.add(CoverageExportResponse.TestSetAnalysis.builder()
                    .testSetId(ts.getId())
                    .testSetName(ts.getName())
                    .coverage(coverage)
                    .totalTests(totalTests)
                    .coveredTests(coveredTests)
                    .flakyTests(flakyTests)
                    .passRate(passRate)
                    .build());
        }

        // Build rule violations
        List<CoverageExportResponse.RuleViolation> ruleViolations = violations.stream()
                .map(v -> CoverageExportResponse.RuleViolation.builder()
                        .ruleId(v.getRuleId())
                        .ruleName(v.getRuleName())
                        .ruleType(v.getRuleType().name())
                        .threshold(v.getThreshold())
                        .currentValue(v.getCurrentValue())
                        .deviation(v.getThreshold().subtract(v.getCurrentValue()))
                        .severity(v.getCurrentValue().compareTo(v.getThreshold().multiply(BigDecimal.valueOf(0.5))) < 0 ? "CRITICAL" : "WARNING")
                        .build())
                .toList();

        CoverageExportResponse.DetailedAnalysis analysis = CoverageExportResponse.DetailedAnalysis.builder()
                .requirementAnalysis(requirementAnalysis)
                .testSetAnalysis(testSetAnalysis)
                .ruleViolations(ruleViolations)
                .build();

        CoverageExportResponse.ReportMetadata metadata = CoverageExportResponse.ReportMetadata.builder()
                .overallCoverage(projectCoverage.getOverallCoverage())
                .totalRequirements(requirementAnalysis.size())
                .totalTests(projectCoverage.getTotalTests())
                .coveredTests(projectCoverage.getCoveredTests())
                .uncoveredTests(projectCoverage.getUncoveredTests())
                .totalRules(violations.size())
                .violatedRules(ruleViolations.size())
                .build();

        return CoverageExportResponse.builder()
                .projectId(request.getProjectId())
                .reportName("Detailed Coverage Analysis")
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(LocalDateTime.now().minusDays(request.getPeriodDays()))
                .periodEnd(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Generate trend report showing coverage evolution over time.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateTrendReport(CoverageExportRequest request) {
        CoverageTrendResponse trends = coverageService.getCoverageTrends(request.getProjectId(), request.getPeriodDays());

        CoverageExportResponse.ReportMetadata metadata = CoverageExportResponse.ReportMetadata.builder()
                .totalRequirements(trends.getTrendPoints() != null ? trends.getTrendPoints().size() : 0)
                .overallCoverage(trends.getSummary() != null ? trends.getSummary().getCurrentCoverage() : BigDecimal.ZERO)
                .trendDirection(trends.getSummary() != null ?
                        new BigDecimal(trends.getSummary().getTrendDirection().compareTo("IMPROVING") == 0 ? "1" :
                                trends.getSummary().getTrendDirection().compareTo("DECLINING") == 0 ? "-1" : "0") : BigDecimal.ZERO)
                .build();

        return CoverageExportResponse.builder()
                .projectId(request.getProjectId())
                .reportName("Coverage Trend Report")
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(trends.getPeriodDays() > 0 ? LocalDateTime.now().minusDays(trends.getPeriodDays()) : null)
                .periodEnd(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Generate compliance report for audit purposes.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateComplianceReport(CoverageExportRequest request) {
        List<CoverageThreshold> thresholds = coverageThresholdRepository.findByProjectIdOrderByCreatedAtDesc(request.getProjectId());
        List<CoverageRule> rules = coverageRuleRepository.findByProjectIdOrderByCreatedAtDesc(request.getProjectId());

        int compliantRequirements = 0;
        int nonCompliantRequirements = 0;
        for (CoverageThreshold t : thresholds) {
            if (t.getCurrentCoverage() != null && t.getMinimumCoverage() != null &&
                    t.getCurrentCoverage().compareTo(t.getMinimumCoverage()) >= 0) {
                compliantRequirements++;
            } else {
                nonCompliantRequirements++;
            }
        }

        int compliantRules = 0;
        int violatedRules = 0;
        for (CoverageRule rule : rules) {
            if (rule.getEnabled() != null && rule.getEnabled()) {
                BigDecimal currentCoverage = getCurrentCoverageForScope(request.getProjectId(), rule);
                if (currentCoverage.compareTo(rule.getThreshold()) >= 0) {
                    compliantRules++;
                } else {
                    violatedRules++;
                }
            }
        }

        CoverageExportResponse.ReportMetadata metadata = CoverageExportResponse.ReportMetadata.builder()
                .totalRequirements(thresholds.size())
                .totalRules(rules.size())
                .violatedRules(violatedRules)
                .highPrioritySuggestions(nonCompliantRequirements + violatedRules)
                .build();

        return CoverageExportResponse.builder()
                .projectId(request.getProjectId())
                .reportName("Coverage Compliance Report")
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now().minusDays(90))
                .periodEnd(request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    /**
     * Generate test diversity report analyzing test type distribution.
     */
    @Transactional(readOnly = true)
    public CoverageExportResponse generateTestDiversityReport(CoverageExportRequest request) {
        List<TestIssue> tests = testIssueRepository.findByProjectId(request.getProjectId());

        Map<TestIssue.TestType, Long> testTypeCounts = tests.stream()
                .filter(t -> t.getTestType() != null)
                .collect(Collectors.groupingBy(TestIssue.TestType, Collectors.counting()));

        int totalDiverseTypes = testTypeCounts.size();
        int minimumDiversity = 3; // Expected minimum test types

        CoverageExportResponse.ReportMetadata metadata = CoverageExportResponse.ReportMetadata.builder()
                .totalTests(tests.size())
                .highPrioritySuggestions(totalDiverseTypes < minimumDiversity ? 1 : 0)
                .build();

        return CoverageExportResponse.builder()
                .projectId(request.getProjectId())
                .reportName("Test Diversity Report")
                .reportType(request.getReportType())
                .format(request.getFormat())
                .periodStart(LocalDateTime.now().minusDays(request.getPeriodDays()))
                .periodEnd(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    // ========== EXPORT FUNCTIONALITY ==========

    /**
     * Export report to specified format.
     */
    @Transactional(readOnly = true)
    public byte[] exportReport(CoverageExportRequest request) {
        CoverageExportResponse report = generateReport(request);

        return switch (request.getFormat()) {
            case JSON -> exportAsJson(report);
            case CSV -> exportAsCsv(report);
            case PDF -> exportAsPdf(report);
            case EXCEL -> exportAsExcel(report);
        };
    }

    /**
     * Export report data as JSON.
     */
    public byte[] exportAsJson(CoverageExportResponse report) {
        try {
            return objectMapper.writeValueAsBytes(report);
        } catch (Exception e) {
            log.error("Failed to export report as JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export report as JSON", e);
        }
    }

    /**
     * Export report data as CSV.
     */
    public byte[] exportAsCsv(CoverageExportResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // Write metadata header
        writer.println("Coverage Report");
        writer.println("Report ID," + report.getReportId());
        writer.println("Project ID," + report.getProjectId());
        writer.println("Report Type," + report.getReportType());
        writer.println("Generated At," + (report.getGeneratedAt() != null ? report.getGeneratedAt().format(DISPLAY_FORMATTER) : ""));
        writer.println();

        if (report.getMetadata() != null) {
            writer.println("Summary");
            writer.println("Overall Coverage," + report.getMetadata().getOverallCoverage());
            writer.println("Total Requirements," + report.getMetadata().getTotalRequirements());
            writer.println("Total Tests," + report.getMetadata().getTotalTests());
            writer.println("Covered Tests," + report.getMetadata().getCoveredTests());
            writer.println("Uncovered Tests," + report.getMetadata().getUncoveredTests());
            writer.println("Total Rules," + report.getMetadata().getTotalRules());
            writer.println("Violated Rules," + report.getMetadata().getViolatedRules());
            writer.println();
        }

        writer.println("Requirement Coverage");
        writer.println("Requirement Key,Current Coverage,Previous Coverage,Total Tests,Covered Tests,Status");

        // Write requirement data if available
        writer.println();

        writer.flush();
        return out.toByteArray();
    }

    /**
     * Export report data as PDF (simplified text-based).
     */
    public byte[] exportAsPdf(CoverageExportResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // Simple PDF-like text format
        writer.println("%PDF-1.4");
        writer.println("1 0 obj");
        writer.println("<< /Type /Catalog /Pages 2 0 R >>");
        writer.println("endobj");
        writer.println();

        writer.println("% Coverage Report Export");
        writer.println("% Generated: " + (report.getGeneratedAt() != null ? report.getGeneratedAt().format(DISPLAY_FORMATTER) : "N/A"));
        writer.println("% Project: " + report.getProjectId());
        writer.println();

        if (report.getMetadata() != null) {
            writer.println("% SUMMARY");
            writer.println("% Overall Coverage: " + report.getMetadata().getOverallCoverage() + "%");
            writer.println("% Total Tests: " + report.getMetadata().getTotalTests());
            writer.println("% Covered: " + report.getMetadata().getCoveredTests());
            writer.println("% Uncovered: " + report.getMetadata().getUncoveredTests());
        }

        writer.println("%%EOF");
        writer.flush();
        return out.toByteArray();
    }

    /**
     * Export report data as Excel-compatible CSV.
     */
    public byte[] exportAsExcel(CoverageExportResponse report) {
        return exportAsCsv(report); // Use CSV format (Excel can open CSV)
    }

    // ========== HELPER METHODS ==========

    private String generateFileName(CoverageExportRequest request) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String extension = switch (request.getFormat()) {
            case JSON -> ".json";
            case CSV -> ".csv";
            case PDF -> ".pdf";
            case EXCEL -> ".xlsx";
        };
        return String.format("coverage_%s_%s_%s%s",
                request.getReportType().name().toLowerCase(),
                request.getProjectId().toString().substring(0, 8),
                timestamp,
                extension);
    }

    private String getContentType(CoverageExportRequest.ExportFormat format) {
        return switch (format) {
            case JSON -> "application/json";
            case CSV -> "text/csv";
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    private BigDecimal getCurrentCoverageForScope(UUID projectId, CoverageRule rule) {
        if (rule.getScope() == CoverageRule.Scope.GLOBAL) {
            ProjectCoverageResponse coverage = coverageService.getProjectCoverage(projectId);
            return coverage.getOverallCoverage();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get summary statistics for a project.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getReportSummary(UUID projectId) {
        ProjectCoverageResponse coverage = coverageService.getProjectCoverage(projectId);
        List<CoverageService.CoverageRuleViolation> violations = coverageService.evaluateRules(projectId);
        CoverageTrendResponse trends = coverageService.getCoverageTrends(projectId, 30);

        Map<String, Object> summary = new HashMap<>();
        summary.put("projectId", projectId);
        summary.put("overallCoverage", coverage.getOverallCoverage());
        summary.put("totalTests", coverage.getTotalTests());
        summary.put("coveredTests", coverage.getCoveredTests());
        summary.put("uncoveredTests", coverage.getUncoveredTests());
        summary.put("totalRequirements", coverage.getTotalRequirements());
        summary.put("ruleViolations", violations.size());
        summary.put("trendDirection", trends.getSummary() != null ? trends.getSummary().getTrendDirection() : "STABLE");
        summary.put("changeRate7d", trends.getSummary() != null ? trends.getSummary().getChangeRate7d() : BigDecimal.ZERO);

        return summary;
    }

    /**
     * List available report templates.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableReportTypes() {
        return List.of(
                Map.of("type", "EXECUTIVE_SUMMARY", "name", "Executive Summary", "description", "High-level overview for stakeholders"),
                Map.of("type", "DETAILED_ANALYSIS", "name", "Detailed Analysis", "description", "In-depth breakdown by requirement and test set"),
                Map.of("type", "TREND_REPORT", "name", "Trend Report", "description", "Coverage trends over time"),
                Map.of("type", "COMPLIANCE_REPORT", "name", "Compliance Report", "description", "Audit-ready compliance status"),
                Map.of("type", "TEST_DIVERSITY_REPORT", "name", "Test Diversity Report", "description", "Test type distribution analysis")
        );
    }
}