-- V7: Add issue type key and enhanced fields to issue_types table
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS issue_type_key VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS is_subtask BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Populate issue_type_key from name (lowercase, replace spaces with hyphens)
UPDATE jira_issue.issue_types SET issue_type_key = LOWER(REPLACE(name, ' ', '-')) WHERE issue_type_key IS NULL;

-- Make issue_type_key unique and not null
ALTER TABLE jira_issue.issue_types ALTER COLUMN issue_type_key SET NOT NULL;
ALTER TABLE jira_issue.issue_types ALTER COLUMN issue_type_key SET DEFAULT LOWER(REPLACE(name, ' ', '-'));

-- Add unique constraint
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_issue_type_key') THEN
        ALTER TABLE jira_issue.issue_types ADD CONSTRAINT uk_issue_type_key UNIQUE (issue_type_key);
    END IF;
END
$$;

-- Seed default issue types if table is empty
INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Bug', 'bug', 'bug-icon', 'A bug or issue in the system', false, 1, '#d73a49'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'bug');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Story', 'story', 'story-icon', 'A user story or feature', false, 2, '#006644'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'story');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Task', 'task', 'task-icon', 'A task or work item', false, 3, '#0052cc'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'task');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Epic', 'epic', 'epic-icon', 'An epic or large feature', false, 4, '#6b2db0'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'epic');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Sub-task', 'sub-task', 'subtask-icon', 'A subtask of a parent issue', true, 5, '#8d919a'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'sub-task');