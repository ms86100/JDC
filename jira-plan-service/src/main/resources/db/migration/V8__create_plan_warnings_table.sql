-- V8__create_plan_warnings_table.sql
-- Warning notifications for Plan issues

CREATE TABLE jira_plan.plan_warnings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    issue_key VARCHAR(50),
    warning_type VARCHAR(50) NOT NULL,
    message TEXT,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    dismissed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plan_warnings_plan_id ON jira_plan.plan_warnings(plan_id);
CREATE INDEX idx_plan_warnings_active ON jira_plan.plan_warnings(plan_id, is_active);
CREATE INDEX idx_plan_warnings_issue_id ON jira_plan.plan_warnings(issue_id);
CREATE INDEX idx_plan_warnings_type ON jira_plan.plan_warnings(warning_type);
CREATE INDEX idx_plan_warnings_severity ON jira_plan.plan_warnings(severity);

-- Comments
COMMENT ON TABLE jira_plan.plan_warnings IS 'Warning notifications for Plan issues';
COMMENT ON COLUMN jira_plan.plan_warnings.warning_type IS 'TARGET_DATE_BEYOND_DUE, ISSUE_DATE_MISSING, etc.';
COMMENT ON COLUMN jira_plan.plan_warnings.severity IS 'INFO, WARNING, ERROR';
COMMENT ON COLUMN jira_plan.plan_warnings.dismissed_at IS 'When the warning was dismissed by a user';