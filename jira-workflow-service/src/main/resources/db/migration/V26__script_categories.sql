-- Add category column to script_definitions for folder organization
ALTER TABLE jira_workflow.script_definitions
    ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'Uncategorized';

-- Add index for category queries
CREATE INDEX IF NOT EXISTS idx_script_definitions_category
    ON jira_workflow.script_definitions(category);

-- Widen script_execution_log execution_mode CHECK to include CALCULATED_FIELD
ALTER TABLE jira_workflow.script_execution_log
    DROP CONSTRAINT IF EXISTS script_execution_log_execution_mode_check;

ALTER TABLE jira_workflow.script_execution_log
    ADD CONSTRAINT script_execution_log_execution_mode_check
    CHECK (execution_mode IN ('WORKFLOW', 'CONSOLE', 'SCHEDULED', 'LISTENER', 'FIELD_BEHAVIOR', 'CALCULATED_FIELD', 'API'));

-- Add execution audit columns
ALTER TABLE jira_workflow.script_execution_log
    ADD COLUMN IF NOT EXISTS executed_by UUID,
    ADD COLUMN IF NOT EXISTS target_issue_id UUID,
    ADD COLUMN IF NOT EXISTS api_call_count INTEGER DEFAULT 0;
