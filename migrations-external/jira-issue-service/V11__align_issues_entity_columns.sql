-- Align jira_issue.issues with Issue JPA entity (test-mgmt + navigator fields).
-- Safe to re-run: all ADD COLUMN IF NOT EXISTS.

ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS labels TEXT[] DEFAULT '{}';

ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_type VARCHAR(50) DEFAULT 'MANUAL';
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_status VARCHAR(30) DEFAULT 'DRAFT';
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_priority VARCHAR(20);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_owner_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_steps TEXT;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS requirement_keys TEXT[];
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_feature_key VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_scenario_id VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_set_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_plan_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_execution_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_repository_folder_id UUID;
