-- Align attachment_metadata with AttachmentMetadataEntity (String id, full column set)

DROP TABLE IF EXISTS jira_migration.attachment_metadata CASCADE;

CREATE TABLE jira_migration.attachment_metadata (
    id VARCHAR(255) PRIMARY KEY,
    issue_id VARCHAR(100) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(500),
    mime_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path TEXT NOT NULL,
    storage_type VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    checksum VARCHAR(128),
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    content_hash VARCHAR(128),
    virus_scan_status VARCHAR(50) NOT NULL DEFAULT 'NOT_SCANNED',
    metadata_json JSONB,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachment_metadata_issue ON jira_migration.attachment_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_attachment_metadata_deleted ON jira_migration.attachment_metadata(deleted);
