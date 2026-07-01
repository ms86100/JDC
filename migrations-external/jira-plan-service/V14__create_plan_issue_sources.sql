
-- V14__create_plan_issue_sources.sql
-- Creates the plan_issue_sources table for tracking issue source configurations

CREATE TABLE jira_plan.plan_issue_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL,  -- 'BOARD', 'PROJECT', 'FILTER'
    source_id UUID NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP,
    issue_count INTEGER DEFAULT 0,
    sync_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint to prevent duplicate sources
CREATE UNIQUE INDEX idx_plan_source_unique
ON jira_plan.plan_issue_sources(plan_id, source_type, source_id)
WHERE is_active = TRUE;

-- Indexes for querying
CREATE INDEX idx_plan_issue_sources_plan_id ON jira_plan.plan_issue_sources(plan_id);
CREATE INDEX idx_plan_issue_sources_source_type ON jira_plan.plan_issue_sources(source_type);
CREATE INDEX idx_plan_issue_sources_last_sync ON jira_plan.plan_issue_sources(last_sync_at);

-- Add source tracking columns to plan_items (if not already present)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_plan' AND table_name = 'plan_items' AND column_name = 'source_type') THEN
        ALTER TABLE jira_plan.plan_items ADD COLUMN source_type VARCHAR(20);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_plan' AND table_name = 'plan_items' AND column_name = 'source_id') THEN
        ALTER TABLE jira_plan.plan_items ADD COLUMN source_id UUID;
    END IF;
END $$;

-- Comments
COMMENT ON TABLE jira_plan.plan_issue_sources IS 'Stores issue sources (boards, projects, filters) linked to plans';
COMMENT ON COLUMN jira_plan.plan_issue_sources.source_type IS 'Type of issue source: BOARD, PROJECT, or FILTER';
COMMENT ON COLUMN jira_plan.plan_issue_sources.source_id IS 'ID of the external source (board, project, or filter ID)';
COMMENT ON COLUMN jira_plan.plan_issue_sources.source_name IS 'Display name of the source for UI purposes';
COMMENT ON COLUMN jira_plan.plan_issue_sources.last_sync_at IS 'Timestamp of last successful sync with the source';
COMMENT ON COLUMN jira_plan.plan_issue_sources.issue_count IS 'Number of issues synced from this source';
COMMENT ON COLUMN jira_plan.plan_issue_sources.sync_error IS 'Error message from last failed sync attempt';