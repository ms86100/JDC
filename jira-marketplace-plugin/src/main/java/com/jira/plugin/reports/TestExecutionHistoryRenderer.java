package com.jira.plugin.reports;

import com.atlassian.jira.plugin.report.AbstractReport;
import com.atlassian.query.Query;

import java.util.Map;

public class TestExecutionHistoryRenderer extends AbstractReport {

    @Override
    public String getReportName() {
        return "Test Execution History";
    }

    @Override
    public String getReportVelocityURI() {
        return "templates/reports/test-execution-history.vm";
    }

    @Override
    public void init(Map<String, Object> params) {
    }

    @Override
    public String generateReportHtml(com.atlassian.jira.user.ApplicationUser user, Query query) {
        return null;
    }
}