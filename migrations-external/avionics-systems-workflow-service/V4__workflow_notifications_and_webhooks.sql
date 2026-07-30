-- V4__workflow_notifications_and_webhooks.sql
-- Workflow Service - Notifications and webhook triggers

CREATE SCHEMA IF NOT EXISTS jira_workflow;

-- ============================================
-- WORKFLOW NOTIFICATIONS TABLE
-- Configure notifications for workflow events
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    trigger_event VARCHAR(50) NOT NULL, -- TRANSITION, STATUS_CHANGE, ASSIGNMENT
    trigger_type VARCHAR(50) NOT NULL, -- AUTOMATIC, MANUAL, SCHEDULED
    notification_type VARCHAR(50) NOT NULL, -- EMAIL, WEBHOOK, IN_APP, SLACK
    recipient_config JSONB NOT NULL,
    message_template TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- WORKFLOW WEBHOOKS TABLE
-- External webhook triggers for workflow events
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    webhook_name VARCHAR(200) NOT NULL,
    webhook_url VARCHAR NOT NULL,
    trigger_events TEXT[],
    http_method VARCHAR(10) DEFAULT 'POST',
    headers JSONB,
    payload_template JSONB,
    retry_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- WEBHOOK EXECUTION LOG TABLE
-- Track webhook execution history
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.webhook_execution_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID REFERENCES jira_workflow.workflow_webhooks(id) ON DELETE SET NULL,
    execution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    http_status_code INTEGER,
    response_body TEXT,
    error_message TEXT,
    execution_duration_ms INTEGER,
    retry_count INTEGER DEFAULT 0,
    request_payload JSONB,
    response_headers JSONB
);

-- ============================================
-- NOTIFICATION TEMPLATES TABLE
-- Pre-defined notification message templates
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(200) NOT NULL,
    template_type VARCHAR(50) NOT NULL, -- EMAIL, SLACK, IN_APP
    subject_template VARCHAR(500),
    body_template TEXT,
    variables JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_workflow_notifications_workflow ON jira_workflow.workflow_notifications(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_notifications_active ON jira_workflow.workflow_notifications(is_active);
CREATE INDEX IF NOT EXISTS idx_workflow_webhooks_workflow ON jira_workflow.workflow_webhooks(workflow_id);
CREATE INDEX IF NOT EXISTS idx_webhook_execution_log_webhook ON jira_workflow.webhook_execution_log(webhook_id);
CREATE INDEX IF NOT EXISTS idx_webhook_execution_log_time ON jira_workflow.webhook_execution_log(execution_time);

-- ============================================
-- SEED DATA: Notification Templates
-- ============================================
INSERT INTO jira_workflow.notification_templates (id, template_name, template_type, subject_template, body_template) VALUES
    (gen_random_uuid(), 'Issue Assigned', 'EMAIL', 'Issue Assigned: {{issue.key}}', 'Issue {{issue.key}} has been assigned to {{assignee.displayName}}'),
    (gen_random_uuid(), 'Status Changed', 'EMAIL', 'Status Changed: {{issue.key}}', 'Issue {{issue.key}} status changed from {{oldStatus}} to {{newStatus}}'),
    (gen_random_uuid(), 'Transition Completed', 'IN_APP', NULL, 'You completed a transition on issue {{issue.key}}')
ON CONFLICT DO NOTHING;