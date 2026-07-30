-- V8__workflow_service_gaps.sql
-- Addresses critical Jira DC workflow parity gaps

-- ============================================
-- WORKFLOW DRAFTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    draft_data JSONB NOT NULL,
    parent_version INTEGER,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_draft_of_published BOOLEAN DEFAULT FALSE,
    draft_status VARCHAR(20) DEFAULT 'ACTIVE',
    -- Only one active draft per workflow
    UNIQUE(workflow_id, draft_status)
);

CREATE INDEX IF NOT EXISTS idx_workflow_drafts_workflow ON jira_workflow.workflow_drafts(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_drafts_status ON jira_workflow.workflow_drafts(draft_status);
CREATE INDEX IF NOT EXISTS idx_workflow_drafts_created_by ON jira_workflow.workflow_drafts(created_by);

COMMENT ON TABLE jira_workflow.workflow_drafts IS 'Draft copies of workflows for safe editing before publishing';

-- ============================================
-- WORKFLOW LAYOUT (Designer positions)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_layouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    layout_data JSONB NOT NULL,
    layout_version INTEGER DEFAULT 1,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_by UUID,
    locked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_layouts_workflow ON jira_workflow.workflow_layouts(workflow_id);

COMMENT ON TABLE jira_workflow.workflow_layouts IS 'Stores visual layout positions for workflow diagram designer';

-- ============================================
-- WORKFLOW LAYOUT NODES (Status positions)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_layout_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_id UUID NOT NULL REFERENCES jira_workflow.workflow_layouts(id) ON DELETE CASCADE,
    status_id UUID NOT NULL,
    node_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    position_x DOUBLE PRECISION NOT NULL,
    position_y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION DEFAULT 120,
    height DOUBLE PRECISION DEFAULT 60,
    color VARCHAR(20),
    is_expanded BOOLEAN DEFAULT TRUE,
    label VARCHAR(100),
    sort_order INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_layout_nodes_layout ON jira_workflow.workflow_layout_nodes(layout_id);
CREATE INDEX IF NOT EXISTS idx_layout_nodes_status ON jira_workflow.workflow_layout_nodes(status_id);

COMMENT ON TABLE jira_workflow.workflow_layout_nodes IS 'Individual node positions in workflow diagram';

-- ============================================
-- WORKFLOW LAYOUT EDGES (Transition paths)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_layout_edges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    layout_id UUID NOT NULL REFERENCES jira_workflow.workflow_layouts(id) ON DELETE CASCADE,
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    from_node_id UUID REFERENCES jira_workflow.workflow_layout_nodes(id),
    to_node_id UUID REFERENCES jira_workflow.workflow_layout_nodes(id),
    edge_type VARCHAR(20) DEFAULT 'CURVED',
    path_points JSONB,
    label_offset_x DOUBLE PRECISION DEFAULT 0,
    label_offset_y DOUBLE PRECISION DEFAULT -15,
    is_looped BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_layout_edges_layout ON jira_workflow.workflow_layout_edges(layout_id);
CREATE INDEX IF NOT EXISTS idx_layout_edges_transition ON jira_workflow.workflow_layout_edges(transition_id);

COMMENT ON TABLE jira_workflow.workflow_layout_edges IS 'Edge routing paths for workflow diagram transitions';

-- ============================================
-- WORKFLOW MIGRATION (Status migration tracking)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_migrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    workflow_version_id UUID,
    old_status_id UUID NOT NULL,
    new_status_id UUID NOT NULL,
    migration_type VARCHAR(20) NOT NULL,
    issue_count INTEGER DEFAULT 0,
    migrated_count INTEGER DEFAULT 0,
    migration_status VARCHAR(20) DEFAULT 'PENDING',
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_workflow_migrations_workflow ON jira_workflow.workflow_migrations(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_migrations_status ON jira_workflow.workflow_migrations(old_status_id, new_status_id);
CREATE INDEX IF NOT EXISTS idx_workflow_migrations_status_mig ON jira_workflow.workflow_migrations(migration_status);

COMMENT ON TABLE jira_workflow.workflow_migrations IS 'Tracks status migrations when workflows change';

-- ============================================
-- WORKFLOW MIGRATION ISSUE STATUS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_migration_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    migration_id UUID NOT NULL REFERENCES jira_workflow.workflow_migrations(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    old_status_id UUID NOT NULL,
    new_status_id UUID NOT NULL,
    migration_status VARCHAR(20) DEFAULT 'PENDING',
    processed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_migration_issues_migration ON jira_workflow.workflow_migration_issues(migration_id);
CREATE INDEX IF NOT EXISTS idx_migration_issues_issue ON jira_workflow.workflow_migration_issues(issue_id);
CREATE INDEX IF NOT EXISTS idx_migration_issues_status ON jira_workflow.workflow_migration_issues(migration_status);

-- ============================================
-- WORKFLOW SHARING (Project associations)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_sharing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    project_id UUID NOT NULL,
    scheme_id UUID,
    shared_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(workflow_id, project_id)
);

CREATE INDEX IF NOT EXISTS idx_workflow_sharing_workflow ON jira_workflow.workflow_sharing(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_sharing_project ON jira_workflow.workflow_sharing(project_id);

COMMENT ON TABLE jira_workflow.workflow_sharing IS 'Tracks which projects use shared workflows';

-- ============================================
-- WORKFLOW STATUS CATEGORIES (Reference table)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_status_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_key VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    sequence INTEGER DEFAULT 0,
    is_system BOOLEAN DEFAULT TRUE
);

-- Seed standard categories
INSERT INTO jira_workflow.workflow_status_categories (id, category_key, category_name, color, sequence, is_system) VALUES
    ('00000000-0000-0000-0001-000000000001', 'TODO', 'To Do', '#0052CC', 1, true),
    ('00000000-0000-0000-0001-000000000002', 'IN_PROGRESS', 'In Progress', '#FF991F', 2, true),
    ('00000000-0000-0000-0001-000000000003', 'DONE', 'Done', '#00875A', 3, true)
ON CONFLICT DO NOTHING;

-- ============================================
-- WORKFLOW TRANSITION TRIGGERS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_transition_triggers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config JSONB,
    is_enabled BOOLEAN DEFAULT TRUE,
    execution_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transition_triggers_transition ON jira_workflow.workflow_transition_triggers(transition_id);
CREATE INDEX IF NOT EXISTS idx_transition_triggers_enabled ON jira_workflow.workflow_transition_triggers(is_enabled);

COMMENT ON TABLE jira_workflow.workflow_transition_triggers IS 'Automation triggers for workflow transitions';

-- ============================================
-- WORKFLOW TRANSITION PROPERTIES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_transition_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID NOT NULL REFERENCES jira_workflow.workflow_transitions(id) ON DELETE CASCADE,
    property_key VARCHAR(100) NOT NULL,
    property_value TEXT,
    property_type VARCHAR(50) DEFAULT 'STRING',
    is_system BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(transition_id, property_key)
);

CREATE INDEX IF NOT EXISTS idx_transition_properties_transition ON jira_workflow.workflow_transition_properties(transition_id);

COMMENT ON TABLE jira_workflow.workflow_transition_properties IS 'Custom properties on workflow transitions';

-- ============================================
-- WORKFLOW PERMISSIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_workflow.workflow_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES jira_workflow.workflows(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    permission_target_type VARCHAR(20),
    permission_target_id UUID,
    permission_value VARCHAR(20) DEFAULT 'GRANT',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_permissions_workflow ON jira_workflow.workflow_permissions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_permissions_target ON jira_workflow.workflow_permissions(permission_target_type, permission_target_id);

-- ============================================
-- ENHANCE EXISTING TABLES
-- ============================================

-- Add missing columns to workflows table
ALTER TABLE jira_workflow.workflows
    ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS locked_by UUID,
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS default_workflow_id UUID,
    ADD COLUMN IF NOT EXISTS original_workflow_id UUID;

COMMENT ON COLUMN jira_workflow.workflows.is_locked IS 'Lock workflow during editing to prevent conflicts';
COMMENT ON COLUMN jira_workflow.workflows.default_workflow_id IS 'Fallback workflow ID when no issue type mapping exists';

-- Add missing columns to workflow_transitions
ALTER TABLE jira_workflow.workflow_transitions
    ADD COLUMN IF NOT EXISTS trigger_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS trigger_config JSONB,
    ADD COLUMN IF NOT EXISTS origin VARCHAR(50) DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS condition_conditions JSONB,
    ADD COLUMN IF NOT EXISTS condition_operator VARCHAR(10) DEFAULT 'AND',
    ADD COLUMN IF NOT EXISTS validator_validators JSONB,
    ADD COLUMN IF NOT EXISTS post_function_functions JSONB;

COMMENT ON COLUMN jira_workflow.workflow_transitions.trigger_type IS 'MANUAL, AUTOMATIC, SCHEDULED, WEBHOOK';
COMMENT ON COLUMN jira_workflow.workflow_transitions.origin IS 'USER, RULE, SCRIPTS, IMPORT';

-- Add missing columns to workflow_schemes
ALTER TABLE jira_workflow.workflow_schemes
    ADD COLUMN IF NOT EXISTS default_workflow_id UUID,
    ADD COLUMN IF NOT EXISTS is_draft BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS draft_of_scheme_id UUID,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN jira_workflow.workflow_schemes.is_draft IS 'Draft scheme for editing before publish';

-- ============================================
-- FUNCTION: Get next version number
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.get_next_workflow_version(p_workflow_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_max_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) INTO v_max_version
    FROM jira_workflow.workflow_versions
    WHERE workflow_id = p_workflow_id;

    RETURN v_max_version + 1;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Check if workflow can be edited
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.can_edit_workflow(p_workflow_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
    v_is_locked BOOLEAN;
    v_locked_by UUID;
    v_is_draft BOOLEAN;
    v_is_system BOOLEAN;
BEGIN
    -- System workflows cannot be edited
    SELECT is_system INTO v_is_system FROM jira_workflow.workflows WHERE id = p_workflow_id;
    IF v_is_system = true THEN
        RETURN false;
    END IF;

    -- Check if workflow is locked by another user
    SELECT is_locked, locked_by INTO v_is_locked, v_locked_by
    FROM jira_workflow.workflows WHERE id = p_workflow_id;

    IF v_is_locked AND v_locked_by != p_user_id THEN
        RETURN false;
    END IF;

    -- Draft workflows can be edited
    SELECT is_draft INTO v_is_draft FROM jira_workflow.workflows WHERE id = p_workflow_id;
    IF v_is_draft THEN
        RETURN true;
    END IF;

    -- Active (published) workflows require draft creation
    RETURN true; -- Allow, but caller should enforce draft creation
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Create workflow draft
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.create_workflow_draft(p_workflow_id UUID, p_user_id UUID)
RETURNS UUID AS $$
DECLARE
    v_draft_id UUID;
    v_draft_exists BOOLEAN;
BEGIN
    -- Check if active draft already exists
    SELECT EXISTS(SELECT 1 FROM jira_workflow.workflow_drafts
                  WHERE workflow_id = p_workflow_id AND draft_status = 'ACTIVE') INTO v_draft_exists;

    IF v_draft_exists THEN
        -- Return existing draft ID
        SELECT id INTO v_draft_id FROM jira_workflow.workflow_drafts
        WHERE workflow_id = p_workflow_id AND draft_status = 'ACTIVE';
        RETURN v_draft_id;
    END IF;

    -- Create new draft
    INSERT INTO jira_workflow.workflow_drafts (workflow_id, name, draft_data, created_by)
    SELECT id, name || ' (Draft)', '{}'::jsonb, p_user_id
    FROM jira_workflow.workflows WHERE id = p_workflow_id
    RETURNING id INTO v_draft_id;

    RETURN v_draft_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Get workflow for project and issue type
-- ============================================
CREATE OR REPLACE FUNCTION jira_workflow.get_workflow_for_project_issue_type(
    p_project_id UUID,
    p_issue_type_id UUID
) RETURNS UUID AS $$
DECLARE
    v_workflow_id UUID;
    v_scheme_id UUID;
BEGIN
    -- Get workflow scheme for project
    SELECT wss.workflow_scheme_id INTO v_scheme_id
    FROM jira_project.project_scheme_associations psa
    JOIN jira_project.workflow_scheme_settings wss ON wss.project_id = psa.project_id
    WHERE psa.project_id = p_project_id
    LIMIT 1;

    -- If no scheme found, try default scheme
    IF v_scheme_id IS NULL THEN
        SELECT id INTO v_scheme_id FROM jira_workflow.workflow_schemes WHERE is_default = true LIMIT 1;
    END IF;

    -- Get workflow from scheme mapping
    IF v_scheme_id IS NOT NULL THEN
        SELECT wsm.workflow_id INTO v_workflow_id
        FROM jira_workflow.workflow_scheme_mappings wsm
        WHERE wsm.scheme_id = v_scheme_id AND wsm.issue_type_id = p_issue_type_id
        LIMIT 1;
    END IF;

    -- If no specific mapping, use default workflow from scheme
    IF v_workflow_id IS NULL AND v_scheme_id IS NOT NULL THEN
        SELECT default_workflow_id INTO v_workflow_id
        FROM jira_workflow.workflow_schemes WHERE id = v_scheme_id;
    END IF;

    RETURN v_workflow_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- VIEW: Workflow usage summary
-- ============================================
CREATE OR REPLACE VIEW jira_workflow.workflow_usage_summary AS
SELECT
    w.id as workflow_id,
    w.name as workflow_name,
    w.is_draft,
    w.is_active,
    w.is_system,
    COUNT(DISTINCT ws.project_id) as project_count,
    COUNT(DISTINCT wsm.issue_type_id) as issue_type_count,
    COUNT(DISTINCT w.id) as status_count,
    w.updated_at as last_modified
FROM jira_workflow.workflows w
LEFT JOIN jira_workflow.workflow_sharing ws ON ws.workflow_id = w.id
LEFT JOIN jira_workflow.workflow_scheme_mappings wsm ON wsm.workflow_id = w.id
LEFT JOIN jira_workflow.workflow_statuses ws2 ON ws2.workflow_id = w.id
GROUP BY w.id, w.name, w.is_draft, w.is_active, w.is_system, w.updated_at;