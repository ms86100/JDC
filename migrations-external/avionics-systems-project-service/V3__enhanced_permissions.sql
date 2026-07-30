-- V3__enhanced_permissions.sql
-- Enhanced Permission System - Jira DC Compatible
-- Adds granular permission grants with user/group/role support

-- ============================================
-- ADD MISSING COLUMNS TO EXISTING TABLES
-- (needed because CREATE TABLE IF NOT EXISTS skips existing tables from V1)
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'jira_project' AND table_name = 'project_roles' AND column_name = 'is_default'
    ) THEN
        ALTER TABLE jira_project.project_roles ADD COLUMN is_default BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- ============================================
-- PERMISSION DEFINITIONS TABLE
-- Stores all available permissions in the system
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.permissions (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL, -- PROJECT, ISSUE, ADMIN, GLOBAL
    key_name VARCHAR(30) NOT NULL  -- Short identifier like 'BROWSE_PROJECTS'
);

-- ============================================
-- PERMISSION GRANTS TABLE
-- Who has what permission granted where
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.permission_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID NOT NULL REFERENCES jira_project.permission_schemes(id) ON DELETE CASCADE,

    -- Grant type: USER, GROUP, PROJECT_ROLE
    grant_type VARCHAR(20) NOT NULL,

    -- The entity receiving the permission
    entity_id UUID,                    -- For USER grants
    group_name VARCHAR(100),           -- For GROUP grants
    project_role_id UUID,              -- For PROJECT_ROLE grants

    -- The permission being granted
    permission_key VARCHAR(50) NOT NULL REFERENCES jira_project.permissions(id),

    -- Additional context
    issue_id UUID,                     -- Optional: applies to specific issue
    issue_security_level_id UUID,      -- Optional: applies to issues at this level or below

    -- When this grant is active
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT valid_grant_type CHECK (grant_type IN ('USER', 'GROUP', 'PROJECT_ROLE')),
    CONSTRAINT valid_entity CHECK (
        (grant_type = 'USER' AND entity_id IS NOT NULL) OR
        (grant_type = 'GROUP' AND group_name IS NOT NULL) OR
        (grant_type = 'PROJECT_ROLE' AND project_role_id IS NOT NULL)
    )
);

-- ============================================
-- PROJECT ROLES TABLE
-- Roles within a project (Developers, Admins, etc.)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,      -- Auto-added to new projects
    is_system_role BOOLEAN DEFAULT FALSE, -- Cannot be deleted
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_role_name UNIQUE (name)
);

-- ============================================
-- PROJECT ROLE MEMBERS
-- Users assigned to project roles
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_role_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    entity_type VARCHAR(10) NOT NULL,  -- 'USER' or 'GROUP'
    entity_id UUID,                     -- For USER type
    group_name VARCHAR(100),           -- For GROUP type
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_project_role_member UNIQUE (project_id, role_id, entity_type, entity_id, group_name)
);

-- ============================================
-- ISSUE SECURITY SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.issue_security_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- SECURITY LEVELS
-- Issues can be assigned a security level
-- Only users with access to that level (or higher) can see the issue
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.security_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID NOT NULL REFERENCES jira_project.issue_security_schemes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    "rank" INT DEFAULT 0,  -- Higher = more restrictive
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- SECURITY LEVEL MEMBERS
-- Who can access issues at a specific security level
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.security_level_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level_id UUID NOT NULL REFERENCES jira_project.security_levels(id) ON DELETE CASCADE,

    member_type VARCHAR(20) NOT NULL,  -- USER, GROUP, PROJECT_ROLE
    member_id UUID,                     -- For USER
    group_name VARCHAR(100),            -- For GROUP
    role_id UUID,                        -- For PROJECT_ROLE

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT valid_member_type CHECK (member_type IN ('USER', 'GROUP', 'PROJECT_ROLE'))
);

-- ============================================
-- PERMISSION SCHEME PROJECT ASSIGNMENTS
-- Which permission scheme is assigned to which project
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.project_permission_scheme (
    project_id UUID PRIMARY KEY REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    scheme_id UUID NOT NULL REFERENCES jira_project.permission_schemes(id),
    override BOOLEAN DEFAULT FALSE,  -- If true, overrides default scheme
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- SEED DATA: Core Permissions (Jira DC Compatible)
-- ============================================
INSERT INTO jira_project.permissions (id, name, description, category, key_name) VALUES
    -- PROJECT PERMISSIONS
    ('perm_browse_projects', 'Browse Projects', 'Ability to view projects', 'PROJECT', 'BROWSE_PROJECTS'),
    ('perm_create_projects', 'Create Projects', 'Ability to create new projects', 'PROJECT', 'CREATE_PROJECTS'),
    ('perm_admin_projects', 'Administer Projects', 'Ability to configure project settings', 'PROJECT', 'ADMINISTER_PROJECTS'),

    -- ISSUE PERMISSIONS
    ('perm_create_issues', 'Create Issues', 'Ability to create issues', 'ISSUE', 'CREATE_ISSUES'),
    ('perm_edit_issues', 'Edit Issues', 'Ability to edit issues', 'ISSUE', 'EDIT_ISSUES'),
    ('perm_delete_issues', 'Delete Issues', 'Ability to delete issues', 'ISSUE', 'DELETE_ISSUES'),
    ('perm_edit_issues_own', 'Edit Own Issues', 'Ability to edit issues created by self', 'ISSUE', 'EDIT_OWN_ISSUES'),
    ('perm_assign_issues', 'Assign Issues', 'Ability to assign issues to users', 'ISSUE', 'ASSIGN_ISSUES'),
    ('perm_assign_issues_own', 'Assign Own Issues', 'Ability to assign issues created by self', 'ISSUE', 'ASSIGN_OWN_ISSUES'),
    ('perm_resolve_issues', 'Resolve Issues', 'Ability to resolve/close issues', 'ISSUE', 'RESOLVE_ISSUES'),
    ('perm_close_issues', 'Close Issues', 'Ability to close issues', 'ISSUE', 'CLOSE_ISSUES'),
    ('perm_modify_reporter', 'Modify Reporter', 'Ability to modify issue reporter', 'ISSUE', 'MODIFY_REPORTER'),
    ('perm_delete_issues_own', 'Delete Own Issues', 'Ability to delete issues created by self', 'ISSUE', 'DELETE_OWN_ISSUES'),

    -- COMMENT PERMISSIONS
    ('perm_create_comments', 'Create Comments', 'Ability to add comments', 'ISSUE', 'CREATE_COMMENTS'),
    ('perm_edit_comments', 'Edit Comments', 'Ability to edit comments', 'ISSUE', 'EDIT_COMMENTS'),
    ('perm_delete_comments', 'Delete Comments', 'Ability to delete comments', 'ISSUE', 'DELETE_COMMENTS'),
    ('perm_edit_all_comments', 'Edit All Comments', 'Ability to edit any comment', 'ISSUE', 'EDIT_ALL_COMMENTS'),
    ('perm_delete_all_comments', 'Delete All Comments', 'Ability to delete any comment', 'ISSUE', 'DELETE_ALL_COMMENTS'),

    -- ATTACHMENT PERMISSIONS
    ('perm_create_attachments', 'Create Attachments', 'Ability to attach files', 'ISSUE', 'CREATE_ATTACHMENTS'),
    ('perm_delete_attachments', 'Delete Attachments', 'Ability to delete attachments', 'ISSUE', 'DELETE_ATTACHMENTS'),
    ('perm_delete_own_attachments', 'Delete Own Attachments', 'Ability to delete own attachments', 'ISSUE', 'DELETE_OWN_ATTACHMENTS'),

    -- WORKLOG PERMISSIONS
    ('perm_work_on_issues', 'Work On Issues', 'Ability to log work', 'ISSUE', 'WORK_ON_ISSUES'),
    ('perm_edit_own_worklogs', 'Edit Own Worklogs', 'Ability to edit own worklogs', 'ISSUE', 'EDIT_OWN_WORKLOGS'),
    ('perm_edit_all_worklogs', 'Edit All Worklogs', 'Ability to edit any worklog', 'ISSUE', 'EDIT_ALL_WORKLOGS'),
    ('perm_delete_own_worklogs', 'Delete Own Worklogs', 'Ability to delete own worklogs', 'ISSUE', 'DELETE_OWN_WORKLOGS'),
    ('perm_delete_all_worklogs', 'Delete All Worklogs', 'Ability to delete any worklog', 'ISSUE', 'DELETE_ALL_WORKLOGS'),

    -- SECURITY PERMISSIONS
    ('perm_security_level', 'Set Security Level', 'Ability to set issue security level', 'ISSUE', 'SET_SECURITY_LEVEL'),
    ('perm_admin_security', 'Administer Security', 'Ability to configure security schemes', 'ADMIN', 'ADMINISTER_SECURITY'),

    -- AGGREGATE PERMISSIONS
    ('perm_view_voters_watchers', 'View Voters and Watchers', 'Ability to see who voted/watched', 'ISSUE', 'VIEW_VOTERS_AND_WATCHERS'),
    ('perm_manage_watchers', 'Manage Watchers', 'Ability to manage watchers', 'ISSUE', 'MANAGE_WATCHERS'),
    ('perm_comment_sticky', 'Comment Sticky', 'Ability to make comments sticky', 'ISSUE', 'COMMENT_STICKY'),

    -- GLOBAL PERMISSIONS
    ('perm_sysadmin', 'System Administrator', 'Full system administration', 'GLOBAL', 'SYSTEM_ADMIN'),
    ('perm_admin', 'Administrator', 'Application administration', 'GLOBAL', 'ADMIN'),
    ('perm_user', 'User', 'Basic user access', 'GLOBAL', 'USER')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SEED DATA: Default Project Roles
-- ============================================
INSERT INTO jira_project.project_roles (id, name, description, is_default, is_system_role) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Project Administrator', 'Can manage project settings and members', TRUE, TRUE),
    ('22222222-2222-2222-2222-222222222222', 'Developer', 'Can create, edit, and resolve issues', TRUE, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Committer', 'Can commit code linked to issues', TRUE, TRUE),
    ('44444444-4444-4444-4444-444444444444', 'Users', 'Can view project and issues', TRUE, TRUE),
    ('55555555-5555-5555-5555-555555555555', 'Viewers', 'Read-only access to the project', TRUE, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- UPDATE DEFAULT PERMISSION SCHEME
-- Give all permissions to admin role for admin scheme
-- ============================================
INSERT INTO jira_project.permission_grants (scheme_id, grant_type, project_role_id, permission_key)
SELECT
    '00000000-0000-0000-0005-000000000001',  -- Default Permission Scheme
    'PROJECT_ROLE',
    '00000000-0000-0000-0000-000000000001',  -- PROJECT_ADMIN role UUID
    p.id
FROM jira_project.permissions p
WHERE p.category IN ('PROJECT', 'ISSUE', 'ADMIN')
ON CONFLICT DO NOTHING;

-- Give developer permissions
INSERT INTO jira_project.permission_grants (scheme_id, grant_type, project_role_id, permission_key)
SELECT
    '00000000-0000-0000-0005-000000000001',
    'PROJECT_ROLE',
    '22222222-2222-2222-2222-222222222222',
    p.id
FROM jira_project.permissions p
WHERE p.key_name IN (
    'BROWSE_PROJECTS', 'CREATE_ISSUES', 'EDIT_ISSUES', 'ASSIGN_ISSUES',
    'RESOLVE_ISSUES', 'CREATE_COMMENTS', 'EDIT_COMMENTS',
    'CREATE_ATTACHMENTS', 'DELETE_OWN_ATTACHMENTS', 'WORK_ON_ISSUES',
    'EDIT_OWN_WORKLOGS', 'DELETE_OWN_WORKLOGS', 'VIEW_VOTERS_AND_WATCHERS'
)
ON CONFLICT DO NOTHING;

-- Give viewer permissions
INSERT INTO jira_project.permission_grants (scheme_id, grant_type, project_role_id, permission_key)
SELECT
    '00000000-0000-0000-0005-000000000001',
    'PROJECT_ROLE',
    '55555555-5555-5555-5555-555555555555',
    p.id
FROM jira_project.permissions p
WHERE p.key_name IN ('BROWSE_PROJECTS', 'CREATE_COMMENTS', 'VIEW_VOTERS_AND_WATCHERS')
ON CONFLICT DO NOTHING;

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_permission_grants_scheme ON jira_project.permission_grants(scheme_id);
CREATE INDEX IF NOT EXISTS idx_permission_grants_user ON jira_project.permission_grants(entity_id) WHERE grant_type = 'USER';
CREATE INDEX IF NOT EXISTS idx_permission_grants_group ON jira_project.permission_grants(group_name) WHERE grant_type = 'GROUP';
CREATE INDEX IF NOT EXISTS idx_permission_grants_role ON jira_project.permission_grants(project_role_id) WHERE grant_type = 'PROJECT_ROLE';
CREATE INDEX IF NOT EXISTS idx_project_role_members_project ON jira_project.project_role_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_role_members_role ON jira_project.project_role_members(role_id);
CREATE INDEX IF NOT EXISTS idx_security_level_members_level ON jira_project.security_level_members(level_id);

-- ============================================
-- FUNCTION: Check if user has permission
-- Usage: SELECT check_permission('user-uuid', 'project-uuid', 'EDIT_ISSUES')
-- ============================================
CREATE OR REPLACE FUNCTION jira_project.check_permission(
    p_user_id UUID,
    p_project_id UUID,
    p_permission_key VARCHAR(50)
) RETURNS BOOLEAN AS $$
DECLARE
    v_has_permission BOOLEAN := FALSE;
    v_is_project_admin BOOLEAN;
    v_is_system_admin BOOLEAN;
    v_user_groups TEXT[];
    v_user_roles UUID[];
BEGIN
    -- First check if user is system admin
    SELECT EXISTS(
        SELECT 1 FROM jira_auth.users u
        JOIN jira_auth.user_roles ur ON u.id = ur.user_id
        JOIN jira_auth.roles r ON ur.role_id = r.id
        WHERE u.id = p_user_id AND r.name = 'ROLE_ADMIN'
    ) INTO v_is_system_admin;

    IF v_is_system_admin THEN
        RETURN TRUE;
    END IF;

    -- Check if user is project admin (has admin permission in project)
    SELECT check_permission(p_user_id, p_project_id, 'ADMINISTER_PROJECTS') INTO v_is_project_admin;

    IF v_is_project_admin THEN
        -- Project admins have all permissions within that project
        RETURN TRUE;
    END IF;

    -- Get user's groups
    SELECT ARRAY_AGG(group_name) INTO v_user_groups
    FROM jira_auth.user_groups
    WHERE user_id = p_user_id;

    -- Get user's roles in this project
    SELECT ARRAY_AGG(role_id) INTO v_user_roles
    FROM jira_project.project_role_members
    WHERE project_id = p_project_id
    AND ((entity_type = 'USER' AND entity_id = p_user_id)
         OR (entity_type = 'GROUP' AND group_name = ANY(v_user_groups)));

    -- Check direct permission grant to user
    SELECT EXISTS(
        SELECT 1 FROM jira_project.permission_grants pg
        JOIN jira_project.permission_schemes ps ON pg.scheme_id = ps.id
        JOIN jira_project.project_permission_scheme pps ON ps.id = pps.scheme_id
        WHERE pps.project_id = p_project_id
        AND pg.grant_type = 'USER'
        AND pg.entity_id = p_user_id
        AND pg.permission_key = p_permission_key
    ) INTO v_has_permission;

    IF v_has_permission THEN
        RETURN TRUE;
    END IF;

    -- Check permission via group
    SELECT EXISTS(
        SELECT 1 FROM jira_project.permission_grants pg
        JOIN jira_project.permission_schemes ps ON pg.scheme_id = ps.id
        JOIN jira_project.project_permission_scheme pps ON ps.id = pps.scheme_id
        WHERE pps.project_id = p_project_id
        AND pg.grant_type = 'GROUP'
        AND pg.group_name = ANY(v_user_groups)
        AND pg.permission_key = p_permission_key
    ) INTO v_has_permission;

    IF v_has_permission THEN
        RETURN TRUE;
    END IF;

    -- Check permission via project role
    SELECT EXISTS(
        SELECT 1 FROM jira_project.permission_grants pg
        JOIN jira_project.permission_schemes ps ON pg.scheme_id = ps.id
        JOIN jira_project.project_permission_scheme pps ON ps.id = pps.scheme_id
        WHERE pps.project_id = p_project_id
        AND pg.grant_type = 'PROJECT_ROLE'
        AND pg.project_role_id = ANY(v_user_roles)
        AND pg.permission_key = p_permission_key
    ) INTO v_has_permission;

    RETURN v_has_permission;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Add security_level column to issues if not exists
-- ============================================
-- jira_issue is owned by issue-service, which may not have migrated yet.
-- Only attempt the cross-service column add when that table actually exists.
DO $$
BEGIN
    -- First check if schema exists (required before checking tables)
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'jira_issue')
       AND EXISTS (
           SELECT 1 FROM information_schema.tables
           WHERE table_schema = 'jira_issue'
           AND table_name = 'issues'
       )
       AND NOT EXISTS (
           SELECT 1 FROM information_schema.columns
           WHERE table_schema = 'jira_issue'
           AND table_name = 'issues'
           AND column_name = 'security_level_id'
       ) THEN
        ALTER TABLE jira_issue.issues ADD COLUMN security_level_id UUID;
    END IF;
END $$;

COMMENT ON TABLE jira_project.permissions IS 'All available permissions in the system';
COMMENT ON TABLE jira_project.permission_grants IS 'Granular permission grants to users, groups, or project roles';
COMMENT ON TABLE jira_project.project_roles IS 'Roles that can be assigned to users within a project';
COMMENT ON TABLE jira_project.project_role_members IS 'Members assigned to project roles';
COMMENT ON TABLE jira_project.issue_security_schemes IS 'Security schemes for controlling issue visibility';
COMMENT ON TABLE jira_project.security_levels IS 'Security levels within a scheme';
COMMENT ON TABLE jira_project.security_level_members IS 'Users/groups who can access each security level';