CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Sprint Service Database Schema (schema: jira_sprint)
CREATE SCHEMA IF NOT EXISTS jira_sprint;

CREATE TABLE IF NOT EXISTS jira_sprint.sprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    project_id UUID NOT NULL,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_sprint.sprint_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    order_index INTEGER DEFAULT 0,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sprints_project_id ON jira_sprint.sprints(project_id);
CREATE INDEX IF NOT EXISTS idx_sprints_status ON jira_sprint.sprints(status);
CREATE INDEX IF NOT EXISTS idx_sprint_issues_sprint_id ON jira_sprint.sprint_issues(sprint_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sprint_issues_unique ON jira_sprint.sprint_issues(sprint_id, issue_id);
