-- V4: Project Template Workflow Enhancement
-- This migration adds:
-- 1. Enhanced project templates with Business category support
-- 2. Workflow templates with status definitions
-- 3. Template workflow associations
-- 4. Updated seed data for Project Management, Task Management, Process Management templates

-- ============================================
-- TEMPLATE WORKFLOW STATUSES (for visualization)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_workflow_statuses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    status_name VARCHAR(50) NOT NULL,
    status_key VARCHAR(20) NOT NULL,
    status_color VARCHAR(7) NOT NULL DEFAULT '#6B778C',
    status_category VARCHAR(20) NOT NULL, -- TODO, IN_PROGRESS, DONE
    sequence INT NOT NULL DEFAULT 0,
    description TEXT,
    icon VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_template_workflow_statuses_unique
ON jira_project.template_workflow_statuses(template_id, status_key);

-- ============================================
-- TEMPLATE WORKFLOW TRANSITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_workflow_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    from_status_key VARCHAR(20) NOT NULL,
    to_status_key VARCHAR(20) NOT NULL,
    transition_name VARCHAR(100),
    transition_icon VARCHAR(50),
    allow_backward BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_template_workflow_transitions_template
ON jira_project.template_workflow_transitions(template_id);

-- ============================================
-- TEMPLATE ISSUE TYPE ASSOCIATIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_issue_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    issue_type_name VARCHAR(50) NOT NULL,
    issue_type_icon VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    is_subtask BOOLEAN DEFAULT FALSE,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_template_issue_types_unique
ON jira_project.template_issue_types(template_id, issue_type_name);

-- ============================================
-- ENHANCE PROJECT TEMPLATES TABLE
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_templates'
                   AND column_name = 'category') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN category VARCHAR(20) DEFAULT 'SOFTWARE';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_templates'
                   AND column_name = 'template_type') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN template_type VARCHAR(50);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_templates'
                   AND column_name = 'default_workflow_name') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN default_workflow_name VARCHAR(100);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_templates'
                   AND column_name = 'instructions') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN instructions TEXT;
    END IF;
END $$;

-- ============================================
-- UPDATE EXISTING TEMPLATES WITH CATEGORY
-- ============================================
UPDATE jira_project.project_templates SET category = 'SOFTWARE' WHERE category IS NULL;
UPDATE jira_project.project_templates SET template_type = 'AGILE' WHERE name IN ('Scrum', 'Kanban');
UPDATE jira_project.project_templates SET template_type = 'SUPPORT' WHERE name IN ('Bug Tracking');
UPDATE jira_project.project_templates SET template_type = 'MANAGEMENT' WHERE name IN ('Task Management', 'Portfolio');
UPDATE jira_project.project_templates SET template_type = 'BASIC' WHERE name = 'Basic';

-- ============================================
-- INSERT NEW TEMPLATES: Business Category
-- ============================================
INSERT INTO jira_project.project_templates
(id, type_id, name, description, icon, color, category, template_type, default_assignee_type, sort_order, is_active, instructions)
VALUES
(
    '00000000-0000-0000-0002-000000000007',
    '00000000-0000-0000-0001-000000000001',
    'Project management',
    'Plan, track and report on all of your work within a project.',
    'project-management',
    '#0052CC',
    'BUSINESS',
    'MANAGEMENT',
    'PROJECT_LEAD',
    1,
    TRUE,
    'Create your tasks, organize and track their progress, and deliver your work on time. Estimations and time tracking allow you to report on where your project is at any stage.'
),
(
    '00000000-0000-0000-0002-000000000008',
    '00000000-0000-0000-0001-000000000001',
    'Task management',
    'Quickly organize and assign simple tasks for you and your team.',
    'task-management',
    '#00875A',
    'BUSINESS',
    'MANAGEMENT',
    'UNASSIGNED',
    2,
    TRUE,
    'Create simple tasks, organize them and get them done. You can use this project to manage your tasks or assign them to someone else.'
),
(
    '00000000-0000-0000-0002-000000000009',
    '00000000-0000-0000-0001-000000000001',
    'Process management',
    'Track all the work activity as it transitions through a streamlined process.',
    'process-management',
    '#6554C0',
    'BUSINESS',
    'PROCESS',
    'PROJECT_LEAD',
    3,
    TRUE,
    'Create your tasks and track them at every step, from start to finish. You can use this project to review documentation, approve expenses, or other processes.'
)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SEED DATA: Template Workflow Statuses
-- ============================================

-- Project Management Workflow: TO DO -> IN PROGRESS -> DONE
INSERT INTO jira_project.template_workflow_statuses
(id, template_id, status_name, status_key, status_color, status_category, sequence, description, icon)
VALUES
('00000000-0000-0000-0008-000000000001', '00000000-0000-0000-0002-000000000007', 'To Do', 'TODO', '#6B778C', 'TODO', 0, 'Tasks that need to be done', 'todo'),
('00000000-0000-0000-0008-000000000002', '00000000-0000-0000-0002-000000000007', 'In Progress', 'IN_PROGRESS', '#0052CC', 'IN_PROGRESS', 1, 'Tasks currently being worked on', 'progress'),
('00000000-0000-0000-0008-000000000003', '00000000-0000-0000-0002-000000000007', 'Done', 'DONE', '#00875A', 'DONE', 2, 'Completed tasks', 'done')
ON CONFLICT DO NOTHING;

-- Task Management Workflow: TO DO -> DONE
INSERT INTO jira_project.template_workflow_statuses
(id, template_id, status_name, status_key, status_color, status_category, sequence, description, icon)
VALUES
('00000000-0000-0000-0008-000000000004', '00000000-0000-0000-0002-000000000008', 'To Do', 'TODO', '#6B778C', 'TODO', 0, 'Tasks that need to be done', 'todo'),
('00000000-0000-0000-0008-000000000005', '00000000-0000-0000-0002-000000000008', 'Done', 'DONE', '#00875A', 'DONE', 1, 'Completed tasks', 'done')
ON CONFLICT DO NOTHING;

-- Process Management Workflow: OPEN -> IN PROGRESS -> UNDER REVIEW -> APPROVED -> DONE (+ REJECTED, CANCELLED)
INSERT INTO jira_project.template_workflow_statuses
(id, template_id, status_name, status_key, status_color, status_category, sequence, description, icon)
VALUES
('00000000-0000-0000-0008-000000000006', '00000000-0000-0000-0002-000000000009', 'Open', 'OPEN', '#6B778C', 'TODO', 0, 'New requests or tasks', 'open'),
('00000000-0000-0000-0008-000000000007', '00000000-0000-0000-0002-000000000009', 'In Progress', 'IN_PROGRESS', '#0052CC', 'IN_PROGRESS', 1, 'Tasks currently being worked on', 'progress'),
('00000000-0000-0000-0008-000000000008', '00000000-0000-0000-0002-000000000009', 'Under Review', 'UNDER_REVIEW', '#00B8D9', 'IN_PROGRESS', 2, 'Awaiting review or approval', 'review'),
('00000000-0000-0000-0008-000000000009', '00000000-0000-0000-0002-000000000009', 'Approved', 'APPROVED', '#36B37E', 'DONE', 3, 'Approved and ready to complete', 'approved'),
('00000000-0000-0000-0008-000000000010', '00000000-0000-0000-0002-000000000009', 'Done', 'DONE', '#00875A', 'DONE', 4, 'Completed tasks', 'done'),
('00000000-0000-0000-0008-000000000011', '00000000-0000-0000-0002-000000000009', 'Rejected', 'REJECTED', '#FF5630', 'DONE', 5, 'Rejected items', 'rejected'),
('00000000-0000-0000-0008-000000000012', '00000000-0000-0000-0002-000000000009', 'Cancelled', 'CANCELLED', '#6B778C', 'DONE', 6, 'Cancelled items', 'cancelled')
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Template Workflow Transitions
-- ============================================

-- Project Management Transitions: TO DO -> IN PROGRESS -> DONE
INSERT INTO jira_project.template_workflow_transitions
(id, template_id, from_status_key, to_status_key, transition_name, transition_icon, allow_backward, sequence)
VALUES
('00000000-0000-0000-0009-000000000001', '00000000-0000-0000-0002-000000000007', 'TODO', 'IN_PROGRESS', 'Start Progress', 'arrow-right', TRUE, 0),
('00000000-0000-0000-0009-000000000002', '00000000-0000-0000-0002-000000000007', 'IN_PROGRESS', 'DONE', 'Complete', 'checkmark', TRUE, 1),
('00000000-0000-0000-0009-000000000003', '00000000-0000-0000-0002-000000000007', 'IN_PROGRESS', 'TODO', 'Stop Progress', 'arrow-left', FALSE, 2),
('00000000-0000-0000-0009-000000000004', '00000000-0000-0000-0002-000000000007', 'DONE', 'IN_PROGRESS', 'Reopen', 'arrow-left', FALSE, 3)
ON CONFLICT DO NOTHING;

-- Task Management Transitions: TO DO -> DONE
INSERT INTO jira_project.template_workflow_transitions
(id, template_id, from_status_key, to_status_key, transition_name, transition_icon, allow_backward, sequence)
VALUES
('00000000-0000-0000-0009-000000000005', '00000000-0000-0000-0002-000000000008', 'TODO', 'DONE', 'Complete', 'checkmark', FALSE, 0)
ON CONFLICT DO NOTHING;

-- Process Management Transitions
INSERT INTO jira_project.template_workflow_transitions
(id, template_id, from_status_key, to_status_key, transition_name, transition_icon, allow_backward, requires_approval, sequence)
VALUES
('00000000-0000-0000-0009-000000000006', '00000000-0000-0000-0002-000000000009', 'OPEN', 'IN_PROGRESS', 'Start', 'arrow-right', FALSE, FALSE, 0),
('00000000-0000-0000-0009-000000000007', '00000000-0000-0000-0002-000000000009', 'IN_PROGRESS', 'UNDER_REVIEW', 'Submit for Review', 'arrow-right', FALSE, TRUE, 1),
('00000000-0000-0000-0009-000000000008', '00000000-0000-0000-0002-000000000009', 'UNDER_REVIEW', 'APPROVED', 'Approve', 'checkmark', FALSE, FALSE, 2),
('00000000-0000-0000-0009-000000000009', '00000000-0000-0000-0002-000000000009', 'UNDER_REVIEW', 'REJECTED', 'Reject', 'close', FALSE, FALSE, 3),
('00000000-0000-0000-0009-000000000010', '00000000-0000-0000-0002-000000000009', 'APPROVED', 'DONE', 'Complete', 'checkmark-circle', FALSE, FALSE, 4),
('00000000-0000-0000-0009-000000000011', '00000000-0000-0000-0002-000000000009', 'REJECTED', 'IN_PROGRESS', 'Revise', 'arrow-left', FALSE, FALSE, 5),
('00000000-0000-0000-0009-000000000012', '00000000-0000-0000-0002-000000000009', 'IN_PROGRESS', 'CANCELLED', 'Cancel', 'slash', FALSE, FALSE, 6),
('00000000-0000-0000-0009-000000000013', '00000000-0000-0000-0002-000000000009', 'CANCELLED', 'OPEN', 'Reactivate', 'arrow-left', FALSE, FALSE, 7)
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Template Issue Types
-- ============================================

-- Project Management Issue Types
INSERT INTO jira_project.template_issue_types
(id, template_id, issue_type_name, issue_type_icon, is_default, is_subtask, sequence)
VALUES
('00000000-0000-0000-0010-000000000001', '00000000-0000-0000-0002-000000000007', 'Task', 'task', TRUE, FALSE, 0),
('00000000-0000-0000-0010-000000000002', '00000000-0000-0000-0002-000000000007', 'Sub-task', 'subtask', FALSE, TRUE, 1)
ON CONFLICT DO NOTHING;

-- Task Management Issue Types
INSERT INTO jira_project.template_issue_types
(id, template_id, issue_type_name, issue_type_icon, is_default, is_subtask, sequence)
VALUES
('00000000-0000-0000-0010-000000000003', '00000000-0000-0000-0002-000000000008', 'Task', 'task', TRUE, FALSE, 0),
('00000000-0000-0000-0010-000000000004', '00000000-0000-0000-0002-000000000008', 'Sub-task', 'subtask', FALSE, TRUE, 1)
ON CONFLICT DO NOTHING;

-- Process Management Issue Types
INSERT INTO jira_project.template_issue_types
(id, template_id, issue_type_name, issue_type_icon, is_default, is_subtask, sequence)
VALUES
('00000000-0000-0000-0010-000000000005', '00000000-0000-0000-0002-000000000009', 'Task', 'task', TRUE, FALSE, 0),
('00000000-0000-0000-0010-000000000006', '00000000-0000-0000-0002-000000000009', 'Sub-task', 'subtask', FALSE, TRUE, 1)
ON CONFLICT DO NOTHING;

-- ============================================
-- UPDATE PROJECT TYPES WITH BUSINESS CATEGORY
-- ============================================
DO $$
BEGIN
    -- Add category field to project_types if not exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_types'
                   AND column_name = 'display_order') THEN
        ALTER TABLE jira_project.project_types ADD COLUMN display_order INT DEFAULT 0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'project_types'
                   AND column_name = 'description') THEN
        ALTER TABLE jira_project.project_types ADD COLUMN description TEXT;
    END IF;
END $$;

-- ============================================
-- CREATE LOOKUP TABLES FOR STATUS DEFINITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.status_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status_key VARCHAR(20) NOT NULL UNIQUE,
    status_name VARCHAR(50) NOT NULL,
    status_color VARCHAR(7) NOT NULL,
    status_icon VARCHAR(50),
    status_category VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed standard status definitions
INSERT INTO jira_project.status_definitions (status_key, status_name, status_color, status_icon, status_category, description)
VALUES
    ('TODO', 'To Do', '#6B778C', 'todo', 'TODO', 'Tasks that need to be done'),
    ('IN_PROGRESS', 'In Progress', '#0052CC', 'progress', 'IN_PROGRESS', 'Tasks currently being worked on'),
    ('DONE', 'Done', '#00875A', 'done', 'DONE', 'Completed tasks'),
    ('OPEN', 'Open', '#6B778C', 'open', 'TODO', 'New or reopened items'),
    ('UNDER_REVIEW', 'Under Review', '#00B8D9', 'review', 'IN_PROGRESS', 'Items under review'),
    ('APPROVED', 'Approved', '#36B37E', 'approved', 'DONE', 'Approved items'),
    ('REJECTED', 'Rejected', '#FF5630', 'rejected', 'DONE', 'Rejected items'),
    ('CANCELLED', 'Cancelled', '#6B778C', 'cancelled', 'DONE', 'Cancelled items')
ON CONFLICT (status_key) DO NOTHING;

-- ============================================
-- CREATE TEMPLATE SCHEME MAPPINGS
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'jira_project'
                   AND table_name = 'template_scheme_mappings') THEN
        CREATE TABLE jira_project.template_scheme_mappings (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
            scheme_type VARCHAR(50) NOT NULL, -- ISSUE_TYPE, WORKFLOW, PERMISSION, NOTIFICATION, SCREEN
            scheme_name VARCHAR(100) NOT NULL,
            scheme_id UUID,
            is_default BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            UNIQUE(template_id, scheme_type, scheme_name)
        );

        -- Map new templates to schemes
        INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, is_default)
        VALUES
            ('00000000-0000-0000-0002-000000000007', 'ISSUE_TYPE', 'Task Management Issue Types', TRUE),
            ('00000000-0000-0000-0002-000000000007', 'WORKFLOW', 'Task Workflow', TRUE),
            ('00000000-0000-0000-0002-000000000008', 'ISSUE_TYPE', 'Task Management Issue Types', TRUE),
            ('00000000-0000-0000-0002-000000000008', 'WORKFLOW', 'Task Workflow', TRUE),
            ('00000000-0000-0000-0002-000000000009', 'ISSUE_TYPE', 'Task Management Issue Types', TRUE),
            ('00000000-0000-0000-0002-000000000009', 'WORKFLOW', 'Process Workflow', TRUE)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_template_workflow_statuses_template
ON jira_project.template_workflow_statuses(template_id);

CREATE INDEX IF NOT EXISTS idx_template_issue_types_template
ON jira_project.template_issue_types(template_id);

CREATE INDEX IF NOT EXISTS idx_status_definitions_category
ON jira_project.status_definitions(status_category);
