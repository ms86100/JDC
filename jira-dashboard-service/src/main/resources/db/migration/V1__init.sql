CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- V1__init.sql
-- Dashboard Service Database Schema (schema: jira_dashboard)

CREATE SCHEMA IF NOT EXISTS jira_dashboard;

-- ============================================
-- DASHBOARDS TABLE
-- User-created dashboards for project/metric visualization
-- ============================================
CREATE TABLE IF NOT EXISTS jira_dashboard.dashboards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    project_id UUID,
    is_public BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    layout_config JSONB,
    refresh_interval INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DASHBOARD GADGETS TABLE
-- Individual gadgets/widgets on dashboards
-- ============================================
CREATE TABLE IF NOT EXISTS jira_dashboard.dashboard_gadgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES jira_dashboard.dashboards(id) ON DELETE CASCADE,
    gadget_type VARCHAR(100) NOT NULL,
    gadget_title VARCHAR(255),
    position_row INTEGER DEFAULT 0,
    position_col INTEGER DEFAULT 0,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    config JSONB,
    preferences JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DASHBOARD PERMISSIONS TABLE
-- Share dashboards with users/groups
-- ============================================
CREATE TABLE IF NOT EXISTS jira_dashboard.dashboard_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES jira_dashboard.dashboards(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- USER, GROUP, PROJECT
    entity_id UUID NOT NULL,
    permission_level VARCHAR(50) DEFAULT 'VIEW', -- VIEW, EDIT, ADMIN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DASHBOARD SHARING TABLE
-- Sharing history for dashboards
-- ============================================
CREATE TABLE IF NOT EXISTS jira_dashboard.dashboard_sharing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_id UUID NOT NULL REFERENCES jira_dashboard.dashboards(id) ON DELETE CASCADE,
    shared_with_user_id UUID,
    shared_with_email VARCHAR(255),
    share_token VARCHAR(255),
    permission_level VARCHAR(50) DEFAULT 'VIEW',
    expires_at TIMESTAMP,
    accessed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- GADGET PREFERENCES TABLE
-- User-specific gadget preferences
-- ============================================
CREATE TABLE IF NOT EXISTS jira_dashboard.gadget_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gadget_id UUID NOT NULL REFERENCES jira_dashboard.dashboard_gadgets(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    preferences JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(gadget_id, user_id)
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_dashboards_owner ON jira_dashboard.dashboards(owner_id);
CREATE INDEX IF NOT EXISTS idx_dashboards_project ON jira_dashboard.dashboards(project_id);
CREATE INDEX IF NOT EXISTS idx_dashboards_favorite ON jira_dashboard.dashboards(is_favorite);
CREATE INDEX IF NOT EXISTS idx_gadgets_dashboard ON jira_dashboard.dashboard_gadgets(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_permissions_dashboard ON jira_dashboard.dashboard_permissions(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_sharing_dashboard ON jira_dashboard.dashboard_sharing(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_preferences_gadget ON jira_dashboard.gadget_preferences(gadget_id);

-- ============================================
-- SEED DATA: Default Dashboard Templates
-- ============================================
INSERT INTO jira_dashboard.dashboards (id, name, description, owner_id, is_public, layout_config)
VALUES
    (gen_random_uuid(), 'Project Overview', 'Default project dashboard template', NULL, true,
     '{"columns": 3, "rows": 2, "layout": "grid"}'),
    (gen_random_uuid(), 'Sprint Board', 'Sprint progress and burndown', NULL, false,
     '{"columns": 2, "rows": 2, "layout": "board"}')
ON CONFLICT DO NOTHING;