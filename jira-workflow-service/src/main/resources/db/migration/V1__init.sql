-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_workflow;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Workflows table
CREATE TABLE IF NOT EXISTS jira_workflow.workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT false,
    is_draft BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    is_system BOOLEAN DEFAULT false,
    type VARCHAR(50) DEFAULT 'CUSTOM',
    draft_of_workflow_id UUID,
    status_category_mapping TEXT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_workflows_name UNIQUE (name)
);

-- Workflow Statuses
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    status_id UUID NOT NULL,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_workflow_statuses UNIQUE (workflow_id, status_id)
);

-- Workflow Transitions
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_transitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    from_status_id UUID NOT NULL,
    to_status_id UUID NOT NULL,
    display_order INT DEFAULT 1,
    type VARCHAR(50) DEFAULT 'MANUAL',
    requires_approval BOOLEAN DEFAULT false,
    conditions TEXT,
    validators TEXT,
    post_functions TEXT,
    screen_id UUID,
    icon VARCHAR(50),
    permission_check TEXT,
    user_group_ids TEXT,
    approval_group_id UUID,
    fields_required TEXT,
    fields_hidden TEXT,
    fields_updated TEXT,
    fields_auto_submit BOOLEAN DEFAULT false,
    allow_loop BOOLEAN DEFAULT true,
    max_loop_count INT DEFAULT 0,
    allow_unassign BOOLEAN DEFAULT false,
    allow_assignee_override BOOLEAN DEFAULT false,
    remote_link_transition BOOLEAN DEFAULT false,
    remote_link_direction VARCHAR(20),
    remote_link_issue_link_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_at TIMESTAMP DEFAULT NOW(),
    updated_by UUID
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_workflows_project ON jira_workflow.workflows(project_id);
CREATE INDEX IF NOT EXISTS idx_workflows_name ON jira_workflow.workflows(name);
CREATE INDEX IF NOT EXISTS idx_workflow_transitions_workflow ON jira_workflow.workflow_transitions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_transitions_from_status ON jira_workflow.workflow_transitions(from_status_id);
CREATE INDEX IF NOT EXISTS idx_workflow_transitions_to_status ON jira_workflow.workflow_transitions(to_status_id);
CREATE INDEX IF NOT EXISTS idx_workflow_statuses_workflow ON jira_workflow.workflow_statuses(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_statuses_status ON jira_workflow.workflow_statuses(status_id);