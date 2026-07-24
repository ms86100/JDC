CREATE TABLE IF NOT EXISTS jira_issue.external_page_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    url TEXT NOT NULL,
    title VARCHAR(500),
    application_link_id UUID,
    page_id VARCHAR(200),
    space_key VARCHAR(100),
    linked_by UUID,
    linked_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_page_links_entity ON jira_issue.external_page_links(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_page_links_url ON jira_issue.external_page_links(url);
