-- V9__assets_and_inventory.sql
-- Assets and Inventory management tables

-- Asset Types: define categories of assets with configurable attribute schemas
CREATE TABLE IF NOT EXISTS jira_admin.asset_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    attribute_schema JSONB DEFAULT '{}',
    permission_scheme JSONB DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_asset_types_active ON jira_admin.asset_types(is_active);

-- Assets: individual tracked assets with flexible attributes
CREATE TABLE IF NOT EXISTS jira_admin.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_type_id UUID NOT NULL REFERENCES jira_admin.asset_types(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    -- Values: ACTIVE, INACTIVE, MAINTENANCE, RETIRED, DISPOSED
    sub_status VARCHAR(50),
    location VARCHAR(255),
    attributes JSONB DEFAULT '{}',
    serial_number VARCHAR(100),
    qr_code_data TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assets_type ON jira_admin.assets(asset_type_id);
CREATE INDEX IF NOT EXISTS idx_assets_status ON jira_admin.assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_location ON jira_admin.assets(location);
CREATE INDEX IF NOT EXISTS idx_assets_serial ON jira_admin.assets(serial_number);
CREATE INDEX IF NOT EXISTS idx_assets_active ON jira_admin.assets(is_active);

-- Asset-Issue Links: track relationships between assets and issues
CREATE TABLE IF NOT EXISTS jira_admin.asset_issue_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES jira_admin.assets(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    link_type VARCHAR(30) DEFAULT 'RELATED',
    -- Values: RELATED, AFFECTED, USED_BY, INSTALLED_ON
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(asset_id, issue_id)
);

CREATE INDEX IF NOT EXISTS idx_asset_issue_links_asset ON jira_admin.asset_issue_links(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_issue_links_issue ON jira_admin.asset_issue_links(issue_id);
