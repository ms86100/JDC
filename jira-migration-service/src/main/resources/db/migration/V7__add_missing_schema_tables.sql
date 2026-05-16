-- V7__add_missing_schema_tables.sql
-- Creates tables referenced by other services but defined centrally for migration tracking

-- ============================================
-- SECURITY LEVELS (referenced by jira-project-service)
-- ============================================
CREATE TABLE jira_project.security_levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    level_type VARCHAR(20) DEFAULT 'RESTRICTED',
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.security_level_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    security_level_id UUID NOT NULL REFERENCES jira_project.security_levels(id) ON DELETE CASCADE,
    member_type VARCHAR(20) NOT NULL,
    member_id UUID,
    group_name VARCHAR(100),
    added_by UUID,
    added_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_project.issue_security_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    default_level_id UUID,
    project_id UUID,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_levels_scheme ON jira_project.security_levels(scheme_id);
CREATE INDEX idx_security_level_members_level ON jira_project.security_level_members(security_level_id);
CREATE INDEX idx_security_level_members_member ON jira_project.security_level_members(member_id);
CREATE INDEX idx_issue_security_schemes_project ON jira_project.issue_security_schemes(project_id);

-- ============================================
-- PROJECT ROLE MEMBERS (referenced by jira-project-service)
-- ============================================
CREATE TABLE jira_project.project_role_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_role_id UUID NOT NULL,
    project_id UUID,
    member_type VARCHAR(20) NOT NULL,
    member_id UUID,
    group_name VARCHAR(100),
    added_by UUID,
    added_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_role_members_role ON jira_project.project_role_members(project_role_id);
CREATE INDEX idx_project_role_members_project ON jira_project.project_role_members(project_id);

-- ============================================
-- USER GROUPS (referenced by jira-auth-service)
-- ============================================
CREATE TABLE jira_auth.user_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    group_type VARCHAR(50) DEFAULT 'JIRA_INTERNAL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_auth.user_group_memberships (
    user_id UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES jira_auth.user_groups(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    added_by UUID,
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_user_groups_active ON jira_auth.user_groups(is_active);
CREATE INDEX idx_user_group_memberships_user ON jira_auth.user_group_memberships(user_id);
CREATE INDEX idx_user_group_memberships_group ON jira_auth.user_group_memberships(group_id);

-- ============================================
-- SCREEN TABS (referenced by jira-admin-service)
-- ============================================
CREATE TABLE jira_admin.screen_tabs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_id UUID,
    tab_name VARCHAR(255),
    field_ids TEXT,
    sequence INTEGER DEFAULT 0
);

CREATE INDEX idx_screen_tabs_screen ON jira_admin.screen_tabs(screen_id);

-- ============================================
-- BOARD CONFIGURATIONS (referenced by jira-sprint-service)
-- ============================================
CREATE TABLE jira_sprint.board_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL,
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

CREATE INDEX idx_board_configs_board ON jira_sprint.board_configs(board_id);

-- ============================================
-- PLAN SERVICE TABLES (referenced by jira-plan-service)
-- ============================================
CREATE TABLE jira_plan.cumulative_flow_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL,
    sprint_id UUID,
    column_name VARCHAR(100),
    data_date DATE,
    issue_count INTEGER,
    last_processed_date TIMESTAMP
);

CREATE TABLE jira_plan.burndown_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID NOT NULL,
    data_date DATE NOT NULL,
    ideal_remaining_points DOUBLE PRECISION,
    actual_remaining_points DOUBLE PRECISION,
    ideal_issue_count INTEGER,
    actual_issue_count INTEGER,
    committed_points INTEGER,
    completed_points INTEGER,
    last_processed_date TIMESTAMP
);

CREATE TABLE jira_plan.board_quick_filter_sharing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quick_filter_id UUID NOT NULL,
    shared_with_user_id UUID,
    shared_with_group VARCHAR(100),
    permission_level VARCHAR(20) DEFAULT 'VIEW',
    shared_at TIMESTAMP,
    shared_by UUID
);

CREATE TABLE jira_plan.lexorank_audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_rank VARCHAR(255),
    new_rank VARCHAR(255),
    bucket INTEGER NOT NULL DEFAULT 0,
    user_id UUID,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    details TEXT
);

CREATE TABLE jira_plan.sprint_goal_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID NOT NULL,
    old_goal TEXT,
    new_goal TEXT,
    changed_by UUID NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    change_reason VARCHAR(255)
);

CREATE INDEX idx_cumulative_flow_data_board ON jira_plan.cumulative_flow_data(board_id);
CREATE INDEX idx_cumulative_flow_data_date ON jira_plan.cumulative_flow_data(data_date);
CREATE INDEX idx_burndown_data_sprint ON jira_plan.burndown_data(sprint_id);
CREATE INDEX idx_burndown_data_date ON jira_plan.burndown_data(data_date);
CREATE INDEX idx_board_quick_filter_sharing_filter ON jira_plan.board_quick_filter_sharing(quick_filter_id);
CREATE INDEX idx_lexorank_audit_log_entity ON jira_plan.lexorank_audit_log(entity_type, entity_id);
CREATE INDEX idx_sprint_goal_history_sprint ON jira_plan.sprint_goal_history(sprint_id);

-- ============================================
-- COMMENTS (referenced by jira-comment-service)
-- ============================================
ALTER TABLE jira_comment.comments
    ADD COLUMN IF NOT EXISTS internal BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN jira_comment.comments.internal IS 'Internal comments hidden from customers';

-- ============================================
-- ATTACHMENTS (referenced by jira-attachment-service)
-- ============================================
ALTER TABLE jira_attachment.attachments
    ADD COLUMN IF NOT EXISTS mime_type_detected VARCHAR(100),
    ADD COLUMN IF NOT EXISTS thumbnail_path VARCHAR(500);

COMMENT ON COLUMN jira_attachment.attachments.mime_type_detected IS 'Server-detected MIME type';
COMMENT ON COLUMN jira_attachment.attachments.thumbnail_path IS 'Path to generated thumbnail';