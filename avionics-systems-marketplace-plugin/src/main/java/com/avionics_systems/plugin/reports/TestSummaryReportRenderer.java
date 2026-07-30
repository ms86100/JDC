package com.avionics_systems.plugin.reports;

import com.atlassian.jira.plugin.report.AbstractReport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.avionics_systems.plugin.config.PluginConfig;

import java.util.Map;

public class TestSummaryReportRenderer extends AbstractReport {

    @Override
    public String getReportName() {
        PluginConfig config = PluginConfig.getInstance();
        return config != null ? config.getSummaryReportName() : "Test Summary Report";
    }

    @Override
    public String getReportVelocityURI() {
        PluginConfig config = PluginConfig.getInstance();
        return config != null ? config.getSummaryReportTemplate() : "templates/reports/test-summary-report.vm";
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
