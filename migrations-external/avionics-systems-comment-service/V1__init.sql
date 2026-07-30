-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;

-- V1__init.sql - Initial schema for jira-comment-service
-- Schema: jira_comment

-- Create extension for UUID generation if not exists

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_comment;

-- Comments table
CREATE TABLE jira_comment.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_comment_id UUID REFERENCES jira_comment.comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_comments_issue_id ON jira_comment.comments(issue_id);
CREATE INDEX idx_comments_user_id ON jira_comment.comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON jira_comment.comments(parent_comment_id);
CREATE INDEX idx_comments_issue_id_deleted ON jira_comment.comments(issue_id, deleted);
CREATE INDEX idx_comments_created_at ON jira_comment.comments(created_at DESC);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION jira_comment.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for comments updated_at
CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON jira_comment.comments
    FOR EACH ROW
    EXECUTE FUNCTION jira_comment.update_updated_at_column();