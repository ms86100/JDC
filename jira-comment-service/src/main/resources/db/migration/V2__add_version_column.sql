-- V2: Add version column for optimistic locking on comments
-- This enables concurrent modification detection

ALTER TABLE jira_comment.comments
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;