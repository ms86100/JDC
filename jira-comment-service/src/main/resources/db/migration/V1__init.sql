-- V1__init.sql - Initial schema for jira-comment-service
-- Schema: jira_comment

-- Create extension for UUID generation if not exists
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_comment;

-- Comments table
CREATE TABLE jira_comment.comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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