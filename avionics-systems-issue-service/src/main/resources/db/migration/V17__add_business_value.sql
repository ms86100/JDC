-- Task 3.1: Add business_value field for board estimation statistic
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS business_value INTEGER;
