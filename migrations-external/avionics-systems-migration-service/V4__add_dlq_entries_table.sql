-- V4__add_dlq_entries_table.sql
-- Persistent Dead Letter Queue for failed migration operations

-- ========================================================================
-- DLQ Entries Table
-- ========================================================================

CREATE TABLE jira_migration.dlq_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_key VARCHAR(255),

    payload TEXT,
    error_message TEXT,
    error_stack_trace TEXT,

    attempt_count INT DEFAULT 0,
    first_failure TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt TIMESTAMP,
    next_retry TIMESTAMP,

    status VARCHAR(20) DEFAULT 'PENDING',
    last_error TEXT,

    metadata JSONB,
    job_id UUID,
    source_system VARCHAR(100),

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    resolution VARCHAR(255),

    optimistic_lock_version BIGINT DEFAULT 0,

    CONSTRAINT chk_dlq_status CHECK (status IN ('PENDING', 'SCHEDULED', 'RETRYING', 'COMPLETED', 'FAILED', 'DISCARDED'))
);

-- ========================================================================
-- Indexes for DLQ Operations
-- ========================================================================

-- Primary query pattern: find pending entries for retry
CREATE INDEX idx_dlq_status_first_failure ON jira_migration.dlq_entries(status, first_failure)
    WHERE status IN ('PENDING', 'SCHEDULED');

-- Job-scoped queries for retry
CREATE INDEX idx_dlq_job_id ON jira_migration.dlq_entries(job_id)
    WHERE job_id IS NOT NULL;

-- Entity lookup for dedupe
CREATE INDEX idx_dlq_entity_lookup ON jira_migration.dlq_entries(entity_type, entity_key)
    WHERE entity_key IS NOT NULL;

-- Scheduled retry queries
CREATE INDEX idx_dlq_next_retry ON jira_migration.dlq_entries(next_retry)
    WHERE status = 'SCHEDULED';

-- Operation type analytics
CREATE INDEX idx_dlq_operation_type ON jira_migration.dlq_entries(operation_type)
    WHERE status NOT IN ('COMPLETED', 'DISCARDED');

-- Cleanup index for retention policy
CREATE INDEX idx_dlq_resolved_at ON jira_migration.dlq_entries(resolved_at)
    WHERE resolved_at IS NOT NULL;

-- ========================================================================
-- Log DLQ table creation
-- ========================================================================

DO $$
BEGIN
    RAISE NOTICE 'DLQ Entries table created successfully';
    RAISE NOTICE '  - Persistent storage for failed migration operations';
    RAISE NOTICE '  - Supports retry scheduling and resolution tracking';
    RAISE NOTICE '  - Includes optimistic locking for concurrent access';
END $$;