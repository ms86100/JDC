-- V2: Seed Default Workflows for Project Templates
-- Creates the standard workflows used by project templates

-- ============================================
-- SCRUM WORKFLOW
-- ============================================
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000000'::uuid, 'Scrum Workflow',
     'Default Scrum workflow with sprint states: Backlog -> To Do -> In Progress -> In Review -> Done', TRUE,
     NOW(), NOW()) ON CONFLICT DO NOTHING;

-- Scrum workflow statuses (linked to jira_issue.issue_statuses)
INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0001-000000000001'::uuid, 1),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0001-000000000002'::uuid, 2),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0001-000000000003'::uuid, 3),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0001-000000000004'::uuid, 4),
    ('00000000-0000-0000-0005-000000000001', '00000000-0000-0000-0001-000000000005'::uuid, 5)
ON CONFLICT DO NOTHING;

-- Scrum workflow transitions
INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id) VALUES
    ('00000000-0000-0000-0005-000000000001', 'Start Progress', '00000000-0000-0000-0001-000000000002'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Start Review', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000004'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Approve', '00000000-0000-0000-0001-000000000004'::uuid, '00000000-0000-0000-0001-000000000005'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Reject', '00000000-0000-0000-0001-000000000004'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Reopen', '00000000-0000-0000-0001-000000000005'::uuid, '00000000-0000-0000-0001-000000000002'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Move to Backlog', '00000000-0000-0000-0001-000000000002'::uuid, '00000000-0000-0000-0001-000000000001'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Move to To Do', '00000000-0000-0000-0001-000000000001'::uuid, '00000000-0000-0000-0001-000000000002'::uuid)
ON CONFLICT DO NOTHING;

-- ============================================
-- KANBAN WORKFLOW
-- ============================================
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000000'::uuid, 'Kanban Workflow',
     'Simple Kanban workflow: To Do -> In Progress -> Done', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0001-000000000002'::uuid, 1),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0001-000000000003'::uuid, 2),
    ('00000000-0000-0000-0005-000000000002', '00000000-0000-0000-0001-000000000005'::uuid, 3)
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id) VALUES
    ('00000000-0000-0000-0005-000000000002', 'Start', '00000000-0000-0000-0001-000000000002'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000002', 'Complete', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000005'::uuid),
    ('00000000-0000-0000-0005-000000000002', 'Reopen', '00000000-0000-0000-0001-000000000005'::uuid, '00000000-0000-0000-0001-000000000002'::uuid)
ON CONFLICT DO NOTHING;

-- ============================================
-- BUG WORKFLOW
-- ============================================
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000000'::uuid, 'Bug Workflow',
     'Bug tracking workflow: Open -> In Progress -> Resolved -> Closed', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0001-000000000006'::uuid, 1),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0001-000000000003'::uuid, 2),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0001-000000000007'::uuid, 3),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0001-000000000008'::uuid, 4)
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id) VALUES
    ('00000000-0000-0000-0005-000000000003', 'Start Fix', '00000000-0000-0000-0001-000000000006'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000003', 'Resolve', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000007'::uuid),
    ('00000000-0000-0000-0005-000000000003', 'Close', '00000000-0000-0000-0001-000000000007'::uuid, '00000000-0000-0000-0001-000000000008'::uuid),
    ('00000000-0000-0000-0005-000000000003', 'Reopen', '00000000-0000-0000-0001-000000000007'::uuid, '00000000-0000-0000-0001-000000000006'::uuid),
    ('00000000-0000-0000-0005-000000000003', 'Reopen Closed', '00000000-0000-0000-0001-000000000008'::uuid, '00000000-0000-0000-0001-000000000006'::uuid)
ON CONFLICT DO NOTHING;

-- ============================================
-- TASK WORKFLOW
-- ============================================
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000000'::uuid, 'Task Workflow',
     'Simple task management workflow: Open -> In Progress -> Completed', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0001-000000000002'::uuid, 1),
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0001-000000000003'::uuid, 2),
    ('00000000-0000-0000-0005-000000000004', '00000000-0000-0000-0001-000000000005'::uuid, 3)
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id) VALUES
    ('00000000-0000-0000-0005-000000000004', 'Start', '00000000-0000-0000-0001-000000000002'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000004', 'Complete', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000005'::uuid),
    ('00000000-0000-0000-0005-000000000004', 'Reopen', '00000000-0000-0000-0001-000000000005'::uuid, '00000000-0000-0000-0001-000000000002'::uuid)
ON CONFLICT DO NOTHING;

-- ============================================
-- PORTFOLIO WORKFLOW
-- ============================================
INSERT INTO jira_workflow.workflows (id, project_id, name, description, is_default, created_at, updated_at) VALUES
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0000-000000000000'::uuid, 'Portfolio Workflow',
     'Portfolio-level tracking workflow for initiatives and features', TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0001-000000000001'::uuid, 1),
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0001-000000000009'::uuid, 2),
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0001-000000000003'::uuid, 3),
    ('00000000-0000-0000-0005-000000000005', '00000000-0000-0000-0001-000000000005'::uuid, 4)
ON CONFLICT DO NOTHING;

INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id) VALUES
    ('00000000-0000-0000-0005-000000000005', 'Define', '00000000-0000-0000-0001-000000000001'::uuid, '00000000-0000-0000-0001-000000000009'::uuid),
    ('00000000-0000-0000-0005-000000000005', 'Start', '00000000-0000-0000-0001-000000000009'::uuid, '00000000-0000-0000-0001-000000000003'::uuid),
    ('00000000-0000-0000-0005-000000000005', 'Complete', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000005'::uuid),
    ('00000000-0000-0000-0005-000000000005', 'Return to Backlog', '00000000-0000-0000-0001-000000000009'::uuid, '00000000-0000-0000-0001-000000000001'::uuid)
ON CONFLICT DO NOTHING;