CREATE TABLE IF NOT EXISTS jira_workflow.script_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    script_id       UUID NOT NULL REFERENCES jira_workflow.script_definitions(id) ON DELETE CASCADE,
    cron_expression VARCHAR(100) NOT NULL,
    is_enabled      BOOLEAN NOT NULL DEFAULT true,
    last_run_at     TIMESTAMP,
    next_run_at     TIMESTAMP,
    last_result     TEXT,
    last_success    BOOLEAN,
    run_count       INTEGER NOT NULL DEFAULT 0,
    created_by      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_script_schedules_next_run
    ON jira_workflow.script_schedules(next_run_at) WHERE is_enabled = true;
CREATE INDEX IF NOT EXISTS idx_script_schedules_script_id
    ON jira_workflow.script_schedules(script_id);
