-- V2__dynamic_field_architecture.sql
-- Dynamic Field Architecture for Enterprise Issue Migration
-- Supports plugin fields, custom fields, and future extensibility

-- ============================================
-- FIELD DEFINITIONS TABLE
-- Metadata-driven field definitions
-- ============================================
CREATE TABLE jira_migration.field_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    field_key VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Field type and rendering
    field_type VARCHAR(50) NOT NULL,  -- TEXT, TEXTAREA, RICHTEXT, NUMBER, DATE, SINGLE_SELECT, etc.
    renderer VARCHAR(100),  -- TEXT, SELECT, USER_PICKER, DATETIME_PICKER, etc.
    screen_region VARCHAR(50),  -- HEADER, LEFT_PRIMARY, SIDEBAR_PEOPLE, SIDEBAR_DETAILS, etc.

    -- Plugin source tracking
    plugin_source VARCHAR(255),
    plugin_namespace VARCHAR(255),

    -- Search and filter configuration
    searchable BOOLEAN DEFAULT TRUE,
    sortable BOOLEAN DEFAULT TRUE,
    filterable BOOLEAN DEFAULT TRUE,

    -- Field constraints
    required BOOLEAN DEFAULT FALSE,
    read_only BOOLEAN DEFAULT FALSE,
    hidden BOOLEAN DEFAULT FALSE,
    search_weight INT DEFAULT 0,

    -- JSON-based configuration
    schema_definition JSONB,  -- Type-specific schema (max length, pattern, etc.)
    visibility_rules JSONB,  -- Rules for when field is visible
    renderer_config JSONB,  -- Renderer-specific configuration
    validation_rules JSONB,  -- Field-level validation rules

    -- Options for select-type fields
    options JSONB,  -- Array of {value, label, order, color, disabled}
    default_value TEXT,

    -- Field classification
    custom BOOLEAN DEFAULT FALSE,
    built_in BOOLEAN DEFAULT FALSE,
    deprecated BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 1,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID
);

-- ============================================
-- ISSUE FIELD VALUES TABLE
-- Dynamic field value storage
-- ============================================
CREATE TABLE jira_migration.issue_field_values (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL,
    field_definition_id UUID NOT NULL REFERENCES jira_migration.field_definitions(id),

    -- Value storage (only one of these will be populated based on field_type)
    string_value TEXT,
    text_value TEXT,
    integer_value INT,
    long_value BIGINT,
    double_value DOUBLE PRECISION,
    boolean_value BOOLEAN,
    date_value DATE,
    datetime_value TIMESTAMP,
    array_value JSONB,
    object_value JSONB,

    -- Formatted and raw values
    formatted_value TEXT,
    raw_value TEXT,
    value_source VARCHAR(100),
    value_hash VARCHAR(64),

    -- Validation
    validation_status VARCHAR(50) DEFAULT 'VALID',  -- VALID, INVALID, WARNING, PENDING, UNMAPPED
    validation_message TEXT,

    -- Import tracking
    imported_from VARCHAR(100),
    import_mapping_id UUID,

    -- Search optimization
    searchable_text TEXT,

    -- Versioning
    version INT DEFAULT 1,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,

    -- Unique constraint: one value per field per issue
    CONSTRAINT unique_issue_field UNIQUE (issue_id, field_definition_id)
);

-- ============================================
-- CUSTOM FIELD DEFINITIONS TABLE
-- User-created custom fields
-- ============================================
CREATE TABLE jira_migration.custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(100) NOT NULL,  -- Jira-style plugin type keys

    -- Searcher and renderer
    searcher_key VARCHAR(255),
    renderer_key VARCHAR(255),

    -- Type configuration
    unique_type BOOLEAN DEFAULT FALSE,
    config JSONB,
    default_values JSONB,

    -- Field key mapping
    field_key VARCHAR(255) UNIQUE,

    -- Status
    enabled BOOLEAN DEFAULT TRUE,
    searchable BOOLEAN DEFAULT TRUE,
    navigable BOOLEAN DEFAULT TRUE,

    -- Clause names for JQL
    clause_names VARCHAR(255)[],

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID
);

-- ============================================
-- CUSTOM FIELD CONTEXTS TABLE
-- Project/type-specific field contexts
-- ============================================
CREATE TABLE jira_migration.custom_field_contexts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    custom_field_id UUID NOT NULL REFERENCES jira_migration.custom_field_definitions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Scope
    all_projects BOOLEAN DEFAULT TRUE,
    project_ids UUID[],
    issue_type_ids UUID[],

    -- Default values for this context
    default_values JSONB,

    -- Display
    display_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- CUSTOM FIELD OPTIONS TABLE
-- Options for select-type custom fields
-- ============================================
CREATE TABLE jira_migration.custom_field_options (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    custom_field_id UUID NOT NULL REFERENCES jira_migration.custom_field_definitions(id) ON DELETE CASCADE,
    parent_option_id UUID REFERENCES jira_migration.custom_field_options(id) ON DELETE CASCADE,

    value VARCHAR(255) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(7),  -- Hex color code

    icon_url VARCHAR(500),
    sequence INT DEFAULT 0,
    disabled BOOLEAN DEFAULT FALSE,
    cast_children BOOLEAN DEFAULT FALSE,

    -- Additional properties
    properties JSONB,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_option_value UNIQUE (custom_field_id, parent_option_id, value)
);

-- ============================================
-- PLUGIN FIELD REGISTRY TABLE
-- Plugin-specific field mappings
-- ============================================
CREATE TABLE jira_migration.plugin_field_registry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plugin_key VARCHAR(255) NOT NULL,
    plugin_name VARCHAR(255) NOT NULL,
    plugin_version VARCHAR(50),

    -- Field mapping
    field_key VARCHAR(255) NOT NULL,
    field_type VARCHAR(100) NOT NULL,
    jira_field_key VARCHAR(255),  -- Mapped Jira field

    -- Schema and mapping configuration
    schema_mapping JSONB,
    import_mapping JSONB,
    export_mapping JSONB,

    -- Feature flags
    searchable BOOLEAN DEFAULT TRUE,
    navigable BOOLEAN DEFAULT TRUE,
    clauses VARCHAR(255)[],

    -- Field definition link
    field_definition_id UUID REFERENCES jira_migration.field_definitions(id),

    -- Status
    enabled BOOLEAN DEFAULT TRUE,
    deployed BOOLEAN DEFAULT TRUE,

    -- Sync tracking
    registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_sync_at TIMESTAMP,

    CONSTRAINT unique_plugin_field UNIQUE (plugin_key, field_key)
);

-- ============================================
-- INDEXES
-- ============================================

-- Field definitions
CREATE INDEX idx_field_def_key ON jira_migration.field_definitions(field_key);
CREATE INDEX idx_field_def_type ON jira_migration.field_definitions(field_type);
CREATE INDEX idx_field_def_region ON jira_migration.field_definitions(screen_region);
CREATE INDEX idx_field_def_plugin ON jira_migration.field_definitions(plugin_source);
CREATE INDEX idx_field_def_custom ON jira_migration.field_definitions(custom);
CREATE INDEX idx_field_def_builtin ON jira_migration.field_definitions(built_in);
CREATE INDEX idx_field_def_searchable ON jira_migration.field_definitions(searchable);
CREATE INDEX idx_field_def_filterable ON jira_migration.field_definitions(filterable);

-- Issue field values
CREATE INDEX idx_issue_fv_issue ON jira_migration.issue_field_values(issue_id);
CREATE INDEX idx_issue_fv_field ON jira_migration.issue_field_values(field_definition_id);
CREATE INDEX idx_issue_fv_issue_field ON jira_migration.issue_field_values(issue_id, field_definition_id);
CREATE INDEX idx_issue_fv_validation ON jira_migration.issue_field_values(validation_status);
CREATE INDEX idx_issue_fv_imported ON jira_migration.issue_field_values(imported_from);
CREATE INDEX idx_issue_fv_searchable ON jira_migration.issue_field_values USING GIN(searchable_text gin_trgm_ops);

-- Custom fields
CREATE INDEX idx_cf_name ON jira_migration.custom_field_definitions(name);
CREATE INDEX idx_cf_field_key ON jira_migration.custom_field_definitions(field_key);
CREATE INDEX idx_cf_type ON jira_migration.custom_field_definitions(type);
CREATE INDEX idx_cf_enabled ON jira_migration.custom_field_definitions(enabled);

-- Custom field contexts
CREATE INDEX idx_cfc_field ON jira_migration.custom_field_contexts(custom_field_id);
CREATE INDEX idx_cfc_all_projects ON jira_migration.custom_field_contexts(all_projects);

-- Custom field options
CREATE INDEX idx_cfo_field ON jira_migration.custom_field_options(custom_field_id);
CREATE INDEX idx_cfo_parent ON jira_migration.custom_field_options(parent_option_id);
CREATE INDEX idx_cfo_sequence ON jira_migration.custom_field_options(custom_field_id, sequence);

-- Plugin field registry
CREATE INDEX idx_pfr_plugin ON jira_migration.plugin_field_registry(plugin_key);
CREATE INDEX idx_pfr_jira_field ON jira_migration.plugin_field_registry(jira_field_key);
CREATE INDEX idx_pfr_enabled ON jira_migration.plugin_field_registry(enabled);
CREATE INDEX idx_pfr_field_def ON jira_migration.plugin_field_registry(field_definition_id);

-- ============================================
-- SEED DATA: Built-in Field Definitions
-- ============================================

-- Core issue metadata
INSERT INTO jira_migration.field_definitions (field_key, display_name, field_type, renderer, screen_region, built_in, searchable, sortable, filterable, required) VALUES
('summary', 'Summary', 'TEXT', 'TEXT', 'HEADER', TRUE, TRUE, TRUE, TRUE, TRUE),
('description', 'Description', 'RICHTEXT', 'RICHTEXT', 'LEFT_DESCRIPTION', TRUE, TRUE, FALSE, FALSE, FALSE),
('environment', 'Environment', 'TEXTAREA', 'TEXTAREA', 'LEFT_DESCRIPTION', TRUE, TRUE, FALSE, FALSE, FALSE),

-- Issue type and status
('issue_type', 'Issue Type', 'ISSUE_TYPE', 'SELECT', 'HEADER', TRUE, TRUE, TRUE, TRUE, TRUE),
('status', 'Status', 'STATUS', 'SELECT', 'HEADER', TRUE, TRUE, TRUE, TRUE, TRUE),

-- Priority and resolution
('priority', 'Priority', 'PRIORITY', 'SELECT', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),
('resolution', 'Resolution', 'RESOLUTION', 'SELECT', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),

-- People section
('assignee', 'Assignee', 'USER', 'USER_PICKER', 'SIDEBAR_PEOPLE', TRUE, TRUE, TRUE, TRUE, FALSE),
('reporter', 'Reporter', 'USER', 'USER_PICKER', 'SIDEBAR_PEOPLE', TRUE, TRUE, TRUE, TRUE, FALSE),
('creator', 'Creator', 'USER', 'USER_PICKER', 'SIDEBAR_PEOPLE', TRUE, TRUE, FALSE, FALSE, FALSE),
('votes', 'Votes', 'VOTES', 'VOTES', 'SIDEBAR_PEOPLE', TRUE, TRUE, TRUE, TRUE, FALSE),
('watchers', 'Watchers', 'WATCHERS', 'WATCHERS', 'SIDEBAR_PEOPLE', TRUE, TRUE, FALSE, FALSE, FALSE),

-- Time tracking
('original_estimate', 'Original Estimate', 'DURATION', 'DURATION', 'SIDEBAR_TIME', TRUE, TRUE, TRUE, TRUE, FALSE),
('remaining_estimate', 'Remaining Estimate', 'DURATION', 'DURATION', 'SIDEBAR_TIME', TRUE, TRUE, TRUE, TRUE, FALSE),
('time_spent', 'Time Spent', 'DURATION', 'DURATION', 'SIDEBAR_TIME', TRUE, TRUE, TRUE, TRUE, FALSE),

-- Details section
('labels', 'Labels', 'LABEL', 'LABEL_EDITOR', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),
('components', 'Components', 'COMPONENT', 'MULTI_SELECT', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),
('security_level', 'Security Level', 'SECURITY_LEVEL', 'SECURITY_LEVEL', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),

-- Dates section
('due_date', 'Due Date', 'DATE', 'DATETIME_PICKER', 'SIDEBAR_DATES', TRUE, TRUE, TRUE, TRUE, FALSE),
('created', 'Created', 'DATETIME', 'READ_ONLY', 'SIDEBAR_DATES', TRUE, TRUE, TRUE, TRUE, FALSE),
('updated', 'Updated', 'DATETIME', 'READ_ONLY', 'SIDEBAR_DATES', TRUE, TRUE, TRUE, TRUE, FALSE),
('resolution_date', 'Resolved', 'DATETIME', 'READ_ONLY', 'SIDEBAR_DATES', TRUE, TRUE, TRUE, TRUE, FALSE),
('last_viewed', 'Last Viewed', 'DATETIME', 'READ_ONLY', 'SIDEBAR_DATES', TRUE, TRUE, FALSE, FALSE, FALSE),

-- Versions section
('affects_versions', 'Affects Version/s', 'VERSION', 'MULTI_SELECT', 'SIDEBAR_VERSIONS', TRUE, TRUE, TRUE, TRUE, FALSE),
('fix_versions', 'Fix Version/s', 'VERSION', 'MULTI_SELECT', 'SIDEBAR_VERSIONS', TRUE, TRUE, TRUE, TRUE, FALSE),

-- Agile section
('sprint', 'Sprint', 'SPRINT', 'SPRINT_SELECTOR', 'SIDEBAR_AGILE', TRUE, TRUE, TRUE, TRUE, FALSE),
('epic_link', 'Epic Link', 'EPIC', 'EPIC_LINK', 'SIDEBAR_AGILE', TRUE, TRUE, TRUE, TRUE, FALSE),
('epic_name', 'Epic Name', 'TEXT', 'TEXT', 'SIDEBAR_AGILE', TRUE, FALSE, FALSE, FALSE, FALSE),
('epic_color', 'Epic Colour', 'TEXT', 'TEXT', 'SIDEBAR_AGILE', TRUE, FALSE, FALSE, FALSE, FALSE),
('story_points', 'Story Points', 'NUMBER', 'NUMBER', 'SIDEBAR_AGILE', TRUE, TRUE, TRUE, TRUE, FALSE),
('rank', 'Rank', 'TEXT', 'TEXT', 'SIDEBAR_AGILE', TRUE, FALSE, FALSE, FALSE, FALSE),

-- Hierarchy
('parent', 'Parent', 'PARENT_ISSUE', 'USER_PICKER', 'SIDEBAR_DETAILS', TRUE, TRUE, TRUE, TRUE, FALSE),
('epic', 'Epic', 'EPIC', 'EPIC_LINK', 'SIDEBAR_AGILE', TRUE, TRUE, TRUE, TRUE, FALSE);

-- ============================================
-- SEED DATA: Default Plugin Mappings
-- ============================================

INSERT INTO jira_migration.plugin_field_registry (plugin_key, plugin_name, field_key, field_type, jira_field_key, searchable, navigable, clauses) VALUES
-- Tempo (Time Tracking)
('tempo', 'Tempo Timesheets', 'worklog', 'WORKLOG', 'time_spent', TRUE, TRUE, ARRAY['worklog']),
('tempo', 'Tempo Timesheets', 'timesheet', 'WORKLOG', 'time_tracking', TRUE, TRUE, ARRAY['timesheet']),

-- Xray (Test Management)
('xray', 'Xray Test Management', 'test_coverage', 'LABEL', 'labels', TRUE, TRUE, ARRAY['testCoverage']),
('xray', 'Xray Test Management', 'test_set', 'LABEL', 'labels', TRUE, TRUE, ARRAY['testSet']),
('xray', 'Xray Test Management', 'test_type', 'SINGLE_SELECT', NULL, TRUE, TRUE, ARRAY['testType']),

-- Zephyr (Test Management)
('zephyr', 'Zephyr Scale', 'cycle', 'SPRINT', 'sprint', TRUE, TRUE, ARRAY['cycle']),
('zephyr', 'Zephyr Scale', 'version', 'VERSION', 'fix_versions', TRUE, TRUE, ARRAY['zephyrVersion']),

-- Structure (Issue Hierarchy)
('structure', 'Structure Plugin', 'outline', 'CUSTOM', 'parent', TRUE, TRUE, ARRAY['structure']),
('structure', 'Structure Plugin', 'hierarchy', 'CUSTOM', 'parent', TRUE, TRUE, ARRAY['hierarchy']),

-- BigPicture
('bigpicture', 'BigPicture', 'epic', 'EPIC', 'epic_link', TRUE, TRUE, ARRAY['epic']),
('bigpicture', 'BigPicture', 'bucket', 'LABEL', 'labels', TRUE, TRUE, ARRAY['bucket']),

-- Custom Fields
('custom', 'Custom Fields', 'customfield', 'CUSTOM', NULL, TRUE, TRUE, ARRAY['customfield']);

-- ============================================
-- SEED DATA: Default Custom Field Types
-- ============================================

INSERT INTO jira_migration.custom_field_definitions (name, description, type, searcher_key, renderer_key, field_key, enabled, searchable, navigable, clause_names) VALUES
('Text Field', 'A single line text field', 'com.atlassian.jira.plugin.system.customfieldtypes:textfield', 'com.atlassian.jira.plugin.system.customfieldtypes:textsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:textfield', 'customfield_textfield', TRUE, TRUE, TRUE, ARRAY['customfield_textfield', 'cf_textfield']),
('Text Area', 'A multi-line text area', 'com.atlassian.jira.plugin.system.customfieldtypes:textarea', 'com.atlassian.jira.plugin.system.customfieldtypes:textsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:textarea', 'customfield_textarea', TRUE, TRUE, TRUE, ARRAY['customfield_textarea', 'cf_textarea']),
('Date Picker', 'A date picker field', 'com.atlassian.jira.plugin.system.customfieldtypes:datepicker', 'com.atlassian.jira.plugin.system.customfieldtypes:datesearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:datepicker', 'customfield_datepicker', TRUE, TRUE, TRUE, ARRAY['customfield_datepicker', 'cf_datepicker']),
('Date Time Picker', 'A date and time picker', 'com.atlassian.jira.plugin.system.customfieldtypes:datetime', 'com.atlassian.jira.plugin.system.customfieldtypes:datesearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:datetime', 'customfield_datetime', TRUE, TRUE, TRUE, ARRAY['customfield_datetime', 'cf_datetime']),
('Number', 'A numeric field', 'com.atlassian.jira.plugin.system.customfieldtypes:number', 'com.atlassian.jira.plugin.system.customfieldtypes:numbersearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:number', 'customfield_number', TRUE, TRUE, TRUE, ARRAY['customfield_number', 'cf_number']),
('Select List', 'A drop-down select field', 'com.atlassian.jira.plugin.system.customfieldtypes:select', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:select', 'customfield_select', TRUE, TRUE, TRUE, ARRAY['customfield_select', 'cf_select']),
('Multi-Select', 'A multi-select field', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselect', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselect', 'customfield_multiselect', TRUE, TRUE, TRUE, ARRAY['customfield_multiselect', 'cf_multiselect']),
('Radio Buttons', 'Radio button options', 'com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons', 'customfield_radiobuttons', TRUE, TRUE, TRUE, ARRAY['customfield_radiobuttons', 'cf_radio']),
('Checkboxes', 'Checkbox options', 'com.atlassian.jira.plugin.system.customfieldtypes:checkbox', 'com.atlassian.jira.plugin.system.customfieldtypes:multiselectsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:checkbox', 'customfield_checkbox', TRUE, TRUE, TRUE, ARRAY['customfield_checkbox', 'cf_checkbox']),
('User Picker', 'A user picker field', 'com.atlassian.jira.plugin.system.customfieldtypes:userpicker', 'com.atlassian.jira.plugin.system.customfieldtypes:usernamesearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:userpicker', 'customfield_userpicker', TRUE, TRUE, TRUE, ARRAY['customfield_userpicker', 'cf_userpicker']),
('Multi-User Picker', 'Multiple user picker', 'com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker', 'com.atlassian.jira.plugin.system.customfieldtypes:usernamesearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker', 'customfield_multiuserpicker', TRUE, TRUE, TRUE, ARRAY['customfield_multiuserpicker', 'cf_multiuserpicker']),
('Project Picker', 'A project picker field', 'com.atlassian.jira.plugin.system.customfieldtypes:projectpicker', 'com.atlassian.jira.plugin.system.customfieldtypes:projectsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:projectpicker', 'customfield_projectpicker', TRUE, TRUE, TRUE, ARRAY['customfield_projectpicker', 'cf_projectpicker']),
('Version Picker', 'A version picker field', 'com.atlassian.jira.plugin.system.customfieldtypes:versionpicker', 'com.atlassian.jira.plugin.system.customfieldtypes:versionsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:versionpicker', 'customfield_versionpicker', TRUE, TRUE, TRUE, ARRAY['customfield_versionpicker', 'cf_versionpicker']),
('Labels', 'Label field', 'com.atlassian.jira.plugin.system.customfieldtypes:labels', 'com.atlassian.jira.plugin.system.customfieldtypes:labelsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:labels', 'customfield_labels', TRUE, TRUE, TRUE, ARRAY['customfield_labels', 'cf_labels']),
('URL Field', 'URL field', 'com.atlassian.jira.plugin.system.customfieldtypes:url', 'com.atlassian.jira.plugin.system.customfieldtypes:textsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:url', 'customfield_url', TRUE, TRUE, TRUE, ARRAY['customfield_url', 'cf_url']),
('Email Field', 'Email field', 'com.atlassian.jira.plugin.system.customfieldtypes:email', 'com.atlassian.jira.plugin.system.customfieldtypes:textsearcher', 'com.atlassian.jira.plugin.system.customfieldtypes:email', 'customfield_email', TRUE, TRUE, TRUE, ARRAY['customfield_email', 'cf_email']);

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE jira_migration.field_definitions IS 'Metadata-driven field definitions for dynamic field architecture';
COMMENT ON TABLE jira_migration.issue_field_values IS 'Dynamic field values for issues - supports both built-in and custom fields';
COMMENT ON TABLE jira_migration.custom_field_definitions IS 'User-created custom field definitions';
COMMENT ON TABLE jira_migration.custom_field_contexts IS 'Project/type-specific contexts for custom fields';
COMMENT ON TABLE jira_migration.custom_field_options IS 'Options for select-type custom fields';
COMMENT ON TABLE jira_migration.plugin_field_registry IS 'Registry for plugin-specific field definitions';