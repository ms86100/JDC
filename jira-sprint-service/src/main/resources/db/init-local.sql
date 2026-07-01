-- Local dev: ensure schema and add missing columns
CREATE SCHEMA IF NOT EXISTS jira_sprint;

-- Ensure tables exist (Hibernate ddl-auto:update will create them, but if some are missing)
-- Add any missing columns that entities expect but the DB doesn't have

-- Sprints
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS goal TEXT;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS start_date TIMESTAMP;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS end_date TIMESTAMP;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'PLANNED';
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS project_id UUID;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Agile boards
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'SCRUM';
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Board sprints
ALTER TABLE jira_sprint.board_sprints ADD COLUMN IF NOT EXISTS position INTEGER DEFAULT 0;
