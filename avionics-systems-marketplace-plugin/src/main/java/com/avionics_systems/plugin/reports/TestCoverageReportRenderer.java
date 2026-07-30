package com.avionics_systems.plugin.reports;

import com.atlassian.jira.plugin.report.AbstractReport;
import com.atlassian.query.Query;
import com.avionics_systems.plugin.config.PluginConfig;

import java.util.Map;

public class TestCoverageReportRenderer extends AbstractReport {

    @Override
    public String getReportName() {
        PluginConfig config = PluginConfig.getInstance();
        return config != null ? config.getCoverageReportName() : "Test Coverage Report";
    }

    @Override
    public String getReportVelocityURI() {
        PluginConfig config = PluginConfig.getInstance();
        return config != null ? config.getCoverageReportTemplate() : "templates/reports/test-coverage-report.vm";
    }

    @Override
    public void init(Map<String, Object> params) {
    }

    @Override
    public String generateReportHtml(com.atlassian.jira.user.ApplicationUser user, Query query) {
        return null;
    }
}
