-- V2__gadgets_and_chart_enhancements.sql
-- Creates gadget catalog and gadget instance tables, plus custom chart enhancements

-- Gadget catalog: defines available gadget types
CREATE TABLE IF NOT EXISTS jira_dashboard.gadgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    module_key VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    thumbnail_url VARCHAR(500),
    config_schema TEXT,
    config_defaults TEXT,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    permission_type VARCHAR(100) DEFAULT 'PROJECT',
    api_version VARCHAR(20) DEFAULT '1.0'
);

CREATE INDEX IF NOT EXISTS idx_gadget_category ON jira_dashboard.gadgets(category);
CREATE INDEX IF NOT EXISTS idx_gadget_module_key ON jira_dashboard.gadgets(module_key);

-- Dashboard sharing records
CREATE TABLE IF NOT EXISTS jira_dashboard.dashboard_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES jira_dashboard.dashboards(id) ON DELETE CASCADE,
    share_type VARCHAR(50) NOT NULL,
    share_target VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dashboard_shares_dashboard ON jira_dashboard.dashboard_shares(dashboard_id);

-- Gadget instances: placed gadgets on dashboards
CREATE TABLE IF NOT EXISTS jira_dashboard.gadget_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES jira_dashboard.dashboards(id) ON DELETE CASCADE,
    gadget_id UUID NOT NULL REFERENCES jira_dashboard.gadgets(id),
    title VARCHAR(255),
    position_row INTEGER NOT NULL DEFAULT 0,
    position_column INTEGER NOT NULL DEFAULT 0,
    width INTEGER NOT NULL DEFAULT 1,
    height INTEGER NOT NULL DEFAULT 1,
    config TEXT,
    filters TEXT,
    color VARCHAR(7) DEFAULT '#ffffff',
    is_minimized BOOLEAN NOT NULL DEFAULT FALSE,
    is_collapsed BOOLEAN NOT NULL DEFAULT FALSE,
    -- Custom chart enhancement columns
    chart_type VARCHAR(30),
    chart_config JSONB DEFAULT '{}',
    reference_id VARCHAR(50),
    data_source_jql TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gadget_instance_dashboard_id ON jira_dashboard.gadget_instances(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_gadget_instance_gadget_id ON jira_dashboard.gadget_instances(gadget_id);
CREATE INDEX IF NOT EXISTS idx_gadget_instance_chart_type ON jira_dashboard.gadget_instances(chart_type);
