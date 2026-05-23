-- Jira DC-style field visibility: screen mappings + per-project/issue-type configuration

CREATE TABLE IF NOT EXISTS jira_migration.field_screen_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    screen_type VARCHAR(32) NOT NULL,
    field_key VARCHAR(255) NOT NULL,
    tab_name VARCHAR(128) DEFAULT 'default',
    display_order INT DEFAULT 0,
    required_on_screen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_field_screen_mapping UNIQUE (project_id, screen_type, field_key)
);

CREATE TABLE IF NOT EXISTS jira_migration.field_configuration_overrides (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    issue_type_id UUID,
    field_key VARCHAR(255) NOT NULL,
    hidden BOOLEAN DEFAULT FALSE,
    required BOOLEAN DEFAULT FALSE,
    read_only BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_field_config_override UNIQUE (project_id, issue_type_id, field_key)
);

CREATE INDEX idx_fsm_project_screen ON jira_migration.field_screen_mappings(project_id, screen_type);
CREATE INDEX idx_fsm_field_key ON jira_migration.field_screen_mappings(field_key);
CREATE INDEX idx_fco_project ON jira_migration.field_configuration_overrides(project_id);

-- Backfill VIEW screen mappings for existing custom fields (migration import parity)
INSERT INTO jira_migration.field_screen_mappings (project_id, screen_type, field_key, tab_name, display_order)
SELECT NULL, 'VIEW', fd.field_key, 'custom_fields', 0
FROM jira_migration.field_definitions fd
JOIN jira_migration.custom_field_definitions cf ON cf.field_key = fd.field_key
WHERE fd.custom = TRUE AND fd.deprecated = FALSE
ON CONFLICT (project_id, screen_type, field_key) DO NOTHING;

INSERT INTO jira_migration.field_screen_mappings (project_id, screen_type, field_key, tab_name, display_order)
SELECT NULL, 'EDIT', fd.field_key, 'custom_fields', 0
FROM jira_migration.field_definitions fd
JOIN jira_migration.custom_field_definitions cf ON cf.field_key = fd.field_key
WHERE fd.custom = TRUE AND fd.deprecated = FALSE
ON CONFLICT (project_id, screen_type, field_key) DO NOTHING;
