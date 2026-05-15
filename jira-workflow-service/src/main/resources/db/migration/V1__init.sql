-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_workflow;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE jira_workflow.workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_workflow.workflow_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    status_id UUID NOT NULL,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(workflow_id, status_id)
);

CREATE TABLE jira_workflow.workflow_transitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    from_status_id UUID NOT NULL,
    to_status_id UUID NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflows_project ON jira_workflow.workflows(project_id);
CREATE INDEX idx_workflow_transitions_workflow ON jira_workflow.workflow_transitions(workflow_id);