CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Phase 3: Field mapping engine extensions (option matrix, workflow/status maps, defaults)

ALTER TABLE jira_migration.migration_jobs
    ADD COLUMN IF NOT EXISTS option_mappings JSONB,
    ADD COLUMN IF NOT EXISTS workflow_status_mappings JSONB,
    ADD COLUMN IF NOT EXISTS field_defaults JSONB;

ALTER TABLE jira_migration.wizard_sessions
    ADD COLUMN IF NOT EXISTS option_mappings JSONB,
    ADD COLUMN IF NOT EXISTS workflow_status_mappings JSONB,
    ADD COLUMN IF NOT EXISTS field_defaults JSONB;

CREATE TABLE IF NOT EXISTS jira_migration.option_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    wizard_session_id UUID REFERENCES jira_migration.wizard_sessions(id) ON DELETE CASCADE,
    source_field_key VARCHAR(255) NOT NULL,
    source_option_value VARCHAR(500) NOT NULL,
    target_field_key VARCHAR(255) NOT NULL,
    target_option_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT option_mapping_owner CHECK (job_id IS NOT NULL OR wizard_session_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_option_mappings_job ON jira_migration.option_mappings(job_id);
CREATE INDEX IF NOT EXISTS idx_option_mappings_session ON jira_migration.option_mappings(wizard_session_id);
CREATE INDEX IF NOT EXISTS idx_option_mappings_field ON jira_migration.option_mappings(source_field_key, target_field_key);
