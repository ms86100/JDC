-- Full database with schemas, tables, functions, and seed data
-- For Docker initialization - auto-runs on container first start

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- SCHEMAS
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS jira_auth;
CREATE SCHEMA IF NOT EXISTS jira_issue;
CREATE SCHEMA IF NOT EXISTS jira_project;
CREATE SCHEMA IF NOT EXISTS jira_migration;
CREATE SCHEMA IF NOT EXISTS jira_workflow;
CREATE SCHEMA IF NOT EXISTS jira_comment;
CREATE SCHEMA IF NOT EXISTS jira_notification;
CREATE SCHEMA IF NOT EXISTS jira_search;
CREATE SCHEMA IF NOT EXISTS jira_audit;
CREATE SCHEMA IF NOT EXISTS jira_sprint;
CREATE SCHEMA IF NOT EXISTS jira_plan;
CREATE SCHEMA IF NOT EXISTS jira_admin;

-- =============================================================================
-- FUNCTIONS
-- =============================================================================
CREATE OR REPLACE FUNCTION jira_issue.update_issue_counters() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF TG_TABLE_NAME = 'watchers' THEN UPDATE jira_issue.issues SET watcher_count = watcher_count + 1 WHERE id = NEW.issue_id;
        ELSIF TG_TABLE_NAME = 'votes' THEN UPDATE jira_issue.issues SET vote_count = vote_count + 1 WHERE id = NEW.issue_id; END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF TG_TABLE_NAME = 'watchers' THEN UPDATE jira_issue.issues SET watcher_count = watcher_count - 1 WHERE id = OLD.issue_id;
        ELSIF TG_TABLE_NAME = 'votes' THEN UPDATE jira_issue.issues SET vote_count = vote_count - 1 WHERE id = OLD.issue_id; END IF;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION jira_issue.update_test_set_count() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN UPDATE jira_issue.test_sets SET test_count = test_count + 1 WHERE id = NEW.test_set_id;
    ELSIF TG_OP = 'DELETE' THEN UPDATE jira_issue.test_sets SET test_count = GREATEST(0, test_count - 1) WHERE id = OLD.test_set_id;
    ELSIF TG_OP = 'UPDATE' THEN
        UPDATE jira_issue.test_sets SET test_count = GREATEST(0, test_count - 1) WHERE id = OLD.test_set_id;
        UPDATE jira_issue.test_sets SET test_count = test_count + 1 WHERE id = NEW.test_set_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- AUTH SCHEMA
-- =============================================================================
CREATE TABLE IF NOT EXISTS jira_auth.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(200),
    active BOOLEAN DEFAULT true,
    last_login TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_auth.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_auth.user_roles (
    user_id UUID REFERENCES jira_auth.users(id) ON DELETE CASCADE,
    role_id UUID REFERENCES jira_auth.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS jira_auth.user_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_auth.user_group_memberships (
    user_id UUID REFERENCES jira_auth.users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES jira_auth.user_groups(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, group_id)
);

-- =============================================================================
-- PROJECT SCHEMA
-- =============================================================================
CREATE TABLE IF NOT EXISTS jira_project.project_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.project_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    category VARCHAR(50),
    is_enabled BOOLEAN DEFAULT true,
    capabilities JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_capabilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    capability_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    config JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_key VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_type VARCHAR(50),
    category VARCHAR(100),
    lead_user_id UUID,
    avatar_url VARCHAR(500),
    default_assignee_type VARCHAR(50) DEFAULT 'PROJECT_LEAD',
    archived BOOLEAN DEFAULT false,
    archived_at TIMESTAMP,
    permission_scheme_id UUID,
    template_id UUID,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.project_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.project_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    member_type VARCHAR(50) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    role_id UUID REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.project_role_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID REFERENCES jira_project.project_roles(id) ON DELETE CASCADE,
    member_type VARCHAR(50) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    project_id UUID REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.issue_type_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.issue_type_scheme_issue_types (
    scheme_id UUID REFERENCES jira_project.issue_type_schemes(id) ON DELETE CASCADE,
    issue_type_name VARCHAR(50) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (scheme_id, issue_type_name)
);

CREATE TABLE IF NOT EXISTS jira_project.workflow_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.workflow_scheme_workflows (
    scheme_id UUID REFERENCES jira_project.workflow_schemes(id) ON DELETE CASCADE,
    workflow_name VARCHAR(100) NOT NULL,
    issue_type_name VARCHAR(50),
    PRIMARY KEY (scheme_id, workflow_name),
    UNIQUE (scheme_id, workflow_name, issue_type_name)
);

CREATE TABLE IF NOT EXISTS jira_project.permission_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.permission_grants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID REFERENCES jira_project.permission_schemes(id) ON DELETE CASCADE,
    permission_key VARCHAR(100) NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    grantee_id VARCHAR(100),
    grantee_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.notification_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    notifications JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.screen_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.screen_scheme_screens (
    scheme_id UUID REFERENCES jira_project.screen_schemes(id) ON DELETE CASCADE,
    screen_type VARCHAR(20) NOT NULL,
    screen_id UUID NOT NULL,
    PRIMARY KEY (scheme_id, screen_type)
);

CREATE TABLE IF NOT EXISTS jira_project.screen_scheme_issue_type_screens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID REFERENCES jira_project.screen_schemes(id) ON DELETE CASCADE,
    issue_type VARCHAR(50) NOT NULL,
    screen_id UUID NOT NULL,
    screen_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.field_configuration_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.field_configuration_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID REFERENCES jira_project.field_configuration_schemes(id) ON DELETE CASCADE,
    field_id VARCHAR(100) NOT NULL,
    field_name VARCHAR(200),
    description TEXT,
    required BOOLEAN DEFAULT FALSE,
    default_value TEXT,
    rendered_view_required BOOLEAN DEFAULT FALSE,
    always_renderplain_text BOOLEAN DEFAULT FALSE,
    key VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.issue_security_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.project_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID,
    workflow_scheme_id UUID,
    permission_scheme_id UUID,
    notification_scheme_id UUID,
    screen_scheme_id UUID,
    field_configuration_scheme_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.status_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status_id VARCHAR(50) NOT NULL,
    status_name VARCHAR(100) NOT NULL,
    status_category VARCHAR(50) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    color VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_scheme_defaults (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    issue_type_scheme_id UUID,
    workflow_scheme_id UUID,
    permission_scheme_id UUID,
    notification_scheme_id UUID,
    screen_scheme_id UUID
);

CREATE TABLE IF NOT EXISTS jira_project.template_issue_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    issue_type_name VARCHAR(50) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_workflow_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    status_name VARCHAR(100) NOT NULL,
    status_category VARCHAR(50) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_workflow_transitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    from_status VARCHAR(100) NOT NULL,
    to_status VARCHAR(100) NOT NULL,
    transition_name VARCHAR(100),
    conditions JSONB DEFAULT '[]',
    post_functions JSONB DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.template_scheme_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL,
    scheme_id UUID NOT NULL,
    scheme_type VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- =============================================================================
-- ISSUE SCHEMA
-- =============================================================================
CREATE TABLE IF NOT EXISTS jira_issue.issue_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    issue_type_key VARCHAR(50) UNIQUE,
    color VARCHAR(7),
    is_subtask BOOLEAN DEFAULT false,
    sequence INTEGER DEFAULT 0,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    sequence INTEGER DEFAULT 0,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_priorities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(100),
    color VARCHAR(7),
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.resolutions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    issue_key VARCHAR(20),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    priority VARCHAR(50),
    issue_type VARCHAR(50),
    reporter_id UUID,
    assignee_id UUID,
    parent_issue_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    epic_id UUID,
    epic_name VARCHAR(200),
    epic_color VARCHAR(7),
    security_level_id UUID,
    affects_versions JSONB DEFAULT '[]',
    fix_versions JSONB DEFAULT '[]',
    story_points INTEGER,
    rank VARCHAR(100),
    original_estimate BIGINT,
    remaining_estimate BIGINT,
    time_spent BIGINT,
    resolution_id UUID,
    resolution_date TIMESTAMP,
    due_date TIMESTAMP,
    vote_count INTEGER DEFAULT 0,
    watcher_count INTEGER DEFAULT 0,
    creator_id UUID,
    environment TEXT,
    aggregate_time_estimate BIGINT,
    aggregate_time_spent BIGINT,
    work_ratio INTEGER,
    last_viewed_at TIMESTAMP,
    external_issue_key VARCHAR(100),
    external_priority VARCHAR(50),
    external_type VARCHAR(50),
    team_id VARCHAR(100),
    team_name VARCHAR(200),
    target_start TIMESTAMP,
    target_end TIMESTAMP,
    original_story_points INTEGER,
    issue_color VARCHAR(7),
    security_level_name VARCHAR(100),
    component_ids JSONB DEFAULT '[]',
    test_type VARCHAR(50),
    test_status VARCHAR(50),
    test_priority VARCHAR(50),
    test_owner_id UUID,
    test_steps TEXT,
    requirement_keys JSONB DEFAULT '[]',
    gherkin_feature_key VARCHAR(100),
    gherkin_scenario_id UUID,
    test_set_id UUID,
    test_plan_id UUID,
    test_execution_id UUID,
    test_repository_folder_id UUID,
    archived BOOLEAN DEFAULT false,
    labels JSONB DEFAULT '[]',
    version INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS jira_issue.epics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    issue_id UUID NOT NULL,
    epic_key VARCHAR(20),
    epic_name VARCHAR(200),
    epic_color VARCHAR(7),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.labels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    label VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_labels (
    issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    label_id UUID REFERENCES jira_issue.labels(id) ON DELETE CASCADE,
    PRIMARY KEY (issue_id, label_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(200),
    content TEXT NOT NULL,
    parent_comment_id UUID,
    internal BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID,
    filename VARCHAR(500) NOT NULL,
    file_path VARCHAR,
    file_size BIGINT,
    mime_type VARCHAR(100),
    thumbnail_path VARCHAR,
    uploader_id UUID,
    uploader_name VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_issue_id UUID NOT NULL,
    target_issue_id UUID NOT NULL,
    link_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_link_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    inward VARCHAR(100),
    outward VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.screens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.screen_tabs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_id UUID NOT NULL REFERENCES jira_issue.screens(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.screen_fields (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_tab_id UUID NOT NULL REFERENCES jira_issue.screen_tabs(id) ON DELETE CASCADE,
    field_id VARCHAR(100) NOT NULL,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.security_levels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.defect_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    defect_id VARCHAR(100),
    link_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.change_groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(200),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.change_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    change_group_id UUID NOT NULL,
    field_type VARCHAR(50),
    field VARCHAR(100),
    old_value TEXT,
    old_string TEXT,
    new_value TEXT,
    new_string TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_transition_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    transition_name VARCHAR(100),
    actor_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.epic_progress_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    epic_id UUID NOT NULL,
    total_story_points INTEGER DEFAULT 0,
    completed_story_points INTEGER DEFAULT 0,
    recorded_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_type_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_type_scheme_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.project_components (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    component_key VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    lead_user_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.project_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    description TEXT,
    released BOOLEAN DEFAULT false,
    release_date DATE,
    start_date DATE,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_components (
    issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    component_id UUID REFERENCES jira_issue.project_components(id) ON DELETE CASCADE,
    PRIMARY KEY (issue_id, component_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_versions (
    issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    version_id UUID REFERENCES jira_issue.project_versions(id) ON DELETE CASCADE,
    type VARCHAR(20) DEFAULT 'AFFECTS_VERSION',
    PRIMARY KEY (issue_id, version_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_sprints (
    issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    sprint_id UUID NOT NULL,
    PRIMARY KEY (issue_id, sprint_id)
);

CREATE TABLE IF NOT EXISTS jira_issue.custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    field_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    field_type VARCHAR(50) NOT NULL,
    default_value TEXT,
    options JSONB,
    is_required BOOLEAN DEFAULT false,
    is_searchable BOOLEAN DEFAULT true,
    is_sortable BOOLEAN DEFAULT true,
    screen_region VARCHAR(50),
    renderer VARCHAR(100),
    searcher VARCHAR(100),
    plugin_source VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.custom_field_values (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    field_id UUID NOT NULL,
    string_value TEXT,
    number_value DOUBLE PRECISION,
    date_value DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.requirement_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_issue_id UUID NOT NULL,
    target_issue_id UUID NOT NULL,
    requirement_key VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.shared_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    step_key VARCHAR(50),
    step_text TEXT NOT NULL,
    step_data TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.shared_step_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_id UUID NOT NULL,
    step_id UUID NOT NULL,
    position INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.cucumber_features (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_key VARCHAR(100),
    feature_file VARCHAR(500),
    feature_name VARCHAR(200),
    feature_tags JSONB,
    background TEXT,
    language VARCHAR(20),
    scenario_count INTEGER DEFAULT 0,
    test_set_id UUID,
    raw_content TEXT,
    import_batch_id UUID,
    imported_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.cucumber_scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_key VARCHAR(100),
    feature_file VARCHAR(500),
    feature_name VARCHAR(200),
    scenario_name VARCHAR(200),
    scenario_key VARCHAR(50),
    scenario_type VARCHAR(50),
    background TEXT,
    tags JSONB,
    examples JSONB,
    line_number INTEGER,
    issue_id UUID,
    test_set_id UUID,
    imported_at TIMESTAMP,
    import_batch_id UUID
);

CREATE TABLE IF NOT EXISTS jira_issue.test_repository_folders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    parent_folder_id UUID,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    path VARCHAR(500),
    depth INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    is_smart_folder BOOLEAN DEFAULT false,
    smart_folder_query TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.test_sets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    folder_id UUID,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    test_type VARCHAR(50),
    labels JSONB,
    test_count INTEGER DEFAULT 0,
    requirement_keys JSONB,
    status VARCHAR(50),
    owner_id UUID,
    created_by UUID,
    archived BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_issue.issue_event_outbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- =============================================================================
-- SEED DATA
-- =============================================================================
INSERT INTO jira_auth.users (id, username, email, password_hash, display_name, active, created_at, updated_at) VALUES
('a75161aa-e77a-4e77-ba7f-ef10d3ce87dd', 'ms86100', 'admin@test.local', '$2a$12$8pkY60ZxHKszkJLrD9Udh.XlenmSWnl8viUxp0vo2cptX/JgF5QXm', 'Admin User', true, NOW(), NOW()),
('60e36e9c-e0e8-48f7-9b4a-0b66921cda01', 'testuser', 'test@test.com', '$2a$12$No/0DA80cmjbW49URhVKZO7PavgeYsaRQyoR60QwRROtdB.7Z3nNO', 'Test User', true, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

INSERT INTO jira_auth.roles (id, role_key, name, description, created_at) VALUES
('b6df8255-9016-44bb-9986-bda7bade182a', 'ROLE_ADMIN', 'System Administrator', 'Full admin access', NOW()),
('c085e9c9-fddc-4a4c-abd4-dc39d7033d8e', 'ROLE_USER', 'Standard User', 'Regular user access', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO jira_auth.user_roles (user_id, role_id) VALUES
('a75161aa-e77a-4e77-ba7f-ef10d3ce87dd', 'b6df8255-9016-44bb-9986-bda7bade182a'),
('a75161aa-e77a-4e77-ba7f-ef10d3ce87dd', 'c085e9c9-fddc-4a4c-abd4-dc39d7033d8e'),
('60e36e9c-e0e8-48f7-9b4a-0b66921cda01', 'c085e9c9-fddc-4a4c-abd4-dc39d7033d8e')
ON CONFLICT DO NOTHING;

INSERT INTO jira_issue.issue_types (id, name, icon, description, issue_type_key, is_subtask, sequence) VALUES
('a0000000-0000-0000-0000-000000000001', 'Bug', 'bug', 'A bug report', 'bug', false, 0),
('a0000000-0000-0000-0000-000000000002', 'Story', 'book', 'A user story', 'story', false, 0),
('a0000000-0000-0000-0000-000000000003', 'Task', 'task', 'A task', 'task', false, 0),
('a0000000-0000-0000-0000-000000000004', 'Epic', 'lightning', 'An epic', 'epic', false, 0),
('a0000000-0000-0000-0000-000000000005', 'Subtask', 'subtask', 'A subtask', 'subtask', false, 0),
('a0000000-0000-0000-0000-000000000006', 'Improvement', 'improvement', 'An improvement', 'improvement', false, 0),
('a0000000-0000-0000-0000-000000000007', 'New Feature', 'newfeature', 'A new feature', 'new-feature', false, 0),
('a0000000-0000-0000-0000-000000000008', 'Question', 'question', 'A question', 'question', false, 0),
('a0000000-0000-0000-0000-000000000009', 'Technical Task', 'technical', 'A technical task', 'technical-task', false, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
('00000000-0000-0000-0001-000000000001', 'Backlog', 0, 'TODO'),
('00000000-0000-0000-0001-000000000002', 'To Do', 1, 'TODO'),
('00000000-0000-0000-0001-000000000003', 'In Progress', 2, 'IN_PROGRESS'),
('00000000-0000-0000-0001-000000000004', 'In Review', 3, 'IN_PROGRESS'),
('00000000-0000-0000-0001-000000000005', 'Done', 4, 'DONE'),
('00000000-0000-0000-0001-000000000006', 'Open', 5, 'TODO'),
('00000000-0000-0000-0001-000000000007', 'Resolved', 6, 'DONE'),
('00000000-0000-0000-0001-000000000008', 'Closed', 7, 'DONE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO jira_issue.issue_priorities (id, name, icon, color, sequence) VALUES
('b0000000-0000-0000-0000-000000000001', 'Highest', 'arrow-up', '#ff0000', 1),
('b0000000-0000-0000-0000-000000000002', 'High', 'arrow-up', '#ff6600', 2),
('b0000000-0000-0000-0000-000000000003', 'Medium', 'minus', '#ffcc00', 3),
('b0000000-0000-0000-0000-000000000004', 'Low', 'arrow-down', '#0099ff', 4),
('b0000000-0000-0000-0000-000000000005', 'Lowest', 'arrow-down', '#99cc00', 5)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_issues_project ON jira_issue.issues(project_id);
CREATE INDEX IF NOT EXISTS idx_issues_status ON jira_issue.issues(status);
CREATE INDEX IF NOT EXISTS idx_issues_assignee ON jira_issue.issues(assignee_id);
CREATE INDEX IF NOT EXISTS idx_comments_issue ON jira_issue.comments(issue_id);
CREATE INDEX IF NOT EXISTS idx_project_members_project ON jira_project.project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON jira_auth.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON jira_auth.users(email);