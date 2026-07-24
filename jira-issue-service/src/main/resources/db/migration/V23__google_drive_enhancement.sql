-- V23__google_drive_enhancement.sql
-- Enhance external_page_links with provider type, file metadata

ALTER TABLE jira_issue.external_page_links ADD COLUMN IF NOT EXISTS link_provider VARCHAR(30) DEFAULT 'GENERIC';
-- Values: GENERIC, GOOGLE_DRIVE, CONFLUENCE, SHAREPOINT

ALTER TABLE jira_issue.external_page_links ADD COLUMN IF NOT EXISTS file_type VARCHAR(50);

ALTER TABLE jira_issue.external_page_links ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_page_links_provider ON jira_issue.external_page_links(link_provider);
