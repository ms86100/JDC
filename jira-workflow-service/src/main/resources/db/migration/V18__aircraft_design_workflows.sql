-- V18__aircraft_design_workflows.sql
-- Seeds VVO, HLVVO, Change Card, TechEvent, Problem Report, and Bench Defect
-- workflows for the aircraft design system.
-- These are configurable state machines; statuses are referenced by UUID.

-- ============================================================================
-- 1. VVO WORKFLOW  (6 states)
-- States: New, To be verified, Verified, Released, Cancelled, Superseded
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id   UUID := 'a0000001-0000-0000-0000-000000000001';

    -- Status IDs (these double as status_id in workflow_statuses)
    s_new             UUID := 'b0000001-0000-0000-0000-000000000001';
    s_to_be_verified  UUID := 'b0000001-0000-0000-0000-000000000002';
    s_verified        UUID := 'b0000001-0000-0000-0000-000000000003';
    s_released        UUID := 'b0000001-0000-0000-0000-000000000004';
    s_cancelled       UUID := 'b0000001-0000-0000-0000-000000000005';
    s_superseded      UUID := 'b0000001-0000-0000-0000-000000000006';

    -- Transition IDs
    t_submit          UUID := 'c0000001-0000-0000-0000-000000000001';
    t_approve         UUID := 'c0000001-0000-0000-0000-000000000002';
    t_return_draft    UUID := 'c0000001-0000-0000-0000-000000000003';
    t_release         UUID := 'c0000001-0000-0000-0000-000000000004';
    t_cancel_verified UUID := 'c0000001-0000-0000-0000-000000000005';
    t_cancel_released UUID := 'c0000001-0000-0000-0000-000000000006';
    t_supersede       UUID := 'c0000001-0000-0000-0000-000000000007';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'VVO Workflow',
         'Verification & Validation Objective workflow: New -> To be verified -> Verified -> Released. Supports cancellation and superseding.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_new,            1),
        (wf_id, s_to_be_verified, 2),
        (wf_id, s_verified,       3),
        (wf_id, s_released,       4),
        (wf_id, s_cancelled,      5),
        (wf_id, s_superseded,     6)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_submit,          wf_id, 'Submit for Review',      'Submit VVO for verification review',        s_new,            s_to_be_verified, 'MANUAL'),
        (t_approve,         wf_id, 'Approve',                'Approve VVO after review',                  s_to_be_verified, s_verified,       'MANUAL'),
        (t_return_draft,    wf_id, 'Return to Draft',        'Return VVO to draft for corrections',       s_to_be_verified, s_new,            'MANUAL'),
        (t_release,         wf_id, 'Release to Baseline',    'Release verified VVO to the baseline',      s_verified,       s_released,       'MANUAL'),
        (t_cancel_verified, wf_id, 'Cancel VVO',             'Cancel a verified VVO',                     s_verified,       s_cancelled,      'MANUAL'),
        (t_cancel_released, wf_id, 'Cancel Released VVO',    'Cancel a previously released VVO',          s_released,       s_cancelled,      'MANUAL'),
        (t_supersede,       wf_id, 'Supersede',              'Auto-transition when a newer clone is verified (hidden)', s_new, s_superseded, 'AUTO')
    ON CONFLICT DO NOTHING;

    -- Additional "Supersede" transitions from every other non-terminal status
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        ('c0000001-0000-0000-0000-000000000008', wf_id, 'Supersede', 'Auto-supersede from To be verified', s_to_be_verified, s_superseded, 'AUTO'),
        ('c0000001-0000-0000-0000-000000000009', wf_id, 'Supersede', 'Auto-supersede from Verified',       s_verified,       s_superseded, 'AUTO'),
        ('c0000001-0000-0000-0000-00000000000a', wf_id, 'Supersede', 'Auto-supersede from Released',       s_released,       s_superseded, 'AUTO'),
        ('c0000001-0000-0000-0000-00000000000b', wf_id, 'Supersede', 'Auto-supersede from Cancelled',      s_cancelled,      s_superseded, 'AUTO')
    ON CONFLICT DO NOTHING;

    -- ---- Conditions ----
    -- "Approve" requires project admin role
    INSERT INTO jira_workflow.workflow_conditions
        (id, transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        ('d0000001-0000-0000-0000-000000000001', t_approve, 'USER_ROLE', NULL, 'IN', 'PROJECT_ADMIN', FALSE, 1)
    ON CONFLICT DO NOTHING;

    -- "Release to Baseline" requires project admin role
    INSERT INTO jira_workflow.workflow_conditions
        (id, transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        ('d0000001-0000-0000-0000-000000000002', t_release, 'USER_ROLE', NULL, 'IN', 'PROJECT_ADMIN', FALSE, 1)
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Submit for Review": summary required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000001-0000-0000-0000-000000000001', t_submit, 'FIELD_REQUIRED', 'summary',
         'Summary is required before submitting for review', 1)
    ON CONFLICT DO NOTHING;

    -- "Submit for Review": description required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000001-0000-0000-0000-000000000002', t_submit, 'FIELD_REQUIRED', 'description',
         'Description is required before submitting for review', 2)
    ON CONFLICT DO NOTHING;

    -- "Release to Baseline": fix_version_id required (baseline tag)
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000001-0000-0000-0000-000000000003', t_release, 'FIELD_REQUIRED', 'fix_version_id',
         'A baseline (fix version) must be tagged before releasing', 1)
    ON CONFLICT DO NOTHING;

    -- ---- Post Functions ----
    -- "Approve": auto-transition linked clones to Superseded
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000001-0000-0000-0000-000000000001', t_approve, 'TRANSITION_LINKED_ISSUES',
         '{"linkType":"clone","targetStatus":"' || s_superseded || '","transitionName":"Supersede"}',
         1, TRUE, FALSE)
    ON CONFLICT DO NOTHING;

    -- "Release to Baseline": auto-transition linked clones to Superseded
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000001-0000-0000-0000-000000000002', t_release, 'TRANSITION_LINKED_ISSUES',
         '{"linkType":"clone","targetStatus":"' || s_superseded || '","transitionName":"Supersede"}',
         1, TRUE, FALSE)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 2. HLVVO WORKFLOW  (5 states)
-- States: New, Plan, VVO Writing in Progress, Supplier in Review, Authorize
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000002-0000-0000-0000-000000000001';

    -- Status IDs
    s_new          UUID := 'b0000002-0000-0000-0000-000000000001';
    s_plan         UUID := 'b0000002-0000-0000-0000-000000000002';
    s_writing      UUID := 'b0000002-0000-0000-0000-000000000003';
    s_supplier_rev UUID := 'b0000002-0000-0000-0000-000000000004';
    s_authorize    UUID := 'b0000002-0000-0000-0000-000000000005';

    -- Transition IDs
    t_start_plan  UUID := 'c0000002-0000-0000-0000-000000000001';
    t_begin_write UUID := 'c0000002-0000-0000-0000-000000000002';
    t_send_review UUID := 'c0000002-0000-0000-0000-000000000003';
    t_authorize   UUID := 'c0000002-0000-0000-0000-000000000004';
    t_reopen_plan UUID := 'c0000002-0000-0000-0000-000000000005';
    t_reopen_wrt  UUID := 'c0000002-0000-0000-0000-000000000006';
    t_reopen_sup  UUID := 'c0000002-0000-0000-0000-000000000007';
    t_reopen_auth UUID := 'c0000002-0000-0000-0000-000000000008';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'HLVVO Workflow',
         'High-Level VVO workflow: New -> Plan -> VVO Writing in Progress -> Supplier in Review -> Authorize',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_new,          1),
        (wf_id, s_plan,         2),
        (wf_id, s_writing,      3),
        (wf_id, s_supplier_rev, 4),
        (wf_id, s_authorize,    5)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_start_plan,  wf_id, 'Start Planning',            'Begin HLVVO planning phase',                s_new,          s_plan,         'MANUAL'),
        (t_begin_write, wf_id, 'Begin VVO Writing',         'Start writing individual VVOs',             s_plan,         s_writing,      'MANUAL'),
        (t_send_review, wf_id, 'Send for Supplier Review',  'Send VVOs to supplier for review',          s_writing,      s_supplier_rev, 'MANUAL'),
        (t_authorize,   wf_id, 'Authorize',                 'Authorize the HLVVO after supplier review', s_supplier_rev, s_authorize,    'MANUAL'),
        -- "Reopen" from any non-New status back to New
        (t_reopen_plan, wf_id, 'Reopen',                    'Reopen from Plan to New',                   s_plan,         s_new,          'MANUAL'),
        (t_reopen_wrt,  wf_id, 'Reopen',                    'Reopen from VVO Writing to New',            s_writing,      s_new,          'MANUAL'),
        (t_reopen_sup,  wf_id, 'Reopen',                    'Reopen from Supplier in Review to New',     s_supplier_rev, s_new,          'MANUAL'),
        (t_reopen_auth, wf_id, 'Reopen',                    'Reopen from Authorize to New',              s_authorize,    s_new,          'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Conditions ----
    -- "Authorize" requires project admin role
    INSERT INTO jira_workflow.workflow_conditions
        (id, transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        ('d0000002-0000-0000-0000-000000000001', t_authorize, 'USER_ROLE', NULL, 'IN', 'PROJECT_ADMIN', FALSE, 1)
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Start Planning": target_date required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000002-0000-0000-0000-000000000001', t_start_plan, 'FIELD_REQUIRED', 'target_date',
         'Target date is required before starting planning', 1)
    ON CONFLICT DO NOTHING;

    -- "Begin VVO Writing": assignee required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000002-0000-0000-0000-000000000002', t_begin_write, 'FIELD_REQUIRED', 'assignee',
         'An assignee is required before beginning VVO writing', 1)
    ON CONFLICT DO NOTHING;

    -- ---- Post Functions ----
    -- "Authorize": transition all child VVOs (link type "is parent of") to Verified
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000002-0000-0000-0000-000000000001', t_authorize, 'SCRIPT',
         '{"script":"transitionLinkedIssues","linkType":"is parent of","targetStatus":"b0000001-0000-0000-0000-000000000003","description":"Transition all child VVOs to Verified status"}',
         1, TRUE, FALSE)
    ON CONFLICT DO NOTHING;

    -- "Send for Supplier Review": transition child VVOs with usage
    -- "Formal verification" or "Non Regression" and status "To be verified" to Verified
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000002-0000-0000-0000-000000000002', t_send_review, 'SCRIPT',
         '{"script":"transitionLinkedIssues","linkType":"is parent of","targetStatus":"b0000001-0000-0000-0000-000000000003","filter":{"field":"usage","values":["Formal verification","Non Regression"]},"sourceStatusFilter":"b0000001-0000-0000-0000-000000000002","description":"Transition child VVOs with usage Formal verification or Non Regression from To be verified to Verified"}',
         1, TRUE, FALSE)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 3. CHANGE CARD WORKFLOW  (5 states)
-- States: In Analysis, In Progress, Closed, No Change, Temporary Acceptance
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000003-0000-0000-0000-000000000001';

    -- Status IDs
    s_analysis  UUID := 'b0000003-0000-0000-0000-000000000001';
    s_progress  UUID := 'b0000003-0000-0000-0000-000000000002';
    s_closed    UUID := 'b0000003-0000-0000-0000-000000000003';
    s_no_change UUID := 'b0000003-0000-0000-0000-000000000004';
    s_temp_acc  UUID := 'b0000003-0000-0000-0000-000000000005';

    -- Transition IDs
    t_start_work      UUID := 'c0000003-0000-0000-0000-000000000001';
    t_close           UUID := 'c0000003-0000-0000-0000-000000000002';
    t_no_change       UUID := 'c0000003-0000-0000-0000-000000000003';
    t_temp_accept     UUID := 'c0000003-0000-0000-0000-000000000004';
    t_reopen_closed   UUID := 'c0000003-0000-0000-0000-000000000005';
    t_reopen_nochange UUID := 'c0000003-0000-0000-0000-000000000006';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Change Card Workflow',
         'Change Card workflow: In Analysis -> In Progress -> Closed / No Change / Temporary Acceptance',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_analysis,  1),
        (wf_id, s_progress,  2),
        (wf_id, s_closed,    3),
        (wf_id, s_no_change, 4),
        (wf_id, s_temp_acc,  5)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_start_work,      wf_id, 'Start Work',         'Begin work on the change card',               s_analysis,  s_progress,  'MANUAL'),
        (t_close,           wf_id, 'Close Change',       'Close the change card',                       s_progress,  s_closed,    'MANUAL'),
        (t_no_change,       wf_id, 'Mark No Change',     'Mark change card as requiring no change',     s_progress,  s_no_change, 'MANUAL'),
        (t_temp_accept,     wf_id, 'Temporary Accept',   'Temporarily accept the change',               s_progress,  s_temp_acc,  'MANUAL'),
        (t_reopen_closed,   wf_id, 'Reopen',             'Reopen a closed change card for re-analysis', s_closed,    s_analysis,  'MANUAL'),
        (t_reopen_nochange, wf_id, 'Reopen',             'Reopen a no-change card for re-analysis',     s_no_change, s_analysis,  'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Close Change": closure_rationale required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000003-0000-0000-0000-000000000001', t_close, 'FIELD_REQUIRED', 'closure_rationale',
         'A closure rationale is required before closing the change card', 1)
    ON CONFLICT DO NOTHING;

    -- ---- Post Functions ----
    -- On transitions to final states: set resolved_by = current user

    -- "Close Change" -> SET_FIELD_VALUE resolved_by
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000003-0000-0000-0000-000000000001', t_close, 'SET_FIELD_VALUE',
         '{"field":"resolved_by","value":"CURRENT_USER"}',
         1, FALSE, TRUE)
    ON CONFLICT DO NOTHING;

    -- "Mark No Change" -> SET_FIELD_VALUE resolved_by
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000003-0000-0000-0000-000000000002', t_no_change, 'SET_FIELD_VALUE',
         '{"field":"resolved_by","value":"CURRENT_USER"}',
         1, FALSE, TRUE)
    ON CONFLICT DO NOTHING;

    -- "Temporary Accept" -> SET_FIELD_VALUE resolved_by
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000003-0000-0000-0000-000000000003', t_temp_accept, 'SET_FIELD_VALUE',
         '{"field":"resolved_by","value":"CURRENT_USER"}',
         1, FALSE, TRUE)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 4. TECH EVENT WORKFLOW  (14 states, per M1668)
-- States: Open, Under Originator Analysis, Under Resolver Analysis,
--         Under Test Mean Analysis, Ready for Review, Classified,
--         To be Assessed, Resolved Corrected, Resolved Contained,
--         Proposed for Cancellation, Cancelled, Closed,
--         To be Refined, Unresolved
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000004-0000-0000-0000-000000000001';

    -- Status IDs
    s_open           UUID := 'b0000004-0000-0000-0000-000000000001';
    s_orig_analysis  UUID := 'b0000004-0000-0000-0000-000000000002';
    s_resv_analysis  UUID := 'b0000004-0000-0000-0000-000000000003';
    s_test_analysis  UUID := 'b0000004-0000-0000-0000-000000000004';
    s_ready_review   UUID := 'b0000004-0000-0000-0000-000000000005';
    s_classified     UUID := 'b0000004-0000-0000-0000-000000000006';
    s_to_be_assessed UUID := 'b0000004-0000-0000-0000-000000000007';
    s_res_corrected  UUID := 'b0000004-0000-0000-0000-000000000008';
    s_res_contained  UUID := 'b0000004-0000-0000-0000-000000000009';
    s_prop_cancel    UUID := 'b0000004-0000-0000-0000-00000000000a';
    s_cancelled      UUID := 'b0000004-0000-0000-0000-00000000000b';
    s_closed         UUID := 'b0000004-0000-0000-0000-00000000000c';
    s_to_be_refined  UUID := 'b0000004-0000-0000-0000-00000000000d';
    s_unresolved     UUID := 'b0000004-0000-0000-0000-00000000000e';

    -- Transition IDs
    t01 UUID := 'c0000004-0000-0000-0000-000000000001';
    t02 UUID := 'c0000004-0000-0000-0000-000000000002';
    t03 UUID := 'c0000004-0000-0000-0000-000000000003';
    t04 UUID := 'c0000004-0000-0000-0000-000000000004';
    t05 UUID := 'c0000004-0000-0000-0000-000000000005';
    t06 UUID := 'c0000004-0000-0000-0000-000000000006';
    t07 UUID := 'c0000004-0000-0000-0000-000000000007';
    t08 UUID := 'c0000004-0000-0000-0000-000000000008';
    t09 UUID := 'c0000004-0000-0000-0000-000000000009';
    t10 UUID := 'c0000004-0000-0000-0000-00000000000a';
    t15 UUID := 'c0000004-0000-0000-0000-00000000000f';
    t16 UUID := 'c0000004-0000-0000-0000-000000000010';
    t17 UUID := 'c0000004-0000-0000-0000-000000000011';

    -- "Any -> Proposed for Cancellation" transition IDs (one per source status)
    t_pc_open      UUID := 'c0000004-0000-0000-0000-000000000020';
    t_pc_orig      UUID := 'c0000004-0000-0000-0000-000000000021';
    t_pc_resv      UUID := 'c0000004-0000-0000-0000-000000000022';
    t_pc_test      UUID := 'c0000004-0000-0000-0000-000000000023';
    t_pc_ready     UUID := 'c0000004-0000-0000-0000-000000000024';
    t_pc_classif   UUID := 'c0000004-0000-0000-0000-000000000025';
    t_pc_assessed  UUID := 'c0000004-0000-0000-0000-000000000026';
    t_pc_corr      UUID := 'c0000004-0000-0000-0000-000000000027';
    t_pc_cont      UUID := 'c0000004-0000-0000-0000-000000000028';
    t_pc_refined   UUID := 'c0000004-0000-0000-0000-000000000029';
    t_pc_unresv    UUID := 'c0000004-0000-0000-0000-00000000002a';
    t12            UUID := 'c0000004-0000-0000-0000-00000000000b'; -- Proposed->Cancelled

    -- "Any -> To be Refined" transition IDs (one per source status)
    t_rf_open      UUID := 'c0000004-0000-0000-0000-000000000030';
    t_rf_orig      UUID := 'c0000004-0000-0000-0000-000000000031';
    t_rf_resv      UUID := 'c0000004-0000-0000-0000-000000000032';
    t_rf_test      UUID := 'c0000004-0000-0000-0000-000000000033';
    t_rf_ready     UUID := 'c0000004-0000-0000-0000-000000000034';
    t_rf_classif   UUID := 'c0000004-0000-0000-0000-000000000035';
    t_rf_assessed  UUID := 'c0000004-0000-0000-0000-000000000036';
    t_rf_corr      UUID := 'c0000004-0000-0000-0000-000000000037';
    t_rf_cont      UUID := 'c0000004-0000-0000-0000-000000000038';
    t_rf_unresv    UUID := 'c0000004-0000-0000-0000-000000000039';

    t14            UUID := 'c0000004-0000-0000-0000-00000000000d'; -- Refined->Orig Analysis
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'TechEvent Workflow',
         'Technical Event workflow per M1668: 14-state lifecycle from Open through analysis, classification, assessment, resolution, and closure',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_open,            1),
        (wf_id, s_orig_analysis,   2),
        (wf_id, s_resv_analysis,   3),
        (wf_id, s_test_analysis,   4),
        (wf_id, s_ready_review,    5),
        (wf_id, s_classified,      6),
        (wf_id, s_to_be_assessed,  7),
        (wf_id, s_res_corrected,   8),
        (wf_id, s_res_contained,   9),
        (wf_id, s_prop_cancel,    10),
        (wf_id, s_cancelled,      11),
        (wf_id, s_closed,         12),
        (wf_id, s_to_be_refined,  13),
        (wf_id, s_unresolved,     14)
    ON CONFLICT DO NOTHING;

    -- ---- Core Transitions ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        -- 1. Open -> Under Originator Analysis
        (t01, wf_id, 'Analyze',                    'Begin originator analysis',                         s_open,           s_orig_analysis,  'MANUAL'),
        -- 2. Under Originator Analysis -> Under Resolver Analysis
        (t02, wf_id, 'Transfer to Resolver',       'Transfer to resolver for analysis',                 s_orig_analysis,  s_resv_analysis,  'MANUAL'),
        -- 3. Under Originator Analysis -> Under Test Mean Analysis
        (t03, wf_id, 'Transfer to Test Means',     'Transfer to test means analysis',                   s_orig_analysis,  s_test_analysis,  'MANUAL'),
        -- 4. Under Resolver Analysis -> Ready for Review
        (t04, wf_id, 'Ready for Review',            'Mark as ready for review',                         s_resv_analysis,  s_ready_review,   'MANUAL'),
        -- 5. Under Resolver Analysis -> Classified
        (t05, wf_id, 'Classify',                    'Classify the tech event',                          s_resv_analysis,  s_classified,     'MANUAL'),
        -- 6. Classified -> To be Assessed
        (t06, wf_id, 'Assessment Ready',            'Mark as ready for assessment',                     s_classified,     s_to_be_assessed, 'MANUAL'),
        -- 7. To be Assessed -> Resolved Corrected
        (t07, wf_id, 'Correction Verified OK',      'Correction verified successfully',                 s_to_be_assessed, s_res_corrected,  'MANUAL'),
        -- 8. To be Assessed -> Resolved Contained
        (t08, wf_id, 'Correction Verified KO',      'Correction verification failed, contained only',   s_to_be_assessed, s_res_contained,  'MANUAL'),
        -- 9. Resolved Corrected -> Closed
        (t09, wf_id, 'Close',                       'Close the resolved tech event',                    s_res_corrected,  s_closed,         'MANUAL'),
        -- 10. Resolved Contained -> Under Resolver Analysis
        (t10, wf_id, 'Reopen for Fix',              'Reopen for further resolver analysis',             s_res_contained,  s_resv_analysis,  'MANUAL'),
        -- 12. Proposed for Cancellation -> Cancelled
        (t12, wf_id, 'Confirm Cancellation',        'Confirm cancellation of the tech event',           s_prop_cancel,    s_cancelled,      'MANUAL'),
        -- 14. To be Refined -> Under Originator Analysis
        (t14, wf_id, 'Clarification Provided',      'Clarification provided, return to originator',     s_to_be_refined,  s_orig_analysis,  'MANUAL'),
        -- 15. Closed -> Open
        (t15, wf_id, 'Reopen',                      'Reopen a closed tech event',                       s_closed,         s_open,           'MANUAL'),
        -- 16. Cancelled -> Open
        (t16, wf_id, 'Reopen',                      'Reopen a cancelled tech event',                    s_cancelled,      s_open,           'MANUAL'),
        -- 17. Unresolved -> Under Resolver Analysis
        (t17, wf_id, 'Resume Analysis',             'Resume resolver analysis for unresolved event',     s_unresolved,     s_resv_analysis,  'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- "Any -> Proposed for Cancellation" transitions ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_pc_open,     wf_id, 'Propose Cancellation', 'Propose cancellation from Open',                    s_open,           s_prop_cancel, 'MANUAL'),
        (t_pc_orig,     wf_id, 'Propose Cancellation', 'Propose cancellation from Under Originator Analysis', s_orig_analysis, s_prop_cancel, 'MANUAL'),
        (t_pc_resv,     wf_id, 'Propose Cancellation', 'Propose cancellation from Under Resolver Analysis',   s_resv_analysis, s_prop_cancel, 'MANUAL'),
        (t_pc_test,     wf_id, 'Propose Cancellation', 'Propose cancellation from Under Test Mean Analysis',  s_test_analysis, s_prop_cancel, 'MANUAL'),
        (t_pc_ready,    wf_id, 'Propose Cancellation', 'Propose cancellation from Ready for Review',          s_ready_review,  s_prop_cancel, 'MANUAL'),
        (t_pc_classif,  wf_id, 'Propose Cancellation', 'Propose cancellation from Classified',                s_classified,    s_prop_cancel, 'MANUAL'),
        (t_pc_assessed, wf_id, 'Propose Cancellation', 'Propose cancellation from To be Assessed',            s_to_be_assessed,s_prop_cancel, 'MANUAL'),
        (t_pc_corr,     wf_id, 'Propose Cancellation', 'Propose cancellation from Resolved Corrected',        s_res_corrected, s_prop_cancel, 'MANUAL'),
        (t_pc_cont,     wf_id, 'Propose Cancellation', 'Propose cancellation from Resolved Contained',        s_res_contained, s_prop_cancel, 'MANUAL'),
        (t_pc_refined,  wf_id, 'Propose Cancellation', 'Propose cancellation from To be Refined',             s_to_be_refined, s_prop_cancel, 'MANUAL'),
        (t_pc_unresv,   wf_id, 'Propose Cancellation', 'Propose cancellation from Unresolved',                s_unresolved,    s_prop_cancel, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- "Any -> To be Refined" transitions ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_rf_open,     wf_id, 'Needs Clarification', 'Needs clarification from Open',                     s_open,           s_to_be_refined, 'MANUAL'),
        (t_rf_orig,     wf_id, 'Needs Clarification', 'Needs clarification from Under Originator Analysis', s_orig_analysis, s_to_be_refined, 'MANUAL'),
        (t_rf_resv,     wf_id, 'Needs Clarification', 'Needs clarification from Under Resolver Analysis',   s_resv_analysis, s_to_be_refined, 'MANUAL'),
        (t_rf_test,     wf_id, 'Needs Clarification', 'Needs clarification from Under Test Mean Analysis',  s_test_analysis, s_to_be_refined, 'MANUAL'),
        (t_rf_ready,    wf_id, 'Needs Clarification', 'Needs clarification from Ready for Review',          s_ready_review,  s_to_be_refined, 'MANUAL'),
        (t_rf_classif,  wf_id, 'Needs Clarification', 'Needs clarification from Classified',                s_classified,    s_to_be_refined, 'MANUAL'),
        (t_rf_assessed, wf_id, 'Needs Clarification', 'Needs clarification from To be Assessed',            s_to_be_assessed,s_to_be_refined, 'MANUAL'),
        (t_rf_corr,     wf_id, 'Needs Clarification', 'Needs clarification from Resolved Corrected',        s_res_corrected, s_to_be_refined, 'MANUAL'),
        (t_rf_cont,     wf_id, 'Needs Clarification', 'Needs clarification from Resolved Contained',        s_res_contained, s_to_be_refined, 'MANUAL'),
        (t_rf_unresv,   wf_id, 'Needs Clarification', 'Needs clarification from Unresolved',                s_unresolved,    s_to_be_refined, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Classify": defect_type, defect_origin, defect_impact required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000004-0000-0000-0000-000000000001', t05, 'FIELD_REQUIRED', 'defect_type',
         'Defect type is required for classification', 1),
        ('e0000004-0000-0000-0000-000000000002', t05, 'FIELD_REQUIRED', 'defect_origin',
         'Defect origin is required for classification', 2),
        ('e0000004-0000-0000-0000-000000000003', t05, 'FIELD_REQUIRED', 'defect_impact',
         'Defect impact is required for classification', 3)
    ON CONFLICT DO NOTHING;

    -- "Close": public_analysis required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000004-0000-0000-0000-000000000004', t09, 'FIELD_REQUIRED', 'public_analysis',
         'Public analysis is required before closing', 1)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 5. PROBLEM REPORT WORKFLOW  (4 states)
-- States: Open, Under Analysis, Closed, Rejected
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000005-0000-0000-0000-000000000001';

    -- Status IDs
    s_open     UUID := 'b0000005-0000-0000-0000-000000000001';
    s_analysis UUID := 'b0000005-0000-0000-0000-000000000002';
    s_closed   UUID := 'b0000005-0000-0000-0000-000000000003';
    s_rejected UUID := 'b0000005-0000-0000-0000-000000000004';

    -- Transition IDs
    t_analyze         UUID := 'c0000005-0000-0000-0000-000000000001';
    t_close           UUID := 'c0000005-0000-0000-0000-000000000002';
    t_reject          UUID := 'c0000005-0000-0000-0000-000000000003';
    t_reopen_closed   UUID := 'c0000005-0000-0000-0000-000000000004';
    t_reopen_rejected UUID := 'c0000005-0000-0000-0000-000000000005';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Problem Report Workflow',
         'Problem Report workflow: Open -> Under Analysis -> Closed / Rejected',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_open,     1),
        (wf_id, s_analysis, 2),
        (wf_id, s_closed,   3),
        (wf_id, s_rejected, 4)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_analyze,         wf_id, 'Begin Analysis',    'Start analysis of the problem report',          s_open,     s_analysis, 'MANUAL'),
        (t_close,           wf_id, 'Close',             'Close the problem report',                      s_analysis, s_closed,   'MANUAL'),
        (t_reject,          wf_id, 'Reject',            'Reject the problem report',                     s_analysis, s_rejected, 'MANUAL'),
        (t_reopen_closed,   wf_id, 'Reopen',            'Reopen from Closed',                            s_closed,   s_open,     'MANUAL'),
        (t_reopen_rejected, wf_id, 'Reopen',            'Reopen from Rejected',                          s_rejected, s_open,     'MANUAL')
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 6. BENCH DEFECT WORKFLOW  (6 states)
-- States: Open, Under Analysis, To be Corrected, Corrected, Closed, Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000006-0000-0000-0000-000000000001';

    -- Status IDs
    s_open       UUID := 'b0000006-0000-0000-0000-000000000001';
    s_analysis   UUID := 'b0000006-0000-0000-0000-000000000002';
    s_to_correct UUID := 'b0000006-0000-0000-0000-000000000003';
    s_corrected  UUID := 'b0000006-0000-0000-0000-000000000004';
    s_closed     UUID := 'b0000006-0000-0000-0000-000000000005';
    s_cancelled  UUID := 'b0000006-0000-0000-0000-000000000006';

    -- Transition IDs
    t_analyze     UUID := 'c0000006-0000-0000-0000-000000000001';
    t_to_correct  UUID := 'c0000006-0000-0000-0000-000000000002';
    t_corrected   UUID := 'c0000006-0000-0000-0000-000000000003';
    t_close       UUID := 'c0000006-0000-0000-0000-000000000004';
    t_cancel      UUID := 'c0000006-0000-0000-0000-000000000005';
    -- "Any -> Open" reopen transition IDs
    t_reopen_analysis  UUID := 'c0000006-0000-0000-0000-000000000006';
    t_reopen_tocorrect UUID := 'c0000006-0000-0000-0000-000000000007';
    t_reopen_corrected UUID := 'c0000006-0000-0000-0000-000000000008';
    t_reopen_closed    UUID := 'c0000006-0000-0000-0000-000000000009';
    t_reopen_cancelled UUID := 'c0000006-0000-0000-0000-00000000000a';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Bench Defect Workflow',
         'Bench Defect workflow: Open -> Under Analysis -> To be Corrected -> Corrected -> Closed. Supports cancellation and reopen.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_open,       1),
        (wf_id, s_analysis,   2),
        (wf_id, s_to_correct, 3),
        (wf_id, s_corrected,  4),
        (wf_id, s_closed,     5),
        (wf_id, s_cancelled,  6)
    ON CONFLICT DO NOTHING;

    -- Transitions
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        -- Core flow
        (t_analyze,    wf_id, 'Begin Analysis',     'Start analysis of the bench defect',              s_open,       s_analysis,   'MANUAL'),
        (t_to_correct, wf_id, 'Mark To be Corrected','Analysis complete, correction needed',           s_analysis,   s_to_correct, 'MANUAL'),
        (t_corrected,  wf_id, 'Mark Corrected',      'Correction has been applied',                   s_to_correct, s_corrected,  'MANUAL'),
        (t_close,      wf_id, 'Close',               'Close the bench defect after correction',       s_corrected,  s_closed,     'MANUAL'),
        -- Cancel from Under Analysis
        (t_cancel,     wf_id, 'Cancel',               'Cancel the bench defect',                      s_analysis,   s_cancelled,  'MANUAL'),
        -- "Any -> Open" reopen transitions
        (t_reopen_analysis,  wf_id, 'Reopen', 'Reopen from Under Analysis',  s_analysis,   s_open, 'MANUAL'),
        (t_reopen_tocorrect, wf_id, 'Reopen', 'Reopen from To be Corrected', s_to_correct, s_open, 'MANUAL'),
        (t_reopen_corrected, wf_id, 'Reopen', 'Reopen from Corrected',       s_corrected,  s_open, 'MANUAL'),
        (t_reopen_closed,    wf_id, 'Reopen', 'Reopen from Closed',          s_closed,     s_open, 'MANUAL'),
        (t_reopen_cancelled, wf_id, 'Reopen', 'Reopen from Cancelled',       s_cancelled,  s_open, 'MANUAL')
    ON CONFLICT DO NOTHING;
END $$;
