-- V8__sysdops_role_presets.sql
-- SYSDOPS-specific project role presets with permission metadata

-- Add columns for permission descriptions and system-role flag
ALTER TABLE jira_admin.project_roles ADD COLUMN IF NOT EXISTS permissions JSONB DEFAULT '[]';
ALTER TABLE jira_admin.project_roles ADD COLUMN IF NOT EXISTS is_system_role BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_admin.project_roles ADD COLUMN IF NOT EXISTS role_type VARCHAR(50);
ALTER TABLE jira_admin.project_roles ADD COLUMN IF NOT EXISTS project_id VARCHAR(255);

-- Seed SYSDOPS-specific project roles
INSERT INTO jira_admin.project_roles (id, name, description, permissions, is_system_role, is_default) VALUES
    ('e0000001-0000-0000-0000-000000000001', 'Administrator',
     'Full project administration',
     '["MANAGE_USERS","MANAGE_BOARDS","DELETE_ISSUES","EDIT_ISSUES","READ_ISSUES","MANAGE_WORKFLOWS","MANAGE_SCHEMES"]',
     true, true),
    ('e0000001-0000-0000-0000-000000000002', 'Maintainer',
     'Project maintenance without deletion rights',
     '["MANAGE_USERS","MANAGE_BOARDS","EDIT_ISSUES","READ_ISSUES"]',
     true, true),
    ('e0000001-0000-0000-0000-000000000003', 'Contributor',
     'Issue creation and editing',
     '["EDIT_ISSUES","READ_ISSUES","CREATE_ISSUES","ADD_COMMENTS","ATTACH_FILES"]',
     true, true),
    ('e0000001-0000-0000-0000-000000000004', 'Reader',
     'Read-only access to project',
     '["READ_ISSUES"]',
     true, true)
ON CONFLICT (id) DO NOTHING;
