package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for exported coverage reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageExportResponse {
    private UUID reportId;
    private UUID projectId;
    private String reportName;
    private CoverageExportRequest.ReportType reportType;
    private CoverageExportRequest.ExportFormat format;
    private LocalDateTime generatedAt;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private ReportMetadata metadata;
    private String fileName;
    private String contentType;
    private byte[] data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private int totalRequirements;
        private int totalTests;
        private int coveredTests;
        private int uncoveredTests;
        private BigDecimal overallCoverage;
        private BigDecimal trendDirection;
        private int totalRules;
        private int violatedRules;
        private int highPrioritySuggestions;
        private LocalDateTime generatedBy;
        private long generationTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutiveSummary {
        private BigDecimal overallCoverage;
        private BigDecimal coverageChange;
        private String trendDirection;
        private int totalRequirements;
        private int requirementsMet;
        private int requirementsAtRisk;
        private int totalRules;
        private int rulesViolated;
        private int criticalAlerts;
        private int warnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedAnalysis {
        private List<RequirementAnalysis> requirementAnalysis;
        private List<TestSetAnalysis> testSetAnalysis;
        private List<RuleViolation> ruleViolations;
        private Map<String, BigDecimal> coverageByRequirement;
        private Map<String, BigDecimal> coverageByTestType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementAnalysis {
        private String requirementKey;
        private UUID requirementId;
        private BigDecimal currentCoverage;
        private BigDecimal previousCoverage;
        private BigDecimal coverageChange;
        private int totalTests;
        private int coveredTests;
        private int missingTests;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestSetAnalysis {
        private UUID testSetId;
        private String testSetName;
        private BigDecimal coverage;
        private int totalTests;
        private int coveredTests;
        private int flakyTests;
        private BigDecimal passRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleViolation {
        private UUID ruleId;
        private String ruleName;
        private String ruleType;
        private BigDecimal threshold;
        private BigDecimal currentValue;
        private BigDecimal deviation;
        private String severity;
    }
}