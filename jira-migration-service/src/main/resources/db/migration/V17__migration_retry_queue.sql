CREATE TABLE IF NOT EXISTS jira_migration.migration_retry_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_key VARCHAR(255),
    operation VARCHAR(64) NOT NULL,
    payload JSONB,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_retry_queue_job FOREIGN KEY (job_id)
        REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_retry_queue_pending ON jira_migration.migration_retry_queue(status, next_retry_at)
    WHERE status = 'PENDING';
