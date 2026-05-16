-- V23__add_optimistic_locking_version.sql
-- Add version columns for optimistic locking to Plan, BoardConfig, Sprint, and PlanItem tables

ALTER TABLE jira_plan.plans ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE jira_plan.board_configs ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE jira_plan.sprints ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE jira_plan.plan_items ADD COLUMN version BIGINT DEFAULT 0;

-- Add comments
COMMENT ON COLUMN jira_plan.plans.version IS 'Version for optimistic locking';
COMMENT ON COLUMN jira_plan.board_configs.version IS 'Version for optimistic locking';
COMMENT ON COLUMN jira_plan.sprints.version IS 'Version for optimistic locking';
COMMENT ON COLUMN jira_plan.plan_items.version IS 'Version for optimistic locking';