-- Wave 2: issue-type screen overrides + field configuration schemes

CREATE TABLE IF NOT EXISTS jira_project.screen_scheme_issue_type_screens (
    scheme_id UUID NOT NULL REFERENCES jira_project.screen_schemes(id) ON DELETE CASCADE,
    issue_type_id UUID NOT NULL,
    screen_type VARCHAR(20) NOT NULL,
    screen_id UUID NOT NULL,
    PRIMARY KEY (scheme_id, issue_type_id, screen_type)
);

CREATE TABLE IF NOT EXISTS jira_project.field_configuration_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_project.field_configuration_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID NOT NULL REFERENCES jira_project.field_configuration_schemes(id) ON DELETE CASCADE,
    issue_type_id UUID,
    field_key VARCHAR(64) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    hidden BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_field_config_entry
    ON jira_project.field_configuration_entries (scheme_id, COALESCE(issue_type_id, '00000000-0000-0000-0000-000000000000'::uuid), field_key);

ALTER TABLE jira_project.project_schemes
    ADD COLUMN IF NOT EXISTS field_configuration_scheme_id UUID
        REFERENCES jira_project.field_configuration_schemes(id);

ALTER TABLE jira_project.template_scheme_defaults
    ADD COLUMN IF NOT EXISTS field_configuration_scheme_id UUID
        REFERENCES jira_project.field_configuration_schemes(id);

INSERT INTO jira_project.field_configuration_schemes (id, name, description, is_default)
VALUES ('f0000000-0000-0000-0000-000000000001', 'Default Field Configuration', 'Baseline required/visible rules for issue create', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO jira_project.field_configuration_entries (scheme_id, issue_type_id, field_key, required, visible, hidden)
SELECT 'f0000000-0000-0000-0000-000000000001', NULL, v.field_key, v.required, v.visible, v.hidden
FROM (VALUES
    ('summary', TRUE, TRUE, FALSE),
    ('issuetype', TRUE, TRUE, FALSE),
    ('priority', FALSE, TRUE, FALSE),
    ('description', FALSE, TRUE, FALSE)
) AS v(field_key, required, visible, hidden)
WHERE NOT EXISTS (
    SELECT 1 FROM jira_project.field_configuration_entries e
    WHERE e.scheme_id = 'f0000000-0000-0000-0000-000000000001'
      AND e.issue_type_id IS NULL
      AND e.field_key = v.field_key
);

UPDATE jira_project.project_schemes
SET field_configuration_scheme_id = 'f0000000-0000-0000-0000-000000000001'
WHERE field_configuration_scheme_id IS NULL;

UPDATE jira_project.template_scheme_defaults
SET field_configuration_scheme_id = 'f0000000-0000-0000-0000-000000000001'
WHERE field_configuration_scheme_id IS NULL;
