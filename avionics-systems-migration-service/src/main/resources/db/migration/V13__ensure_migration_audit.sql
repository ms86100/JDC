CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Repair: Flyway baseline may have skipped V1 objects while history shows v12.
-- Ensures migration_audit exists for MigrationAuditEntry JPA validation.


CREATE TABLE IF NOT EXISTS jira_migration.migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_key VARCHAR(255),
    details JSONB,
    performed_by UUID,
    performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_migration_audit_job_id
    ON jira_migration.migration_audit(job_id);

CREATE INDEX IF NOT EXISTS idx_migration_audit_performed_at
    ON jira_migration.migration_audit(performed_at DESC);

COMMENT ON TABLE jira_migration.migration_audit IS 'Audit trail for migration operations';
