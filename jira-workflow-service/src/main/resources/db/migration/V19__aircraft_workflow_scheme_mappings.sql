-- V19__aircraft_workflow_scheme_mappings.sql
-- Seeds aircraft-specific issue types (if not present) and creates a workflow scheme
-- that maps each V&V / defect issue type to its corresponding workflow from V18.

-- ============================================================================
-- 1. Seed aircraft-specific issue types into jira_issue.issue_types
--    (shared database, cross-schema access)
-- ============================================================================
INSERT INTO jira_issue.issue_types (id, name, issue_type_key, icon, description, is_subtask, sequence)
VALUES
    ('a0000000-0000-0000-0000-000000000101', 'VVO',            'vvo',            'verification',  'Verification & Validation Objective',         false, 20),
    ('a0000000-0000-0000-0000-000000000102', 'HLVVO',          'hlvvo',          'hierarchy',     'High-Level Verification & Validation Objective', false, 21),
    ('a0000000-0000-0000-0000-000000000103', 'Change Card',    'change_card',    'change',        'Change Card for design modifications',        false, 22),
    ('a0000000-0000-0000-0000-000000000104', 'Tech Event',     'tech_event',     'event',         'Technical Event per M1668 lifecycle',          false, 23),
    ('a0000000-0000-0000-0000-000000000105', 'Problem Report', 'problem_report', 'problem',       'Problem Report for V&V defects',              false, 24),
    ('a0000000-0000-0000-0000-000000000106', 'Bench Defect',   'bench_defect',   'defect',        'Bench Defect for test-bench issues',          false, 25),
    ('a0000000-0000-0000-0000-000000000107', 'Design Item',    'design_item',    'design',        'Design Item for system design elements',      false, 26),
    ('a0000000-0000-0000-0000-000000000108', 'DCL',            'dcl',            'document',      'Design Change Log entry',                     false, 27),
    ('a0000000-0000-0000-0000-000000000109', 'Deliverable',    'deliverable',    'package',       'Deliverable artifact',                        false, 28),
    ('a0000000-0000-0000-0000-000000000110', 'Test Request',   'test_request',   'test',          'Test Request for V&V execution',              false, 29)
ON CONFLICT (id) DO NOTHING;

-- Also handle conflict on name (some environments may already have partial seeds)
-- This is safe because ON CONFLICT (id) DO NOTHING above covers the ID case.

-- ============================================================================
-- 2. Create the Aircraft Design System workflow scheme
-- ============================================================================
DO $$
DECLARE
    scheme_id UUID := 'a1000000-0000-0000-0000-000000000001';

    -- Workflow IDs from V18
    wf_vvo            UUID := 'a0000001-0000-0000-0000-000000000001';
    wf_hlvvo          UUID := 'a0000002-0000-0000-0000-000000000001';
    wf_change_card    UUID := 'a0000003-0000-0000-0000-000000000001';
    wf_tech_event     UUID := 'a0000004-0000-0000-0000-000000000001';
    wf_problem_report UUID := 'a0000005-0000-0000-0000-000000000001';
    wf_bench_defect   UUID := 'a0000006-0000-0000-0000-000000000001';

    -- Issue type IDs (matching the seeds above)
    it_vvo            UUID := 'a0000000-0000-0000-0000-000000000101';
    it_hlvvo          UUID := 'a0000000-0000-0000-0000-000000000102';
    it_change_card    UUID := 'a0000000-0000-0000-0000-000000000103';
    it_tech_event     UUID := 'a0000000-0000-0000-0000-000000000104';
    it_problem_report UUID := 'a0000000-0000-0000-0000-000000000105';
    it_bench_defect   UUID := 'a0000000-0000-0000-0000-000000000106';
    it_design_item    UUID := 'a0000000-0000-0000-0000-000000000107';
    it_dcl            UUID := 'a0000000-0000-0000-0000-000000000108';
    it_deliverable    UUID := 'a0000000-0000-0000-0000-000000000109';
    it_test_request   UUID := 'a0000000-0000-0000-0000-000000000110';
BEGIN
    -- Create the scheme (default_workflow_id = VVO workflow as fallback)
    INSERT INTO jira_workflow.workflow_schemes
        (id, name, description, is_default, default_workflow_id, is_active, created_at, updated_at)
    VALUES
        (scheme_id,
         'Aircraft Design System Scheme',
         'Workflow scheme for SYSDOPS aircraft design V&V lifecycle — maps each issue type to its specialized workflow',
         true, wf_vvo, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO NOTHING;

    -- Map each issue type to its workflow
    INSERT INTO jira_workflow.workflow_scheme_mappings (id, scheme_id, issue_type_id, workflow_id, created_at)
    VALUES
        (gen_random_uuid(), scheme_id, it_vvo,            wf_vvo,            CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_hlvvo,          wf_hlvvo,          CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_change_card,    wf_change_card,    CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_tech_event,     wf_tech_event,     CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_problem_report, wf_problem_report, CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_bench_defect,   wf_bench_defect,   CURRENT_TIMESTAMP),
        -- Secondary mappings: these issue types share workflows with related types
        (gen_random_uuid(), scheme_id, it_design_item,    wf_change_card,    CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_dcl,            wf_change_card,    CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_deliverable,    wf_change_card,    CURRENT_TIMESTAMP),
        (gen_random_uuid(), scheme_id, it_test_request,   wf_vvo,            CURRENT_TIMESTAMP)
    ON CONFLICT (scheme_id, issue_type_id) DO NOTHING;
END $$;
