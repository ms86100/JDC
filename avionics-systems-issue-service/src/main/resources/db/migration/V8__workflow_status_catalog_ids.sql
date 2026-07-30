-- Align issue status IDs with workflow seed (jira_workflow.workflow_statuses.status_id)
-- Jira DC: workflows reference global statuses by stable id; UI shows names (Backlog, To Do, ...).

UPDATE jira_issue.issue_statuses
SET name = name || ' (legacy)'
WHERE id::text NOT LIKE '00000000-0000-0000-0001-%'
  AND name IN ('Backlog', 'To Do', 'In Progress', 'In Review', 'Done', 'Open', 'Resolved', 'Closed', 'Defined');

INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
    ('00000000-0000-0000-0001-000000000001', 'Backlog', 0, 'TODO'),
    ('00000000-0000-0000-0001-000000000002', 'To Do', 1, 'TODO'),
    ('00000000-0000-0000-0001-000000000003', 'In Progress', 2, 'IN_PROGRESS'),
    ('00000000-0000-0000-0001-000000000004', 'In Review', 3, 'IN_PROGRESS'),
    ('00000000-0000-0000-0001-000000000005', 'Done', 4, 'DONE'),
    ('00000000-0000-0000-0001-000000000006', 'Open', 5, 'TODO'),
    ('00000000-0000-0000-0001-000000000007', 'Resolved', 6, 'DONE'),
    ('00000000-0000-0000-0001-000000000008', 'Closed', 7, 'DONE'),
    ('00000000-0000-0000-0001-000000000009', 'Defined', 8, 'TODO')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    sequence = EXCLUDED.sequence,
    category = EXCLUDED.category;
