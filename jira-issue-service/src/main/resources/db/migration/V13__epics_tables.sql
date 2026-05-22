-- Epic management tables (required by EpicController / EpicService)
CREATE TABLE IF NOT EXISTS jira_issue.epics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    description TEXT,
    color VARCHAR(7) DEFAULT '#0052CC',
    lead_id VARCHAR(64),
    lead_name VARCHAR(200),
    status VARCHAR(50) DEFAULT 'OPEN',
    start_date DATE,
    end_date DATE,
    linked_issue_id UUID,
    total_story_points DECIMAL(10, 2) DEFAULT 0,
    completed_story_points DECIMAL(10, 2) DEFAULT 0,
    total_issue_count INTEGER DEFAULT 0,
    completed_issue_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.epic_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epic_id UUID NOT NULL REFERENCES jira_issue.epics(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(64),
    UNIQUE (epic_id, issue_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.epic_progress_history (
    id BIGSERIAL PRIMARY KEY,
    epic_id UUID NOT NULL REFERENCES jira_issue.epics(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    total_points DECIMAL(10, 2) DEFAULT 0,
    completed_points DECIMAL(10, 2) DEFAULT 0,
    total_issues INTEGER DEFAULT 0,
    completed_issues INTEGER DEFAULT 0,
    percent_complete DECIMAL(5, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (epic_id, record_date)
);

CREATE INDEX IF NOT EXISTS idx_epics_lead ON jira_issue.epics(lead_id);
CREATE INDEX IF NOT EXISTS idx_epics_status ON jira_issue.epics(status);
CREATE INDEX IF NOT EXISTS idx_epic_issues_epic ON jira_issue.epic_issues(epic_id);
CREATE INDEX IF NOT EXISTS idx_epic_issues_issue ON jira_issue.epic_issues(issue_id);
CREATE INDEX IF NOT EXISTS idx_epic_progress_epic ON jira_issue.epic_progress_history(epic_id);
CREATE INDEX IF NOT EXISTS idx_epic_progress_date ON jira_issue.epic_progress_history(record_date);
