package com.jira.plugin.reports;

import com.atlassian.jira.plugin.report.AbstractReport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;

import java.util.Map;

public class TestSummaryReportRenderer extends AbstractReport {

    @Override
    public String getReportName() {
        return "Test Summary Report";
    }

    @Override
    public String getReportVelocityURI() {
        return "templates/reports/test-summary-report.vm";
    }

    @Override
    public void init(Map<String, Object> params) {
        // Initialize report parameters
    }

    @Override
    public String generateReportHtml(com.atlassian.jira.user.ApplicationUser user, Query query) {
        return null;
    }
}