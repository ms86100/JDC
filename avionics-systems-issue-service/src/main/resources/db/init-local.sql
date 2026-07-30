-- Local dev: add columns that Hibernate ddl-auto:update may have missed
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS issue_type_key VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS is_subtask BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS icon VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Issue priorities
ALTER TABLE jira_issue.issue_priorities ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_priorities ADD COLUMN IF NOT EXISTS icon VARCHAR(50);
ALTER TABLE jira_issue.issue_priorities ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_priorities ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Issue statuses
ALTER TABLE jira_issue.issue_statuses ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_statuses ADD COLUMN IF NOT EXISTS icon VARCHAR(50);
ALTER TABLE jira_issue.issue_statuses ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_statuses ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE jira_issue.issue_statuses ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Resolutions
ALTER TABLE jira_issue.resolutions ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE jira_issue.resolutions ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
