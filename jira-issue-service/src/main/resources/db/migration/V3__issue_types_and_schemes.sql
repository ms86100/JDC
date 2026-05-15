-- V3: Expand Issue Types and Statuses for Project Templates
-- Adds Sub-task issue type and additional statuses for all workflow templates

-- ============================================
-- ADDITIONAL ISSUE TYPES
-- ============================================
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
    ('00000000-0000-0001-0001-000000000005', 'Sub-task', 'subtask', 'A sub-task of a parent issue')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- ADDITIONAL STATUSES FOR WORKFLOWS
-- ============================================
INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
    ('00000000-0000-0000-0001-000000000001', 'Backlog', 0, 'TODO'),
    ('00000000-0000-0000-0001-000000000006', 'Open', 1, 'TODO'),
    ('00000000-0000-0000-0001-000000000007', 'Resolved', 5, 'DONE'),
    ('00000000-0000-0000-0001-000000000008', 'Closed', 6, 'DONE'),
    ('00000000-0000-0000-0001-000000000009', 'Defined', 1, 'TODO')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- ISSUE TYPE SCHEME CONFIGURATIONS
-- These match the scheme IDs in jira_project.issue_type_schemes
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.issue_type_schemes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_type_scheme_mappings (
    scheme_id UUID NOT NULL,
    issue_type_id UUID NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (scheme_id, issue_type_id)
);

-- Seed issue type schemes (matching project service scheme IDs)
INSERT INTO jira_issue.issue_type_schemes (id, name, description, is_default) VALUES
    ('00000000-0000-0000-0003-000000000001', 'Scrum Issue Types', 'Default issue types for Scrum projects', TRUE),
    ('00000000-0000-0000-0003-000000000002', 'Kanban Issue Types', 'Simplified issue types for Kanban', FALSE),
    ('00000000-0000-0000-0003-000000000003', 'Bug Tracking Issue Types', 'Issue types focused on bug tracking', FALSE),
    ('00000000-0000-0000-0003-000000000004', 'Task Management Issue Types', 'Simple task management issue types', FALSE),
    ('00000000-0000-0000-0003-000000000005', 'Portfolio Issue Types', 'Portfolio-level tracking issue types', FALSE),
    ('00000000-0000-0000-0003-000000000006', 'Team-managed Issue Types', 'Simple issue types for team projects', FALSE)
ON CONFLICT (id) DO NOTHING;

-- Seed issue type scheme mappings
INSERT INTO jira_issue.issue_type_scheme_mappings (scheme_id, issue_type_id, is_default) VALUES
    -- Scrum (Epic, Story, Task, Bug, Sub-task)
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0001-0001-000000000004', TRUE), -- Epic is default
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000002', FALSE), -- Story
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000003', FALSE), -- Task
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000001', FALSE), -- Bug
    ('00000000-0000-0000-0003-000000000001', '00000000-0000-0001-0001-000000000005', FALSE), -- Sub-task
    -- Kanban (Task, Bug)
    ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000003', TRUE), -- Task
    ('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000001', FALSE), -- Bug
    -- Bug Tracking (Bug, Story, Task)
    ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000001', TRUE), -- Bug
    ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000002', FALSE), -- Story
    ('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000003', FALSE), -- Task
    -- Task Management (Task, Sub-task)
    ('00000000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000003', TRUE), -- Task
    ('00000000-0000-0000-0003-000000000004', '00000000-0000-0001-0001-000000000005', FALSE), -- Sub-task
    -- Portfolio (Epic, Story, Task)
    ('00000000-0000-0000-0003-000000000005', '00000000-0000-0001-0001-000000000004', TRUE), -- Epic
    ('00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0000-000000000002', FALSE), -- Story
    ('00000000-0000-0000-0003-000000000005', '00000000-0000-0000-0000-000000000003', FALSE), -- Task
    -- Team-managed Basic (Task, Bug)
    ('00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0000-000000000003', TRUE), -- Task
    ('00000000-0000-0000-0003-000000000006', '00000000-0000-0000-0000-000000000001', FALSE) -- Bug
ON CONFLICT DO NOTHING;