-- V22__mod_workflow.sql
-- Seeds MOD (Modification) workflow with MAJOR and MINOR paths.
-- MAJOR: Open -> Impact Analysis -> Design Review -> Safety Review -> Certification Review -> Approved -> Implemented -> Closed
-- MINOR: Open -> Quick Review -> Approved -> Implemented -> Closed
-- Conditions route based on modType field.

DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000008-0000-0000-0000-000000000001';

    -- Status IDs
    s_open                UUID := 'b0000008-0000-0000-0000-000000000001';
    s_impact_analysis     UUID := 'b0000008-0000-0000-0000-000000000002';
    s_design_review       UUID := 'b0000008-0000-0000-0000-000000000003';
    s_safety_review       UUID := 'b0000008-0000-0000-0000-000000000004';
    s_certification_review UUID := 'b0000008-0000-0000-0000-000000000005';
    s_quick_review        UUID := 'b0000008-0000-0000-0000-000000000006';
    s_approved            UUID := 'b0000008-0000-0000-0000-000000000007';
    s_implemented         UUID := 'b0000008-0000-0000-0000-000000000008';
    s_closed              UUID := 'b0000008-0000-0000-0000-000000000009';

    -- Transition IDs
    t_start_impact        UUID := 'c0000008-0000-0000-0000-000000000001';
    t_start_quick         UUID := 'c0000008-0000-0000-0000-000000000002';
    t_design_review       UUID := 'c0000008-0000-0000-0000-000000000003';
    t_safety_review       UUID := 'c0000008-0000-0000-0000-000000000004';
    t_cert_review         UUID := 'c0000008-0000-0000-0000-000000000005';
    t_approve_major       UUID := 'c0000008-0000-0000-0000-000000000006';
    t_approve_minor       UUID := 'c0000008-0000-0000-0000-000000000007';
    t_implement           UUID := 'c0000008-0000-0000-0000-000000000008';
    t_close               UUID := 'c0000008-0000-0000-0000-000000000009';
    t_reopen              UUID := 'c0000008-0000-0000-0000-00000000000a';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'MOD Workflow',
         'Modification workflow with MAJOR (full review) and MINOR (quick review) paths for aircraft modifications.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_open,                 1),
        (wf_id, s_impact_analysis,      2),
        (wf_id, s_design_review,        3),
        (wf_id, s_safety_review,        4),
        (wf_id, s_certification_review, 5),
        (wf_id, s_quick_review,         6),
        (wf_id, s_approved,             7),
        (wf_id, s_implemented,          8),
        (wf_id, s_closed,               9)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        -- MAJOR path
        (t_start_impact,  wf_id, 'Start Impact Analysis',    'Route to impact analysis (MAJOR modifications)',      s_open,                s_impact_analysis,      'MANUAL'),
        (t_design_review, wf_id, 'Submit for Design Review',  'Move to design review after impact analysis',          s_impact_analysis,     s_design_review,        'MANUAL'),
        (t_safety_review, wf_id, 'Submit for Safety Review',  'Move to safety review after design review',            s_design_review,       s_safety_review,        'MANUAL'),
        (t_cert_review,   wf_id, 'Submit for Cert Review',    'Move to certification review after safety review',     s_safety_review,       s_certification_review, 'MANUAL'),
        (t_approve_major, wf_id, 'Approve (Major)',            'Approve modification after certification review',      s_certification_review, s_approved,            'MANUAL'),

        -- MINOR path
        (t_start_quick,   wf_id, 'Start Quick Review',        'Route to quick review (MINOR modifications)',           s_open,                s_quick_review,         'MANUAL'),
        (t_approve_minor, wf_id, 'Approve (Minor)',            'Approve modification after quick review',               s_quick_review,        s_approved,             'MANUAL'),

        -- Common path
        (t_implement,     wf_id, 'Mark Implemented',           'Mark modification as implemented',                      s_approved,            s_implemented,          'MANUAL'),
        (t_close,         wf_id, 'Close',                      'Close the modification',                                s_implemented,         s_closed,               'MANUAL'),
        (t_reopen,        wf_id, 'Reopen',                     'Reopen a closed modification',                          s_closed,              s_open,                 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- Conditions: route MAJOR to Impact Analysis, MINOR to Quick Review
    INSERT INTO jira_workflow.workflow_conditions
        (transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        (t_start_impact, 'FIELD_VALUE', 'modType', 'EQUALS', 'MAJOR', FALSE, 1),
        (t_start_quick,  'FIELD_VALUE', 'modType', 'EQUALS', 'MINOR', FALSE, 1)
    ON CONFLICT DO NOTHING;
END $$;
