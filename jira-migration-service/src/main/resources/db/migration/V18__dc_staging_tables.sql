-- Jira DC XML import staging (normalized import pipeline)

CREATE TABLE IF NOT EXISTS jira_migration.dc_staging_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    import_batch_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100),
    source_key VARCHAR(255),
    validation_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    checksum VARCHAR(128),
    raw_xml TEXT,
    parsed_payload JSONB,
    sequence_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dc_staging_job ON jira_migration.dc_staging_entries(job_id);
CREATE INDEX IF NOT EXISTS idx_dc_staging_batch ON jira_migration.dc_staging_entries(import_batch_id);
CREATE INDEX IF NOT EXISTS idx_dc_staging_type ON jira_migration.dc_staging_entries(job_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_dc_staging_key ON jira_migration.dc_staging_entries(job_id, source_key);
CREATE INDEX IF NOT EXISTS idx_dc_staging_validation ON jira_migration.dc_staging_entries(job_id, validation_state);

CREATE TABLE IF NOT EXISTS jira_migration.dc_unknown_custom_fields (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    field_id VARCHAR(100) NOT NULL,
    field_name VARCHAR(255),
    sample_value TEXT,
    detected_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (job_id, field_id)
);

CREATE INDEX IF NOT EXISTS idx_dc_unknown_cf_job ON jira_migration.dc_unknown_custom_fields(job_id);
