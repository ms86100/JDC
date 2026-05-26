-- V1__init.sql
-- Report Service Database Schema (schema: jira_report)

CREATE SCHEMA IF NOT EXISTS jira_report;

-- ============================================
-- REPORT DEFINITIONS TABLE
-- Saved report configurations
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(100) NOT NULL, -- BURNDOWN, VELOCITY, CUMULATIVE_FLOW, ISSUE_COUNT, CUSTOM
    query_config JSONB NOT NULL,
    chart_config JSONB,
    table_config JSONB,
    owner_id UUID NOT NULL,
    project_id UUID,
    is_shared BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    schedule_config JSONB,
    last_generated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REPORT RESULTS TABLE
-- Cached report results
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.report_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES jira_report.reports(id) ON DELETE CASCADE,
    execution_date TIMESTAMP NOT NULL,
    result_data JSONB NOT NULL,
    execution_time_ms INTEGER,
    record_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REPORT SHARES TABLE
-- Sharing configuration for reports
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.report_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES jira_report.reports(id) ON DELETE CASCADE,
    shared_with_user_id UUID,
    shared_with_group_id UUID,
    permission_level VARCHAR(50) DEFAULT 'VIEW', -- VIEW, EDIT
    share_token VARCHAR(255),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REPORT SUBSCRIPTIONS TABLE
-- Email subscriptions for reports
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.report_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES jira_report.reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    frequency VARCHAR(50) NOT NULL, -- DAILY, WEEKLY, MONTHLY
    schedule_time TIME,
    schedule_day VARCHAR(20),
    format VARCHAR(20) DEFAULT 'PDF', -- PDF, CSV, HTML, EXCEL
    recipients TEXT[],
    is_active BOOLEAN DEFAULT TRUE,
    last_sent_at TIMESTAMP,
    next_send_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REPORT SCHEDULES TABLE
-- Scheduled report generation
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.report_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES jira_report.reports(id) ON DELETE CASCADE,
    cron_expression VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'UTC',
    is_active BOOLEAN DEFAULT TRUE,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REPORT EXECUTIONS TABLE
-- Historical report execution log
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.report_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES jira_report.reports(id) ON DELETE CASCADE,
    execution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_status VARCHAR(50) DEFAULT 'SUCCESS', -- SUCCESS, FAILED, TIMEOUT
    execution_time_ms INTEGER,
    result_count INTEGER,
    error_message TEXT,
    triggered_by VARCHAR(50) DEFAULT 'SCHEDULE', -- SCHEDULE, MANUAL, API
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CHART TEMPLATES TABLE
-- Reusable chart configurations
-- ============================================
CREATE TABLE IF NOT EXISTS jira_report.chart_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(255) NOT NULL,
    chart_type VARCHAR(50) NOT NULL, -- BAR, LINE, PIE, AREA, SCATTER
    default_config JSONB,
    customization_options JSONB,
    is_system BOOLEAN DEFAULT FALSE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_reports_owner ON jira_report.reports(owner_id);
CREATE INDEX IF NOT EXISTS idx_reports_project ON jira_report.reports(project_id);
CREATE INDEX IF NOT EXISTS idx_reports_type ON jira_report.reports(report_type);
CREATE INDEX IF NOT EXISTS idx_reports_favorite ON jira_report.reports(is_favorite);
CREATE INDEX IF NOT EXISTS idx_report_results_report ON jira_report.report_results(report_id);
CREATE INDEX IF NOT EXISTS idx_report_results_date ON jira_report.report_results(execution_date);
CREATE INDEX IF NOT EXISTS idx_report_shares_report ON jira_report.report_shares(report_id);
CREATE INDEX IF NOT EXISTS idx_report_subscriptions_report ON jira_report.report_subscriptions(report_id);
CREATE INDEX IF NOT EXISTS idx_report_subscriptions_user ON jira_report.report_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_report_executions_report ON jira_report.report_executions(report_id);

-- ============================================
-- SEED DATA: Default Chart Templates
-- ============================================
INSERT INTO jira_report.chart_templates (id, template_name, chart_type, default_config, is_system) VALUES
    (gen_random_uuid(), 'Sprint Burndown', 'LINE', '{"showLegend": true, "showGrid": true}', true),
    (gen_random_uuid(), 'Issue Count by Status', 'BAR', '{"horizontal": true, "showValues": true}', true),
    (gen_random_uuid(), 'Cumulative Flow', 'AREA', '{"stacked": true, "showLegend": true}', true),
    (gen_random_uuid(), 'Velocity Chart', 'BAR', '{"showTrend": true}', true)
ON CONFLICT DO NOTHING;