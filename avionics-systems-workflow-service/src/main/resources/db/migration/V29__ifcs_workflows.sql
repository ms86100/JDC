-- V29__ifcs_workflows.sql
-- Seeds Group, VVM Card, IVV Card, and Sub-Change workflows for the IFCS integration.
-- These are configurable state machines; statuses are referenced by UUID.

-- ============================================================================
-- 1. GROUP WORKFLOW  (6 states)
-- States: Backlog, To Do, In Progress, In Review, Done, Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000007-0000-0000-0000-000000000001';

    -- Status IDs
    s_backlog     UUID := 'b0000007-0000-0000-0000-000000000001';
    s_todo        UUID := 'b0000007-0000-0000-0000-000000000002';
    s_in_progress UUID := 'b0000007-0000-0000-0000-000000000003';
    s_in_review   UUID := 'b0000007-0000-0000-0000-000000000004';
    s_done        UUID := 'b0000007-0000-0000-0000-000000000005';
    s_cancelled   UUID := 'b0000007-0000-0000-0000-000000000006';

    -- Transition IDs
    t_pick_up        UUID := 'c0000007-0000-0000-0000-000000000001';
    t_start_work     UUID := 'c0000007-0000-0000-0000-000000000002';
    t_submit_review  UUID := 'c0000007-0000-0000-0000-000000000003';
    t_approve        UUID := 'c0000007-0000-0000-0000-000000000004';
    t_return_prog    UUID := 'c0000007-0000-0000-0000-000000000005';
    t_cancel_backlog UUID := 'c0000007-0000-0000-0000-000000000006';
    t_cancel_todo    UUID := 'c0000007-0000-0000-0000-000000000007';
    t_cancel_prog    UUID := 'c0000007-0000-0000-0000-000000000008';
    t_cancel_review  UUID := 'c0000007-0000-0000-0000-000000000009';
    t_reopen_done    UUID := 'c0000007-0000-0000-0000-00000000000a';
    t_reopen_cancel  UUID := 'c0000007-0000-0000-0000-00000000000b';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Group Workflow',
         'Group issue workflow: Backlog -> To Do -> In Progress -> In Review -> Done. Supports cancellation and reopen.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_backlog,     1),
        (wf_id, s_todo,        2),
        (wf_id, s_in_progress, 3),
        (wf_id, s_in_review,   4),
        (wf_id, s_done,        5),
        (wf_id, s_cancelled,   6)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_pick_up,        wf_id, 'Pick Up',           'Move group from backlog to to-do',          s_backlog,     s_todo,        'MANUAL'),
        (t_start_work,     wf_id, 'Start Work',        'Begin work on the group',                   s_todo,        s_in_progress, 'MANUAL'),
        (t_submit_review,  wf_id, 'Submit for Review',  'Submit group work for review',             s_in_progress, s_in_review,   'MANUAL'),
        (t_approve,        wf_id, 'Approve',            'Approve the group and mark done',          s_in_review,   s_done,        'MANUAL'),
        (t_return_prog,    wf_id, 'Return to Progress', 'Return from review to in progress',        s_in_review,   s_in_progress, 'MANUAL'),
        -- Cancel from any active state
        (t_cancel_backlog, wf_id, 'Cancel', 'Cancel from Backlog',     s_backlog,     s_cancelled, 'MANUAL'),
        (t_cancel_todo,    wf_id, 'Cancel', 'Cancel from To Do',       s_todo,        s_cancelled, 'MANUAL'),
        (t_cancel_prog,    wf_id, 'Cancel', 'Cancel from In Progress', s_in_progress, s_cancelled, 'MANUAL'),
        (t_cancel_review,  wf_id, 'Cancel', 'Cancel from In Review',   s_in_review,   s_cancelled, 'MANUAL'),
        -- Reopen
        (t_reopen_done,    wf_id, 'Reopen', 'Reopen from Done',       s_done,        s_backlog,   'MANUAL'),
        (t_reopen_cancel,  wf_id, 'Reopen', 'Reopen from Cancelled',  s_cancelled,   s_backlog,   'MANUAL')
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 2. VVM CARD WORKFLOW  (5 states)
-- States: To Do, In Progress, CIA Frozen, Done, Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000008-0000-0000-0000-000000000001';

    -- Status IDs
    s_todo        UUID := 'b0000008-0000-0000-0000-000000000001';
    s_in_progress UUID := 'b0000008-0000-0000-0000-000000000002';
    s_cia_frozen  UUID := 'b0000008-0000-0000-0000-000000000003';
    s_done        UUID := 'b0000008-0000-0000-0000-000000000004';
    s_cancelled   UUID := 'b0000008-0000-0000-0000-000000000005';

    -- Transition IDs
    t_start_work     UUID := 'c0000008-0000-0000-0000-000000000001';
    t_freeze_cia     UUID := 'c0000008-0000-0000-0000-000000000002';
    t_complete       UUID := 'c0000008-0000-0000-0000-000000000003';
    t_unfreeze       UUID := 'c0000008-0000-0000-0000-000000000004';
    t_cancel_todo    UUID := 'c0000008-0000-0000-0000-000000000005';
    t_cancel_prog    UUID := 'c0000008-0000-0000-0000-000000000006';
    t_cancel_frozen  UUID := 'c0000008-0000-0000-0000-000000000007';
    t_reopen_done    UUID := 'c0000008-0000-0000-0000-000000000008';
    t_reopen_cancel  UUID := 'c0000008-0000-0000-0000-000000000009';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'VVM Card Workflow',
         'VVM Card workflow: To Do -> In Progress -> CIA Frozen -> Done. Supports cancellation and reopen.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_todo,        1),
        (wf_id, s_in_progress, 2),
        (wf_id, s_cia_frozen,  3),
        (wf_id, s_done,        4),
        (wf_id, s_cancelled,   5)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_start_work,    wf_id, 'Start Work',      'Begin work on the VVM card',               s_todo,        s_in_progress, 'MANUAL'),
        (t_freeze_cia,    wf_id, 'Freeze CIA',       'Freeze the CIA for the VVM card',         s_in_progress, s_cia_frozen,  'MANUAL'),
        (t_complete,      wf_id, 'Complete',          'Mark VVM card as done',                   s_cia_frozen,  s_done,        'MANUAL'),
        (t_unfreeze,      wf_id, 'Unfreeze',          'Unfreeze CIA and return to in progress',  s_cia_frozen,  s_in_progress, 'MANUAL'),
        -- Cancel from active states
        (t_cancel_todo,   wf_id, 'Cancel', 'Cancel from To Do',       s_todo,        s_cancelled, 'MANUAL'),
        (t_cancel_prog,   wf_id, 'Cancel', 'Cancel from In Progress', s_in_progress, s_cancelled, 'MANUAL'),
        (t_cancel_frozen, wf_id, 'Cancel', 'Cancel from CIA Frozen',  s_cia_frozen,  s_cancelled, 'MANUAL'),
        -- Reopen
        (t_reopen_done,   wf_id, 'Reopen', 'Reopen from Done',       s_done,        s_todo,      'MANUAL'),
        (t_reopen_cancel, wf_id, 'Reopen', 'Reopen from Cancelled',  s_cancelled,   s_todo,      'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Freeze CIA": summary required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000008-0000-0000-0000-000000000001', t_freeze_cia, 'FIELD_REQUIRED', 'summary',
         'Summary is required before freezing CIA', 1)
    ON CONFLICT DO NOTHING;

    -- "Complete": assignee required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000008-0000-0000-0000-000000000002', t_complete, 'FIELD_REQUIRED', 'assignee',
         'An assignee is required before completing the VVM card', 1)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 3. IVV CARD WORKFLOW  (4 states)
-- States: Backlog, Planned, Done, Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000009-0000-0000-0000-000000000001';

    -- Status IDs
    s_backlog   UUID := 'b0000009-0000-0000-0000-000000000001';
    s_planned   UUID := 'b0000009-0000-0000-0000-000000000002';
    s_done      UUID := 'b0000009-0000-0000-0000-000000000003';
    s_cancelled UUID := 'b0000009-0000-0000-0000-000000000004';

    -- Transition IDs
    t_plan            UUID := 'c0000009-0000-0000-0000-000000000001';
    t_complete        UUID := 'c0000009-0000-0000-0000-000000000002';
    t_cancel_backlog  UUID := 'c0000009-0000-0000-0000-000000000003';
    t_cancel_planned  UUID := 'c0000009-0000-0000-0000-000000000004';
    t_reopen_done     UUID := 'c0000009-0000-0000-0000-000000000005';
    t_reopen_cancel   UUID := 'c0000009-0000-0000-0000-000000000006';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'IVV Card Workflow',
         'IVV Card workflow: Backlog -> Planned -> Done. Supports cancellation and reopen.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_backlog,   1),
        (wf_id, s_planned,   2),
        (wf_id, s_done,      3),
        (wf_id, s_cancelled, 4)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_plan,           wf_id, 'Plan',     'Move IVV card from backlog to planned',   s_backlog,   s_planned,   'MANUAL'),
        (t_complete,       wf_id, 'Complete', 'Mark IVV card as done',                   s_planned,   s_done,      'MANUAL'),
        -- Cancel from active states
        (t_cancel_backlog, wf_id, 'Cancel',   'Cancel from Backlog',                     s_backlog,   s_cancelled, 'MANUAL'),
        (t_cancel_planned, wf_id, 'Cancel',   'Cancel from Planned',                     s_planned,   s_cancelled, 'MANUAL'),
        -- Reopen
        (t_reopen_done,    wf_id, 'Reopen',   'Reopen from Done',                        s_done,      s_backlog,   'MANUAL'),
        (t_reopen_cancel,  wf_id, 'Reopen',   'Reopen from Cancelled',                   s_cancelled, s_backlog,   'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Complete": assignee required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000009-0000-0000-0000-000000000001', t_complete, 'FIELD_REQUIRED', 'assignee',
         'An assignee is required before completing the IVV card', 1)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 4. SUB-CHANGE WORKFLOW  (5 states)
-- States: To Do, In Progress, In Review, Done, Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a000000a-0000-0000-0000-000000000001';

    -- Status IDs
    s_todo        UUID := 'b000000a-0000-0000-0000-000000000001';
    s_in_progress UUID := 'b000000a-0000-0000-0000-000000000002';
    s_in_review   UUID := 'b000000a-0000-0000-0000-000000000003';
    s_done        UUID := 'b000000a-0000-0000-0000-000000000004';
    s_cancelled   UUID := 'b000000a-0000-0000-0000-000000000005';

    -- Transition IDs
    t_start_work     UUID := 'c000000a-0000-0000-0000-000000000001';
    t_submit_review  UUID := 'c000000a-0000-0000-0000-000000000002';
    t_approve        UUID := 'c000000a-0000-0000-0000-000000000003';
    t_return_prog    UUID := 'c000000a-0000-0000-0000-000000000004';
    t_cancel_todo    UUID := 'c000000a-0000-0000-0000-000000000005';
    t_cancel_prog    UUID := 'c000000a-0000-0000-0000-000000000006';
    t_cancel_review  UUID := 'c000000a-0000-0000-0000-000000000007';
    t_reopen_done    UUID := 'c000000a-0000-0000-0000-000000000008';
    t_reopen_cancel  UUID := 'c000000a-0000-0000-0000-000000000009';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Sub-Change Workflow',
         'Sub-Change Card workflow: To Do -> In Progress -> In Review -> Done. Supports cancellation and reopen.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_todo,        1),
        (wf_id, s_in_progress, 2),
        (wf_id, s_in_review,   3),
        (wf_id, s_done,        4),
        (wf_id, s_cancelled,   5)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_start_work,    wf_id, 'Start Work',        'Begin work on the sub-change card',        s_todo,        s_in_progress, 'MANUAL'),
        (t_submit_review, wf_id, 'Submit for Review',  'Submit sub-change for review',            s_in_progress, s_in_review,   'MANUAL'),
        (t_approve,       wf_id, 'Approve',            'Approve the sub-change and mark done',    s_in_review,   s_done,        'MANUAL'),
        (t_return_prog,   wf_id, 'Return to Progress', 'Return from review to in progress',       s_in_review,   s_in_progress, 'MANUAL'),
        -- Cancel from active states
        (t_cancel_todo,   wf_id, 'Cancel', 'Cancel from To Do',       s_todo,        s_cancelled, 'MANUAL'),
        (t_cancel_prog,   wf_id, 'Cancel', 'Cancel from In Progress', s_in_progress, s_cancelled, 'MANUAL'),
        (t_cancel_review, wf_id, 'Cancel', 'Cancel from In Review',   s_in_review,   s_cancelled, 'MANUAL'),
        -- Reopen
        (t_reopen_done,   wf_id, 'Reopen', 'Reopen from Done',       s_done,        s_todo,      'MANUAL'),
        (t_reopen_cancel, wf_id, 'Reopen', 'Reopen from Cancelled',  s_cancelled,   s_todo,      'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Submit for Review": summary required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e000000a-0000-0000-0000-000000000001', t_submit_review, 'FIELD_REQUIRED', 'summary',
         'Summary is required before submitting for review', 1)
    ON CONFLICT DO NOTHING;
END $$;
