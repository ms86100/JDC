-- V5: Add version column for optimistic locking on projects
-- This enables concurrent modification detection to prevent data loss

ALTER TABLE jira_project.projects
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Add index for potential future use (version-based queries)
CREATE INDEX IF NOT EXISTS idx_projects_version ON jira_project.projects(version);