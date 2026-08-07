-- Add visibility restriction columns to worklogs table
ALTER TABLE jira_issue.worklogs ADD COLUMN IF NOT EXISTS visibility VARCHAR(50);
ALTER TABLE jira_issue.worklogs ADD COLUMN IF NOT EXISTS visibility_group_id UUID;
