-- V17__add_missing_indexes.sql
-- Add missing indexes for query performance optimization

-- Index for non_working_days date queries (WorkingDaysService.isHoliday)
CREATE INDEX idx_non_working_days_date ON jira_plan.non_working_days(date);

-- Index for plan_items plan_id + sort_order (BacklogService.getBacklog)
CREATE INDEX idx_plan_items_plan_sort ON jira_plan.plan_items(plan_id, sort_order);

-- Index for plan_items parent_id for hierarchy queries
CREATE INDEX idx_plan_items_parent ON jira_plan.plan_items(parent_id);

-- Index for issue_dependencies blocking/blocked issue lookups
CREATE INDEX idx_issue_deps_blocking ON jira_plan.issue_dependencies(blocking_issue_id);
CREATE INDEX idx_issue_deps_blocked ON jira_plan.issue_dependencies(blocked_issue_id);

-- Index for sprint_issues sprint_id lookups
CREATE INDEX idx_sprint_issues_sprint ON jira_plan.sprint_issues(sprint_id);

-- Index for plan_teams plan_id lookups
CREATE INDEX idx_plan_teams_plan ON jira_plan.plan_teams(plan_id);

-- Index for plan_team_members team_id lookups
CREATE INDEX idx_plan_team_members_team ON jira_plan.plan_team_members(team_id);

-- Index for plan_releases plan_id lookups
CREATE INDEX idx_plan_releases_plan ON jira_plan.plan_releases(plan_id);

-- Index for board_columns board_id for ordering
CREATE INDEX idx_board_columns_board_order ON jira_plan.board_columns(board_id, sequence);

-- Index for plan_warnings plan_id + type lookups
CREATE INDEX idx_plan_warnings_plan_type ON jira_plan.plan_warnings(plan_id, warning_type);