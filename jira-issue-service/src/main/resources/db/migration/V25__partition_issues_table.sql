-- Partition strategy documented for future use when data exceeds 10M rows
-- Actual partitioning deferred — requires composite PK (id, project_id)
COMMENT ON TABLE jira_issue.issues IS 'Partition-ready: LIST partition by project_id when data exceeds 10M rows';
