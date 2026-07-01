-- V18__add_extra_indexes.sql
-- Add additional indexes for query performance optimization

-- Index for plan_warnings issue_id lookups
CREATE INDEX idx_plan_warnings_issue ON jira_plan.plan_warnings(issue_id);

-- Index for plan_items issue_id external lookups
CREATE INDEX idx_plan_items_issue_id ON jira_plan.plan_items(issue_id);

-- Index for sprint_audit_log sprint_id + event_type queries
-- (column name is event_type, not action_type)
CREATE INDEX idx_sprint_audit_sprint_action ON jira_plan.sprint_audit_log(sprint_id, event_type);

-- Index for board_permissions board_id lookups
-- (column name is board_id, not board_config_id)
CREATE INDEX IF NOT EXISTS idx_board_permissions_board_id ON jira_plan.board_permissions(board_id, permission_type);