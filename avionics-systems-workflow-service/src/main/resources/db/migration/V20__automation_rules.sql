-- Automation Rule Engine tables (Automation for Jira DC 9.0+)
-- Rules fire on issue events independent of workflow transitions

CREATE TABLE IF NOT EXISTS jira_workflow.automation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id UUID,  -- null = global rule
    is_enabled BOOLEAN DEFAULT true,

    -- Trigger
    trigger_type VARCHAR(50) NOT NULL,
    -- ISSUE_CREATED, ISSUE_UPDATED, FIELD_CHANGED, STATUS_CHANGED, COMMENT_ADDED, SCHEDULED, MANUAL
    trigger_config JSONB DEFAULT '{}',
    -- e.g. {"fieldName": "end_date"} for FIELD_CHANGED, {"cron": "0 0 * * *"} for SCHEDULED

    -- Conditions (optional)
    conditions JSONB DEFAULT '[]',
    -- Array of: {"type": "FIELD_VALUE", "field": "status", "operator": "EQUALS", "value": "In Progress"}

    -- Actions
    actions JSONB NOT NULL DEFAULT '[]',
    -- Array of: {"type": "UPDATE_FIELD", "field": "end_date", "value": "{{trigger.newValue}}"}

    -- Branch (optional: iterate over linked/sub issues)
    branch_type VARCHAR(30),
    -- FOR_EACH_LINKED_ISSUE, FOR_EACH_SUBTASK, null
    branch_link_type VARCHAR(50),
    -- e.g. "blocks", "relates to"
    branch_actions JSONB DEFAULT '[]',

    -- Audit
    execution_count INTEGER DEFAULT 0,
    last_executed_at TIMESTAMP,
    last_error TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_workflow.automation_execution_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES jira_workflow.automation_rules(id) ON DELETE CASCADE,
    trigger_issue_id UUID,
    trigger_event VARCHAR(50),
    status VARCHAR(20) DEFAULT 'SUCCESS',  -- SUCCESS, FAILED, SKIPPED
    actions_executed INTEGER DEFAULT 0,
    error_message TEXT,
    execution_duration_ms INTEGER,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auto_rules_project ON jira_workflow.automation_rules(project_id);
CREATE INDEX IF NOT EXISTS idx_auto_rules_enabled ON jira_workflow.automation_rules(is_enabled);
CREATE INDEX IF NOT EXISTS idx_auto_rules_trigger ON jira_workflow.automation_rules(trigger_type);
CREATE INDEX IF NOT EXISTS idx_auto_exec_log_rule ON jira_workflow.automation_execution_log(rule_id);
CREATE INDEX IF NOT EXISTS idx_auto_exec_log_date ON jira_workflow.automation_execution_log(executed_at);
