CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Outbox table for migration domain events (OS-P1-A-02)
CREATE TABLE IF NOT EXISTS jira_migration.migration_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    error_message TEXT,
    CONSTRAINT fk_migration_events_job FOREIGN KEY (job_id)
        REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_migration_events_job ON jira_migration.migration_events(job_id);
CREATE INDEX IF NOT EXISTS idx_migration_events_status ON jira_migration.migration_events(status);
CREATE INDEX IF NOT EXISTS idx_migration_events_pending ON jira_migration.migration_events(status, created_at)
    WHERE status = 'PENDING';

COMMENT ON TABLE jira_migration.migration_events IS 'Outbox for post-migration events (reindex, notifications, etc.)';
