-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_project;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE jira_project.projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_key VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    lead_user_id UUID,
    project_type VARCHAR(20) NOT NULL DEFAULT 'COMPANY_MANAGED',
    template_id UUID,
    category VARCHAR(50),
    avatar_url VARCHAR(500),
    default_assignee_type VARCHAR(20) DEFAULT 'PROJECT_LEAD',
    allow_issue_creation BOOLEAN DEFAULT TRUE,
    archived BOOLEAN DEFAULT FALSE,
    archived_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.project_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID, -- Nullable to allow system-wide default roles
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE, -- For system-wide default roles
    is_default BOOLEAN DEFAULT FALSE, -- Default role for new project members
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, name)
);

-- Seed system-wide default project roles (no project association)
INSERT INTO jira_project.project_roles (id, project_id, name, description, is_system_role, permissions) VALUES
  ('00000000-0000-0000-0000-000000000001', NULL, 'PROJECT_ADMIN', 'Project administrators with full access', TRUE, '["*"]'),
  ('00000000-0000-0000-0000-000000000002', NULL, 'DEVELOPER', 'Developers who can edit and comment', TRUE, '["read","edit","comment","create_issues","transition"]'),
  ('00000000-0000-0000-0000-000000000003', NULL, 'VIEWER', 'Read-only access to the project', TRUE, '["read","comment"]');

CREATE TABLE jira_project.project_members (
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    project_role_id UUID NOT NULL REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_projects_key ON jira_project.projects(project_key);
CREATE INDEX idx_project_members_user ON jira_project.project_members(user_id);