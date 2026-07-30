CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Spec result tables + validation_rules/migration_audit already in V1; add per-row stores

CREATE TABLE IF NOT EXISTS jira_migration.migration_validation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    wizard_session_id UUID REFERENCES jira_migration.wizard_sessions(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_key VARCHAR(255),
    severity VARCHAR(10) NOT NULL DEFAULT 'ERROR',
    field_name VARCHAR(100),
    error_code VARCHAR(50),
    message TEXT NOT NULL,
    row_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.migration_issue_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    source_issue_key VARCHAR(255),
    target_issue_id UUID,
    target_issue_key VARCHAR(50),
    row_number INT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.migration_attachment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    source_issue_key VARCHAR(255),
    target_issue_id UUID,
    file_name TEXT,
    checksum VARCHAR(128),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_validation_results_job ON jira_migration.migration_validation_results(job_id);
CREATE INDEX IF NOT EXISTS idx_validation_results_session ON jira_migration.migration_validation_results(wizard_session_id);
CREATE INDEX IF NOT EXISTS idx_issue_results_job ON jira_migration.migration_issue_results(job_id);
CREATE INDEX IF NOT EXISTS idx_attachment_results_job ON jira_migration.migration_attachment_results(job_id);
