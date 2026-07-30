-- V10__master_data_infrastructure.sql
-- Master data tables and seed data for dynamic configuration

-- ============================================================
-- 1. system_configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.system_configuration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(200) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_editable BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_system_config_category ON jira_admin.system_configuration(category);

-- ============================================================
-- 2. master_statuses
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_statuses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(30) NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#6C757D',
    icon VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. master_priorities
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    priority_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(7) NOT NULL,
    icon_url VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. master_issue_types
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_issue_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(50) DEFAULT 'standard',
    color VARCHAR(7),
    is_subtask BOOLEAN NOT NULL DEFAULT FALSE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. master_resolutions
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_resolutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resolution_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. master_link_types
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_link_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_key VARCHAR(50) NOT NULL UNIQUE,
    outward_name VARCHAR(100) NOT NULL,
    inward_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 7. master_roles
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_key VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. master_permissions
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    permission_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 9. master_role_permissions (join table)
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES jira_admin.master_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES jira_admin.master_permissions(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);

-- ============================================================
-- 10. master_board_types
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_board_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_key VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 11. master_board_column_templates
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_board_column_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_type_id UUID NOT NULL REFERENCES jira_admin.master_board_types(id) ON DELETE CASCADE,
    column_name VARCHAR(100) NOT NULL,
    status_category VARCHAR(30) NOT NULL,
    color VARCHAR(7),
    wip_limit INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status_mappings JSONB DEFAULT '[]'
);

-- ============================================================
-- 12. master_notification_events
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_notification_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 13. master_quick_filter_presets
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.master_quick_filter_presets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filter_name VARCHAR(100) NOT NULL,
    jql_query VARCHAR(500) NOT NULL,
    icon VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 14. i18n_messages
-- ============================================================
CREATE TABLE IF NOT EXISTS jira_admin.i18n_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_key VARCHAR(200) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    message_value TEXT NOT NULL,
    category VARCHAR(50),
    UNIQUE(message_key, locale)
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Statuses
INSERT INTO jira_admin.master_statuses (status_key, display_name, description, category, color, icon, sort_order, is_system, is_active)
VALUES
    ('BACKLOG',     'Backlog',     'Items not yet planned for a sprint',  'TODO',        '#6C757D', 'inbox',        1,  TRUE, TRUE),
    ('TODO',        'To Do',       'Planned but not started',             'TODO',        '#0052CC', 'clipboard',    2,  TRUE, TRUE),
    ('OPEN',        'Open',        'Newly created issue',                 'TODO',        '#4C9AFF', 'circle',       3,  TRUE, TRUE),
    ('DEFINED',     'Defined',     'Requirements are defined',            'TODO',        '#00B8D9', 'file-text',    4,  FALSE, TRUE),
    ('IN_PROGRESS', 'In Progress', 'Actively being worked on',            'IN_PROGRESS', '#0065FF', 'loader',       5,  TRUE, TRUE),
    ('IN_REVIEW',   'In Review',   'Under peer review',                   'IN_PROGRESS', '#6554C0', 'eye',          6,  FALSE, TRUE),
    ('BLOCKED',     'Blocked',     'Work is blocked by a dependency',     'IN_PROGRESS', '#FF5630', 'alert-circle', 7,  FALSE, TRUE),
    ('RESOLVED',    'Resolved',    'Fix or work is complete, awaiting verification', 'DONE', '#36B37E', 'check-circle', 8,  TRUE, TRUE),
    ('DONE',        'Done',        'Work is fully complete',              'DONE',        '#00875A', 'check',        9,  TRUE, TRUE),
    ('CLOSED',      'Closed',      'Issue has been closed',               'DONE',        '#5E6C84', 'x-circle',     10, TRUE, TRUE)
ON CONFLICT (status_key) DO NOTHING;

-- Priorities
INSERT INTO jira_admin.master_priorities (priority_key, display_name, description, color, icon_url, sort_order, is_default, is_active)
VALUES
    ('BLOCKER',  'Blocker',  'Blocks development and/or testing',        '#FF0000', '/icons/priorities/blocker.svg',  1,  FALSE, TRUE),
    ('CRITICAL', 'Critical', 'Production system is down or major impact', '#CC0000', '/icons/priorities/critical.svg', 2,  FALSE, TRUE),
    ('HIGHEST',  'Highest',  'Serious problem requiring immediate fix',  '#FF5630', '/icons/priorities/highest.svg',  3,  FALSE, TRUE),
    ('HIGH',     'High',     'Major problem or feature',                 '#FF7452', '/icons/priorities/high.svg',     4,  FALSE, TRUE),
    ('MAJOR',    'Major',    'Has a significant impact',                 '#FF991F', '/icons/priorities/major.svg',    5,  FALSE, TRUE),
    ('MEDIUM',   'Medium',   'Normal priority',                          '#FFAB00', '/icons/priorities/medium.svg',   6,  TRUE,  TRUE),
    ('LOW',      'Low',      'Minor problem or easily worked around',    '#36B37E', '/icons/priorities/low.svg',      7,  FALSE, TRUE),
    ('MINOR',    'Minor',    'Minor issue with minimal impact',          '#57D9A3', '/icons/priorities/minor.svg',    8,  FALSE, TRUE),
    ('LOWEST',   'Lowest',   'Trivial problem with little or no impact', '#ABF5D1', '/icons/priorities/lowest.svg',   9,  FALSE, TRUE),
    ('TRIVIAL',  'Trivial',  'Cosmetic problem, no functional impact',   '#B3D4FF', '/icons/priorities/trivial.svg',  10, FALSE, TRUE)
ON CONFLICT (priority_key) DO NOTHING;

-- Issue Types
INSERT INTO jira_admin.master_issue_types (type_key, display_name, description, icon, color, is_subtask, is_system, is_active, sort_order)
VALUES
    ('BUG',             'Bug',             'A defect or error in the software',                   'bug',             '#FF5630', FALSE, TRUE,  TRUE, 1),
    ('TASK',            'Task',            'A piece of work to be done',                          'check-square',    '#4C9AFF', FALSE, TRUE,  TRUE, 2),
    ('STORY',           'Story',           'A user story describing a feature',                   'bookmark',        '#36B37E', FALSE, TRUE,  TRUE, 3),
    ('EPIC',            'Epic',            'A large body of work that can be broken down',        'zap',             '#6554C0', FALSE, TRUE,  TRUE, 4),
    ('SUB_TASK',        'Sub-task',        'A smaller piece of work within a parent issue',       'sub-task',        '#4C9AFF', TRUE,  TRUE,  TRUE, 5),
    ('IMPROVEMENT',     'Improvement',     'An enhancement to an existing feature',               'trending-up',     '#00B8D9', FALSE, FALSE, TRUE, 6),
    ('NEW_FEATURE',     'New Feature',     'A completely new feature request',                    'plus-circle',     '#36B37E', FALSE, FALSE, TRUE, 7),
    ('CHANGE_REQUEST',  'Change Request',  'A formal request to change existing functionality',   'edit',            '#FF991F', FALSE, FALSE, TRUE, 8),
    ('INCIDENT',        'Incident',        'An unplanned interruption or service degradation',    'alert-triangle',  '#FF5630', FALSE, FALSE, TRUE, 9),
    ('SERVICE_REQUEST', 'Service Request', 'A request from a user for information or action',     'headphones',      '#0065FF', FALSE, FALSE, TRUE, 10),
    ('PROBLEM',         'Problem',         'The root cause of one or more incidents',             'search',          '#FF7452', FALSE, FALSE, TRUE, 11)
ON CONFLICT (type_key) DO NOTHING;

-- Resolutions
INSERT INTO jira_admin.master_resolutions (resolution_key, display_name, description, sort_order, is_default, is_active)
VALUES
    ('FIXED',            'Fixed',            'The issue has been fixed',                              1, FALSE, TRUE),
    ('WONT_FIX',         'Won''t Fix',       'The issue will not be fixed',                           2, FALSE, TRUE),
    ('DUPLICATE',        'Duplicate',        'The issue is a duplicate of another issue',             3, FALSE, TRUE),
    ('CANNOT_REPRODUCE', 'Cannot Reproduce', 'The issue could not be reproduced',                    4, FALSE, TRUE),
    ('DONE',             'Done',             'Work has been completed',                               5, TRUE,  TRUE)
ON CONFLICT (resolution_key) DO NOTHING;

-- Link Types
INSERT INTO jira_admin.master_link_types (link_key, outward_name, inward_name, description, is_system, is_active, sort_order)
VALUES
    ('BLOCKS',     'blocks',         'is blocked by',    'Blocking relationship between issues',     TRUE,  TRUE, 1),
    ('CLONES',     'clones',         'is cloned by',     'Clone relationship between issues',        TRUE,  TRUE, 2),
    ('DUPLICATES', 'duplicates',     'is duplicated by', 'Duplicate relationship between issues',    TRUE,  TRUE, 3),
    ('RELATES',    'relates to',     'relates to',       'General relationship between issues',      TRUE,  TRUE, 4)
ON CONFLICT (link_key) DO NOTHING;

-- Roles
INSERT INTO jira_admin.master_roles (role_key, display_name, description, is_system, is_active)
VALUES
    ('ADMINISTRATORS', 'Administrators', 'Full system administration access',                  TRUE, TRUE),
    ('DEVELOPERS',     'Developers',     'Development team members with project-level access',  TRUE, TRUE),
    ('VIEWERS',        'Viewers',        'Read-only access to project data',                    TRUE, TRUE)
ON CONFLICT (role_key) DO NOTHING;

-- Permissions
INSERT INTO jira_admin.master_permissions (permission_key, display_name, description, category, is_system, is_active)
VALUES
    ('BROWSE_PROJECTS',       'Browse Projects',       'View projects and their issues',              'PROJECT',  TRUE, TRUE),
    ('CREATE_ISSUES',         'Create Issues',         'Create new issues in a project',              'ISSUE',    TRUE, TRUE),
    ('EDIT_ISSUES',           'Edit Issues',           'Edit existing issues',                        'ISSUE',    TRUE, TRUE),
    ('DELETE_ISSUES',         'Delete Issues',         'Delete issues permanently',                   'ISSUE',    TRUE, TRUE),
    ('ASSIGN_ISSUES',         'Assign Issues',         'Assign issues to users',                      'ISSUE',    TRUE, TRUE),
    ('RESOLVE_ISSUES',        'Resolve Issues',        'Resolve and close issues',                    'ISSUE',    TRUE, TRUE),
    ('TRANSITION_ISSUES',     'Transition Issues',     'Move issues through workflow transitions',     'ISSUE',    TRUE, TRUE),
    ('ADD_COMMENTS',          'Add Comments',          'Add comments to issues',                      'COMMENT',  TRUE, TRUE),
    ('EDIT_ALL_COMMENTS',     'Edit All Comments',     'Edit any comment on issues',                  'COMMENT',  TRUE, TRUE),
    ('DELETE_ALL_COMMENTS',   'Delete All Comments',   'Delete any comment on issues',                'COMMENT',  TRUE, TRUE),
    ('MANAGE_WATCHERS',       'Manage Watchers',       'Add or remove watchers from issues',          'ISSUE',    TRUE, TRUE),
    ('CREATE_ATTACHMENTS',    'Create Attachments',    'Add attachments to issues',                   'ATTACHMENT', TRUE, TRUE),
    ('DELETE_ALL_ATTACHMENTS','Delete All Attachments', 'Delete any attachment from issues',           'ATTACHMENT', TRUE, TRUE),
    ('ADMINISTER_PROJECTS',   'Administer Projects',   'Full administration of project settings',     'PROJECT',  TRUE, TRUE),
    ('SYSTEM_ADMIN',          'System Admin',          'Full system administration access',           'GLOBAL',   TRUE, TRUE)
ON CONFLICT (permission_key) DO NOTHING;

-- Role-permission assignments (after both roles and permissions are seeded)
INSERT INTO jira_admin.master_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM jira_admin.master_roles r
CROSS JOIN jira_admin.master_permissions p
WHERE r.role_key = 'ADMINISTRATORS'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO jira_admin.master_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM jira_admin.master_roles r
CROSS JOIN jira_admin.master_permissions p
WHERE r.role_key = 'DEVELOPERS'
  AND p.permission_key IN (
    'BROWSE_PROJECTS', 'CREATE_ISSUES', 'EDIT_ISSUES', 'ASSIGN_ISSUES',
    'RESOLVE_ISSUES', 'TRANSITION_ISSUES', 'ADD_COMMENTS',
    'MANAGE_WATCHERS', 'CREATE_ATTACHMENTS'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO jira_admin.master_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM jira_admin.master_roles r
CROSS JOIN jira_admin.master_permissions p
WHERE r.role_key = 'VIEWERS'
  AND p.permission_key IN ('BROWSE_PROJECTS', 'ADD_COMMENTS')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Board Types
INSERT INTO jira_admin.master_board_types (id, type_key, display_name, description, is_active)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'SCRUM',  'Scrum Board',  'Agile Scrum board with sprints and backlog', TRUE),
    ('a0000000-0000-0000-0000-000000000002', 'KANBAN', 'Kanban Board', 'Continuous flow board with WIP limits',      TRUE)
ON CONFLICT (type_key) DO NOTHING;

-- Board Column Templates - Scrum
INSERT INTO jira_admin.master_board_column_templates (board_type_id, column_name, status_category, color, wip_limit, sort_order, status_mappings)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'To Do',        'TODO',        '#E2E4E9', NULL, 1, '["TODO", "OPEN", "BACKLOG"]'),
    ('a0000000-0000-0000-0000-000000000001', 'In Progress',  'IN_PROGRESS', '#DEEBFF', NULL, 2, '["IN_PROGRESS"]'),
    ('a0000000-0000-0000-0000-000000000001', 'In Review',    'IN_PROGRESS', '#EAE6FF', NULL, 3, '["IN_REVIEW"]'),
    ('a0000000-0000-0000-0000-000000000001', 'Done',         'DONE',        '#E3FCEF', NULL, 4, '["DONE", "RESOLVED", "CLOSED"]');

-- Board Column Templates - Kanban
INSERT INTO jira_admin.master_board_column_templates (board_type_id, column_name, status_category, color, wip_limit, sort_order, status_mappings)
VALUES
    ('a0000000-0000-0000-0000-000000000002', 'Backlog',      'TODO',        '#E2E4E9', NULL, 1, '["BACKLOG"]'),
    ('a0000000-0000-0000-0000-000000000002', 'Selected',     'TODO',        '#B3D4FF', 5,    2, '["TODO", "OPEN", "DEFINED"]'),
    ('a0000000-0000-0000-0000-000000000002', 'In Progress',  'IN_PROGRESS', '#DEEBFF', 3,    3, '["IN_PROGRESS"]'),
    ('a0000000-0000-0000-0000-000000000002', 'In Review',    'IN_PROGRESS', '#EAE6FF', 3,    4, '["IN_REVIEW"]'),
    ('a0000000-0000-0000-0000-000000000002', 'Done',         'DONE',        '#E3FCEF', NULL, 5, '["DONE", "RESOLVED", "CLOSED"]');

-- Notification Events
INSERT INTO jira_admin.master_notification_events (event_key, display_name, description, category, is_system, is_active)
VALUES
    ('ISSUE_CREATED',       'Issue Created',        'Fired when a new issue is created',                'ISSUE',    TRUE, TRUE),
    ('ISSUE_UPDATED',       'Issue Updated',        'Fired when an issue is updated',                   'ISSUE',    TRUE, TRUE),
    ('ISSUE_ASSIGNED',      'Issue Assigned',       'Fired when an issue is assigned or reassigned',    'ISSUE',    TRUE, TRUE),
    ('ISSUE_RESOLVED',      'Issue Resolved',       'Fired when an issue is resolved',                  'ISSUE',    TRUE, TRUE),
    ('ISSUE_CLOSED',        'Issue Closed',         'Fired when an issue is closed',                    'ISSUE',    TRUE, TRUE),
    ('ISSUE_DELETED',       'Issue Deleted',        'Fired when an issue is deleted',                   'ISSUE',    TRUE, TRUE),
    ('COMMENT_ADDED',       'Comment Added',        'Fired when a comment is added to an issue',        'COMMENT',  TRUE, TRUE),
    ('COMMENT_EDITED',      'Comment Edited',       'Fired when a comment is edited',                   'COMMENT',  TRUE, TRUE),
    ('COMMENT_DELETED',     'Comment Deleted',      'Fired when a comment is deleted',                  'COMMENT',  TRUE, TRUE),
    ('WORKLOG_ADDED',       'Work Log Added',       'Fired when work is logged on an issue',            'WORKLOG',  TRUE, TRUE),
    ('SPRINT_STARTED',      'Sprint Started',       'Fired when a sprint is started',                   'SPRINT',   TRUE, TRUE),
    ('SPRINT_COMPLETED',    'Sprint Completed',     'Fired when a sprint is completed',                 'SPRINT',   TRUE, TRUE),
    ('VERSION_RELEASED',    'Version Released',     'Fired when a version is released',                 'VERSION',  TRUE, TRUE),
    ('USER_MENTIONED',      'User Mentioned',       'Fired when a user is mentioned in a comment',      'USER',     TRUE, TRUE)
ON CONFLICT (event_key) DO NOTHING;

-- Quick Filter Presets
INSERT INTO jira_admin.master_quick_filter_presets (filter_name, jql_query, icon, sort_order, is_system, is_active)
VALUES
    ('My Open Issues',      'assignee = currentUser() AND status != Done AND status != Closed', 'user',        1, TRUE, TRUE),
    ('Recently Updated',    'updatedDate >= -7d ORDER BY updatedDate DESC',                     'clock',       2, TRUE, TRUE),
    ('Critical & Blocker',  'priority in (Blocker, Critical)',                                  'alert-circle',3, TRUE, TRUE),
    ('Bugs Only',           'type = Bug',                                                      'bug',         4, TRUE, TRUE),
    ('Unassigned',          'assignee is EMPTY',                                               'user-x',      5, TRUE, TRUE),
    ('Created This Week',   'createdDate >= startOfWeek()',                                     'calendar',    6, TRUE, TRUE),
    ('Overdue',             'dueDate < now() AND status != Done AND status != Closed',          'alert-triangle', 7, TRUE, TRUE),
    ('Blocked Issues',      'status = Blocked',                                                'x-octagon',   8, TRUE, TRUE);

-- System Configuration
INSERT INTO jira_admin.system_configuration (config_key, config_value, value_type, category, description, is_editable)
VALUES
    -- Issue defaults
    ('issue.default.priority',           'MEDIUM',   'STRING',  'issue_defaults',    'Default priority for new issues',                TRUE),
    ('issue.default.status',             'OPEN',     'STRING',  'issue_defaults',    'Default status for new issues',                  TRUE),
    ('issue.default.type',               'TASK',     'STRING',  'issue_defaults',    'Default issue type for new issues',              TRUE),
    ('issue.default.resolution',         'FIXED',    'STRING',  'issue_defaults',    'Default resolution when resolving issues',       TRUE),
    ('issue.max.summary.length',         '255',      'INTEGER', 'issue_defaults',    'Maximum length of issue summary',                TRUE),
    ('issue.max.description.length',     '32000',    'INTEGER', 'issue_defaults',    'Maximum length of issue description',            TRUE),
    ('issue.allow.unassigned',           'true',     'BOOLEAN', 'issue_defaults',    'Allow issues without an assignee',               TRUE),

    -- Project defaults
    ('project.default.board.type',       'SCRUM',    'STRING',  'project_defaults',  'Default board type for new projects',            TRUE),
    ('project.default.permission.scheme','Default Permission Scheme', 'STRING', 'project_defaults', 'Default permission scheme name', TRUE),
    ('project.max.name.length',          '80',       'INTEGER', 'project_defaults',  'Maximum length of project name',                 TRUE),
    ('project.key.max.length',           '10',       'INTEGER', 'project_defaults',  'Maximum length of project key',                  TRUE),
    ('project.allow.subtasks',           'true',     'BOOLEAN', 'project_defaults',  'Allow subtasks in projects by default',          TRUE),
    ('project.allow.attachments',        'true',     'BOOLEAN', 'project_defaults',  'Allow attachments in projects by default',       TRUE),
    ('project.allow.comments',           'true',     'BOOLEAN', 'project_defaults',  'Allow comments in projects by default',          TRUE),

    -- Board defaults
    ('board.default.wip.limit',          '5',        'INTEGER', 'board_defaults',    'Default WIP limit for Kanban columns',           TRUE),
    ('board.default.swimlane',           'ASSIGNEE', 'STRING',  'board_defaults',    'Default swimlane grouping',                      TRUE),
    ('board.card.fields',                'priority,assignee,labels', 'STRING', 'board_defaults', 'Default fields shown on board cards', TRUE),
    ('board.estimation.field',           'story_points', 'STRING', 'board_defaults', 'Default estimation field',                       TRUE),

    -- Sprint defaults
    ('sprint.default.duration.weeks',    '2',        'INTEGER', 'sprint_defaults',   'Default sprint duration in weeks',               TRUE),
    ('sprint.auto.complete.moved',       'true',     'BOOLEAN', 'sprint_defaults',   'Auto-move incomplete issues on sprint complete',  TRUE),
    ('sprint.naming.pattern',            'Sprint {n}', 'STRING', 'sprint_defaults',  'Default sprint naming pattern',                  TRUE),

    -- Test defaults
    ('test.require.approval',            'false',    'BOOLEAN', 'test_defaults',     'Require approval before test execution',         TRUE),
    ('test.auto.link.defects',           'true',     'BOOLEAN', 'test_defaults',     'Auto-link defects discovered during testing',    TRUE),
    ('test.default.cycle.name',          'Regression', 'STRING', 'test_defaults',    'Default test cycle name',                        TRUE),

    -- Quality thresholds
    ('quality.code.coverage.min',        '80',       'INTEGER', 'quality_thresholds','Minimum code coverage percentage',               TRUE),
    ('quality.max.blocker.bugs',         '0',        'INTEGER', 'quality_thresholds','Maximum allowed blocker bugs for release',       TRUE),
    ('quality.max.critical.bugs',        '3',        'INTEGER', 'quality_thresholds','Maximum allowed critical bugs for release',      TRUE),
    ('quality.review.required',          'true',     'BOOLEAN', 'quality_thresholds','Require code review before merge',               TRUE),

    -- User defaults
    ('user.default.language',            'en',       'STRING',  'user_defaults',     'Default language for new users',                 TRUE),
    ('user.default.timezone',            'UTC',      'STRING',  'user_defaults',     'Default timezone for new users',                 TRUE),
    ('user.session.timeout.minutes',     '480',      'INTEGER', 'user_defaults',     'Session timeout in minutes',                     TRUE),
    ('user.max.login.attempts',          '5',        'INTEGER', 'user_defaults',     'Maximum failed login attempts before lockout',   TRUE),
    ('user.password.min.length',         '8',        'INTEGER', 'user_defaults',     'Minimum password length',                        TRUE),

    -- Attachment settings
    ('attachment.max.size.mb',           '25',       'INTEGER', 'attachment_settings','Maximum attachment file size in MB',             TRUE),
    ('attachment.allowed.extensions',    'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,csv,zip,rar', 'STRING', 'attachment_settings', 'Allowed file extensions for attachments', TRUE),
    ('attachment.max.per.issue',         '20',       'INTEGER', 'attachment_settings','Maximum attachments per issue',                  TRUE),
    ('attachment.thumbnail.enabled',     'true',     'BOOLEAN', 'attachment_settings','Generate thumbnails for image attachments',      TRUE),

    -- Notification settings
    ('notification.email.enabled',       'true',     'BOOLEAN', 'notification_settings','Enable email notifications',                  TRUE),
    ('notification.batch.interval.min',  '5',        'INTEGER', 'notification_settings','Batch notification interval in minutes',      TRUE),
    ('notification.digest.enabled',      'false',    'BOOLEAN', 'notification_settings','Enable daily digest notifications',            TRUE),

    -- System settings
    ('system.base.url',                  'http://localhost:3000', 'STRING', 'system_settings', 'Base URL of the application',           TRUE),
    ('system.date.format',               'yyyy-MM-dd',          'STRING', 'system_settings', 'System date format',                     TRUE),
    ('system.datetime.format',           'yyyy-MM-dd HH:mm:ss', 'STRING', 'system_settings', 'System datetime format',                TRUE),
    ('system.items.per.page',            '25',       'INTEGER', 'system_settings',   'Default items per page for lists',               TRUE),
    ('system.max.export.rows',           '10000',    'INTEGER', 'system_settings',   'Maximum rows for data export',                   TRUE)
ON CONFLICT (config_key) DO NOTHING;
