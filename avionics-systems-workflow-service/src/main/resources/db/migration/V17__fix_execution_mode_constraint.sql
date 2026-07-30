ALTER TABLE jira_workflow.script_execution_log
    DROP CONSTRAINT IF EXISTS script_execution_log_execution_mode_check;

ALTER TABLE jira_workflow.script_execution_log
    ADD CONSTRAINT script_execution_log_execution_mode_check
    CHECK (execution_mode IN ('WORKFLOW', 'CONSOLE', 'SCHEDULED', 'LISTENER', 'FIELD_BEHAVIOR', 'API'));
