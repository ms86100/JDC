-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_attachment;

-- Enable UUID extension

CREATE TABLE jira_attachment.attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
