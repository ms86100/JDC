-- Remove duplicate transitions (e.g. from re-applied seeds) — keep earliest row per logical transition

DELETE FROM jira_workflow.workflow_transitions t1
USING jira_workflow.workflow_transitions t2
WHERE t1.workflow_id = t2.workflow_id
  AND t1.name = t2.name
  AND t1.from_status_id = t2.from_status_id
  AND t1.to_status_id = t2.to_status_id
  AND t1.id > t2.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_workflow_transition_logical
    ON jira_workflow.workflow_transitions (workflow_id, name, from_status_id, to_status_id);
