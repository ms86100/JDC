-- Update workflow_post_functions table with enhanced fields
-- Adds enabled, continue_on_error, and updated_at columns

-- Add new columns to workflow_post_functions
ALTER TABLE jira_workflow.workflow_post_functions
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE jira_workflow.workflow_post_functions
    ADD COLUMN IF NOT EXISTS continue_on_error BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE jira_workflow.workflow_post_functions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Create index for efficient lookup by transition
CREATE INDEX IF NOT EXISTS idx_wf_post_functions_transition
    ON jira_workflow.workflow_post_functions(transition_id, sequence);

-- Create index for enabled filtering
CREATE INDEX IF NOT EXISTS idx_wf_post_functions_enabled
    ON jira_workflow.workflow_post_functions(enabled) WHERE enabled = FALSE;