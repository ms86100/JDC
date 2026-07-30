CREATE TABLE IF NOT EXISTS jira_workflow.script_listeners (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID NOT NULL REFERENCES jira_workflow.script_definitions(id) ON DELETE CASCADE,
    event_type      VARCHAR(50) NOT NULL,
    project_filter  UUID,
    issue_type_filter UUID,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    execution_order INTEGER NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_listeners_event ON jira_workflow.script_listeners(event_type) WHERE is_enabled = true;
CREATE INDEX IF NOT EXISTS idx_script_listeners_script ON jira_workflow.script_listeners(script_id);

CREATE TABLE IF NOT EXISTS jira_workflow.script_field_behaviors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID NOT NULL REFERENCES jira_workflow.script_definitions(id) ON DELETE CASCADE,
    screen_context  VARCHAR(20) NOT NULL CHECK (screen_context IN ('CREATE', 'EDIT', 'TRANSITION', 'VIEW')),
    project_id      UUID,
    issue_type_id   UUID,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    execution_order INTEGER NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_field_behaviors_context ON jira_workflow.script_field_behaviors(screen_context) WHERE is_enabled = true;
CREATE INDEX IF NOT EXISTS idx_script_field_behaviors_script ON jira_workflow.script_field_behaviors(script_id);

CREATE TABLE IF NOT EXISTS jira_workflow.script_calculated_fields (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID NOT NULL REFERENCES jira_workflow.script_definitions(id) ON DELETE CASCADE,
    custom_field_id UUID NOT NULL,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    cache_ttl_ms    BIGINT NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_calc_fields_field ON jira_workflow.script_calculated_fields(custom_field_id) WHERE is_enabled = true;

ALTER TABLE jira_workflow.script_definitions
    DROP CONSTRAINT IF EXISTS script_definitions_script_type_check;
ALTER TABLE jira_workflow.script_definitions
    ADD CONSTRAINT script_definitions_script_type_check
    CHECK (script_type IN ('CONDITION', 'VALIDATOR', 'POST_FUNCTION', 'LISTENER', 'FIELD_BEHAVIOR', 'CALCULATED_FIELD', 'CONSOLE', 'SCHEDULED', 'LIBRARY'));
