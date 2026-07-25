-- V22__modification_entities.sql
-- MOD issue type metadata and issue type registration

CREATE TABLE IF NOT EXISTS jira_issue.modification_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    mod_type VARCHAR(20),
    -- Values: MAJOR, MINOR
    ata_chapter VARCHAR(50),
    certification_impact TEXT,
    mod_rationale TEXT,
    affected_documents TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_modification_metadata_issue ON jira_issue.modification_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_modification_metadata_type ON jira_issue.modification_metadata(mod_type);

-- Register MOD issue type (matches pattern from V3__issue_types_and_schemes.sql)
INSERT INTO jira_issue.issue_types (id, name, description, icon_url, is_subtask, issue_type_key, icon, sequence)
VALUES (
    'f0000001-0000-0000-0000-000000000001',
    'MOD',
    'Modification issue type for tracking MAJOR and MINOR aircraft modifications',
    '/icons/issuetypes/modification.svg',
    false,
    'mod',
    'modification',
    30
)
ON CONFLICT (id) DO NOTHING;
