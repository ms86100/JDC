-- ============================================
-- JIRA PLATFORM - CONSOLIDATED DATABASE
-- Single database for all microservices
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SCHEMAS
-- ============================================
CREATE SCHEMA IF NOT EXISTS jira_auth;
CREATE SCHEMA IF NOT EXISTS jira_user;
CREATE SCHEMA IF NOT EXISTS jira_project;
CREATE SCHEMA IF NOT EXISTS jira_issue;
CREATE SCHEMA IF NOT EXISTS jira_workflow;
CREATE SCHEMA IF NOT EXISTS jira_comment;
CREATE SCHEMA IF NOT EXISTS jira_notification;
CREATE SCHEMA IF NOT EXISTS jira_search;
CREATE SCHEMA IF NOT EXISTS jira_audit;
CREATE SCHEMA IF NOT EXISTS jira_attachment;
CREATE SCHEMA IF NOT EXISTS jira_sprint;
CREATE SCHEMA IF NOT EXISTS jira_plan;

-- ============================================
-- JIRA_AUTH SCHEMA (Users, Roles, Auth)
-- ============================================
CREATE TABLE jira_auth.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_auth.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_auth.user_roles (
    user_id UUID NOT NULL REFERENCES jira_auth.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES jira_auth.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE jira_auth.user_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_auth.user_group_members (
    user_id UUID NOT NULL REFERENCES jira_auth.users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES jira_auth.user_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

INSERT INTO jira_auth.roles (name, description) VALUES
    ('ROLE_ADMIN', 'System administrator with full access'),
    ('ROLE_USER', 'Regular user with standard access');

CREATE INDEX idx_users_username ON jira_auth.users(username);
CREATE INDEX idx_users_email ON jira_auth.users(email);
CREATE INDEX idx_user_roles_user ON jira_auth.user_roles(user_id);
CREATE INDEX idx_user_roles_role ON jira_auth.user_roles(role_id);
CREATE INDEX idx_user_groups_name ON jira_auth.user_groups(group_name);

-- ============================================
-- JIRA_USER SCHEMA (Profiles, Organizations, Teams)
-- ============================================
CREATE TABLE jira_user.profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_user.organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_user.teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES jira_user.organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_user.organization_members (
    org_id UUID NOT NULL REFERENCES jira_user.organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (org_id, user_id)
);

CREATE INDEX idx_profiles_user_id ON jira_user.profiles(user_id);
CREATE INDEX idx_organizations_slug ON jira_user.organizations(slug);
CREATE INDEX idx_teams_organization_id ON jira_user.teams(organization_id);
CREATE INDEX idx_organization_members_user_id ON jira_user.organization_members(user_id);

CREATE OR REPLACE FUNCTION jira_user.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON jira_user.profiles
    FOR EACH ROW EXECUTE FUNCTION jira_user.update_updated_at_column();

-- ============================================
-- JIRA_PROJECT SCHEMA (Projects, Permissions, Schemes)
-- ============================================
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
    project_id UUID,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT FALSE,
    is_default BOOLEAN DEFAULT FALSE,
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, name)
);

CREATE TABLE jira_project.project_members (
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    project_role_id UUID NOT NULL REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE jira_project.project_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.project_templates (
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

CREATE TABLE jira_project.issue_type_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.issue_type_scheme_issue_types (
    scheme_id UUID NOT NULL REFERENCES jira_project.issue_type_schemes(id) ON DELETE CASCADE,
    issue_type_name VARCHAR(50) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (scheme_id, issue_type_name)
);

CREATE TABLE jira_project.workflow_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.workflow_scheme_workflows (
    scheme_id UUID NOT NULL REFERENCES jira_project.workflow_schemes(id) ON DELETE CASCADE,
    workflow_name VARCHAR(100) NOT NULL,
    issue_type_name VARCHAR(50),
    PRIMARY KEY (scheme_id, workflow_name),
    UNIQUE (scheme_id, workflow_name, issue_type_name)
);

CREATE TABLE jira_project.permission_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.notification_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    notifications JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.screen_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.screen_scheme_screens (
    scheme_id UUID NOT NULL REFERENCES jira_project.screen_schemes(id) ON DELETE CASCADE,
    screen_type VARCHAR(20) NOT NULL,
    screen_id UUID NOT NULL,
    PRIMARY KEY (scheme_id, screen_type)
);

CREATE TABLE jira_project.project_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID REFERENCES jira_project.issue_type_schemes(id),
    workflow_scheme_id UUID REFERENCES jira_project.workflow_schemes(id),
    permission_scheme_id UUID REFERENCES jira_project.permission_schemes(id),
    notification_scheme_id UUID REFERENCES jira_project.notification_schemes(id),
    screen_scheme_id UUID REFERENCES jira_project.screen_schemes(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.template_scheme_defaults (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID REFERENCES jira_project.issue_type_schemes(id),
    workflow_scheme_id UUID REFERENCES jira_project.workflow_schemes(id),
    permission_scheme_id UUID REFERENCES jira_project.permission_schemes(id),
    notification_scheme_id UUID REFERENCES jira_project.notification_schemes(id),
    screen_scheme_id UUID REFERENCES jira_project.screen_schemes(id)
);

-- Enhanced Permissions (V3)
CREATE TABLE jira_project.permissions (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL,
    key_name VARCHAR(30) NOT NULL
);

CREATE TABLE jira_project.permission_grants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID NOT NULL REFERENCES jira_project.permission_schemes(id) ON DELETE CASCADE,
    grant_type VARCHAR(20) NOT NULL,
    entity_id UUID,
    group_name VARCHAR(100),
    project_role_id UUID,
    permission_key VARCHAR(50) NOT NULL,
    issue_id UUID,
    issue_security_level_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_grant_type CHECK (grant_type IN ('USER', 'GROUP', 'PROJECT_ROLE'))
);

CREATE TABLE jira_project.project_role_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    entity_type VARCHAR(10) NOT NULL,
    entity_id UUID,
    group_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_project_role_member UNIQUE (project_id, role_id, entity_type, entity_id, group_name)
);

CREATE TABLE jira_project.issue_security_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.security_levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID NOT NULL REFERENCES jira_project.issue_security_schemes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    "rank" INT DEFAULT 0,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.security_level_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    level_id UUID NOT NULL REFERENCES jira_project.security_levels(id) ON DELETE CASCADE,
    member_type VARCHAR(20) NOT NULL,
    member_id UUID,
    group_name VARCHAR(100),
    role_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_member_type CHECK (member_type IN ('USER', 'GROUP', 'PROJECT_ROLE'))
);

CREATE TABLE jira_project.project_permission_scheme (
    project_id UUID PRIMARY KEY REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    scheme_id UUID NOT NULL REFERENCES jira_project.permission_schemes(id),
    override BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_key ON jira_project.projects(project_key);
CREATE INDEX idx_project_members_user ON jira_project.project_members(user_id);
CREATE INDEX idx_permission_grants_scheme ON jira_project.permission_grants(scheme_id);
CREATE INDEX idx_project_role_members_project ON jira_project.project_role_members(project_id);
CREATE INDEX idx_project_role_members_role ON jira_project.project_role_members(role_id);
CREATE INDEX idx_security_level_members_level ON jira_project.security_level_members(level_id);

-- Seed system-wide default project roles
INSERT INTO jira_project.project_roles (id, project_id, name, description, is_system_role, permissions) VALUES
  ('00000000-0000-0000-0000-000000000001', NULL, 'PROJECT_ADMIN', 'Project administrators with full access', TRUE, '["*"]'),
  ('00000000-0000-0000-0000-000000000002', NULL, 'DEVELOPER', 'Developers who can edit and comment', TRUE, '["read","edit","comment","create_issues","transition"]'),
  ('00000000-0000-0000-0000-000000000003', NULL, 'VIEWER', 'Read-only access to the project', TRUE, '["read","comment"]');

-- Seed Project Types
INSERT INTO jira_project.project_types (id, name, description, category, icon, sort_order) VALUES
    ('00000000-0000-0000-0001-000000000001', 'Company-managed', 'Classic Jira project with full configuration control', 'COMPANY_MANAGED', 'briefcase', 1),
    ('00000000-0000-0000-0001-000000000002', 'Team-managed', 'Lightweight next-gen project for autonomous teams', 'TEAM_MANAGED', 'users', 2);

-- Seed Project Templates
INSERT INTO jira_project.project_templates (id, type_id, name, description, icon, color, default_assignee_type, sort_order) VALUES
    ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', 'Scrum', 'Agile software development with sprints', 'scrum', '#0066FF', 'PROJECT_LEAD', 1),
    ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0001-000000000001', 'Kanban', 'Visual workflow with continuous delivery', 'kanban', '#FF9200', 'UNASSIGNED', 2),
    ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0001-000000000001', 'Bug Tracking', 'Track and manage software bugs', 'bug', '#DC3545', 'PROJECT_LEAD', 3),
    ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0001-000000000001', 'Task Management', 'Manage tasks and action items', 'task', '#28A745', 'UNASSIGNED', 4),
    ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0001-000000000001', 'Portfolio', 'Track multiple projects and initiatives', 'portfolio', '#6C757D', 'PROJECT_LEAD', 5),
    ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0001-000000000002', 'Basic', 'Simple project for team collaboration', 'team', '#17A2B8', 'PROJECT_LEAD', 1);

-- Seed Issue Type Schemes
INSERT INTO jira_project.issue_type_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0003-000000000001', 'Scrum Issue Types', 'Epic, Story, Task, Bug, Sub-task', TRUE),
    ('00000000-0000-0000-0003-000000000002', 'Kanban Issue Types', 'Task, Bug', FALSE),
    ('00000000-0000-0000-0003-000000000003', 'Bug Tracking Issue Types', 'Bug, Story, Task', FALSE),
    ('00000000-0000-0000-0003-000000000004', 'Task Management Issue Types', 'Task, Sub-task', FALSE),
    ('00000000-0000-0000-0003-000000000005', 'Portfolio Issue Types', 'Epic, Story, Task', FALSE),
    ('00000000-0000-0000-0003-000000000006', 'Team-managed Issue Types', 'Task, Bug', FALSE);

-- Seed Workflow Schemes
INSERT INTO jira_project.workflow_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0004-000000000001', 'Scrum Workflow', 'Backlog -> To Do -> In Progress -> In Review -> Done', TRUE),
    ('00000000-0000-0000-0004-000000000002', 'Kanban Workflow', 'To Do -> In Progress -> Done', FALSE),
    ('00000000-0000-0000-0004-000000000003', 'Bug Workflow', 'Open -> In Progress -> Resolved -> Closed', FALSE),
    ('00000000-0000-0000-0004-000000000004', 'Task Workflow', 'Open -> In Progress -> Completed', FALSE),
    ('00000000-0000-0000-0004-000000000005', 'Portfolio Workflow', 'Backlog -> Defined -> In Progress -> Done', FALSE);

-- Seed Permission and Notification Schemes
INSERT INTO jira_project.permission_schemes (id, name, description, permissions, is_default) VALUES
    ('00000000-0000-0000-0005-000000000001', 'Default Permission Scheme', 'Standard permissions for new projects', '["admin","developer","viewer"]', TRUE);

INSERT INTO jira_project.notification_schemes (id, name, description, notifications, is_default) VALUES
    ('00000000-0000-0000-0006-000000000001', 'Default Notification Scheme', 'Standard notifications', '["issue_created","issue_updated","issue_assigned","comment_added"]', TRUE);

INSERT INTO jira_project.screen_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0007-000000000001', 'Default Screen Scheme', 'Standard screens for new projects', TRUE);

-- Template Scheme Defaults
INSERT INTO jira_project.template_scheme_defaults (template_id, issue_type_scheme_id, workflow_scheme_id, permission_scheme_id, notification_scheme_id, screen_scheme_id) VALUES
    ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000003', '00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0004-000000000003', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000004', '00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0004-000000000004', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000005', '00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0004-000000000005', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001'),
    ('00000000-0000-0000-0002-000000000006', '00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0004-000000000002', '00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0007-000000000001');

-- Seed Permissions
INSERT INTO jira_project.permissions (id, name, description, category, key_name) VALUES
    ('perm_browse_projects', 'Browse Projects', 'Ability to view projects', 'PROJECT', 'BROWSE_PROJECTS'),
    ('perm_create_projects', 'Create Projects', 'Ability to create new projects', 'PROJECT', 'CREATE_PROJECTS'),
    ('perm_admin_projects', 'Administer Projects', 'Ability to configure project settings', 'PROJECT', 'ADMINISTER_PROJECTS'),
    ('perm_create_issues', 'Create Issues', 'Ability to create issues', 'ISSUE', 'CREATE_ISSUES'),
    ('perm_edit_issues', 'Edit Issues', 'Ability to edit issues', 'ISSUE', 'EDIT_ISSUES'),
    ('perm_delete_issues', 'Delete Issues', 'Ability to delete issues', 'ISSUE', 'DELETE_ISSUES'),
    ('perm_assign_issues', 'Assign Issues', 'Ability to assign issues to users', 'ISSUE', 'ASSIGN_ISSUES'),
    ('perm_resolve_issues', 'Resolve Issues', 'Ability to resolve/close issues', 'ISSUE', 'RESOLVE_ISSUES'),
    ('perm_create_comments', 'Create Comments', 'Ability to add comments', 'ISSUE', 'CREATE_COMMENTS'),
    ('perm_edit_comments', 'Edit Comments', 'Ability to edit comments', 'ISSUE', 'EDIT_COMMENTS'),
    ('perm_delete_comments', 'Delete Comments', 'Ability to delete comments', 'ISSUE', 'DELETE_COMMENTS'),
    ('perm_create_attachments', 'Create Attachments', 'Ability to attach files', 'ISSUE', 'CREATE_ATTACHMENTS'),
    ('perm_delete_attachments', 'Delete Attachments', 'Ability to delete attachments', 'ISSUE', 'DELETE_ATTACHMENTS'),
    ('perm_work_on_issues', 'Work On Issues', 'Ability to log work', 'ISSUE', 'WORK_ON_ISSUES'),
    ('perm_sysadmin', 'System Administrator', 'Full system administration', 'GLOBAL', 'SYSTEM_ADMIN'),
    ('perm_admin', 'Administrator', 'Application administration', 'GLOBAL', 'ADMIN'),
    ('perm_user', 'User', 'Basic user access', 'GLOBAL', 'USER');

-- Seed Default Project Roles
INSERT INTO jira_project.project_roles (id, name, description, is_default, is_system_role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Project Administrator', 'Can manage project settings and members', TRUE, TRUE),
    ('22222222-2222-2222-2222-222222222222', 'Developer', 'Can create, edit, and resolve issues', TRUE, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Committer', 'Can commit code linked to issues', TRUE, TRUE),
    ('44444444-4444-4444-4444-444444444444', 'Users', 'Can view project and issues', TRUE, TRUE),
    ('55555555-5555-5555-5555-555555555555', 'Viewers', 'Read-only access to the project', TRUE, TRUE);

-- ============================================
-- JIRA_ISSUE SCHEMA (Issues, Types, Statuses, Priorities)
-- ============================================
CREATE TABLE jira_issue.issue_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issue_priorities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(100),
    color VARCHAR(20),
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issue_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    sequence INT NOT NULL DEFAULT 0,
    category VARCHAR(20) NOT NULL DEFAULT 'TODO',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status UUID NOT NULL REFERENCES jira_issue.issue_statuses(id),
    priority UUID REFERENCES jira_issue.issue_priorities(id),
    issue_type UUID NOT NULL REFERENCES jira_issue.issue_types(id),
    reporter_id UUID,
    assignee_id UUID,
    parent_issue_id UUID REFERENCES jira_issue.issues(id),
    -- Enhanced fields
    epic_id UUID,
    epic_name VARCHAR(255),
    epic_color VARCHAR(7),
    security_level_id UUID,
    affects_versions UUID[],
    fix_versions UUID[],
    story_points INTEGER,
    rank VARCHAR(255),
    original_estimate BIGINT,
    remaining_estimate BIGINT,
    time_spent BIGINT,
    resolution_id UUID,
    resolution_date TIMESTAMP,
    due_date DATE,
    vote_count INTEGER DEFAULT 0,
    watcher_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.watchers (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, user_id)
);

CREATE TABLE jira_issue.votes (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, user_id)
);

CREATE TABLE jira_issue.issue_link_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    inward VARCHAR(50) NOT NULL,
    outward VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.issue_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    link_type_id UUID NOT NULL REFERENCES jira_issue.issue_link_types(id),
    source_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT unique_issue_link UNIQUE (link_type_id, source_issue_id, target_issue_id)
);

CREATE TABLE jira_issue.resolutions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(255),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.project_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE,
    release_date DATE,
    is_released BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    released_by UUID,
    released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_version_name_per_project UNIQUE (project_id, name)
);

CREATE TABLE jira_issue.project_components (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    lead_id UUID,
    assignee_type VARCHAR(20) DEFAULT 'PROJECT_LEAD',
    default_assignee_id UUID,
    is_assignee_type_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_component_name_per_project UNIQUE (project_id, name)
);

CREATE TABLE jira_issue.worklogs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL,
    time_spent_seconds BIGINT NOT NULL,
    time_spent_display VARCHAR(30),
    work_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.labels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7),
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.change_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.change_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    change_group_id UUID NOT NULL REFERENCES jira_issue.change_groups(id) ON DELETE CASCADE,
    field_type VARCHAR(50) DEFAULT 'jira',
    field VARCHAR(100) NOT NULL,
    old_value TEXT,
    old_string TEXT,
    new_value TEXT,
    new_string TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_issues_project ON jira_issue.issues(project_id);
CREATE INDEX idx_issues_key ON jira_issue.issues(issue_key);
CREATE INDEX idx_issues_status ON jira_issue.issues(status);
CREATE INDEX idx_issues_assignee ON jira_issue.issues(assignee_id);
CREATE INDEX idx_issues_epic ON jira_issue.issues(epic_id);
CREATE INDEX idx_issues_parent ON jira_issue.issues(parent_issue_id);
CREATE INDEX idx_watchers_user ON jira_issue.watchers(user_id);
CREATE INDEX idx_votes_user ON jira_issue.votes(user_id);
CREATE INDEX idx_worklogs_author ON jira_issue.worklogs(author_id);
CREATE INDEX idx_issue_links_source ON jira_issue.issue_links(source_issue_id);
CREATE INDEX idx_issue_links_target ON jira_issue.issue_links(target_issue_id);
CREATE INDEX idx_versions_project ON jira_issue.project_versions(project_id);
CREATE INDEX idx_components_project ON jira_issue.project_components(project_id);
CREATE INDEX idx_labels_issue ON jira_issue.labels(issue_id);
CREATE INDEX idx_change_groups_issue ON jira_issue.change_groups(issue_id);

-- Seed Issue Types
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'Bug', 'bug', 'A bug report'),
  ('a0000000-0000-0000-0000-000000000002', 'Story', 'book', 'A user story'),
  ('a0000000-0000-0000-0000-000000000003', 'Task', 'task', 'A task'),
  ('a0000000-0000-0000-0000-000000000004', 'Epic', 'lightning', 'An epic'),
  ('a0000000-0000-0000-0000-000000000005', 'Sub-task', 'subtask', 'A sub-task of a parent issue');

-- Seed Issue Priorities
INSERT INTO jira_issue.issue_priorities (id, name, icon, color, sequence) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'Highest', 'arrow-up', '#ff0000', 1),
  ('b0000000-0000-0000-0000-000000000002', 'High', 'arrow-up', '#ff6600', 2),
  ('b0000000-0000-0000-0000-000000000003', 'Medium', 'minus', '#ffcc00', 3),
  ('b0000000-0000-0000-0000-000000000004', 'Low', 'arrow-down', '#0099ff', 4),
  ('b0000000-0000-0000-0000-000000000005', 'Lowest', 'arrow-down', '#99cc00', 5);

-- Seed Issue Statuses
INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
  ('c0000000-0000-0000-0000-000000000001', 'Backlog', 0, 'TODO'),
  ('c0000000-0000-0000-0000-000000000002', 'To Do', 1, 'TODO'),
  ('c0000000-0000-0000-0000-000000000003', 'In Progress', 2, 'IN_PROGRESS'),
  ('c0000000-0000-0000-0000-000000000004', 'In Review', 3, 'IN_PROGRESS'),
  ('c0000000-0000-0000-0000-000000000005', 'Done', 4, 'DONE'),
  ('c0000000-0000-0000-0000-000000000006', 'Open', 1, 'TODO'),
  ('c0000000-0000-0000-0000-000000000007', 'Resolved', 5, 'DONE'),
  ('c0000000-0000-0000-0000-000000000008', 'Closed', 6, 'DONE'),
  ('c0000000-0000-0000-0000-000000000009', 'Defined', 1, 'TODO');

-- Seed Issue Link Types
INSERT INTO jira_issue.issue_link_types (id, name, inward, outward) VALUES
    ('link-blocks', 'Blocks', 'is blocked by', 'blocks'),
    ('link-is-blocked-by', 'Is blocked by', 'blocks', 'is blocked by'),
    ('link-duplicates', 'Duplicates', 'is duplicated by', 'duplicates'),
    ('link-is-duplicated-by', 'Is duplicated by', 'duplicates', 'is duplicated by'),
    ('link-relates-to', 'Relates to', 'relates to', 'relates to'),
    ('link-causes', 'Causes', 'is caused by', 'causes'),
    ('link-is-caused-by', 'Is caused by', 'causes', 'is caused by'),
    ('link-depends-on', 'Depends on', 'is depended upon by', 'depends on'),
    ('link-clones', 'Clones', 'is cloned by', 'clones'),
    ('link-splits-into', 'Splits into', 'is split from', 'splits into'),
    ('link-supercedes', 'Supercedes', 'is superseded by', 'supercedes');

-- Seed Resolutions
INSERT INTO jira_issue.resolutions (id, name, description, sort_order) VALUES
    ('res-fixed', 'Fixed', 'The issue has been fixed', 1),
    ('res-wont-fix', 'Won''t Fix', 'The issue will not be fixed', 2),
    ('res-duplicate', 'Duplicate', 'The issue is a duplicate', 3),
    ('res-incomplete', 'Incomplete', 'The issue cannot be completed', 4),
    ('res-cannot-reproduce', 'Cannot Reproduce', 'The issue cannot be reproduced', 5),
    ('res-done', 'Done', 'The issue has been completed', 6);

-- ============================================
-- JIRA_WORKFLOW SCHEMA (Workflows, Transitions)
-- ============================================
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

-- Seed Default Workflows
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000000'::uuid, 'Scrum Workflow',
     'Default Scrum workflow with sprint states', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000000'::uuid, 'Kanban Workflow',
     'Simple Kanban workflow', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000000'::uuid, 'Bug Workflow',
     'Bug tracking workflow', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000000'::uuid, 'Task Workflow',
     'Simple task management workflow', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0000-000000000000'::uuid, 'Portfolio Workflow',
     'Portfolio-level tracking workflow', TRUE, NOW(), NOW());

-- ============================================
-- JIRA_COMMENT SCHEMA (Comments)
-- ============================================
CREATE TABLE jira_comment.comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_comment_id UUID REFERENCES jira_comment.comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    internal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comments_issue_id ON jira_comment.comments(issue_id);
CREATE INDEX idx_comments_user_id ON jira_comment.comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON jira_comment.comments(parent_comment_id);
CREATE INDEX idx_comments_issue_id_deleted ON jira_comment.comments(issue_id, deleted);
CREATE INDEX idx_comments_created_at ON jira_comment.comments(created_at DESC);

CREATE OR REPLACE FUNCTION jira_comment.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_comments_updated_at
    BEFORE UPDATE ON jira_comment.comments
    FOR EACH ROW EXECUTE FUNCTION jira_comment.update_updated_at_column();

-- ============================================
-- JIRA_NOTIFICATION SCHEMA (Notifications)
-- ============================================
CREATE TABLE jira_notification.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    reference_type VARCHAR(100),
    reference_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_notification.notification_preferences (
    user_id UUID NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, notification_type)
);

CREATE INDEX idx_notifications_user_id ON jira_notification.notifications(user_id);
CREATE INDEX idx_notifications_user_id_is_read ON jira_notification.notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON jira_notification.notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON jira_notification.notifications(reference_type, reference_id);

-- ============================================
-- JIRA_SEARCH SCHEMA (Full-text Search Index)
-- ============================================
CREATE TABLE jira_search.search_index (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    search_vector tsvector,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

CREATE INDEX idx_search_index_search_vector ON jira_search.search_index USING GIN(search_vector);
CREATE INDEX idx_search_index_entity_type ON jira_search.search_index(entity_type);
CREATE INDEX idx_search_index_entity_id ON jira_search.search_index(entity_id);
CREATE INDEX idx_search_index_created_at ON jira_search.search_index(created_at DESC);

CREATE OR REPLACE FUNCTION jira_search.update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_search_index_vector
    BEFORE INSERT OR UPDATE ON jira_search.search_index
    FOR EACH ROW EXECUTE FUNCTION jira_search.update_search_vector();

-- ============================================
-- JIRA_AUDIT SCHEMA (Audit Logs)
-- ============================================
CREATE TABLE jira_audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    service_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    changes JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_service ON jira_audit.audit_logs(service_name);
CREATE INDEX idx_audit_entity ON jira_audit.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON jira_audit.audit_logs(user_id);
CREATE INDEX idx_audit_created ON jira_audit.audit_logs(created_at DESC);
CREATE INDEX idx_audit_action ON jira_audit.audit_logs(action);

-- ============================================
-- JIRA_ATTACHMENT SCHEMA (File Attachments)
-- ============================================
CREATE TABLE jira_attachment.attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR NOT NULL,
    uploader_id UUID,
    uploader_name VARCHAR(200),
    thumbnail_path VARCHAR(500),
    mime_type_detected VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_issue ON jira_attachment.attachments(issue_id);
CREATE INDEX idx_attachments_uploader ON jira_attachment.attachments(uploader_id);
CREATE INDEX idx_attachments_created ON jira_attachment.attachments(created_at DESC);

-- ============================================
-- JIRA_SPRINT SCHEMA (Sprints, Agile Boards)
-- ============================================
CREATE TABLE jira_sprint.sprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    project_id UUID NOT NULL,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_sprint.sprint_issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    order_index INTEGER DEFAULT 0,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_sprint.agile_boards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_id UUID NOT NULL,
    board_type VARCHAR(50) NOT NULL DEFAULT 'SCRUM',
    filter_id UUID,
    jql_query TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    allow_all_issues BOOLEAN DEFAULT TRUE,
    card_layout VARCHAR(50) DEFAULT 'FULL',
    estimation_statistic VARCHAR(100),
    days_on_board INTEGER DEFAULT 5,
    last_viewed TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_sprint.board_columns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    status_category VARCHAR(50) DEFAULT 'TODO',
    is_done BOOLEAN DEFAULT FALSE,
    max_issues INTEGER,
    color VARCHAR(20) DEFAULT '#6c757d',
    is_collapsible BOOLEAN DEFAULT TRUE,
    is_hidden BOOLEAN DEFAULT FALSE
);

CREATE TABLE jira_sprint.board_sprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL DEFAULT 0,
    state VARCHAR(20) DEFAULT 'FUTURE',
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    complete_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (board_id, sprint_id)
);

CREATE TABLE jira_sprint.quick_filter_presets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    user_id UUID,
    name VARCHAR(100) NOT NULL,
    jql_query TEXT NOT NULL,
    icon VARCHAR(50),
    is_system BOOLEAN DEFAULT FALSE,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_sprint.board_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    user_id UUID,
    swimlane_field VARCHAR(50) DEFAULT 'none',
    collapsed_swimlanes TEXT[],
    card_color_field VARCHAR(50) DEFAULT 'none',
    show_work_vs_capacity BOOLEAN DEFAULT TRUE,
    default_view VARCHAR(20) DEFAULT 'board',
    column_order TEXT[],
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (board_id, user_id)
);

CREATE INDEX idx_sprints_project_id ON jira_sprint.sprints(project_id);
CREATE INDEX idx_sprints_status ON jira_sprint.sprints(status);
CREATE INDEX idx_sprint_issues_sprint_id ON jira_sprint.sprint_issues(sprint_id);
CREATE INDEX idx_agile_boards_project ON jira_sprint.agile_boards(project_id);
CREATE INDEX idx_agile_boards_type ON jira_sprint.agile_boards(board_type);
CREATE INDEX idx_board_columns_board ON jira_sprint.board_columns(board_id);
CREATE INDEX idx_board_sprints_board ON jira_sprint.board_sprints(board_id);
CREATE INDEX idx_board_sprints_sprint ON jira_sprint.board_sprints(sprint_id);
CREATE INDEX idx_quick_filters_board ON jira_sprint.quick_filter_presets(board_id);
CREATE INDEX idx_board_configs_board ON jira_sprint.board_configs(board_id);

-- Seed Quick Filters
INSERT INTO jira_sprint.quick_filter_presets (id, name, jql_query, icon, is_system, sequence) VALUES
    ('00000000-0000-0001-0001-000000000001', 'Assigned to Me', 'assignee = currentUser()', 'user', TRUE, 1),
    ('00000000-0000-0001-0001-000000000002', 'Reported by Me', 'reporter = currentUser()', 'document', TRUE, 2),
    ('00000000-0000-0001-0001-000000000003', 'Recently Updated', 'updated >= -1d', 'refresh', TRUE, 3),
    ('00000000-0000-0001-0001-000000000004', 'Unassigned', 'assignee is empty', 'question', TRUE, 4),
    ('00000000-0000-0001-0001-000000000005', 'Has Due Date', 'duedate is not empty', 'calendar', TRUE, 5),
    ('00000000-0000-0001-0001-000000000006', 'High Priority', 'priority in (High, Highest)', 'warning', TRUE, 6);

-- ============================================
-- JIRA_PLAN SCHEMA (Plans, Programs, Teams)
-- ============================================
CREATE TABLE jira_plan.programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    access_type VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    settings JSONB DEFAULT '{}',
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.program_plans (
    program_id UUID NOT NULL REFERENCES jira_plan.programs(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (program_id, plan_id)
);

CREATE TABLE jira_plan.plan_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    issue_key VARCHAR(50),
    issue_title VARCHAR(500),
    issue_type VARCHAR(20) NOT NULL,
    parent_id UUID,
    sort_order VARCHAR(255) NOT NULL,
    target_date DATE,
    status VARCHAR(50),
    status_category VARCHAR(50),
    story_points INTEGER,
    assignee_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_items_plan_issue UNIQUE (plan_id, issue_id)
);

CREATE TABLE jira_plan.plan_teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.plan_team_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID NOT NULL REFERENCES jira_plan.plan_teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    user_name VARCHAR(255),
    capacity_hours DECIMAL(5,2) DEFAULT 40.00,
    role VARCHAR(50),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_team_member_user UNIQUE (team_id, user_id)
);

CREATE TABLE jira_plan.plan_releases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    description TEXT,
    release_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by UUID,
    approved_at TIMESTAMP,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.issue_dependencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    blocking_issue_id UUID NOT NULL,
    blocking_issue_key VARCHAR(50),
    blocked_issue_id UUID NOT NULL,
    blocked_issue_key VARCHAR(50),
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_issue_dependency UNIQUE (plan_id, blocking_issue_id, blocked_issue_id)
);

CREATE TABLE jira_plan.plan_warnings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    issue_key VARCHAR(50),
    warning_type VARCHAR(50) NOT NULL,
    message TEXT,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    dismissed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- LexoRank tables
CREATE TABLE jira_plan.lexorank_buckets (
    id BIGSERIAL PRIMARY KEY,
    bucket_index INTEGER NOT NULL UNIQUE,
    name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_plan.lexorank_buckets (bucket_index, name) VALUES
    (0, 'Default'),
    (1, 'Archive'),
    (2, 'Suspended');

CREATE TABLE jira_plan.lexorank_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    bucket_id BIGINT REFERENCES jira_plan.lexorank_buckets(id) DEFAULT 0,
    rank_value VARCHAR(255) NOT NULL,
    locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP,
    locked_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

CREATE TABLE jira_plan.lexorank_balancer (
    id BIGSERIAL PRIMARY KEY,
    bucket_index INTEGER NOT NULL UNIQUE,
    last_rank VARCHAR(255),
    balance_threshold INTEGER DEFAULT 5,
    last_balanced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO jira_plan.lexorank_balancer (bucket_index, balance_threshold) VALUES (0, 5);

-- Working days
CREATE TABLE jira_plan.working_days_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    monday BOOLEAN DEFAULT TRUE,
    tuesday BOOLEAN DEFAULT TRUE,
    wednesday BOOLEAN DEFAULT TRUE,
    thursday BOOLEAN DEFAULT TRUE,
    friday BOOLEAN DEFAULT TRUE,
    saturday BOOLEAN DEFAULT FALSE,
    sunday BOOLEAN DEFAULT FALSE,
    hours_per_day DECIMAL(4,2) DEFAULT 8.00,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.non_working_days (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_id UUID REFERENCES jira_plan.working_days_config(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(config_id, date)
);

CREATE TABLE jira_plan.team_availability (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES jira_plan.plan_teams(id) ON DELETE CASCADE,
    user_id UUID,
    date DATE NOT NULL,
    hours DECIMAL(4,2),
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(team_id, user_id, date)
);

-- Board config tables
CREATE TABLE jira_plan.board_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    board_type VARCHAR(20) NOT NULL,
    column_config_mode VARCHAR(20) DEFAULT 'DEFAULT',
    constraint_source VARCHAR(50),
    is_enabled BOOLEAN DEFAULT TRUE,
    card_layout_mode VARCHAR(20) DEFAULT 'COMPACT',
    default_swimlane VARCHAR(50) DEFAULT 'NONE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.board_columns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    status_mapping JSONB DEFAULT '[]',
    label_values JSONB DEFAULT '[]',
    min_width INTEGER DEFAULT 100,
    max_width INTEGER DEFAULT 600,
    color VARCHAR(7),
    max_issues INTEGER,
    constraint_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.board_quick_filters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    filter_query TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    icon VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.board_swimlanes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    grouping_field VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    collapsed_by_default BOOLEAN DEFAULT FALSE,
    sequence INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.board_card_colors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7) NOT NULL,
    conditions JSONB NOT NULL,
    sequence INTEGER NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint tables for plan service
CREATE TYPE sprint_state AS ENUM ('FUTURE', 'ACTIVE', 'CLOSED', 'ABANDONED');

CREATE TABLE jira_plan.sprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    complete_date TIMESTAMP,
    state VARCHAR(20) DEFAULT 'FUTURE',
    sequence INTEGER,
    velocity INTEGER DEFAULT 0,
    committed_points INTEGER DEFAULT 0,
    completed_points INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.sprint_issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    plan_item_id UUID,
    issue_id UUID NOT NULL,
    rank_value VARCHAR(255),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by UUID,
    removed_at TIMESTAMP,
    removed_by UUID,
    completion_status VARCHAR(50),
    completed_at TIMESTAMP,
    UNIQUE(sprint_id, plan_item_id)
);

CREATE TABLE jira_plan.sprint_audit_log (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jira_plan.sprint_burndown (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    total_issues INTEGER NOT NULL DEFAULT 0,
    completed_issues INTEGER NOT NULL DEFAULT 0,
    remaining_points INTEGER NOT NULL DEFAULT 0,
    ideal_remaining INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sprint_id, snapshot_date)
);

-- Permissions tables
CREATE TABLE jira_plan.board_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    principal_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    UNIQUE(board_id, permission_type, principal_type, principal_id)
);

CREATE TABLE jira_plan.project_sprint_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    permission_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    principal_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    UNIQUE(project_id, permission_key, principal_type, principal_id)
);

CREATE TABLE jira_plan.board_admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    UNIQUE(board_id, user_id)
);

CREATE TABLE jira_plan.board_favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(board_id, user_id)
);

-- Indexes for plan service
CREATE INDEX idx_programs_owner_id ON jira_plan.programs(owner_id);
CREATE INDEX idx_plans_owner_id ON jira_plan.plans(owner_id);
CREATE INDEX idx_plan_items_plan_id ON jira_plan.plan_items(plan_id);
CREATE INDEX idx_plan_items_sort ON jira_plan.plan_items(plan_id, sort_order);
CREATE INDEX idx_plan_teams_plan_id ON jira_plan.plan_teams(plan_id);
CREATE INDEX idx_plan_team_members_team_id ON jira_plan.plan_team_members(team_id);
CREATE INDEX idx_plan_releases_plan_id ON jira_plan.plan_releases(plan_id);
CREATE INDEX idx_issue_dependencies_plan_id ON jira_plan.issue_dependencies(plan_id);
CREATE INDEX idx_plan_warnings_plan_id ON jira_plan.plan_warnings(plan_id);
CREATE INDEX idx_lexorank_entity ON jira_plan.lexorank_entries(entity_type, entity_id);
CREATE INDEX idx_lexorank_rank ON jira_plan.lexorank_entries(rank_value);
CREATE INDEX idx_sprints_board ON jira_plan.sprints(board_id);
CREATE INDEX idx_sprints_state ON jira_plan.sprints(state);
CREATE INDEX idx_board_configs_plan ON jira_plan.board_configs(plan_id);
CREATE INDEX idx_board_columns_board ON jira_plan.board_columns(board_id);
CREATE INDEX idx_board_quick_filters_board ON jira_plan.board_quick_filters(board_id);
CREATE INDEX idx_board_permissions_board ON jira_plan.board_permissions(board_id);
CREATE INDEX idx_project_sprint_permissions_project ON jira_plan.project_sprint_permissions(project_id);
CREATE INDEX idx_board_admins_board ON jira_plan.board_admins(board_id);
CREATE INDEX idx_board_favorites_user ON jira_plan.board_favorites(user_id);

-- ============================================
-- ADMIN TABLES (in jira_auth schema)
-- ============================================
CREATE TABLE jira_auth.admin_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    avatar_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT FALSE,
    timezone VARCHAR(100) DEFAULT 'UTC',
    language VARCHAR(20) DEFAULT 'en-US',
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_auth.user_preferences (
    user_id UUID NOT NULL REFERENCES jira_auth.admin_users(id) ON DELETE CASCADE,
    preference_key VARCHAR(255) NOT NULL,
    preference_value TEXT,
    PRIMARY KEY (user_id, preference_key)
);

CREATE TABLE jira_auth.system_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    data_type VARCHAR(50),
    is_sensitive BOOLEAN DEFAULT FALSE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_auth.appearance_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    app_name VARCHAR(255) DEFAULT 'Jira Clone',
    login_page_message TEXT,
    footer_message TEXT,
    theme VARCHAR(50) DEFAULT 'light',
    theme_config TEXT,
    color_scheme VARCHAR(100),
    fonts TEXT,
    use_system_font BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_auth.licenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_type VARCHAR(100),
    license_key TEXT,
    max_users INTEGER,
    max_projects INTEGER,
    purchase_date TIMESTAMP,
    expiry_date TIMESTAMP,
    support_entitlement VARCHAR(255),
    metadata TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_admin_users_username ON jira_auth.admin_users(username);
CREATE INDEX idx_admin_users_email ON jira_auth.admin_users(email);
CREATE INDEX idx_settings_key ON jira_auth.system_settings(setting_key);
CREATE INDEX idx_settings_category ON jira_auth.system_settings(category);

-- Seed Admin Data
INSERT INTO jira_auth.admin_users (id, username, email, display_name, password_hash, status, role, email_verified)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@example.com',
    'Administrator',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzjGQz8wTdLQnMQ.xQ9h7z7dCq',
    'ACTIVE',
    'ADMIN',
    TRUE
);

INSERT INTO jira_auth.system_settings (setting_key, setting_value, description, category, data_type, is_sensitive) VALUES
    ('application.title', 'Jira Clone', 'Application Title', 'general', 'string', FALSE),
    ('application.baseUrl', 'http://localhost:3000', 'Base URL', 'general', 'string', FALSE),
    ('application.adminEmail', 'admin@example.com', 'Admin Email', 'general', 'string', TRUE),
    ('application.dateFormat', 'MMM dd, yyyy', 'Date Format', 'general', 'string', FALSE),
    ('application.timeZone', 'UTC', 'Time Zone', 'general', 'string', FALSE),
    ('security.allowSignUp', 'true', 'Allow User Registration', 'security', 'boolean', FALSE),
    ('security.passwordMinLength', '8', 'Minimum Password Length', 'security', 'number', FALSE),
    ('security.sessionTimeout', '30', 'Session Timeout (minutes)', 'security', 'number', FALSE),
    ('attachments.maxSize', '10485760', 'Max Attachment Size (bytes)', 'attachments', 'number', FALSE),
    ('api.enabled', 'true', 'Enable API', 'api', 'boolean', FALSE),
    ('logging.level', 'INFO', 'Log Level', 'logging', 'string', FALSE),
    ('logging.audit', 'true', 'Enable Audit Logging', 'logging', 'boolean', FALSE);

INSERT INTO jira_auth.appearance_settings (id, logo_url, favicon_url, app_name, login_page_message, footer_message, theme, theme_config, color_scheme, fonts, use_system_font)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '/assets/logo.png',
    '/assets/favicon.ico',
    'Jira Clone',
    'Welcome to Jira Clone - Your Project Management Solution',
    'Powered by Jira Clone Platform',
    'light',
    '{"primaryColor":"#0052CC","secondaryColor":"#6C757D","accentColor":"#00B8D9"}',
    'default',
    '{"primaryFont":"Inter","monospaceFont":"JetBrains Mono","baseFontSize":"14px"}',
    FALSE
);

INSERT INTO jira_auth.licenses (license_type, max_users, max_projects, purchase_date, expiry_date, support_entitlement)
VALUES (
    'Standard',
    100,
    50,
    NOW() - INTERVAL '1 year',
    NOW() + INTERVAL '6 months',
    'Standard Support'
);

-- ============================================
-- FLYWAY SCHEMA HISTORY TABLE
-- ============================================
CREATE SCHEMA IF NOT EXISTS flyway;

CREATE TABLE flyway.schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR NOT NULL,
    checksum VARCHAR(200),
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);