-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;


-- Runtime workflow execution: transition history + event outbox
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_transition_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    project_id UUID,
    workflow_id UUID NOT NULL,
    transition_id UUID NOT NULL,
    transition_name VARCHAR(200),
    from_status_id UUID NOT NULL,
    to_status_id UUID NOT NULL,
    user_id UUID,
    comment TEXT,
    screen_input JSONB,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wf_transition_history_issue ON jira_workflow.workflow_transition_history(issue_id);
CREATE INDEX IF NOT EXISTS idx_wf_transition_history_executed ON jira_workflow.workflow_transition_history(executed_at);

CREATE TABLE IF NOT EXISTS jira_workflow.workflow_event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wf_event_outbox_unpublished ON jira_workflow.workflow_event_outbox(published, created_at);

-- Project → workflow scheme association (runtime resolution)
CREATE TABLE IF NOT EXISTS jira_workflow.project_workflow_schemes (
    project_id UUID PRIMARY KEY,
    scheme_id UUID NOT NULL REFERENCES jira_workflow.workflow_schemes(id),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
