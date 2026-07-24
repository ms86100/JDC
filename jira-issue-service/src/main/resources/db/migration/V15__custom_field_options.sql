-- ============================================
-- CUSTOM FIELD OPTIONS TABLE
-- Supports select, multi-select, cascading select, radio, checkbox field types
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.custom_field_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES jira_issue.custom_field_definitions(id) ON DELETE CASCADE,
    value VARCHAR(500) NOT NULL,
    label VARCHAR(500),
    position INT NOT NULL DEFAULT 0,
    disabled BOOLEAN NOT NULL DEFAULT FALSE,
    parent_option_id UUID REFERENCES jira_issue.custom_field_options(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cfo_field ON jira_issue.custom_field_options(field_id);
CREATE INDEX IF NOT EXISTS idx_cfo_parent ON jira_issue.custom_field_options(parent_option_id);
CREATE INDEX IF NOT EXISTS idx_cfo_field_position ON jira_issue.custom_field_options(field_id, position);
