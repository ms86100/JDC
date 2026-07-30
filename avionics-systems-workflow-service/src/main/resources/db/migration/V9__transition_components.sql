-- V9__transition_components.sql
-- Creates tables for Workflow Conditions, Validators, and PostFunctions

-- ============================================
-- WORKFLOW CONDITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    condition_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    operator VARCHAR(20),
    value VARCHAR(500),
    condition_data TEXT,
    negate BOOLEAN NOT NULL DEFAULT FALSE,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_conditions_transition ON jira_workflow.workflow_conditions(transition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_conditions_type ON jira_workflow.workflow_conditions(condition_type);

COMMENT ON TABLE jira_workflow.workflow_conditions IS 'Conditions that must be met for a transition to be available';

-- ============================================
-- WORKFLOW VALIDATORS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_validators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    validator_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    validator_data TEXT,
    error_message TEXT,
    sequence INT NOT NULL DEFAULT 0,
    continue_on_error BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_validators_transition ON jira_workflow.workflow_validators(transition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_validators_type ON jira_workflow.workflow_validators(validator_type);

COMMENT ON TABLE jira_workflow.workflow_validators IS 'Validators that check if a transition can complete';

-- ============================================
-- WORKFLOW POST FUNCTIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_post_functions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    function_type VARCHAR(50) NOT NULL,
    function_data TEXT,
    sequence INT NOT NULL DEFAULT 0,
    async BOOLEAN NOT NULL DEFAULT FALSE,
    fail_on_error BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_post_functions_transition ON jira_workflow.workflow_post_functions(transition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_post_functions_type ON jira_workflow.workflow_post_functions(function_type);

COMMENT ON TABLE jira_workflow.workflow_post_functions IS 'Actions executed after a transition completes';