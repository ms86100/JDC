-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_issue;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE jira_issue.issue_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issue_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    color VARCHAR(20),
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issue_statuses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    sequence INT NOT NULL DEFAULT 0,
    category VARCHAR(20) NOT NULL DEFAULT 'TODO',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status UUID NOT NULL REFERENCES jira_issue.issue_statuses(id),
    priority UUID REFERENCES jira_issue.issue_priorities(id),
    issue_type UUID NOT NULL REFERENCES jira_issue.issue_types(id),
    reporter_id UUID,
    assignee_id UUID,
    parent_issue_id UUID REFERENCES jira_issue.issues(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_issues_project ON jira_issue.issues(project_id);
CREATE INDEX idx_issues_key ON jira_issue.issues(issue_key);
CREATE INDEX idx_issues_status ON jira_issue.issues(status);
CREATE INDEX idx_issues_assignee ON jira_issue.issues(assignee_id);

-- Seed default values
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'Bug', 'bug', 'A bug report'),
  ('a0000000-0000-0000-0000-000000000002', 'Story', 'book', 'A user story'),
  ('a0000000-0000-0000-0000-000000000003', 'Task', 'task', 'A task'),
  ('a0000000-0000-0000-0000-000000000004', 'Epic', 'lightning', 'An epic');

INSERT INTO jira_issue.issue_priorities (id, name, icon, color, sequence) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'Highest', 'arrow-up', '#ff0000', 1),
  ('b0000000-0000-0000-0000-000000000002', 'High', 'arrow-up', '#ff6600', 2),
  ('b0000000-0000-0000-0000-000000000003', 'Medium', 'minus', '#ffcc00', 3),
  ('b0000000-0000-0000-0000-000000000004', 'Low', 'arrow-down', '#0099ff', 4),
  ('b0000000-0000-0000-0000-000000000005', 'Lowest', 'arrow-down', '#99cc00', 5);

INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
  ('c0000000-0000-0000-0000-000000000001', 'To Do', 1, 'TODO'),
  ('c0000000-0000-0000-0000-000000000002', 'In Progress', 2, 'IN_PROGRESS'),
  ('c0000000-0000-0000-0000-000000000003', 'In Review', 3, 'IN_PROGRESS'),
  ('c0000000-0000-0000-0000-000000000004', 'Done', 4, 'DONE');