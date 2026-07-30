-- Application Links (Jira DC Confluence / cross-product integration)

CREATE TABLE IF NOT EXISTS jira_admin.application_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    application_type VARCHAR(50) NOT NULL DEFAULT 'confluence',
    direction VARCHAR(20) NOT NULL DEFAULT 'two-way',
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_application_links_type ON jira_admin.application_links(application_type);
