-- V22__add_is_active_to_plan_items.sql
-- Add is_active column to plan_items for soft delete support

ALTER TABLE jira_plan.plan_items
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_plan_items_active ON jira_plan.plan_items(is_active) WHERE is_active = FALSE;

COMMENT ON COLUMN jira_plan.plan_items.is_active IS 'Soft delete flag - FALSE means removed from backlog';