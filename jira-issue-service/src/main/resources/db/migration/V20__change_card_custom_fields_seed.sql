-- ============================================================
-- V20: Change Card Custom Field Definitions Seed
-- Seeds custom fields for the 6-tab Change Card structure
-- ============================================================

-- Design Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_impact', 'Impact', 'Impact on permanence, documentation, cost, in-service aircraft, development planning', 'TEXTAREA', 'DESIGN', true, false),
('cc_risk_description', 'Risk Description', 'Potential risks of corrective actions', 'TEXTAREA', 'DESIGN', true, false),
('cc_function', 'Function', 'Function impacted by the change', 'MULTI_SELECT', 'DESIGN', true, true),
('cc_big_picture_team', 'BigPicture Team', 'Team defined in BigPicture configuration', 'LABELS', 'DESIGN', true, false),
('cc_requirement_impact', 'Requirement Impact', 'Requirements impacted', 'LABELS', 'DESIGN', true, false),
('cc_sheet_impact', 'Sheet Impact', 'Sheets impacted', 'LABELS', 'DESIGN', true, false),
('cc_closure_rationale', 'Closure Rationale', 'Information when the change is finished', 'TEXTAREA', 'DESIGN', false, false)
ON CONFLICT (field_key) DO NOTHING;

-- EIF Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_eif_function', 'EIF Function', 'Engine Integration Function', 'LABELS', 'EIF', true, false),
('cc_icd_cntrl_impact', 'ICD CNTRL Impact', 'ICD Control Impact', 'SELECT', 'EIF', true, true),
('cc_icd_bite_impact', 'ICD BITE Impact', 'ICD BITE Impact', 'SELECT', 'EIF', true, true),
('cc_code_cntrl_impact', 'Code CNTRL Impact', 'Code Control Impact', 'SELECT', 'EIF', true, true),
('cc_code_bite_impact', 'Code BITE Impact', 'Code BITE Impact', 'SELECT', 'EIF', true, true),
('cc_scade_impact', 'SCADE Impact', 'SCADE Model Impact', 'SELECT', 'EIF', true, true)
ON CONFLICT (field_key) DO NOTHING;

-- Planning Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_start_date', 'Start Date', 'Change start date', 'DATE', 'PLANNING', true, true),
('cc_end_date', 'End Date', 'Change end date', 'DATE', 'PLANNING', true, true),
('cc_story_points', 'Story Points', 'Estimation of charge', 'NUMBER', 'PLANNING', true, true),
('cc_task_progress', 'Task Progress', 'Percentage of task progression', 'NUMBER', 'PLANNING', true, true)
ON CONFLICT (field_key) DO NOTHING;

-- Review Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_quality_control_status', 'Quality Control Status', 'Status of quality control check', 'SELECT', 'REVIEW', true, true),
('cc_code_generation_status', 'Code Generation Status', 'Status of code generation', 'SELECT', 'REVIEW', true, true),
('cc_design_review_status', 'Design Review Status', 'Result of design review', 'SELECT', 'REVIEW', true, true),
('cc_design_review_assignee', 'Design Review Assignee', 'Person in charge of design review', 'USER_PICKER_MULTI', 'REVIEW', true, false),
('cc_design_review_comment', 'Design Review Comment', 'Comment from reviewer', 'TEXTAREA', 'REVIEW', false, false)
ON CONFLICT (field_key) DO NOTHING;

-- Certification Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_current_behavior', 'Current Behavior', 'Problem description for certification documentation', 'TEXTAREA', 'CERTIFICATION', true, false),
('cc_change_rationale', 'Change Rationale', 'Reason of the change for certification documentation', 'TEXTAREA', 'CERTIFICATION', true, false),
('cc_change_description', 'Change Description', 'Specific description for certification', 'TEXTAREA', 'CERTIFICATION', true, false)
ON CONFLICT (field_key) DO NOTHING;

-- Maturity Test Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_maturity_test', 'Maturity Test', 'Whether designer requests maturity test', 'SELECT', 'MATURITY_TEST', true, true),
('cc_maturity_test_priority', 'Maturity Test Priority', 'Priority of maturity test', 'SELECT', 'MATURITY_TEST', true, true),
('cc_maturity_test_objective', 'Maturity Test Objective', 'Objective of maturity test', 'TEXTAREA', 'MATURITY_TEST', false, false)
ON CONFLICT (field_key) DO NOTHING;

-- Safety Tab Fields
INSERT INTO jira_issue.custom_field_definitions (field_key, name, description, field_type, screen_region, is_searchable, is_sortable)
VALUES
('cc_safety_review_required', 'Safety Team Review Required', 'Whether change has potential safety impact', 'SELECT', 'SAFETY', true, true),
('cc_safety_design_analysis', 'Safety Design Analysis', 'Justification of no impact or foreseen impact details', 'TEXTAREA', 'SAFETY', false, false),
('cc_safety_review_status', 'Safety Review Status', 'Safety specialist assessment', 'SELECT', 'SAFETY', true, true),
('cc_safety_review_assignee', 'Safety Review Assignee', 'Safety specialist', 'USER_PICKER_MULTI', 'SAFETY', true, false),
('cc_safety_review_comment', 'Safety Review Comment', 'Safety specialist justification', 'TEXTAREA', 'SAFETY', false, false)
ON CONFLICT (field_key) DO NOTHING;

-- Add options for SELECT fields
-- ICD impacts: TBC, Yes, No
UPDATE jira_issue.custom_field_definitions SET options = '["TBC", "Yes", "No"]'::jsonb
WHERE field_key IN ('cc_icd_cntrl_impact', 'cc_icd_bite_impact', 'cc_code_cntrl_impact', 'cc_code_bite_impact', 'cc_scade_impact');

-- Quality Control: OK, KO, Not Applicable, Outdated
UPDATE jira_issue.custom_field_definitions SET options = '["OK", "KO", "Not Applicable", "Outdated"]'::jsonb
WHERE field_key = 'cc_quality_control_status';

-- Code Generation: OK, KO, Not Applicable, Outdated
UPDATE jira_issue.custom_field_definitions SET options = '["OK", "KO", "Not Applicable", "Outdated"]'::jsonb
WHERE field_key = 'cc_code_generation_status';

-- Design Review: Green, Amber, Red
UPDATE jira_issue.custom_field_definitions SET options = '["Green", "Amber", "Red"]'::jsonb
WHERE field_key = 'cc_design_review_status';

-- Maturity Test: Yes, No
UPDATE jira_issue.custom_field_definitions SET options = '["Yes", "No"]'::jsonb
WHERE field_key = 'cc_maturity_test';

-- Maturity Priority: P1 High, P2 Medium, P3 Low
UPDATE jira_issue.custom_field_definitions SET options = '["P1 : High", "P2 : Medium", "P3 : Low"]'::jsonb
WHERE field_key = 'cc_maturity_test_priority';

-- Safety Review Required: Yes, No
UPDATE jira_issue.custom_field_definitions SET options = '["Yes", "No"]'::jsonb
WHERE field_key = 'cc_safety_review_required';

-- Safety Review Status
UPDATE jira_issue.custom_field_definitions SET options = '["To Do", "Impact on Safety documentation", "No impact on Safety activities and deliverables", "Not Compliant"]'::jsonb
WHERE field_key = 'cc_safety_review_status';
