-- V4__workflow_entity_updates.sql
-- Updates to workflow and workflow_transition tables for admin functionality

-- Add missing columns to workflows table
ALTER TABLE jira_workflow.workflows
ADD COLUMN IF NOT EXISTS is_system BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'CUSTOM';

-- Make project_id nullable
ALTER TABLE jira_workflow.workflows
ALTER COLUMN project_id DROP NOT NULL;

-- Add missing columns to workflow_transitions table
ALTER TABLE jira_workflow.workflow_transitions
ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'MANUAL',
ADD COLUMN IF NOT EXISTS conditions TEXT,
ADD COLUMN IF NOT EXISTS validators TEXT,
ADD COLUMN IF NOT EXISTS post_functions TEXT,
ADD COLUMN IF NOT EXISTS screen_id UUID;

-- Add index for transitions by type
CREATE INDEX IF NOT EXISTS idx_workflow_transitions_type
ON jira_workflow.workflow_transitions(type);

-- Add index for transitions with screen
CREATE INDEX IF NOT EXISTS idx_workflow_transitions_screen
ON jira_workflow.workflow_transitions(screen_id)
WHERE screen_id IS NOT NULL;

-- Add index for workflows by system flag
CREATE INDEX IF NOT EXISTS idx_workflows_system
ON jira_workflow.workflows(is_system);

-- Add index for workflows by type
CREATE INDEX IF NOT EXISTS idx_workflows_type
ON jira_workflow.workflows(type);

COMMENT ON COLUMN jira_workflow.workflows.is_system IS 'System workflows cannot be modified or deleted';
COMMENT ON COLUMN jira_workflow.workflows.type IS 'BUILD_IN or CUSTOM workflow type';
COMMENT ON COLUMN jira_workflow.workflow_transitions.type IS 'MANUAL, AUTO, or SCRIPT transition type';
COMMENT ON COLUMN jira_workflow.workflow_transitions.conditions IS 'JSON array of transition conditions';
COMMENT ON COLUMN jira_workflow.workflow_transitions.validators IS 'JSON array of transition validators';
COMMENT ON COLUMN jira_workflow.workflow_transitions.post_functions IS 'JSON array of post transition functions';
COMMENT ON COLUMN jira_workflow.workflow_transitions.screen_id IS 'Screen ID for transition screen';