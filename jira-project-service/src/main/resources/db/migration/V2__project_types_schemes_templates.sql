-- V2: Add Project Types, Templates, and Schemes (incremental - adds to existing V1 tables)
-- This migration ONLY ADDS new tables - does not modify existing tables

-- ============================================
-- PROJECT TYPES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL, -- COMPANY_MANAGED, TEAM_MANAGED
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- PROJECT TEMPLATES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_id UUID NOT NULL REFERENCES jira_project.project_types(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    default_assignee_type VARCHAR(20) DEFAULT 'PROJECT_LEAD',
    allow_issue_creation BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- ISSUE TYPE SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.issue_type_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.issue_type_scheme_issue_types (
    scheme_id UUID NOT NULL REFERENCES jira_project.issue_type_schemes(id) ON DELETE CASCADE,
    issue_type_name VARCHAR(50) NOT NULL, -- Store name instead of ID since issue types are in different DB
    is_default BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (scheme_id, issue_type_name)
);

-- ============================================
-- WORKFLOW SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.workflow_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.workflow_scheme_workflows (
    scheme_id UUID NOT NULL REFERENCES jira_project.workflow_schemes(id) ON DELETE CASCADE,
    workflow_name VARCHAR(100) NOT NULL,
    issue_type_name VARCHAR(50), -- Nullable: NULL means applies to all issue types
    PRIMARY KEY (scheme_id, workflow_name),
    UNIQUE (scheme_id, workflow_name, issue_type_name)
);

-- ============================================
-- PERMISSION SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.permission_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- NOTIFICATION SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.notification_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    notifications JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- SCREEN SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.screen_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.screen_scheme_screens (
    scheme_id UUID NOT NULL REFERENCES jira_project.screen_schemes(id) ON DELETE CASCADE,
    screen_type VARCHAR(20) NOT NULL,
    screen_id UUID NOT NULL,
    PRIMARY KEY (scheme_id, screen_type)
);

-- ============================================
-- PROJECT SCHEME CONFIGURATION
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID REFERENCES jira_project.issue_type_schemes(id),
    workflow_scheme_id UUID REFERENCES jira_project.workflow_schemes(id),
    permission_scheme_id UUID REFERENCES jira_project.permission_schemes(id),
    notification_scheme_id UUID REFERENCES jira_project.notification_schemes(id),
    screen_scheme_id UUID REFERENCES jira_project.screen_schemes(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- TEMPLATE SCHEME DEFAULTS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_scheme_defaults (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID REFERENCES jira_project.issue_type_schemes(id),
    workflow_scheme_id UUID REFERENCES jira_project.workflow_schemes(id),
    permission_scheme_id UUID REFERENCES jira_project.permission_schemes(id),
    notification_scheme_id UUID REFERENCES jira_project.notification_schemes(id),
    screen_scheme_id UUID REFERENCES jira_project.screen_schemes(id)
);

-- Add is_system_role column if not exists (for system-wide default roles)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'jira_project' AND table_name = 'project_roles' AND column_name = 'is_system_role') THEN
        ALTER TABLE jira_project.project_roles ADD COLUMN is_system_role BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- ============================================
-- SEED DATA: Project Types
-- ============================================
INSERT INTO jira_project.project_types (id, name, description, category, icon, sort_order) VALUES
    ('00000000-0000-0000-0001-000000000001', 'Company-managed', 'Classic Jira project with full configuration control', 'COMPANY_MANAGED', 'briefcase', 1),
    ('00000000-0000-0000-0001-000000000002', 'Team-managed', 'Lightweight next-gen project for autonomous teams', 'TEAM_MANAGED', 'users', 2)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Project Templates
-- ============================================
INSERT INTO jira_project.project_templates (id, type_id, name, description, icon, color, default_assignee_type, sort_order) VALUES
    ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', 'Scrum', 'Agile software development with sprints', 'scrum', '#0066FF', 'PROJECT_LEAD', 1),
    ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000001', 'Kanban', 'Visual workflow with continuous delivery', 'kanban', '#FF9200', 'UNASSIGNED', 2),
    ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000001', 'Bug Tracking', 'Track and manage software bugs', 'bug', '#DC3545', 'PROJECT_LEAD', 3),
    ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000001', 'Task Management', 'Manage tasks and action items', 'task', '#28A745', 'UNASSIGNED', 4),
    ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000001', 'Portfolio', 'Track multiple projects and initiatives', 'portfolio', '#6C757D', 'PROJECT_LEAD', 5),
    ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000002', 'Basic', 'Simple project for team collaboration', 'team', '#17A2B8', 'PROJECT_LEAD', 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Issue Type Schemes
-- ============================================
INSERT INTO jira_project.issue_type_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0003-000000000001', 'Scrum Issue Types', 'Epic, Story, Task, Bug, Sub-task', TRUE),
    ('00000000-0000-0000-0003-000000000002', 'Kanban Issue Types', 'Task, Bug', FALSE),
    ('00000000-0000-0000-0003-000000000003', 'Bug Tracking Issue Types', 'Bug, Story, Task', FALSE),
    ('00000000-0000-0000-0003-000000000004', 'Task Management Issue Types', 'Task, Sub-task', FALSE),
    ('00000000-0000-0000-0003-000000000005', 'Portfolio Issue Types', 'Epic, Story, Task', FALSE),
    ('00000000-0000-0000-0003-000000000006', 'Team-managed Issue Types', 'Task, Bug', FALSE)
ON CONFLICT DO NOTHING;

-- Issue type scheme mappings (using names instead of IDs since issue types are in different DB)
INSERT INTO jira_project.issue_type_scheme_issue_types (scheme_id, issue_type_name, is_default) VALUES
    ('00000000-0000-0000-0003-000000000001', 'Epic', TRUE),
    ('00000000-0000-0000-0003-000000000001', 'Story', FALSE),
    ('00000000-0000-0000-0003-000000000001', 'Task', FALSE),
    ('00000000-0000-0000-0003-000000000001', 'Bug', FALSE),
    ('00000000-0000-0000-0003-000000000001', 'Sub-task', FALSE),
    ('00000000-0000-0000-0003-000000000002', 'Task', TRUE),
    ('00000000-0000-0000-0003-000000000002', 'Bug', FALSE),
    ('00000000-0000-0000-0003-000000000003', 'Bug', TRUE),
    ('00000000-0000-0000-0003-000000000003', 'Story', FALSE),
    ('00000000-0000-0000-0003-000000000003', 'Task', FALSE),
    ('00000000-0000-0000-0003-000000000004', 'Task', TRUE),
    ('00000000-0000-0000-0003-000000000004', 'Sub-task', FALSE),
    ('00000000-0000-0000-0003-000000000005', 'Epic', TRUE),
    ('00000000-0000-0000-0003-000000000005', 'Story', FALSE),
    ('00000000-0000-0000-0003-000000000005', 'Task', FALSE),
    ('00000000-0000-0000-0003-000000000006', 'Task', TRUE),
    ('00000000-0000-0000-0003-000000000006', 'Bug', FALSE)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Workflow Schemes
-- ============================================
INSERT INTO jira_project.workflow_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0004-000000000001', 'Scrum Workflow', 'Backlog -> To Do -> In Progress -> In Review -> Done', TRUE),
    ('00000000-0000-0000-0004-000000000002', 'Kanban Workflow', 'To Do -> In Progress -> Done', FALSE),
    ('00000000-0000-0000-0004-000000000003', 'Bug Workflow', 'Open -> In Progress -> Resolved -> Closed', FALSE),
    ('00000000-0000-0000-0004-000000000004', 'Task Workflow', 'Open -> In Progress -> Completed', FALSE),
    ('00000000-0000-0000-0004-000000000005', 'Portfolio Workflow', 'Backlog -> Defined -> In Progress -> Done', FALSE)
ON CONFLICT DO NOTHING;

-- Workflow scheme mappings (using names since workflows are in different DB)
INSERT INTO jira_project.workflow_scheme_workflows (scheme_id, workflow_name, issue_type_name) VALUES
    ('00000000-0000-0000-0004-000000000001', 'Scrum Workflow', NULL),
    ('00000000-0000-0000-0004-000000000002', 'Kanban Workflow', NULL),
    ('00000000-0000-0000-0004-000000000003', 'Bug Workflow', NULL),
    ('00000000-0000-0000-0004-000000000004', 'Task Workflow', NULL),
    ('00000000-0000-0000-0004-000000000005', 'Portfolio Workflow', NULL)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Permission Schemes
-- ============================================
INSERT INTO jira_project.permission_schemes (id, name, description, permissions, is_default) VALUES
    ('00000000-0000-0000-0005-000000000001', 'Default Permission Scheme', 'Standard permissions for new projects', '["admin","developer","viewer"]', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Notification Schemes
-- ============================================
INSERT INTO jira_project.notification_schemes (id, name, description, notifications, is_default) VALUES
    ('00000000-0000-0000-0006-000000000001', 'Default Notification Scheme', 'Standard notifications', '["issue_created","issue_updated","issue_assigned","comment_added"]', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Screen Schemes
-- ============================================
INSERT INTO jira_project.screen_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0007-000000000001', 'Default Screen Scheme', 'Standard screens for new projects', TRUE)
ON CONFLICT DO NOTHING;

-- ============================================
-- TEMPLATE SCHEME DEFAULTS
-- ============================================
INSERT INTO jira_project.template_scheme_defaults (template_id, issue_type_scheme_id, workflow_scheme_id, permission_scheme_id, notification_scheme_id, screen_scheme_id) VALUES
    ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001')
ON CONFLICT DO NOTHING;