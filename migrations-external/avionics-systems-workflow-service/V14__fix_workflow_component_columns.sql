
-- V14__fix_workflow_component_columns.sql
-- Fixes schema mismatch between V3/V9 migrations and entity expectations

-- The workflow_conditions, validators, and post_functions tables were created
-- with V3 schema which has different column names. This migration adds the
-- missing columns that the JPA entities expect.

-- ============================================
-- WORKFLOW CONDITIONS - Add missing columns
-- ============================================
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS condition_data TEXT;
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS field_name VARCHAR(100);
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS operator VARCHAR(20);
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS value VARCHAR(500);
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS negate BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_workflow.workflow_conditions ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;

-- Migrate data: copy old V3 columns to new V9 columns
-- Cast jsonb to text for the migration
UPDATE jira_workflow.workflow_conditions
SET condition_data = condition_config::text,
    field_name = NULL,
    operator = NULL,
    value = NULL,
    negate = COALESCE(negate, false),
    sequence = COALESCE(sequence, condition_order)
WHERE condition_data IS NULL;

-- ============================================
-- WORKFLOW VALIDATORS - Add missing columns
-- ============================================
ALTER TABLE jira_workflow.workflow_validators ADD COLUMN IF NOT EXISTS field_name VARCHAR(100);
ALTER TABLE jira_workflow.workflow_validators ADD COLUMN IF NOT EXISTS validator_data TEXT;
ALTER TABLE jira_workflow.workflow_validators ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_workflow.workflow_validators ADD COLUMN IF NOT EXISTS continue_on_error BOOLEAN DEFAULT FALSE;

-- Migrate data
UPDATE jira_workflow.workflow_validators
SET field_name = NULL,
    validator_data = validator_config::text,
    sequence = COALESCE(sequence, validator_order)
WHERE validator_data IS NULL;

-- ============================================
-- WORKFLOW POST FUNCTIONS - Ensure all columns exist
-- ============================================
ALTER TABLE jira_workflow.workflow_post_functions ADD COLUMN IF NOT EXISTS function_data TEXT;

-- Note: function_type column already exists from V9

-- ============================================
-- Create seed condition types if empty
-- ============================================
INSERT INTO jira_workflow.condition_templates (id, template_name, condition_type, default_config)
SELECT gen_random_uuid(), 'Permission Check', 'PERMISSION', '{"permission":"LOGICED"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.condition_templates WHERE condition_type='PERMISSION');

INSERT INTO jira_workflow.condition_templates (id, template_name, condition_type, default_config)
SELECT gen_random_uuid(), 'User Group', 'USER_GROUP', '{"group":"jira-users"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.condition_templates WHERE condition_type='USER_GROUP');

INSERT INTO jira_workflow.condition_templates (id, template_name, condition_type, default_config)
SELECT gen_random_uuid(), 'Field Value', 'FIELD_VALUE', '{"field":"status","operator":"EQUALS"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.condition_templates WHERE condition_type='FIELD_VALUE');

-- ============================================
-- Create seed validator types if empty
-- ============================================
INSERT INTO jira_workflow.validator_templates (id, template_name, validator_type, default_config)
SELECT gen_random_uuid(), 'Field Required', 'FIELD_REQUIRED', '{"field":"summary","required":true}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.validator_templates WHERE validator_type='FIELD_REQUIRED');

INSERT INTO jira_workflow.validator_templates (id, template_name, validator_type, default_config)
SELECT gen_random_uuid(), 'Field Has Value', 'FIELD_HAS_VALUE', '{"field":"description"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.validator_templates WHERE validator_type='FIELD_HAS_VALUE');

INSERT INTO jira_workflow.validator_templates (id, template_name, validator_type, default_config)
SELECT gen_random_uuid(), 'Permission Check', 'PERMISSION', '{"permission":"EDIT"}'::jsonb
WHERE NOT EXISTS (SELECT 1 FROM jira_workflow.validator_templates WHERE validator_type='PERMISSION');