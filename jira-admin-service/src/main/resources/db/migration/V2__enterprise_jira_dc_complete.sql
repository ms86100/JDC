-- V2__enterprise_jira_dc_complete.sql
-- Complete Enterprise Jira DC Features
-- Fills remaining gaps for Jira Data Center Scrum compliance

CREATE SCHEMA IF NOT EXISTS jira_search;
CREATE SCHEMA IF NOT EXISTS jira_plan;
CREATE SCHEMA IF NOT EXISTS jira_issue;

-- ============================================
-- SCHEMA: Permission Schemes (Enterprise RBAC)
-- ============================================

-- Permission scheme definition
CREATE TABLE IF NOT EXISTS jira_admin.permission_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Project permission scheme associations
CREATE TABLE IF NOT EXISTS jira_admin.project_permission_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    permission_scheme_id UUID NOT NULL REFERENCES jira_admin.permission_schemes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id)
);

-- Permission types (Jira DC standard permissions)
CREATE TABLE IF NOT EXISTS jira_admin.permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    permission_type VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed standard Jira DC permissions
INSERT INTO jira_admin.permissions (permission_key, permission_type, description) VALUES
    ('BROWSE_PROJECTS', 'PROJECT', 'Browse projects'),
    ('CREATE_PROJECTS', 'PROJECT', 'Create projects'),
    ('ADMINISTER_PROJECTS', 'PROJECT', 'Administer projects'),
    ('VIEW_DEV_TOOLS', 'PROJECT', 'View development tools'),
    ('EDIT_ISSUES', 'ISSUE', 'Edit issues'),
    ('ASSIGN_ISSUES', 'ISSUE', 'Assign issues'),
    ('ASSIGNABLE_USER', 'ISSUE', 'Assign to users'),
    ('COMMENT_ISSUES', 'ISSUE', 'Comment on issues'),
    ('CREATE_ATTACHMENTS', 'ISSUE', 'Create attachments'),
    ('DELETE_ISSUES', 'ISSUE', 'Delete issues'),
    ('DELETE_ATTACHMENTS', 'ISSUE', 'Delete attachments'),
    ('EDIT_COMMENTS', 'ISSUE', 'Edit comments'),
    ('DELETE_COMMENTS', 'ISSUE', 'Delete comments'),
    ('WORK_ON_ISSUES', 'ISSUE', 'Work on issues'),
    ('SCHEDULE_ISSUES', 'ISSUE', 'Schedule issues'),
    ('MOVE_ISSUES', 'ISSUE', 'Move issues'),
    ('TRANSITION_ISSUES', 'ISSUE', 'Transition issues'),
    ('RESOLVE_ISSUES', 'ISSUE', 'Resolve issues'),
    ('CLOSED_ISSUES', 'ISSUE', 'Close issues'),
    ('MODIFY_REPORTER', 'ISSUE', 'Modify reporter'),
    ('DELETE_OWN_COMMENTS', 'ISSUE', 'Delete own comments'),
    ('EDIT_OWN_COMMENTS', 'ISSUE', 'Edit own comments'),
    ('CREATE_SPRINT', 'PROJECT', 'Create sprint'),
    ('EDIT_SPRINT', 'PROJECT', 'Edit sprint'),
    ('DELETE_SPRINT', 'PROJECT', 'Delete sprint'),
    ('START_SPRINT', 'PROJECT', 'Start sprint'),
    ('CLOSE_SPRINT', 'PROJECT', 'Close sprint'),
    ('MANAGE_SPRINTS', 'PROJECT', 'Manage sprints'),
    ('VIEW_SPRINTS', 'PROJECT', 'View sprints'),
    ('ADMINISTER', 'GLOBAL', 'Jira system administration'),
    ('SYS_ADMIN', 'GLOBAL', 'System administration'),
    ('LICENSED_APPLICATION', 'GLOBAL', 'Application access')
ON CONFLICT (permission_key) DO NOTHING;

-- Permission scheme grants (who gets which permission)
CREATE TABLE IF NOT EXISTS jira_admin.permission_scheme_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_scheme_id UUID NOT NULL REFERENCES jira_admin.permission_schemes(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES jira_admin.permissions(id),
    holder_type VARCHAR(50) NOT NULL,
    holder_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(permission_scheme_id, permission_id, holder_type, holder_id)
);

-- Project roles
CREATE TABLE IF NOT EXISTS jira_admin.project_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default project roles
INSERT INTO jira_admin.project_roles (id, name, description, is_default) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Administrators', 'Project administrators with full access', TRUE),
    ('a0000000-0000-0000-0000-000000000002', 'Developers', 'Developers who work on issues', TRUE),
    ('a0000000-0000-0000-0000-000000000003', 'Users', 'General project users', TRUE),
    ('a0000000-0000-0000-0000-000000000004', 'Viewers', 'Users with view-only access', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Role permissions (what each role can do)
CREATE TABLE IF NOT EXISTS jira_admin.role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_role_id UUID NOT NULL REFERENCES jira_admin.project_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES jira_admin.permissions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_role_id, permission_id)
);

-- User-role associations for projects
CREATE TABLE IF NOT EXISTS jira_admin.project_role_actors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    project_role_id UUID NOT NULL REFERENCES jira_admin.project_roles(id),
    holder_type VARCHAR(50) NOT NULL,
    holder_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, project_role_id, holder_type, holder_id)
);

-- Groups table (for permission resolution)
CREATE TABLE IF NOT EXISTS jira_admin.groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User-group membership
CREATE TABLE IF NOT EXISTS jira_admin.user_group_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES jira_admin.groups(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, group_id)
);

-- Seed default groups
INSERT INTO jira_admin.groups (id, group_name, description) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'jira-administrators', 'Jira administrators with full access'),
    ('b0000000-0000-0000-0000-000000000002', 'jira-software-users', 'Standard Jira software users')
ON CONFLICT (group_name) DO NOTHING;

-- Role-permission defaults (assign default permissions to roles)
INSERT INTO jira_admin.role_permissions (project_role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000001'::uuid, id FROM jira_admin.permissions WHERE permission_type IN ('PROJECT', 'ISSUE')
ON CONFLICT DO NOTHING;

INSERT INTO jira_admin.role_permissions (project_role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000002'::uuid, id FROM jira_admin.permissions
WHERE permission_key IN ('EDIT_ISSUES', 'ASSIGN_ISSUES', 'COMMENT_ISSUES', 'CREATE_ATTACHMENTS',
    'WORK_ON_ISSUES', 'SCHEDULE_ISSUES', 'MOVE_ISSUES', 'TRANSITION_ISSUES', 'VIEW_SPRINTS')
ON CONFLICT DO NOTHING;

INSERT INTO jira_admin.role_permissions (project_role_id, permission_id)
SELECT 'a0000000-0000-0000-0000-000000000003'::uuid, id FROM jira_admin.permissions
WHERE permission_key IN ('BROWSE_PROJECTS', 'EDIT_ISSUES', 'COMMENT_ISSUES', 'CREATE_ATTACHMENTS', 'VIEW_SPRINTS')
ON CONFLICT DO NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_permission_schemes_project ON jira_admin.project_permission_schemes(project_id);
CREATE INDEX IF NOT EXISTS idx_permission_scheme_grants_scheme ON jira_admin.permission_scheme_grants(permission_scheme_id);
CREATE INDEX IF NOT EXISTS idx_permission_scheme_grants_permission ON jira_admin.permission_scheme_grants(permission_id);
CREATE INDEX IF NOT EXISTS idx_permission_scheme_grants_holder ON jira_admin.permission_scheme_grants(holder_type, holder_id);
CREATE INDEX IF NOT EXISTS idx_role_actors_project ON jira_admin.project_role_actors(project_id);
CREATE INDEX IF NOT EXISTS idx_role_actors_role ON jira_admin.project_role_actors(project_role_id);
CREATE INDEX IF NOT EXISTS idx_role_actors_holder ON jira_admin.project_role_actors(holder_type, holder_id);
CREATE INDEX IF NOT EXISTS idx_user_group_user ON jira_admin.user_group_membership(user_id);
CREATE INDEX IF NOT EXISTS idx_user_group_group ON jira_admin.user_group_membership(group_id);

-- ============================================
-- SCHEMA: Screen Schemes and Field Configuration
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.screen_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_admin.issue_type_screen_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_admin.issue_type_screen_scheme_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_type_screen_scheme_id UUID NOT NULL REFERENCES jira_admin.issue_type_screen_schemes(id) ON DELETE CASCADE,
    issue_type_id VARCHAR(50),
    screen_scheme_id UUID NOT NULL REFERENCES jira_admin.screen_schemes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(issue_type_screen_scheme_id, issue_type_id)
);

CREATE TABLE IF NOT EXISTS jira_admin.field_configuration_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_admin.field_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_admin.field_configuration_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_configuration_id UUID NOT NULL REFERENCES jira_admin.field_configurations(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    is_shown BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    is_editable BOOLEAN DEFAULT TRUE,
    renderer VARCHAR(100),
    ordering INTEGER DEFAULT 0,
    UNIQUE(field_configuration_id, field_key)
);

CREATE TABLE IF NOT EXISTS jira_admin.field_layout_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default screen scheme
INSERT INTO jira_admin.screen_schemes (id, name, description, is_default) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Default Screen Scheme', 'Default screen scheme for all projects', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Seed default issue type screen scheme
INSERT INTO jira_admin.issue_type_screen_schemes (id, name, description, is_default) VALUES
    ('c0000000-0000-0000-0000-000000000002', 'Default Issue Type Screen Scheme', 'Default mapping', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Seed default field configuration
INSERT INTO jira_admin.field_configurations (id, name, description) VALUES
    ('c0000000-0000-0000-0000-000000000003', 'Default Field Configuration', 'Default field settings')
ON CONFLICT (id) DO NOTHING;

-- Seed default field configuration items
INSERT INTO jira_admin.field_configuration_items (field_configuration_id, field_key, is_shown, is_required) VALUES
    ('c0000000-0000-0000-0000-000000000003', 'summary', TRUE, TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'issuetype', TRUE, TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'status', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'priority', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'assignee', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'reporter', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'labels', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'project', TRUE, TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'description', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'attachment', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'duedate', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'security', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'components', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'fixforversion', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'affectsversions', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'linkedissues', TRUE, FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'customfield_10010', TRUE, FALSE)
ON CONFLICT DO NOTHING;

-- Seed default field configuration scheme
INSERT INTO jira_admin.field_configuration_schemes (id, name, description, is_default) VALUES
    ('c0000000-0000-0000-0000-000000000004', 'Default Field Configuration Scheme', 'Default scheme', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_screen_schemes_default ON jira_admin.screen_schemes(is_default);
CREATE INDEX IF NOT EXISTS idx_issue_type_screen_schemes_default ON jira_admin.issue_type_screen_schemes(is_default);
CREATE INDEX IF NOT EXISTS idx_field_config_items_config ON jira_admin.field_configuration_items(field_configuration_id);

-- ============================================
-- SCHEMA: Notification Schemes
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.notification_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_admin.notification_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed notification events
INSERT INTO jira_admin.notification_events (id, event_key, name, description) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'issue_created', 'Issue Created', 'Triggered when an issue is created'),
    ('d0000000-0000-0000-0000-000000000002', 'issue_updated', 'Issue Updated', 'Triggered when an issue is updated'),
    ('d0000000-0000-0000-0000-000000000003', 'issue_assigned', 'Issue Assigned', 'Triggered when an issue is assigned'),
    ('d0000000-0000-0000-0000-000000000004', 'issue_resolved', 'Issue Resolved', 'Triggered when an issue is resolved'),
    ('d0000000-0000-0000-0000-000000000005', 'issue_closed', 'Issue Closed', 'Triggered when an issue is closed'),
    ('d0000000-0000-0000-0000-000000000006', 'issue_commented', 'Issue Commented', 'Triggered when a comment is added'),
    ('d0000000-0000-0000-0000-000000000007', 'issue_deleted', 'Issue Deleted', 'Triggered when an issue is deleted'),
    ('d0000000-0000-0000-0000-000000000008', 'worklog_added', 'Work Log Added', 'Triggered when work is logged'),
    ('d0000000-0000-0000-0000-000000000009', 'sprint_started', 'Sprint Started', 'Triggered when a sprint starts'),
    ('d0000000-0000-0000-0000-00000000000a', 'sprint_closed', 'Sprint Closed', 'Triggered when a sprint closes')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS jira_admin.notification_scheme_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_scheme_id UUID NOT NULL REFERENCES jira_admin.notification_schemes(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES jira_admin.notification_events(id),
    notification_type VARCHAR(50) NOT NULL,
    notifier_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(notification_scheme_id, event_id, notification_type, notifier_id)
);

CREATE TABLE IF NOT EXISTS jira_admin.project_notification_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    notification_scheme_id UUID NOT NULL REFERENCES jira_admin.notification_schemes(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id)
);

-- Seed default notification scheme
INSERT INTO jira_admin.notification_schemes (id, name, description, is_default) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'Default Notification Scheme', 'Default scheme for all projects', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Default notification: reporter and assignee for issue events
INSERT INTO jira_admin.notification_scheme_events (notification_scheme_id, event_id, notification_type, notifier_id) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'PROJECT_ROLE', 'a0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'PROJECT_ROLE', 'a0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003', 'CURRENT_USER', NULL),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000004', 'REPORTER', NULL),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000005', 'REPORTER', NULL),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000006', 'CURRENT_USER', NULL),
    ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000008', 'REPORTER', NULL)
ON CONFLICT DO NOTHING;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_notification_scheme_events_scheme ON jira_admin.notification_scheme_events(notification_scheme_id);
CREATE INDEX IF NOT EXISTS idx_notification_scheme_events_event ON jira_admin.notification_scheme_events(event_id);
CREATE INDEX IF NOT EXISTS idx_project_notification_schemes_project ON jira_admin.project_notification_schemes(project_id);

-- ============================================
-- SCHEMA: Saved Filters (JQL)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_search.saved_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    jql_query TEXT NOT NULL,
    owner_id UUID NOT NULL,
    is_shareable BOOLEAN DEFAULT TRUE,
    is_favorite BOOLEAN DEFAULT FALSE,
    favorite_count INTEGER DEFAULT 0,
    filter_columns JSONB DEFAULT '[]',
    view_format VARCHAR(50) DEFAULT 'list',
    group_by VARCHAR(100),
    sort_columns JSONB DEFAULT '[{"field": "created", "direction": "DESC"}]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_search.filter_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filter_id UUID NOT NULL REFERENCES jira_search.saved_filters(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    permission_id UUID,
    can_edit BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(filter_id, permission_type, permission_id)
);

CREATE TABLE IF NOT EXISTS jira_search.filter_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filter_id UUID NOT NULL REFERENCES jira_search.saved_filters(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(filter_id, user_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_saved_filters_owner ON jira_search.saved_filters(owner_id);
CREATE INDEX IF NOT EXISTS idx_saved_filters_favorite ON jira_search.saved_filters(is_favorite, favorite_count DESC);
CREATE INDEX IF NOT EXISTS idx_filter_permissions_filter ON jira_search.filter_permissions(filter_id);
CREATE INDEX IF NOT EXISTS idx_filter_favorites_user ON jira_search.filter_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_filter_favorites_sequence ON jira_search.filter_favorites(user_id, sequence);

-- ============================================
-- SCHEMA: Sprint Snapshots (for Velocity/Burndown)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_plan.sprint_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    snapshot_type VARCHAR(50) NOT NULL,
    snapshot_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_issues INTEGER DEFAULT 0,
    completed_issues INTEGER DEFAULT 0,
    incomplete_issues INTEGER DEFAULT 0,
    added_after_start INTEGER DEFAULT 0,
    removed_after_start INTEGER DEFAULT 0,
    total_points DECIMAL(10,2) DEFAULT 0,
    completed_points DECIMAL(10,2) DEFAULT 0,
    incomplete_points DECIMAL(10,2) DEFAULT 0,
    original_points DECIMAL(10,2) DEFAULT 0,
    ideal_remaining_points DECIMAL(10,2) DEFAULT 0,
    scope_change_points DECIMAL(10,2) DEFAULT 0,
    velocity_trend DECIMAL(5,2) DEFAULT 0,
    issue_breakdown JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_plan.velocity_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL REFERENCES jira_plan.board_configs(id),
    sprint_id UUID NOT NULL REFERENCES jira_plan.sprints(id),
    sprint_name VARCHAR(255),
    sprint_start_date DATE,
    sprint_end_date DATE,
    sprint_completed_date DATE,
    planned_points DECIMAL(10,2) DEFAULT 0,
    completed_points DECIMAL(10,2) DEFAULT 0,
    issue_count INTEGER DEFAULT 0,
    velocity_trend DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(board_id, sprint_id)
);

CREATE TABLE IF NOT EXISTS jira_plan.cumulative_flow_data (
    id BIGSERIAL PRIMARY KEY,
    board_id UUID NOT NULL,
    sprint_id UUID,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status_name VARCHAR(100) NOT NULL,
    status_color VARCHAR(7),
    issue_count INTEGER DEFAULT 0,
    issue_points DECIMAL(10,2) DEFAULT 0,
    record_date DATE NOT NULL,
    UNIQUE(board_id, sprint_id, status_name, record_date)
);

CREATE TABLE IF NOT EXISTS jira_plan.burndown_data (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES jira_plan.sprints(id),
    board_id UUID NOT NULL,
    record_date DATE NOT NULL,
    day_number INTEGER NOT NULL,
    total_points DECIMAL(10,2) NOT NULL,
    remaining_points DECIMAL(10,2) NOT NULL,
    ideal_remaining_points DECIMAL(10,2) NOT NULL,
    completed_points DECIMAL(10,2) NOT NULL,
    issue_count INTEGER DEFAULT 0,
    completed_issues INTEGER DEFAULT 0,
    added_issues INTEGER DEFAULT 0,
    removed_issues INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sprint_id, record_date)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sprint_snapshots_sprint ON jira_plan.sprint_snapshots(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_snapshots_type ON jira_plan.sprint_snapshots(snapshot_type);
CREATE INDEX IF NOT EXISTS idx_velocity_board ON jira_plan.velocity_history(board_id);
CREATE INDEX IF NOT EXISTS idx_velocity_sprint ON jira_plan.velocity_history(sprint_id);
CREATE INDEX IF NOT EXISTS idx_cfd_board ON jira_plan.cumulative_flow_data(board_id);
CREATE INDEX IF NOT EXISTS idx_cfd_sprint ON jira_plan.cumulative_flow_data(sprint_id);
CREATE INDEX IF NOT EXISTS idx_cfd_date ON jira_plan.cumulative_flow_data(record_date);
CREATE INDEX IF NOT EXISTS idx_burndown_sprint ON jira_plan.burndown_data(sprint_id);
CREATE INDEX IF NOT EXISTS idx_burndown_date ON jira_plan.burndown_data(record_date);

-- ============================================
-- SCHEMA: Epic Progress Tracking
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.epics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    description TEXT,
    color VARCHAR(7) DEFAULT '#0052CC',
    lead_id UUID,
    lead_name VARCHAR(200),
    status VARCHAR(50) DEFAULT 'OPEN',
    start_date DATE,
    end_date DATE,
    linked_issue_id UUID,
    total_story_points DECIMAL(10,2) DEFAULT 0,
    completed_story_points DECIMAL(10,2) DEFAULT 0,
    total_issue_count INTEGER DEFAULT 0,
    completed_issue_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.epic_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    epic_id UUID NOT NULL REFERENCES jira_issue.epics(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by UUID,
    UNIQUE(epic_id, issue_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.epic_progress_history (
    id BIGSERIAL PRIMARY KEY,
    epic_id UUID NOT NULL REFERENCES jira_issue.epics(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    total_points DECIMAL(10,2) DEFAULT 0,
    completed_points DECIMAL(10,2) DEFAULT 0,
    total_issues INTEGER DEFAULT 0,
    completed_issues INTEGER DEFAULT 0,
    percent_complete DECIMAL(5,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(epic_id, record_date)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_epics_lead ON jira_issue.epics(lead_id);
CREATE INDEX IF NOT EXISTS idx_epics_status ON jira_issue.epics(status);
CREATE INDEX IF NOT EXISTS idx_epic_issues_epic ON jira_issue.epic_issues(epic_id);
CREATE INDEX IF NOT EXISTS idx_epic_issues_issue ON jira_issue.epic_issues(issue_id);
CREATE INDEX IF NOT EXISTS idx_epic_progress_epic ON jira_issue.epic_progress_history(epic_id);
CREATE INDEX IF NOT EXISTS idx_epic_progress_date ON jira_issue.epic_progress_history(record_date);

-- ============================================
-- SCHEMA: Distributed Safety (Optimistic Locking)
-- ============================================

ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS last_modified_version BIGINT DEFAULT 0;

CREATE OR REPLACE FUNCTION jira_issue.update_version()
RETURNS TRIGGER AS $$
BEGIN
    NEW.version := OLD.version + 1;
    NEW.last_modified_version := OLD.last_modified_version + 1;
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_issue_version ON jira_issue.issues;
CREATE TRIGGER trigger_issue_version
    BEFORE UPDATE ON jira_issue.issues
    FOR EACH ROW
    WHEN (OLD.version IS NOT DISTINCT FROM NEW.version)
    EXECUTE FUNCTION jira_issue.update_version();

-- ============================================
-- SCHEMA: Issue Link Type Configuration
-- ============================================

ALTER TABLE jira_issue.issue_link_types ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE jira_issue.issue_link_types ADD COLUMN IF NOT EXISTS icon_url VARCHAR(255);

CREATE TABLE IF NOT EXISTS jira_issue.issue_link_directions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_type_id UUID NOT NULL REFERENCES jira_issue.issue_link_types(id) ON DELETE CASCADE,
    direction VARCHAR(20) NOT NULL,
    show_in_ui BOOLEAN DEFAULT TRUE,
    UNIQUE(link_type_id, direction)
);

INSERT INTO jira_issue.issue_link_directions (link_type_id, direction, show_in_ui)
SELECT id, 'OUTWARD', TRUE FROM jira_issue.issue_link_types WHERE name = 'blocks'
ON CONFLICT DO NOTHING;

INSERT INTO jira_issue.issue_link_directions (link_type_id, direction, show_in_ui)
SELECT id, 'INWARD', TRUE FROM jira_issue.issue_link_types WHERE name = 'blocks'
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_issue_link_directions_type ON jira_issue.issue_link_directions(link_type_id);

-- ============================================
-- FINAL INDEXES & CONSTRAINTS (best-effort, skip if columns missing)
-- ============================================

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_issues_epic_status ON jira_issue.issues(epic_id, status) WHERE epic_id IS NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_issues_assignee_status ON jira_issue.issues(assignee_id, status) WHERE assignee_id IS NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_issues_project_status ON jira_issue.issues(project_id, status);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_issues_created_rank ON jira_issue.issues(created_at DESC, rank);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_change_items_new_value ON jira_issue.change_items USING GIN (new_value);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_change_items_old_value ON jira_issue.change_items USING GIN (old_value);
EXCEPTION WHEN OTHERS THEN NULL; END $$;

DO $$ BEGIN
    CREATE INDEX IF NOT EXISTS idx_saved_filters_jql ON jira_search.saved_filters USING GIN (to_tsvector('english', jql_query));
EXCEPTION WHEN OTHERS THEN NULL; END $$;

COMMENT ON TABLE jira_admin.permission_schemes IS 'Enterprise permission schemes - Jira DC RBAC';
COMMENT ON TABLE jira_admin.screen_schemes IS 'Screen schemes for issue operations';
COMMENT ON TABLE jira_admin.notification_schemes IS 'Notification schemes for event-based alerts';
COMMENT ON TABLE jira_search.saved_filters IS 'User saved JQL filters';
COMMENT ON TABLE jira_plan.sprint_snapshots IS 'Sprint state snapshots for velocity and burndown charts';
COMMENT ON TABLE jira_issue.epics IS 'Epic tracking with progress metrics';
