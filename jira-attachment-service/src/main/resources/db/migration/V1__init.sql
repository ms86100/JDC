-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_attachment;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE jira_attachment.attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR NOT NULL,
    uploader_id UUID,
    uploader_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_issue ON jira_attachment.attachments(issue_id);
CREATE INDEX idx_attachments_uploader ON jira_attachment.attachments(uploader_id);
CREATE INDEX idx_attachments_created ON jira_attachment.attachments(created_at DESC);
