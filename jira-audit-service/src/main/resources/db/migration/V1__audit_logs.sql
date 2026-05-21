CREATE SCHEMA IF NOT EXISTS jira_audit;

CREATE TABLE IF NOT EXISTS jira_audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    service_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    changes JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON jira_audit.audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON jira_audit.audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON jira_audit.audit_logs (created_at DESC);
