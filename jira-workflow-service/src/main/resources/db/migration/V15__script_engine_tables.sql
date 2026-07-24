CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS jira_workflow.script_definitions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    script_type     VARCHAR(20) NOT NULL CHECK (script_type IN ('CONDITION', 'VALIDATOR', 'POST_FUNCTION')),
    script_key      VARCHAR(255) NOT NULL UNIQUE,
    script_body     TEXT NOT NULL,
    version         INTEGER NOT NULL DEFAULT 1,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_definitions_key ON jira_workflow.script_definitions(script_key);
CREATE INDEX IF NOT EXISTS idx_script_definitions_type ON jira_workflow.script_definitions(script_type);
CREATE INDEX IF NOT EXISTS idx_script_definitions_enabled ON jira_workflow.script_definitions(is_enabled);

CREATE TABLE IF NOT EXISTS jira_workflow.script_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID NOT NULL REFERENCES jira_workflow.script_definitions(id) ON DELETE CASCADE,
    version         INTEGER NOT NULL,
    script_body     TEXT NOT NULL,
    change_summary  TEXT,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (script_id, version)
);

CREATE INDEX IF NOT EXISTS idx_script_versions_script_id ON jira_workflow.script_versions(script_id);

CREATE TABLE IF NOT EXISTS jira_workflow.script_execution_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID REFERENCES jira_workflow.script_definitions(id) ON DELETE SET NULL,
    script_key      VARCHAR(255) NOT NULL,
    script_type     VARCHAR(20) NOT NULL,
    execution_mode  VARCHAR(20) NOT NULL DEFAULT 'WORKFLOW' CHECK (execution_mode IN ('WORKFLOW', 'CONSOLE')),
    issue_id        UUID,
    project_id      UUID,
    user_id         UUID,
    transition_id   UUID,
    success         BOOLEAN NOT NULL,
    result_value    TEXT,
    error_message   TEXT,
    execution_ms    BIGINT NOT NULL DEFAULT 0,
    context_summary TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_exec_log_script_id ON jira_workflow.script_execution_log(script_id);
CREATE INDEX IF NOT EXISTS idx_script_exec_log_created_at ON jira_workflow.script_execution_log(created_at);
CREATE INDEX IF NOT EXISTS idx_script_exec_log_issue_id ON jira_workflow.script_execution_log(issue_id);
