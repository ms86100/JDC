-- Align DB columns with JPA entities (migration_jobs, entity_status, leader_elections)

ALTER TABLE jira_migration.migration_jobs
    ADD COLUMN IF NOT EXISTS optimistic_lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE jira_migration.entity_status
    ADD COLUMN IF NOT EXISTS optimistic_lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE jira_migration.entity_status
    ADD COLUMN IF NOT EXISTS source_identifier VARCHAR(255);

ALTER TABLE jira_migration.entity_status
    ADD COLUMN IF NOT EXISTS target_id VARCHAR(64);

ALTER TABLE jira_migration.entity_status
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

ALTER TABLE jira_migration.entity_status
    ADD COLUMN IF NOT EXISTS warnings JSONB;

DROP TABLE IF EXISTS jira_migration.leader_elections CASCADE;

CREATE TABLE jira_migration.leader_elections (
    leadership_group VARCHAR(255) PRIMARY KEY,
    leader_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    host VARCHAR(255),
    port INT,
    elected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_heartbeat TIMESTAMP NOT NULL DEFAULT NOW(),
    lease_expires_at TIMESTAMP NOT NULL DEFAULT NOW(),
    term BIGINT NOT NULL DEFAULT 1,
    votes INT NOT NULL DEFAULT 1,
    metadata JSONB
);
