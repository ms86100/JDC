-- Backups
CREATE TABLE IF NOT EXISTS jira_admin.backups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(500),
    file_size BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    initiated_by UUID,
    started_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS jira_admin.backup_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cron_expression VARCHAR(100) DEFAULT '0 0 2 * * ?',
    retention_days INTEGER DEFAULT 30,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Webhooks
CREATE TABLE IF NOT EXISTS jira_admin.webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    url TEXT NOT NULL,
    secret VARCHAR(500),
    events TEXT NOT NULL,
    is_enabled BOOLEAN DEFAULT true,
    jql_filter TEXT,
    exclude_body BOOLEAN DEFAULT false,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_admin.webhook_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID NOT NULL REFERENCES jira_admin.webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(100),
    payload TEXT,
    response_status INTEGER,
    response_body TEXT,
    delivery_status VARCHAR(20) DEFAULT 'PENDING',
    attempt_count INTEGER DEFAULT 0,
    error_message TEXT,
    delivered_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_webhook ON jira_admin.webhook_delivery_logs(webhook_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_status ON jira_admin.webhook_delivery_logs(delivery_status);

-- Priority Schemes
CREATE TABLE IF NOT EXISTS jira_admin.priority_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_admin.priority_scheme_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID NOT NULL REFERENCES jira_admin.priority_schemes(id) ON DELETE CASCADE,
    priority_id VARCHAR(255) NOT NULL,
    position INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_priority_scheme_items_scheme ON jira_admin.priority_scheme_items(scheme_id);

CREATE TABLE IF NOT EXISTS jira_admin.project_priority_schemes (
    project_id UUID NOT NULL,
    scheme_id UUID NOT NULL REFERENCES jira_admin.priority_schemes(id),
    PRIMARY KEY (project_id)
);

-- Application Access
CREATE TABLE IF NOT EXISTS jira_admin.application_access (
    user_id UUID NOT NULL,
    application_key VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP DEFAULT NOW(),
    granted_by UUID,
    PRIMARY KEY (user_id, application_key)
);
