-- V21__add_is_active_to_plan_releases.sql
-- Add is_active column to plan_releases for soft delete support

ALTER TABLE jira_plan.plan_releases
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Index for filtering active releases
CREATE INDEX IF NOT EXISTS idx_plan_releases_active ON jira_plan.plan_releases(is_active) WHERE is_active = TRUE;

COMMENT ON COLUMN jira_plan.plan_releases.is_active IS 'Soft delete flag - FALSE means deleted';