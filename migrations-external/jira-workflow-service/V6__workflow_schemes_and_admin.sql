-- V3__workflow_schemes_and_admin.sql
-- Complete Workflow Scheme and Admin Tables

-- ============================================
-- WORKFLOW SCHEMES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT uk_workflow_schemes_name UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_workflow_schemes_name ON jira_workflow.workflow_schemes(name);
CREATE INDEX IF NOT EXISTS idx_workflow_schemes_default ON jira_workflow.workflow_schemes(is_default);
CREATE INDEX IF NOT EXISTS idx_workflow_schemes_created_by ON jira_workflow.workflow_schemes(created_by);

COMMENT ON TABLE jira_workflow.workflow_schemes IS 'Workflow schemes map issue types to workflows';
COMMENT ON COLUMN jira_workflow.workflow_schemes.is_default IS 'Only one scheme can be default per scheme type';

-- ============================================
-- WORKFLOW SCHEME MAPPINGS (Issue Type -> Workflow)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_scheme_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID NOT NULL REFERENCES jira_workflow.workflow_schemes(id) ON DELETE CASCADE,
    issue_type_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(scheme_id, issue_type_id)
);

CREATE INDEX IF NOT EXISTS idx_scheme_mappings_scheme ON jira_workflow.workflow_scheme_mappings(scheme_id);
CREATE INDEX IF NOT EXISTS idx_scheme_mappings_issue_type ON jira_workflow.workflow_scheme_mappings(issue_type_id);
CREATE INDEX IF NOT EXISTS idx_scheme_mappings_workflow ON jira_workflow.workflow_scheme_mappings(workflow_id);

COMMENT ON TABLE jira_workflow.workflow_scheme_mappings IS 'Maps issue types to specific workflows within a scheme';

-- ============================================
-- WORKFLOW VERSIONS (Version History) — extend V5 table if present
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    workflow_snapshot JSONB,
    statuses_snapshot JSONB,
    transitions_snapshot JSONB,
    conditions_snapshot JSONB,
    validators_snapshot JSONB,
    post_functions_snapshot JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    change_description TEXT,
    change_type VARCHAR(50),
    UNIQUE(workflow_id, version_number)
);

ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS workflow_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS statuses_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS transitions_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS conditions_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS validators_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS post_functions_snapshot JSONB;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS change_description TEXT;
ALTER TABLE jira_workflow.workflow_versions ADD COLUMN IF NOT EXISTS change_type VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_workflow_versions_workflow ON jira_workflow.workflow_versions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_versions_version ON jira_workflow.workflow_versions(workflow_id, version_number DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_versions_created ON jira_workflow.workflow_versions(created_at DESC);

COMMENT ON TABLE jira_workflow.workflow_versions IS 'Stores historical versions of workflow for rollback and audit';
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'jira_workflow' AND table_name = 'workflow_versions' AND column_name = 'change_type'
    ) THEN
        COMMENT ON COLUMN jira_workflow.workflow_versions.change_type IS 'CREATE, UPDATE, PUBLISH, etc.';
    END IF;
END $$;

-- ============================================
-- WORKFLOW SCREENS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    screen_type VARCHAR(50) NOT NULL,
    is_system BOOLEAN DEFAULT false,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_workflow_screens_name_type UNIQUE (name, screen_type)
);

CREATE INDEX IF NOT EXISTS idx_workflow_screens_name ON jira_workflow.workflow_screens(name);
CREATE INDEX IF NOT EXISTS idx_workflow_screens_type ON jira_workflow.workflow_screens(screen_type);
CREATE INDEX IF NOT EXISTS idx_workflow_screens_default ON jira_workflow.workflow_screens(is_default);

COMMENT ON TABLE jira_workflow.workflow_screens IS 'Screens for workflow transition forms';
COMMENT ON COLUMN jira_workflow.workflow_screens.screen_type IS 'INITIAL, DEFAULT, TRANSITION, MPS, MSP';

-- ============================================
-- WORKFLOW SCREEN TABS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_screen_tabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL REFERENCES jira_workflow.workflow_screens(id) ON DELETE CASCADE,
    tab_name VARCHAR(100) NOT NULL,
    description TEXT,
    order_index INT NOT NULL DEFAULT 0,
    is_system BOOLEAN DEFAULT false,
    UNIQUE(screen_id, order_index)
);

CREATE INDEX IF NOT EXISTS idx_screen_tabs_screen ON jira_workflow.workflow_screen_tabs(screen_id);
CREATE INDEX IF NOT EXISTS idx_screen_tabs_order ON jira_workflow.workflow_screen_tabs(screen_id, order_index);

-- ============================================
-- WORKFLOW SCREEN FIELDS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_screen_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tab_id UUID NOT NULL REFERENCES jira_workflow.workflow_screen_tabs(id) ON DELETE CASCADE,
    field_id VARCHAR(100) NOT NULL,
    field_label VARCHAR(200),
    field_type VARCHAR(50),
    required BOOLEAN DEFAULT false,
    hidden BOOLEAN DEFAULT false,
    readonly BOOLEAN DEFAULT false,
    renderertype VARCHAR(50),
    order_index INT NOT NULL DEFAULT 0,
    config JSONB
);

CREATE INDEX IF NOT EXISTS idx_screen_fields_tab ON jira_workflow.workflow_screen_fields(tab_id);
CREATE INDEX IF NOT EXISTS idx_screen_fields_field_id ON jira_workflow.workflow_screen_fields(field_id);
CREATE INDEX IF NOT EXISTS idx_screen_fields_order ON jira_workflow.workflow_screen_fields(tab_id, order_index);

COMMENT ON TABLE jira_workflow.workflow_screen_fields IS 'Configures which fields appear on transition screens';
COMMENT ON COLUMN jira_workflow.workflow_screen_fields.config IS 'Field-specific configuration like default values, validators';

-- ============================================
-- TRANSITION SCREENS (Associates screens to transitions)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_transition_screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    screen_id UUID NOT NULL REFERENCES jira_workflow.workflow_screens(id) ON DELETE RESTRICT,
    screen_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(transition_id, screen_type)
);

CREATE INDEX IF NOT EXISTS idx_transition_screens_transition ON jira_workflow.workflow_transition_screens(transition_id);
CREATE INDEX IF NOT EXISTS idx_transition_screens_screen ON jira_workflow.workflow_transition_screens(screen_id);

COMMENT ON TABLE jira_workflow.workflow_transition_screens IS 'Links transition screens to workflow transitions';

-- ============================================
-- WORKFLOW AUDIT LOG
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID,
    scheme_id UUID,
    transition_id UUID,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    entity_name VARCHAR(200),
    user_id UUID,
    username VARCHAR(100),
    details JSONB,
    previous_state JSONB,
    new_state JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- V5 may have already created workflow_audit_log with an entity-based schema that
-- lacks these columns; CREATE TABLE IF NOT EXISTS above is then a no-op. Add the
-- columns idempotently so the indexes below succeed on both fresh and upgraded DBs.
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS workflow_id UUID;
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS scheme_id UUID;
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS transition_id UUID;
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS entity_name VARCHAR(200);
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS username VARCHAR(100);
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS details JSONB;
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS previous_state JSONB;
ALTER TABLE jira_workflow.workflow_audit_log ADD COLUMN IF NOT EXISTS new_state JSONB;

CREATE INDEX IF NOT EXISTS idx_workflow_audit_workflow ON jira_workflow.workflow_audit_log(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_scheme ON jira_workflow.workflow_audit_log(scheme_id);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_user ON jira_workflow.workflow_audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_action ON jira_workflow.workflow_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_created ON jira_workflow.workflow_audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_audit_entity ON jira_workflow.workflow_audit_log(entity_type, entity_id);

COMMENT ON TABLE jira_workflow.workflow_audit_log IS 'Comprehensive audit trail for all workflow operations';

-- ============================================
-- WORKFLOW EXECUTION LOG
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_execution_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    transition_id UUID,
    from_status_id UUID,
    to_status_id UUID,
    executed_by UUID NOT NULL,
    execution_status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    execution_time_ms BIGINT,
    error_message TEXT,
    conditions_passed BOOLEAN,
    validators_passed BOOLEAN,
    post_functions_executed INT DEFAULT 0,
    post_functions_failed INT DEFAULT 0,
    snapshot_before JSONB,
    snapshot_after JSONB,
    details JSONB
);

CREATE INDEX IF NOT EXISTS idx_workflow_exec_issue ON jira_workflow.workflow_execution_log(issue_id);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_workflow ON jira_workflow.workflow_execution_log(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_executed_by ON jira_workflow.workflow_execution_log(executed_by);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_status ON jira_workflow.workflow_execution_log(execution_status);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_started ON jira_workflow.workflow_execution_log(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_exec_issue_workflow ON jira_workflow.workflow_execution_log(issue_id, started_at DESC);

COMMENT ON TABLE jira_workflow.workflow_execution_log IS 'Runtime execution log for issue transitions';

-- ============================================
-- WORKFLOW POST FUNCTION DEFINITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_post_function_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    function_key VARCHAR(100) NOT NULL UNIQUE,
    function_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    config_schema JSONB,
    is_system BOOLEAN DEFAULT false,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO jira_workflow.workflow_post_function_definitions (function_key, function_name, description, category, is_system, config_schema) VALUES
('ASSIGN_TO_CURRENT_USER', 'Assign to Current User', 'Assigns the issue to the user performing the transition', 'assignment', true, '{"type": "object"}'),
('ASSIGN_TO_REPORTER', 'Assign to Reporter', 'Assigns the issue to the original reporter', 'assignment', true, '{"type": "object"}'),
('ASSIGN_TO_PROJECT_LEAD', 'Assign to Project Lead', 'Assigns the issue to the project lead', 'assignment', true, '{"type": "object"}'),
('SET_FIELD_VALUE', 'Set Field Value', 'Sets a specific field to a given value', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "value": {"type": "string"}}}'),
('COPY_VALUE_FROM_FIELD', 'Copy Value from Field', 'Copies value from one field to another', 'field', true, '{"type": "object", "properties": {"sourceField": {"type": "string"}, "targetField": {"type": "string"}}}'),
('SET_RESOLUTION', 'Set Resolution', 'Sets the resolution field', 'resolution', true, '{"type": "object", "properties": {"resolution": {"type": "string"}}}'),
('CLEAR_RESOLUTION', 'Clear Resolution', 'Clears the resolution field', 'resolution', true, '{"type": "object"}'),
('SET_ISSUE_STATUS', 'Set Issue Status', 'Sets the issue to a specific status', 'status', true, '{"type": "object", "properties": {"statusId": {"type": "string"}}}'),
('CREATE_SUBTASK', 'Create Sub-task', 'Creates a subtask of the current issue', 'subtask', true, '{"type": "object", "properties": {"issueTypeId": {"type": "string"}, "summary": {"type": "string"}}}'),
('ADD_COMMENT', 'Add Comment', 'Adds a comment to the issue', 'comment', true, '{"type": "object", "properties": {"comment": {"type": "string"}, "visibility": {"type": "string"}}}'),
('SEND_EMAIL', 'Send Email', 'Sends an email notification', 'notification', true, '{"type": "object", "properties": {"recipients": {"type": "string"}, "template": {"type": "string"}, "subject": {"type": "string"}}}'),
('FIRE_EVENT', 'Fire Event', 'Fires a custom event for automation', 'automation', true, '{"type": "object", "properties": {"eventKey": {"type": "string"}}}'),
('REINDEX_ISSUE', 'Reindex Issue', 'Triggers reindexing of the issue', 'system', true, '{"type": "object"}'),
('STORE_ISSUE', 'Store Issue', 'Stores the issue in the database', 'system', true, '{"type": "object"}'),
('GENERATE_CHANGE_HISTORY', 'Generate Change History', 'Records field changes in history', 'history', true, '{"type": "object"}'),
('LINK_ISSUE', 'Link Issue', 'Creates a link to another issue', 'linking', true, '{"type": "object", "properties": {"linkType": {"type": "string"}, "targetIssueId": {"type": "string"}}}'),
('AUTO_TRANSITION', 'Auto Transition', 'Automatically performs another transition', 'workflow', true, '{"type": "object", "properties": {"targetStatusId": {"type": "string"}}}');

-- ============================================
-- CONDITION DEFINITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_condition_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condition_key VARCHAR(100) NOT NULL UNIQUE,
    condition_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    config_schema JSONB,
    is_system BOOLEAN DEFAULT false,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO jira_workflow.workflow_condition_definitions (condition_key, condition_name, description, category, is_system, config_schema) VALUES
('PERMISSION', 'User has Permission', 'Checks if the user has a specific permission', 'permission', true, '{"type": "object", "properties": {"permission": {"type": "string"}}}'),
('USER_IN_GROUP', 'User in Group', 'Checks if the user is a member of a specific group', 'user', true, '{"type": "object", "properties": {"group": {"type": "string"}}}'),
('USER_IS_REPORTER', 'User is Reporter', 'Checks if the current user is the issue reporter', 'user', true, '{"type": "object"}'),
('USER_IS_ASSIGNEE', 'User is Assignee', 'Checks if the current user is the issue assignee', 'user', true, '{"type": "object"}'),
('FIELD_VALUE', 'Field Value Condition', 'Checks if a field has a specific value', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "operator": {"type": "string"}, "value": {"type": "string"}}}'),
('FIELD_CHANGED', 'Field Value Changed', 'Checks if a field value has changed', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "fromValue": {"type": "string"}, "toValue": {"type": "string"}}}'),
('PREVIOUS_STATUS', 'Previous Status', 'Checks if the issue was in a specific status before', 'status', true, '{"type": "object", "properties": {"statusId": {"type": "string"}}}'),
('ISSUE_TYPE', 'Issue Type Condition', 'Checks the issue type', 'issue', true, '{"type": "object", "properties": {"issueTypeId": {"type": "string"}}}'),
('PROJECT', 'Project Condition', 'Checks the project', 'project', true, '{"type": "object", "properties": {"projectId": {"type": "string"}}}'),
('SPRINT_STATUS', 'Sprint Status', 'Checks if sprint is in a specific state', 'sprint', true, '{"type": "object", "properties": {"sprintStatus": {"type": "string"}}}'),
('SUBTASK_STATUS', 'Sub-task Status', 'Checks status of sub-tasks', 'subtask', true, '{"type": "object", "properties": {"statusId": {"type": "string"}, "comparison": {"type": "string"}}}'),
('LINKED_ISSUE_STATUS', 'Linked Issue Status', 'Checks status of linked issues', 'link', true, '{"type": "object", "properties": {"linkType": {"type": "string"}, "statusId": {"type": "string"}}}'),
('AND', 'AND Condition', 'All child conditions must be true', 'logical', true, '{"type": "array", "items": {"type": "object"}}'),
('OR', 'OR Condition', 'At least one child condition must be true', 'logical', true, '{"type": "array", "items": {"type": "object"}}'),
('NOT', 'NOT Condition', 'The child condition must be false', 'logical', true, '{"type": "object"}'),
('SCRIPT', 'Script Condition', 'Custom script evaluation', 'advanced', true, '{"type": "object", "properties": {"script": {"type": "string"}}}');

-- ============================================
-- VALIDATOR DEFINITIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_validator_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    validator_key VARCHAR(100) NOT NULL UNIQUE,
    validator_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    config_schema JSONB,
    error_message_template VARCHAR(500),
    is_system BOOLEAN DEFAULT false,
    is_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO jira_workflow.workflow_validator_definitions (validator_key, validator_name, description, category, is_system, config_schema, error_message_template) VALUES
('FIELD_REQUIRED', 'Required Field', 'Validates that required fields have values', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "fieldName": {"type": "string"}}}', 'Field {fieldName} is required'),
('FIELD_VALUE', 'Field Value Validator', 'Validates field has specific value', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "operator": {"type": "string"}, "value": {"type": "string"}}}', 'Field validation failed'),
('REGEX', 'Regular Expression', 'Validates field matches regex pattern', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "pattern": {"type": "string"}}}', 'Field does not match required format'),
('DATE_RANGE', 'Date Range', 'Validates date is within range', 'field', true, '{"type": "object", "properties": {"fieldId": {"type": "string"}, "minDate": {"type": "string"}, "maxDate": {"type": "string"}}}', 'Date must be between {minDate} and {maxDate}'),
('COMMENT_REQUIRED', 'Comment Required', 'Validates comment is provided', 'comment', true, '{"type": "object", "properties": {"minLength": {"type": "number"}}}', 'A comment is required'),
('ATTACHMENT_COUNT', 'Attachment Count', 'Validates number of attachments', 'attachment', true, '{"type": "object", "properties": {"minCount": {"type": "number"}, "maxCount": {"type": "number"}}}', 'Attachment count requirement not met'),
('SUBTASK_RESOLUTION', 'Sub-task Resolution', 'Validates sub-tasks are resolved', 'subtask', true, '{"type": "object", "properties": {"requireAllResolved": {"type": "boolean"}}}', 'All sub-tasks must be resolved'),
('LINKED_ISSUE_RESOLUTION', 'Linked Issue Resolution', 'Validates linked issues are resolved', 'link', true, '{"type": "object", "properties": {"linkType": {"type": "string"}, "requireResolved": {"type": "boolean"}}}', 'Linked issues must be resolved'),
('USER_PERMISSION', 'User Permission', 'Validates user has permission', 'permission', true, '{"type": "object", "properties": {"permission": {"type": "string"}}}', 'You do not have permission to perform this transition'),
('SCRIPT', 'Script Validation', 'Custom script validation', 'advanced', true, '{"type": "object", "properties": {"script": {"type": "string"}}}', 'Validation failed: {error}');

-- ============================================
-- SEED DATA: Default Workflow Screens
-- ============================================
INSERT INTO jira_workflow.workflow_screens (name, description, screen_type, is_system, is_default) VALUES
('Default Transition Screen', 'Default screen shown during transition', 'TRANSITION', true, true),
('Resolution Screen', 'Screen for setting resolution during transitions', 'TRANSITION', true, false),
('Assignee Screen', 'Screen for changing assignee', 'TRANSITION', true, false);

-- ============================================
-- FUNCTION: Get workflow for issue type
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.get_workflow_for_issue_type(
    p_scheme_id UUID,
    p_issue_type_id UUID
) RETURNS UUID AS $$
DECLARE
    v_workflow_id UUID;
BEGIN
    -- First try specific mapping
    SELECT workflow_id INTO v_workflow_id
    FROM jira_workflow.workflow_scheme_mappings
    WHERE scheme_id = p_scheme_id AND issue_type_id = p_issue_type_id;

    -- If no specific mapping, try default workflow
    IF v_workflow_id IS NULL THEN
        SELECT default_workflow_id INTO v_workflow_id
        FROM jira_workflow.workflow_schemes
        WHERE id = p_scheme_id;
    END IF;

    RETURN v_workflow_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Validate workflow has no orphan issues
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.can_delete_workflow(p_workflow_id UUID)
RETURNS TABLE(can_delete BOOLEAN, reason TEXT) AS $$
DECLARE
    v_usage_count BIGINT;
    v_project_count BIGINT;
BEGIN
    -- Check if workflow is used by any project
    SELECT COUNT(*) INTO v_project_count
    FROM jira_workflow.workflow_scheme_mappings wsm
    JOIN jira_workflow.workflow_schemes ws ON ws.id = wsm.scheme_id
    WHERE wsm.workflow_id = p_workflow_id;

    IF v_project_count > 0 THEN
        RETURN QUERY SELECT false, 'Workflow is used by ' || v_project_count || ' project(s)';
        RETURN;
    END IF;

    -- Check if this is the system workflow
    IF EXISTS (SELECT 1 FROM jira_workflow.workflows WHERE id = p_workflow_id AND is_system = true) THEN
        RETURN QUERY SELECT false, 'System workflows cannot be deleted';
        RETURN;
    END IF;

    RETURN QUERY SELECT true, 'Workflow can be deleted';
END;
$$ LANGUAGE plpgsql;