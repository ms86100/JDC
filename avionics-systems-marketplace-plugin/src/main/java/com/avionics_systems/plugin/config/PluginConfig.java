package com.avionics_systems.plugin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Centralized configuration for the Test Management Plugin.
 * All values are configurable via system properties with sensible defaults.
 * Non-Spring-managed classes (conditions, workflow functions, report renderers)
 * access this via {@link #getInstance()}.
 */
@Component
public class PluginConfig {

    private static PluginConfig instance;

    // --- Issue type configuration ---

    @Value("${plugin.issueType.test:Test}")
    private String testIssueType;

    @Value("${plugin.issueTypes.managed:Test,Test Set,Test Plan}")
    private String managedIssueTypesStr;

    // --- Report configuration ---

    @Value("${plugin.report.summary.name:Test Summary Report}")
    private String summaryReportName;

    @Value("${plugin.report.summary.template:templates/reports/test-summary-report.vm}")
    private String summaryReportTemplate;

    @Value("${plugin.report.executionHistory.name:Test Execution History}")
    private String executionHistoryReportName;

    @Value("${plugin.report.executionHistory.template:templates/reports/test-execution-history.vm}")
    private String executionHistoryReportTemplate;

    @Value("${plugin.report.coverage.name:Test Coverage Report}")
    private String coverageReportName;

    @Value("${plugin.report.coverage.template:templates/reports/test-coverage-report.vm}")
    private String coverageReportTemplate;

    // --- Response message configuration ---

    @Value("${plugin.response.status.created:created}")
    private String responseStatusCreated;

    @Value("${plugin.response.status.updated:updated}")
    private String responseStatusUpdated;

    @Value("${plugin.response.status.deleted:deleted}")
    private String responseStatusDeleted;

    @Value("${plugin.response.placeholder.testName:Sample Test}")
    private String placeholderTestName;

    public PluginConfig() {
        instance = this;
    }

    public static PluginConfig getInstance() {
        return instance;
    }

    // --- Issue type getters ---

    public String getTestIssueType() {
        return testIssueType;
    }

    public List<String> getManagedIssueTypes() {
        return Arrays.asList(managedIssueTypesStr.split(","));
    }

    // --- Report getters ---

    public String getSummaryReportName() {
        return summaryReportName;
    }

    public String getSummaryReportTemplate() {
        return summaryReportTemplate;
    }

    public String getExecutionHistoryReportName() {
        return executionHistoryReportName;
    }

    public String getExecutionHistoryReportTemplate() {
        return executionHistoryReportTemplate;
    }

    public String getCoverageReportName() {
        return coverageReportName;
    }

    public String getCoverageReportTemplate() {
        return coverageReportTemplate;
    }

    // --- Response message getters ---

    public String getResponseStatusCreated() {
        return responseStatusCreated;
    }

    public String getResponseStatusUpdated() {
        return responseStatusUpdated;
    }

    public String getResponseStatusDeleted() {
        return responseStatusDeleted;
    }

    public String getPlaceholderTestName() {
        return placeholderTestName;
    }
}
