-- Add LDAP columns to directories table
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS server_url VARCHAR(500);
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS base_dn VARCHAR(500);
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS bind_dn VARCHAR(500);
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS encrypted_bind_password TEXT;
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS user_search_base VARCHAR(500);
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS user_search_filter VARCHAR(500) DEFAULT '(objectClass=person)';
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS group_search_base VARCHAR(500);
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS group_search_filter VARCHAR(500) DEFAULT '(objectClass=group)';
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS sync_interval_minutes INTEGER DEFAULT 60;
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS last_sync_at TIMESTAMP;
ALTER TABLE jira_admin.directories ADD COLUMN IF NOT EXISTS sync_status VARCHAR(20) DEFAULT 'IDLE';

-- Sync logs table
CREATE TABLE IF NOT EXISTS jira_user.directory_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    directory_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    users_added INTEGER DEFAULT 0,
    users_updated INTEGER DEFAULT 0,
    users_removed INTEGER DEFAULT 0,
    groups_synced INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'RUNNING',
    errors TEXT,
    CONSTRAINT fk_sync_log_directory FOREIGN KEY (directory_id) REFERENCES jira_admin.directories(id)
);

CREATE INDEX IF NOT EXISTS idx_sync_logs_directory ON jira_user.directory_sync_logs(directory_id);
CREATE INDEX IF NOT EXISTS idx_sync_logs_started ON jira_user.directory_sync_logs(started_at DESC);
