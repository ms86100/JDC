CREATE TABLE IF NOT EXISTS jira_sprint.saved_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    jql TEXT,
    owner_id UUID,
    is_shared BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    share_type VARCHAR(32) DEFAULT 'PRIVATE',
    is_system BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    last_used TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
