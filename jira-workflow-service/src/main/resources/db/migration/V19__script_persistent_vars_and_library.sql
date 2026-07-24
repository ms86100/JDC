CREATE TABLE IF NOT EXISTS jira_workflow.script_persistent_vars (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    var_key         VARCHAR(255) NOT NULL,
    var_value       TEXT,
    scope           VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' CHECK (scope IN ('GLOBAL', 'PROJECT', 'ISSUE')),
    scope_id        UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (var_key, scope, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_script_persistent_vars_key ON jira_workflow.script_persistent_vars(var_key);
CREATE INDEX IF NOT EXISTS idx_script_persistent_vars_scope ON jira_workflow.script_persistent_vars(scope, scope_id);
