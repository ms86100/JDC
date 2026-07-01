package com.jira.plugin.reports;

import com.atlassian.jira.plugin.report.AbstractReport;
import com.atlassian.query.Query;

import java.util.Map;

public class TestCoverageReportRenderer extends AbstractReport {

    @Override
    public String getReportName() {
        return "Test Coverage Report";
    }

    @Override
    public String getReportVelocityURI() {
        return "templates/reports/test-coverage-report.vm";
    }

    @Override
    public void init(Map<String, Object> params) {
    }

    @Override
    public String generateReportHtml(com.atlassian.jira.user.ApplicationUser user, Query query) {
        return null;
    }
}