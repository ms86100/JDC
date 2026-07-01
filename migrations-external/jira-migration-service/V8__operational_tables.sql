-- Operational tables previously created only via Hibernate ddl-auto

CREATE TABLE IF NOT EXISTS jira_migration.job_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    node_id VARCHAR(100) NOT NULL,
    claimed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    UNIQUE (job_id, node_id)
);

CREATE TABLE IF NOT EXISTS jira_migration.cluster_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id VARCHAR(100) NOT NULL UNIQUE,
    host VARCHAR(255),
    port INT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    last_heartbeat TIMESTAMP,
    registered_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_migration.leader_elections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lock_name VARCHAR(100) NOT NULL UNIQUE,
    leader_node_id VARCHAR(100),
    acquired_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_migration.distributed_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lock_key VARCHAR(255) NOT NULL UNIQUE,
    owner_node_id VARCHAR(100),
    acquired_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_migration.attachment_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,
    issue_id VARCHAR(100),
    file_name TEXT,
    mime_type VARCHAR(255),
    file_size BIGINT,
    checksum VARCHAR(128),
    storage_path TEXT,
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_job_claims_job ON jira_migration.job_claims(job_id);
CREATE INDEX IF NOT EXISTS idx_attachment_metadata_job ON jira_migration.attachment_metadata(job_id);
