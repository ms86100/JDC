CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Repair: Flyway history advanced without V1 DDL (baseline). Idempotent V1 core tables.

CREATE SCHEMA IF NOT EXISTS jira_migration;

CREATE TABLE IF NOT EXISTS jira_migration.migration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(20) NOT NULL,
    job_status VARCHAR(20) NOT NULL,
    import_source VARCHAR(50),
    total_entities INT DEFAULT 0,
    processed_entities INT DEFAULT 0,
    failed_entities INT DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0,
    config JSONB,
    options JSONB,
    initiated_by UUID,
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    error_details JSONB,
    source_project_id UUID,
    target_project_id UUID,
    file_path VARCHAR,
    can_rollback BOOLEAN DEFAULT FALSE,
    rollback_job_id UUID,
    result_metadata JSONB,
    CONSTRAINT valid_job_type CHECK (job_type IN ('IMPORT', 'EXPORT')),
    CONSTRAINT valid_job_status CHECK (job_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS jira_migration.entity_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_key VARCHAR(255),
    entity_id UUID,
    status VARCHAR(20) NOT NULL,
    processing_order INT,
    error_code VARCHAR(50),
    error_message TEXT,
    error_row INT,
    error_field VARCHAR(100),
    error_context JSONB,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms INT,
    validation_errors JSONB,
    warnings JSONB,
    CONSTRAINT valid_entity_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

CREATE TABLE IF NOT EXISTS jira_migration.csv_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    version VARCHAR(20) DEFAULT '1.0',
    columns JSONB NOT NULL,
    header_row INT DEFAULT 1,
    data_start_row INT DEFAULT 2,
    validation_rules JSONB,
    custom_validators JSONB,
    field_mapping JSONB,
    supports_bulk_import BOOLEAN DEFAULT TRUE,
    max_rows_per_file INT DEFAULT 50000,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT unique_template_version UNIQUE (template_name, version)
);

CREATE TABLE IF NOT EXISTS jira_migration.field_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mapping_name VARCHAR(100) NOT NULL,
    mapping_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    mappings JSONB NOT NULL,
    sample_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    is_shared BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS jira_migration.validation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    rule_type VARCHAR(30) NOT NULL,
    rule_config JSONB NOT NULL,
    error_message_template VARCHAR(500),
    severity VARCHAR(10) DEFAULT 'ERROR',
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.backup_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_key VARCHAR(255),
    entity_data JSONB NOT NULL,
    dependencies JSONB,
    parent_key VARCHAR(255),
    sequence_order INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.user_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    source_identifier VARCHAR(255) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    target_user_id UUID,
    target_username VARCHAR(150),
    target_email VARCHAR(255),
    mapping_type VARCHAR(20) NOT NULL,
    confidence_score DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.project_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    source_key VARCHAR(10) NOT NULL,
    target_key VARCHAR(10),
    target_id UUID,
    issue_key_sequence INT DEFAULT 0,
    component_mappings JSONB,
    version_mappings JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_migration_jobs_status ON jira_migration.migration_jobs(job_status);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_type ON jira_migration.migration_jobs(job_type);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_initiated_by ON jira_migration.migration_jobs(initiated_by);
CREATE INDEX IF NOT EXISTS idx_migration_jobs_initiated_at ON jira_migration.migration_jobs(initiated_at DESC);
CREATE INDEX IF NOT EXISTS idx_entity_status_job ON jira_migration.entity_status(job_id);
CREATE INDEX IF NOT EXISTS idx_entity_status_type ON jira_migration.entity_status(entity_type);
CREATE INDEX IF NOT EXISTS idx_entity_status_status ON jira_migration.entity_status(status);
CREATE INDEX IF NOT EXISTS idx_csv_templates_entity_type ON jira_migration.csv_templates(entity_type);
CREATE INDEX IF NOT EXISTS idx_field_mappings_type ON jira_migration.field_mappings(mapping_type);
CREATE INDEX IF NOT EXISTS idx_validation_rules_entity_type ON jira_migration.validation_rules(entity_type);
CREATE INDEX IF NOT EXISTS idx_backup_entities_backup_id ON jira_migration.backup_entities(backup_id);
CREATE INDEX IF NOT EXISTS idx_user_mappings_job_id ON jira_migration.user_mappings(job_id);
CREATE INDEX IF NOT EXISTS idx_project_mappings_job_id ON jira_migration.project_mappings(job_id);
