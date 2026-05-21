CREATE TABLE IF NOT EXISTS jira_issue.issue_event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(64) NOT NULL,
    issue_id UUID NOT NULL,
    project_id UUID,
    payload TEXT,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_issue_event_outbox_unpublished
    ON jira_issue.issue_event_outbox (published, created_at);
