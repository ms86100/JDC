-- V31: Complete HLVVO workflow to match nFMS VVO Guidelines (11 states)
-- Adds: DO Review, LAB Review, VVO Allocation, Closed, On Hold, Cancelled
-- Reroutes: VVO Writing -> DO Review (instead of directly to Supplier)

DO $$
DECLARE
    wf_id UUID := 'a0000002-0000-0000-0000-000000000001';

    -- Existing status IDs
    s_new          UUID := 'b0000002-0000-0000-0000-000000000001';
    s_plan         UUID := 'b0000002-0000-0000-0000-000000000002';
    s_writing      UUID := 'b0000002-0000-0000-0000-000000000003';
    s_supplier_rev UUID := 'b0000002-0000-0000-0000-000000000004';
    s_authorize    UUID := 'b0000002-0000-0000-0000-000000000005';

    -- NEW status IDs
    s_do_review    UUID := 'b0000002-0000-0000-0000-000000000006';
    s_lab_review   UUID := 'b0000002-0000-0000-0000-000000000007';
    s_vvo_assign   UUID := 'b0000002-0000-0000-0000-000000000008';
    s_closed       UUID := 'b0000002-0000-0000-0000-000000000009';
    s_on_hold      UUID := 'b0000002-0000-0000-0000-00000000000a';
    s_cancelled    UUID := 'b0000002-0000-0000-0000-00000000000b';
BEGIN
    -- Add new statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_do_review, 6),
        (wf_id, s_lab_review, 7),
        (wf_id, s_vvo_assign, 8),
        (wf_id, s_closed, 9),
        (wf_id, s_on_hold, 10),
        (wf_id, s_cancelled, 11)
    ON CONFLICT DO NOTHING;

    -- Update existing transition: VVO Writing now goes to DO Review (not directly to Supplier)
    -- Delete old direct transition from writing to supplier_rev
    DELETE FROM jira_workflow.workflow_transitions
    WHERE id = 'c0000002-0000-0000-0000-000000000003' AND workflow_id = wf_id;

    -- Add new transitions for the full review chain
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        -- Phase 1: DO Review
        ('c0000002-0000-0000-0000-000000000010', wf_id, 'Send for DO Review', 'Submit VVOs for Design Office proofreading', s_writing, s_do_review, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000011', wf_id, 'DO Review NOK', 'Return to VVO writing after DO review rejection', s_do_review, s_writing, 'MANUAL'),
        -- Phase 2: LAB Review
        ('c0000002-0000-0000-0000-000000000012', wf_id, 'DO Review OK', 'DO review passed, send to LAB for proofreading', s_do_review, s_lab_review, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000013', wf_id, 'LAB Review NOK', 'Return to VVO writing after LAB review rejection', s_lab_review, s_writing, 'MANUAL'),
        -- VVO Allocation
        ('c0000002-0000-0000-0000-000000000014', wf_id, 'LAB Review OK', 'LAB review passed, proceed to VVO allocation', s_lab_review, s_vvo_assign, 'MANUAL'),
        -- Phase 3: Supplier Review
        ('c0000002-0000-0000-0000-000000000015', wf_id, 'Send to Supplier', 'Send VVOs to supplier(s) for review', s_vvo_assign, s_supplier_rev, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000016', wf_id, 'Supplier Review NOK', 'Return to VVO writing after supplier rejection', s_supplier_rev, s_writing, 'MANUAL'),
        -- Terminal states
        ('c0000002-0000-0000-0000-000000000017', wf_id, 'Close HLVVO', 'Close the authorized HLVVO', s_authorize, s_closed, 'MANUAL'),
        -- On Hold (from any active state)
        ('c0000002-0000-0000-0000-000000000020', wf_id, 'Put on Hold', 'Put HLVVO on hold', s_writing, s_on_hold, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000021', wf_id, 'Put on Hold', 'Put HLVVO on hold', s_do_review, s_on_hold, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000022', wf_id, 'Put on Hold', 'Put HLVVO on hold', s_lab_review, s_on_hold, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000023', wf_id, 'Put on Hold', 'Put HLVVO on hold', s_supplier_rev, s_on_hold, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000024', wf_id, 'Resume from Hold', 'Resume HLVVO from on-hold', s_on_hold, s_writing, 'MANUAL'),
        -- Cancel (from any active state)
        ('c0000002-0000-0000-0000-000000000030', wf_id, 'Cancel', 'Cancel HLVVO', s_new, s_cancelled, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000031', wf_id, 'Cancel', 'Cancel HLVVO', s_plan, s_cancelled, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000032', wf_id, 'Cancel', 'Cancel HLVVO', s_writing, s_cancelled, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000033', wf_id, 'Cancel', 'Cancel HLVVO', s_do_review, s_cancelled, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000034', wf_id, 'Cancel', 'Cancel HLVVO', s_lab_review, s_cancelled, 'MANUAL'),
        ('c0000002-0000-0000-0000-000000000035', wf_id, 'Cancel', 'Cancel HLVVO', s_supplier_rev, s_cancelled, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- Update workflow description
    UPDATE jira_workflow.workflows SET
        description = 'HLVVO workflow per nFMS VVO Guidelines: New -> Plan -> VVO Writing -> DO Review -> LAB Review -> VVO Allocation -> Supplier Review -> Authorize -> Closed (+ On Hold, Cancelled)',
        updated_at = NOW()
    WHERE id = wf_id;
END $$;

-- ============================================================================
-- Add status definitions for the 6 new HLVVO statuses
-- (extends V28 workflow_status_definitions table)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000002-0000-0000-0000-000000000006', 'DO Review',        'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000007', 'LAB Review',       'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000008', 'VVO Allocation',   'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000009', 'Closed',           'DONE'),
    ('b0000002-0000-0000-0000-00000000000a', 'On Hold',          'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-00000000000b', 'Cancelled',        'DONE')
ON CONFLICT (status_id) DO NOTHING;
