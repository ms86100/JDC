-- V18__add_extra_indexes.sql
-- Add additional indexes for query performance optimization

-- Index for plan_warnings issue_id lookups
CREATE INDEX idx_plan_warnings_issue ON jira_plan.plan_warnings(issue_id);

-- Index for plan_items issue_id external lookups
CREATE INDEX idx_plan_items_issue_id ON jira_plan.plan_items(issue_id);

-- Index for sprint_audit_log sprint_id + action queries
CREATE INDEX idx_sprint_audit_sprint_action ON jira_plan.sprint_audit_log(sprint_id, action_type);

-- Index for board_permissions board_id lookups
CREATE INDEX idx_board_permissions_board ON jira_plan.board_permissions(board_config_id, permission_type);