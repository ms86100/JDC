package com.jira.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for exporting coverage reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageExportRequest {
    private UUID projectId;
    private ExportFormat format;
    private ReportType reportType;
    private int periodDays;
    private List<UUID> requirementIds;
    private List<UUID> testSetIds;
    private boolean includeTrend;
    private boolean includeSuggestions;
    private boolean includeViolations;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ReportLevel level;

    public enum ExportFormat {
        JSON, CSV, PDF, EXCEL
    }

    public enum ReportType {
        EXECUTIVE_SUMMARY,
        DETAILED_ANALYSIS,
        TREND_REPORT,
        COMPLIANCE_REPORT,
        TEST_DIVERSITY_REPORT
    }

    public enum ReportLevel {
        PROJECT,
        REQUIREMENT,
        TEST_SET,
        TEST
    }
}