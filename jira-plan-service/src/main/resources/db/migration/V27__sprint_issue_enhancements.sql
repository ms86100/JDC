-- Add flagged/impediment support to sprint issues
ALTER TABLE jira_plan.sprint_issues ADD COLUMN IF NOT EXISTS flagged BOOLEAN DEFAULT false;
ALTER TABLE jira_plan.sprint_issues ADD COLUMN IF NOT EXISTS flag_reason VARCHAR(500);
