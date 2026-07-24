-- V21__system_standard_workflows.sql
-- Seeds M1659.2 System Standard workflow (17 states) and
-- Review Sub-Task workflow (6 states) for the SYSDOPS aircraft design system.

-- ============================================================================
-- 1. M1659.2 SYSTEM STANDARD WORKFLOW  (17 states)
-- States: Backlog -> Internal KoM -> Common KoM -> Plans Review -> FCR ->
--         PDR -> DDR -> CDR -> LAR -> FAR -> FFR -> CR ->
--         In Service Release -> In Service -> Certified -> Closed -> Cancelled
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000007-0000-0000-0000-000000000001';

    -- Status IDs
    s_backlog              UUID := 'b0000007-0000-0000-0000-000000000001';
    s_internal_kom         UUID := 'b0000007-0000-0000-0000-000000000002';
    s_common_kom           UUID := 'b0000007-0000-0000-0000-000000000003';
    s_plans_review         UUID := 'b0000007-0000-0000-0000-000000000004';
    s_fcr                  UUID := 'b0000007-0000-0000-0000-000000000005';
    s_pdr                  UUID := 'b0000007-0000-0000-0000-000000000006';
    s_ddr                  UUID := 'b0000007-0000-0000-0000-000000000007';
    s_cdr                  UUID := 'b0000007-0000-0000-0000-000000000008';
    s_lar                  UUID := 'b0000007-0000-0000-0000-000000000009';
    s_far                  UUID := 'b0000007-0000-0000-0000-00000000000a';
    s_ffr                  UUID := 'b0000007-0000-0000-0000-00000000000b';
    s_cr                   UUID := 'b0000007-0000-0000-0000-00000000000c';
    s_in_service_release   UUID := 'b0000007-0000-0000-0000-00000000000d';
    s_in_service           UUID := 'b0000007-0000-0000-0000-00000000000e';
    s_certified            UUID := 'b0000007-0000-0000-0000-00000000000f';
    s_closed               UUID := 'b0000007-0000-0000-0000-000000000010';
    s_cancelled            UUID := 'b0000007-0000-0000-0000-000000000011';

    -- Transition IDs (forward flow)
    t01 UUID := 'c0000007-0000-0000-0000-000000000001';
    t02 UUID := 'c0000007-0000-0000-0000-000000000002';
    t03 UUID := 'c0000007-0000-0000-0000-000000000003';
    t04 UUID := 'c0000007-0000-0000-0000-000000000004';
    t05 UUID := 'c0000007-0000-0000-0000-000000000005';
    t06 UUID := 'c0000007-0000-0000-0000-000000000006';
    t07 UUID := 'c0000007-0000-0000-0000-000000000007';
    t08 UUID := 'c0000007-0000-0000-0000-000000000008';
    t09 UUID := 'c0000007-0000-0000-0000-000000000009';
    t10 UUID := 'c0000007-0000-0000-0000-00000000000a';
    t11 UUID := 'c0000007-0000-0000-0000-00000000000b';
    t12 UUID := 'c0000007-0000-0000-0000-00000000000c';
    t13 UUID := 'c0000007-0000-0000-0000-00000000000d';
    t14 UUID := 'c0000007-0000-0000-0000-00000000000e';
    t15 UUID := 'c0000007-0000-0000-0000-00000000000f';
    t16 UUID := 'c0000007-0000-0000-0000-000000000010';

    -- Cancel transition IDs (any non-terminal -> Cancelled)
    t_cancel_backlog     UUID := 'c0000007-0000-0000-0000-000000000020';
    t_cancel_ikom        UUID := 'c0000007-0000-0000-0000-000000000021';
    t_cancel_ckom        UUID := 'c0000007-0000-0000-0000-000000000022';
    t_cancel_plans       UUID := 'c0000007-0000-0000-0000-000000000023';
    t_cancel_fcr         UUID := 'c0000007-0000-0000-0000-000000000024';
    t_cancel_pdr         UUID := 'c0000007-0000-0000-0000-000000000025';
    t_cancel_ddr         UUID := 'c0000007-0000-0000-0000-000000000026';
    t_cancel_cdr         UUID := 'c0000007-0000-0000-0000-000000000027';
    t_cancel_lar         UUID := 'c0000007-0000-0000-0000-000000000028';
    t_cancel_far         UUID := 'c0000007-0000-0000-0000-000000000029';
    t_cancel_ffr         UUID := 'c0000007-0000-0000-0000-00000000002a';
    t_cancel_cr          UUID := 'c0000007-0000-0000-0000-00000000002b';
    t_cancel_isr         UUID := 'c0000007-0000-0000-0000-00000000002c';
    t_cancel_is          UUID := 'c0000007-0000-0000-0000-00000000002d';
    t_cancel_cert        UUID := 'c0000007-0000-0000-0000-00000000002e';

    -- Reopen from Closed / Cancelled
    t_reopen_closed      UUID := 'c0000007-0000-0000-0000-000000000030';
    t_reopen_cancelled   UUID := 'c0000007-0000-0000-0000-000000000031';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'M1659.2 System Standard Workflow',
         'System Standard workflow per M1659.2: 17-state lifecycle from Backlog through milestone reviews (Internal KoM, Common KoM, Plans Review, FCR, PDR, DDR, CDR, LAR, FAR, FFR, CR) to In Service Release, In Service, Certified, and Closed. Supports cancellation from any active state.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_backlog,             1),
        (wf_id, s_internal_kom,        2),
        (wf_id, s_common_kom,          3),
        (wf_id, s_plans_review,        4),
        (wf_id, s_fcr,                 5),
        (wf_id, s_pdr,                 6),
        (wf_id, s_ddr,                 7),
        (wf_id, s_cdr,                 8),
        (wf_id, s_lar,                 9),
        (wf_id, s_far,                10),
        (wf_id, s_ffr,                11),
        (wf_id, s_cr,                 12),
        (wf_id, s_in_service_release, 13),
        (wf_id, s_in_service,         14),
        (wf_id, s_certified,          15),
        (wf_id, s_closed,             16),
        (wf_id, s_cancelled,          17)
    ON CONFLICT DO NOTHING;

    -- ---- Forward Transitions (sequential milestone flow) ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t01, wf_id, 'Start Internal KoM',       'Begin Internal Kick-off Meeting phase',             s_backlog,            s_internal_kom,       'MANUAL'),
        (t02, wf_id, 'Proceed to Common KoM',    'Advance to Common Kick-off Meeting',                s_internal_kom,       s_common_kom,         'MANUAL'),
        (t03, wf_id, 'Proceed to Plans Review',   'Advance to Plans Review milestone',                s_common_kom,         s_plans_review,       'MANUAL'),
        (t04, wf_id, 'Proceed to FCR',            'Advance to Functional Configuration Review',       s_plans_review,       s_fcr,                'MANUAL'),
        (t05, wf_id, 'Proceed to PDR',            'Advance to Preliminary Design Review',             s_fcr,                s_pdr,                'MANUAL'),
        (t06, wf_id, 'Proceed to DDR',            'Advance to Detailed Design Review',                s_pdr,                s_ddr,                'MANUAL'),
        (t07, wf_id, 'Proceed to CDR',            'Advance to Critical Design Review',                s_ddr,                s_cdr,                'MANUAL'),
        (t08, wf_id, 'Proceed to LAR',            'Advance to Lab Acceptance Review',                 s_cdr,                s_lar,                'MANUAL'),
        (t09, wf_id, 'Proceed to FAR',            'Advance to Flight Acceptance Review',              s_lar,                s_far,                'MANUAL'),
        (t10, wf_id, 'Proceed to FFR',            'Advance to First Flight Review',                   s_far,                s_ffr,                'MANUAL'),
        (t11, wf_id, 'Proceed to CR',             'Advance to Certification Review',                  s_ffr,                s_cr,                 'MANUAL'),
        (t12, wf_id, 'Release to Service',        'Advance to In Service Release',                    s_cr,                 s_in_service_release, 'MANUAL'),
        (t13, wf_id, 'Enter Service',             'System enters operational service',                 s_in_service_release, s_in_service,         'MANUAL'),
        (t14, wf_id, 'Certify',                   'System achieves certification',                    s_in_service,         s_certified,          'MANUAL'),
        (t15, wf_id, 'Close',                     'Close the system standard',                        s_certified,          s_closed,             'MANUAL'),
        (t16, wf_id, 'Close from In Service',     'Close directly from In Service',                   s_in_service,         s_closed,             'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Cancel Transitions (any non-terminal -> Cancelled) ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_cancel_backlog, wf_id, 'Cancel', 'Cancel from Backlog',              s_backlog,            s_cancelled, 'MANUAL'),
        (t_cancel_ikom,    wf_id, 'Cancel', 'Cancel from Internal KoM',         s_internal_kom,       s_cancelled, 'MANUAL'),
        (t_cancel_ckom,    wf_id, 'Cancel', 'Cancel from Common KoM',           s_common_kom,         s_cancelled, 'MANUAL'),
        (t_cancel_plans,   wf_id, 'Cancel', 'Cancel from Plans Review',         s_plans_review,       s_cancelled, 'MANUAL'),
        (t_cancel_fcr,     wf_id, 'Cancel', 'Cancel from FCR',                  s_fcr,                s_cancelled, 'MANUAL'),
        (t_cancel_pdr,     wf_id, 'Cancel', 'Cancel from PDR',                  s_pdr,                s_cancelled, 'MANUAL'),
        (t_cancel_ddr,     wf_id, 'Cancel', 'Cancel from DDR',                  s_ddr,                s_cancelled, 'MANUAL'),
        (t_cancel_cdr,     wf_id, 'Cancel', 'Cancel from CDR',                  s_cdr,                s_cancelled, 'MANUAL'),
        (t_cancel_lar,     wf_id, 'Cancel', 'Cancel from LAR',                  s_lar,                s_cancelled, 'MANUAL'),
        (t_cancel_far,     wf_id, 'Cancel', 'Cancel from FAR',                  s_far,                s_cancelled, 'MANUAL'),
        (t_cancel_ffr,     wf_id, 'Cancel', 'Cancel from FFR',                  s_ffr,                s_cancelled, 'MANUAL'),
        (t_cancel_cr,      wf_id, 'Cancel', 'Cancel from CR',                   s_cr,                 s_cancelled, 'MANUAL'),
        (t_cancel_isr,     wf_id, 'Cancel', 'Cancel from In Service Release',   s_in_service_release, s_cancelled, 'MANUAL'),
        (t_cancel_is,      wf_id, 'Cancel', 'Cancel from In Service',           s_in_service,         s_cancelled, 'MANUAL'),
        (t_cancel_cert,    wf_id, 'Cancel', 'Cancel from Certified',            s_certified,          s_cancelled, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Reopen Transitions ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        (t_reopen_closed,    wf_id, 'Reopen', 'Reopen from Closed to Backlog',    s_closed,    s_backlog, 'MANUAL'),
        (t_reopen_cancelled, wf_id, 'Reopen', 'Reopen from Cancelled to Backlog', s_cancelled, s_backlog, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Conditions ----
    -- "Certify" requires project admin role
    INSERT INTO jira_workflow.workflow_conditions
        (id, transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        ('d0000007-0000-0000-0000-000000000001', t14, 'USER_ROLE', NULL, 'IN', 'PROJECT_ADMIN', FALSE, 1)
    ON CONFLICT DO NOTHING;

    -- "Close" requires project admin role
    INSERT INTO jira_workflow.workflow_conditions
        (id, transition_id, condition_type, field_name, operator, value, negate, sequence)
    VALUES
        ('d0000007-0000-0000-0000-000000000002', t15, 'USER_ROLE', NULL, 'IN', 'PROJECT_ADMIN', FALSE, 1)
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Start Internal KoM": standard_type required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000007-0000-0000-0000-000000000001', t01, 'FIELD_REQUIRED', 'standard_type',
         'Standard type (LAB or LAB_AND_FLIGHT) is required before starting', 1)
    ON CONFLICT DO NOTHING;

    -- "Release to Service": spec_freeze_date required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000007-0000-0000-0000-000000000002', t12, 'FIELD_REQUIRED', 'spec_freeze_date',
         'Spec freeze date must be set before releasing to service', 1)
    ON CONFLICT DO NOTHING;

    -- ---- Post Functions ----
    -- "Start Internal KoM": auto-create review sub-tasks
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000007-0000-0000-0000-000000000001', t01, 'SCRIPT',
         '{"script":"autoCreateReviewSubTasks","description":"Automatically create the 10 standard M1659.2 review sub-tasks when entering Internal KoM"}',
         1, TRUE, FALSE)
    ON CONFLICT DO NOTHING;
END $$;


-- ============================================================================
-- 2. REVIEW SUB-TASK WORKFLOW  (6 states)
-- States: Backlog -> Planned -> Not Required | Passed Green | Passed Amber | Passed Red
-- On "Passed Red": post-function CLONE_ISSUE (creates follow-up review)
-- ============================================================================
DO $$
DECLARE
    -- Workflow
    wf_id UUID := 'a0000008-0000-0000-0000-000000000001';

    -- Status IDs
    s_backlog       UUID := 'b0000008-0000-0000-0000-000000000001';
    s_planned       UUID := 'b0000008-0000-0000-0000-000000000002';
    s_not_required  UUID := 'b0000008-0000-0000-0000-000000000003';
    s_passed_green  UUID := 'b0000008-0000-0000-0000-000000000004';
    s_passed_amber  UUID := 'b0000008-0000-0000-0000-000000000005';
    s_passed_red    UUID := 'b0000008-0000-0000-0000-000000000006';

    -- Transition IDs
    t_plan          UUID := 'c0000008-0000-0000-0000-000000000001';
    t_not_required  UUID := 'c0000008-0000-0000-0000-000000000002';
    t_pass_green    UUID := 'c0000008-0000-0000-0000-000000000003';
    t_pass_amber    UUID := 'c0000008-0000-0000-0000-000000000004';
    t_pass_red      UUID := 'c0000008-0000-0000-0000-000000000005';
    -- Not Required from Backlog directly
    t_not_req_bl    UUID := 'c0000008-0000-0000-0000-000000000006';
    -- Reopen transitions
    t_reopen_green  UUID := 'c0000008-0000-0000-0000-000000000007';
    t_reopen_amber  UUID := 'c0000008-0000-0000-0000-000000000008';
    t_reopen_red    UUID := 'c0000008-0000-0000-0000-000000000009';
    t_reopen_notr   UUID := 'c0000008-0000-0000-0000-00000000000a';
BEGIN
    -- Workflow record
    INSERT INTO jira_workflow.workflows
        (id, name, description, is_active, is_system, is_draft, version, created_at, updated_at)
    VALUES
        (wf_id, 'Review Sub-Task Workflow',
         'Review Sub-Task workflow: Backlog -> Planned -> Not Required / Passed Green / Passed Amber / Passed Red. On Passed Red a follow-up review is auto-cloned.',
         TRUE, TRUE, FALSE, 1, NOW(), NOW())
    ON CONFLICT DO NOTHING;

    -- Statuses
    INSERT INTO jira_workflow.workflow_statuses (workflow_id, status_id, sequence) VALUES
        (wf_id, s_backlog,      1),
        (wf_id, s_planned,      2),
        (wf_id, s_not_required, 3),
        (wf_id, s_passed_green, 4),
        (wf_id, s_passed_amber, 5),
        (wf_id, s_passed_red,   6)
    ON CONFLICT DO NOTHING;

    -- ---- Transitions ----
    INSERT INTO jira_workflow.workflow_transitions
        (id, workflow_id, name, description, from_status_id, to_status_id, type)
    VALUES
        -- Core flow
        (t_plan,         wf_id, 'Plan Review',       'Schedule the review milestone',                  s_backlog, s_planned,      'MANUAL'),
        (t_not_required, wf_id, 'Mark Not Required',  'Mark review as not required from Planned',      s_planned, s_not_required, 'MANUAL'),
        (t_pass_green,   wf_id, 'Pass Green',         'Review passed with no issues (green)',          s_planned, s_passed_green, 'MANUAL'),
        (t_pass_amber,   wf_id, 'Pass Amber',         'Review passed with minor issues (amber)',       s_planned, s_passed_amber, 'MANUAL'),
        (t_pass_red,     wf_id, 'Pass Red',            'Review passed with major issues (red) — triggers follow-up clone', s_planned, s_passed_red, 'MANUAL'),
        -- Not Required directly from Backlog
        (t_not_req_bl,   wf_id, 'Mark Not Required',  'Mark review as not required from Backlog',     s_backlog, s_not_required, 'MANUAL'),
        -- Reopen transitions
        (t_reopen_green, wf_id, 'Reopen',             'Reopen from Passed Green to Planned',          s_passed_green, s_planned, 'MANUAL'),
        (t_reopen_amber, wf_id, 'Reopen',             'Reopen from Passed Amber to Planned',          s_passed_amber, s_planned, 'MANUAL'),
        (t_reopen_red,   wf_id, 'Reopen',             'Reopen from Passed Red to Planned',            s_passed_red,   s_planned, 'MANUAL'),
        (t_reopen_notr,  wf_id, 'Reopen',             'Reopen from Not Required to Backlog',          s_not_required, s_backlog, 'MANUAL')
    ON CONFLICT DO NOTHING;

    -- ---- Validators ----
    -- "Plan Review": baseline_start_date required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000008-0000-0000-0000-000000000001', t_plan, 'FIELD_REQUIRED', 'baseline_start_date',
         'A baseline start date is required before planning the review', 1)
    ON CONFLICT DO NOTHING;

    -- "Plan Review": baseline_end_date required
    INSERT INTO jira_workflow.workflow_validators
        (id, transition_id, validator_type, field_name, error_message, sequence)
    VALUES
        ('e0000008-0000-0000-0000-000000000002', t_plan, 'FIELD_REQUIRED', 'baseline_end_date',
         'A baseline end date is required before planning the review', 2)
    ON CONFLICT DO NOTHING;

    -- ---- Post Functions ----
    -- "Pass Red": clone issue to create follow-up review
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000008-0000-0000-0000-000000000001', t_pass_red, 'CLONE_ISSUE',
         '{"cloneFields":["review_type","parent_system_standard_id"],"targetStatus":"' || s_backlog || '","linkType":"follow-up","description":"Auto-clone review sub-task on Passed Red to create follow-up review"}',
         1, FALSE, TRUE)
    ON CONFLICT DO NOTHING;

    -- "Pass Red": set review_status field on the original
    INSERT INTO jira_workflow.workflow_post_functions
        (id, transition_id, function_type, function_data, sequence, async, fail_on_error)
    VALUES
        ('f0000008-0000-0000-0000-000000000002', t_pass_red, 'SET_FIELD_VALUE',
         '{"field":"review_status","value":"PASSED_RED"}',
         2, FALSE, FALSE)
    ON CONFLICT DO NOTHING;
END $$;
