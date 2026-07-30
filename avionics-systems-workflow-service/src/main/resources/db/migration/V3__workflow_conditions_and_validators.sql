-- V3__workflow_conditions_and_validators.sql
-- Workflow Service - Conditions and validators for workflow transitions

CREATE SCHEMA IF NOT EXISTS jira_workflow;

-- ============================================
-- WORKFLOW CONDITIONS TABLE
-- Define conditions for transition execution
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    condition_type VARCHAR(50) NOT NULL, -- FIELD_VALUE, USER_ROLE, SCRIPT, REGEX
    condition_config JSONB NOT NULL,
    condition_order INTEGER DEFAULT 0,
    is_required BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- VALIDATORS TABLE
-- Define validation rules for transitions
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_validators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    validator_type VARCHAR(50) NOT NULL, -- FIELD_REQUIRED, FIELD_LENGTH, SCRIPT, CUSTOM
    validator_config JSONB NOT NULL,
    validator_order INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- CONDITION EXAMPLES TABLE
-- Common pre-defined conditions
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.condition_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(200) NOT NULL,
    template_description TEXT,
    condition_type VARCHAR(50) NOT NULL,
    default_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- VALIDATOR TEMPLATES TABLE
-- Common pre-defined validators
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.validator_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(200) NOT NULL,
    template_description TEXT,
    validator_type VARCHAR(50) NOT NULL,
    default_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_conditions_transition ON jira_workflow.workflow_conditions(transition_id);
CREATE INDEX IF NOT EXISTS idx_validators_transition ON jira_workflow.workflow_validators(transition_id);
CREATE INDEX IF NOT EXISTS idx_condition_templates_type ON jira_workflow.condition_templates(condition_type);
CREATE INDEX IF NOT EXISTS idx_validator_templates_type ON jira_workflow.validator_templates(validator_type);

-- ============================================
-- SEED DATA: Condition Templates
-- ============================================
INSERT INTO jira_workflow.condition_templates (id, template_name, condition_type, default_config) VALUES
    (gen_random_uuid(), 'Assignee is Current User', 'FIELD_VALUE', '{"field": "assignee", "operator": "CURRENT_USER"}'),
    (gen_random_uuid(), 'Reporter is Current User', 'FIELD_VALUE', '{"field": "reporter", "operator": "CURRENT_USER"}'),
    (gen_random_uuid(), 'Field is Empty', 'FIELD_VALUE', '{"field": "description", "operator": "IS_EMPTY"}'),
    (gen_random_uuid(), 'Priority is High', 'FIELD_VALUE', '{"field": "priority", "operator": "IN", "values": ["High", "Highest"]}')
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED DATA: Validator Templates
-- ============================================
INSERT INTO jira_workflow.validator_templates (id, template_name, validator_type, default_config) VALUES
    (gen_random_uuid(), 'Summary Required', 'FIELD_REQUIRED', '{"field": "summary", "required": true}'),
    (gen_random_uuid(), 'Description Required', 'FIELD_REQUIRED', '{"field": "description", "required": true}'),
    (gen_random_uuid(), 'Assignee Required', 'FIELD_REQUIRED', '{"field": "assignee", "required": true}'),
    (gen_random_uuid(), 'Priority Required', 'FIELD_REQUIRED', '{"field": "priority", "required": true}')
ON CONFLICT DO NOTHING;