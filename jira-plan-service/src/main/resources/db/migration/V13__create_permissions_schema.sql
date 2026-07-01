CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Board and project-level permissions
-- Mimics Jira GreenHopper permission system

-- Board permission types
CREATE TYPE board_permission_type AS ENUM (
    'VIEW',      -- Can view board
    'EDIT',      -- Can edit board configuration
    'ADMIN',     -- Full board admin access
    'MANAGE_SPRINTS',  -- Can create/edit/delete sprints
    'EDIT_SPRINTS'     -- Can edit sprint name and goal
);

-- Board permissions
CREATE TABLE jira_plan.board_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,  -- 'USER', 'GROUP'
    principal_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    UNIQUE(board_id, permission_type, principal_type, principal_id)
);

-- Project-level sprint permissions (mimics Jira GreenHopper)
CREATE TABLE jira_plan.project_sprint_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID,  -- References jira_projects(id) if exists, otherwise NULL for global
    permission_key VARCHAR(100) NOT NULL,  -- 'MANAGE_SPRINTS', 'START_STOP_SPRINTS', 'EDIT_SPRINT_NAME_AND_GOAL'
    principal_type VARCHAR(20) NOT NULL,  -- 'USER', 'GROUP'
    principal_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    UNIQUE(project_id, permission_key, principal_type, principal_id)
);

-- Board admin (specific admin users for each board)
CREATE TABLE jira_plan.board_admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    UNIQUE(board_id, user_id)
);

-- Quick filter sharing (who can see/use quick filters)
CREATE TABLE jira_plan.board_quick_filter_sharing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quick_filter_id UUID REFERENCES jira_plan.board_quick_filters(id) ON DELETE CASCADE,
    shared_with_type VARCHAR(20) NOT NULL,  -- 'USER', 'GROUP', 'PROJECT'
    shared_with_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Board favorite (users who have favorited the board)
CREATE TABLE jira_plan.board_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    sequence INTEGER DEFAULT 0,  -- Order in user's favorites list
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(board_id, user_id)
);

-- Indexes
CREATE INDEX idx_board_permissions_board ON jira_plan.board_permissions(board_id);
CREATE INDEX idx_board_permissions_principal ON jira_plan.board_permissions(principal_type, principal_id);
CREATE INDEX idx_project_sprint_permissions_project ON jira_plan.project_sprint_permissions(project_id);
CREATE INDEX idx_project_sprint_permissions_key ON jira_plan.project_sprint_permissions(permission_key);
CREATE INDEX idx_board_admins_board ON jira_plan.board_admins(board_id);
CREATE INDEX idx_board_admins_user ON jira_plan.board_admins(user_id);
CREATE INDEX idx_board_favorites_user ON jira_plan.board_favorites(user_id);
CREATE INDEX idx_board_favorites_board ON jira_plan.board_favorites(board_id);