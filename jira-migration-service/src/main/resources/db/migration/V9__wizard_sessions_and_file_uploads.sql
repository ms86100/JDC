-- Wizard session persistence (Phase 2)
-- Links multi-step import wizard state to optional migration_jobs

CREATE TABLE IF NOT EXISTS jira_migration.wizard_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    current_step VARCHAR(50) NOT NULL DEFAULT 'UPLOAD',
    import_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    target_project_id UUID,
    migration_job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE SET NULL,
    initiated_by UUID,
    file_name VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    detected_headers JSONB,
    detected_entity_type VARCHAR(50),
    attachment_column VARCHAR(100),
    parent_column VARCHAR(100),
    epic_column VARCHAR(100),
    total_rows INT DEFAULT 0,
    validation_result JSONB,
    field_mappings JSONB,
    user_mappings JSONB,
    import_options JSONB,
    session_data JSONB,
    preview_rows JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    CONSTRAINT valid_wizard_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT valid_wizard_step CHECK (current_step IN (
        'UPLOAD', 'VALIDATE', 'MAP_FIELDS', 'MAP_USERS', 'CONFIGURE', 'REVIEW', 'EXECUTE', 'COMPLETED'
    ))
);

CREATE TABLE IF NOT EXISTS jira_migration.migration_file_uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wizard_session_id UUID NOT NULL REFERENCES jira_migration.wizard_sessions(id) ON DELETE CASCADE,
    migration_job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE SET NULL,
    file_name TEXT NOT NULL,
    mime_type VARCHAR(255),
    file_size BIGINT NOT NULL,
    checksum VARCHAR(128),
    file_content BYTEA,
    storage_path TEXT,
    virus_scan_status VARCHAR(30) DEFAULT 'PENDING',
    parse_status VARCHAR(30) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_virus_scan CHECK (virus_scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'SKIPPED')),
    CONSTRAINT valid_parse_status CHECK (parse_status IN ('PENDING', 'PARSED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_wizard_sessions_user ON jira_migration.wizard_sessions(initiated_by);
CREATE INDEX IF NOT EXISTS idx_wizard_sessions_status ON jira_migration.wizard_sessions(status);
CREATE INDEX IF NOT EXISTS idx_wizard_sessions_job ON jira_migration.wizard_sessions(migration_job_id);
CREATE INDEX IF NOT EXISTS idx_file_uploads_session ON jira_migration.migration_file_uploads(wizard_session_id);
