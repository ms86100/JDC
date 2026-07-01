-- V5__workflow_audit_and_history.sql
-- Workflow Service - Audit trail and history tracking

CREATE SCHEMA IF NOT EXISTS jira_workflow;

-- ============================================
-- WORKFLOW AUDIT LOG TABLE
-- Track all workflow-related changes
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL, -- WORKFLOW, TRANSITION, STATUS, SCREEN
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE, EXECUTE
    field_changed VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    user_id UUID,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TRANSITION HISTORY TABLE
-- Historical record of all transitions executed
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.transition_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    transition_id UUID REFERENCES jira_workflow.workflow_transitions(id),
    from_status VARCHAR(100),
    to_status VARCHAR(100),
    executed_by UUID,
    execution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_duration_ms INTEGER,
    comments TEXT,
    attachment_ids UUID[],
    custom_fields JSONB
);

-- ============================================
-- STATUS HISTORY TABLE
-- Track status changes over time
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    from_status VARCHAR(100),
    to_status VARCHAR(100),
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    time_in_status_ms INTEGER,
    status_category VARCHAR(50)
);

-- ============================================
-- WORKFLOW VERSIONING TABLE
-- Track workflow versions for audit
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    workflow_data JSONB NOT NULL,
    version_note TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (workflow_id, version_number)
);

-- ============================================
-- BULK OPERATION AUDIT TABLE
-- Track bulk workflow operations
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.bulk_operation_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(50) NOT NULL, -- BULK_TRANSITION, BULK_UPDATE
    workflow_id UUID REFERENCES jira_workflow.workflows(id),
    transition_id UUID REFERENCES jira_workflow.workflow_transitions(id),
    total_issues INTEGER NOT NULL,
    successful_issues INTEGER DEFAULT 0,
    failed_issues INTEGER DEFAULT 0,
    failed_issue_ids UUID[],
    initiated_by UUID,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_workflow_audit_entity ON jira_workflow.workflow_audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_user ON jira_workflow.workflow_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_time ON jira_workflow.workflow_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_transition_history_issue ON jira_workflow.transition_history(issue_id);
CREATE INDEX IF NOT EXISTS idx_transition_history_time ON jira_workflow.transition_history(execution_time);
CREATE INDEX IF NOT EXISTS idx_status_history_issue ON jira_workflow.status_history(issue_id);
CREATE INDEX IF NOT EXISTS idx_workflow_versions_workflow ON jira_workflow.workflow_versions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_bulk_audit_time ON jira_workflow.bulk_operation_audit(started_at);