-- Kanban board drag-and-drop: allow moving issues backward on the Scrum workflow
-- (In Progress / In Review -> To Do / Backlog)

INSERT INTO jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id)
VALUES
    ('00000000-0000-0000-0005-000000000001', 'Move to To Do', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000002'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Move to Backlog', '00000000-0000-0000-0001-000000000003'::uuid, '00000000-0000-0000-0001-000000000001'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Move to To Do from Review', '00000000-0000-0000-0001-000000000004'::uuid, '00000000-0000-0000-0001-000000000002'::uuid),
    ('00000000-0000-0000-0005-000000000001', 'Move to Backlog from Review', '00000000-0000-0000-0001-000000000004'::uuid, '00000000-0000-0000-0001-000000000001'::uuid)
ON CONFLICT DO NOTHING;
