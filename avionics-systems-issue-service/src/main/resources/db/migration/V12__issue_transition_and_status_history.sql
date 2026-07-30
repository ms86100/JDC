CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- V12: Per-issue workflow transition and status history (Jira DC parity)

CREATE TABLE IF NOT EXISTS jira_issue.issue_transition_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    project_id UUID,
    workflow_id UUID,
    transition_id UUID,
    transition_name VARCHAR(255),
    from_status_id UUID,
    to_status_id UUID,
    user_id UUID,
    comment TEXT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_issue_transition_history_issue
    ON jira_issue.issue_transition_history(issue_id, executed_at DESC);

CREATE TABLE IF NOT EXISTS jira_issue.issue_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    from_status_id UUID,
    to_status_id UUID NOT NULL,
    transition_id UUID,
    user_id UUID,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_issue_status_history_issue
    ON jira_issue.issue_status_history(issue_id, changed_at DESC);
