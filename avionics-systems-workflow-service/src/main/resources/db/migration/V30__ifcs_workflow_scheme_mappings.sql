-- V30__ifcs_workflow_scheme_mappings.sql
-- Maps the 4 IFCS issue types to their workflows within the Aircraft Design System scheme.

DO $$
DECLARE
    v_scheme_id UUID := 'a1000000-0000-0000-0000-000000000001';

    -- Workflow IDs from V29
    wf_group       UUID := 'a0000007-0000-0000-0000-000000000001';
    wf_vvm_card    UUID := 'a0000008-0000-0000-0000-000000000001';
    wf_ivv_card    UUID := 'a0000009-0000-0000-0000-000000000001';
    wf_sub_change  UUID := 'a000000a-0000-0000-0000-000000000001';

    -- Issue type IDs for IFCS types (seeded by V12 in admin-service or V24 in workflow-service)
    -- Use fixed IDs to ensure consistency across environments
    it_group       UUID := 'a0000000-0000-0000-0000-000000000201';
    it_vvm_card    UUID := 'a0000000-0000-0000-0000-000000000202';
    it_ivv_card    UUID := 'a0000000-0000-0000-0000-000000000203';
    it_sub_change  UUID := 'a0000000-0000-0000-0000-000000000204';
BEGIN
    -- Seed IFCS issue types into jira_issue.issue_types with fixed IDs
    INSERT INTO jira_issue.issue_types (id, name, issue_type_key, icon, description, is_subtask, sequence)
    VALUES
        (it_group,      'Group',           'group',       'group',     'IFCS aircraft functionality grouping',       false, 30),
        (it_vvm_card,   'VVM Card',        'vvm_card',    'vvm',       'V&V Management strategy card (IFCS)',        false, 31),
        (it_ivv_card,   'IVV Card',        'ivv_card',    'ivv',       'Formal validation/verification item (IFCS)', false, 32),
        (it_sub_change, 'Sub-Change Card', 'sub_change',  'subchange', 'Detailed Change Card breakdown (IFCS)',      true,  33)
    ON CONFLICT DO NOTHING;

    -- Map each IFCS issue type to its workflow in the existing scheme
    INSERT INTO jira_workflow.workflow_scheme_mappings (id, scheme_id, issue_type_id, workflow_id, created_at)
    VALUES
        (gen_random_uuid(), v_scheme_id, it_group,      wf_group,      CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_scheme_id, it_vvm_card,   wf_vvm_card,   CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_scheme_id, it_ivv_card,   wf_ivv_card,   CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_scheme_id, it_sub_change, wf_sub_change, CURRENT_TIMESTAMP)
    ON CONFLICT DO NOTHING;
END $$;
