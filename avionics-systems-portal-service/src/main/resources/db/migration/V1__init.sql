CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- V1__init.sql
-- Portal Service Database Schema (schema: jira_portal)

CREATE SCHEMA IF NOT EXISTS jira_portal;

-- ============================================
-- PORTAL PAGES TABLE
-- Configurable portal pages
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.portal_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_key VARCHAR(100) NOT NULL UNIQUE,
    page_title VARCHAR(255) NOT NULL,
    page_type VARCHAR(50) DEFAULT 'CUSTOM', -- WELCOME, PROJECT_LIST, DASHBOARD, CUSTOM
    layout_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    required_roles VARCHAR(50)[],
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- PORTAL WIDGETS TABLE
-- Widgets displayed on portal pages
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.portal_widgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id UUID NOT NULL REFERENCES jira_portal.portal_pages(id) ON DELETE CASCADE,
    widget_type VARCHAR(100) NOT NULL,
    widget_title VARCHAR(255),
    position_row INTEGER DEFAULT 0,
    position_col INTEGER DEFAULT 0,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    config JSONB,
    permissions JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- PORTAL ANNOUNCEMENTS TABLE
-- System-wide announcements
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.portal_announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    announcement_type VARCHAR(50) DEFAULT 'INFO', -- INFO, WARNING, ERROR, SUCCESS
    priority INTEGER DEFAULT 0,
    is_dismissible BOOLEAN DEFAULT TRUE,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    target_audience VARCHAR(50) DEFAULT 'ALL', -- ALL, ADMIN, USER, ANONYMOUS
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- PORTAL CONFIGURATION TABLE
-- Global portal settings
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.portal_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(50) DEFAULT 'STRING',
    category VARCHAR(50),
    description TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    updated_by UUID,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- PORTAL USER PREFERENCES TABLE
-- User-specific portal preferences
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, preference_key)
);

-- ============================================
-- PORTAL NAVIGATION TABLE
-- Custom navigation menus
-- ============================================
CREATE TABLE IF NOT EXISTS jira_portal.portal_navigation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_key VARCHAR(100) NOT NULL,
    menu_item_label VARCHAR(255) NOT NULL,
    menu_item_order INTEGER DEFAULT 0,
    menu_item_type VARCHAR(50) DEFAULT 'LINK', -- LINK, PAGE, DROPDOWN, DIVIDER
    parent_item_id UUID,
    target_url VARCHAR(500),
    target_page_id UUID,
    icon VARCHAR(50),
    roles VARCHAR(50)[],
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_portal_pages_active ON jira_portal.portal_pages(is_active);
CREATE INDEX IF NOT EXISTS idx_portal_pages_key ON jira_portal.portal_pages(page_key);
CREATE INDEX IF NOT EXISTS idx_portal_widgets_page ON jira_portal.portal_widgets(page_id);
CREATE INDEX IF NOT EXISTS idx_portal_announcements_active ON jira_portal.portal_announcements(is_active);
CREATE INDEX IF NOT EXISTS idx_portal_announcements_dates ON jira_portal.portal_announcements(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_portal_config_key ON jira_portal.portal_config(config_key);
CREATE INDEX IF NOT EXISTS idx_user_preferences_user ON jira_portal.user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_portal_navigation_menu ON jira_portal.portal_navigation(menu_key);

-- ============================================
-- SEED DATA: Default Portal Configuration
-- ============================================
INSERT INTO jira_portal.portal_config (config_key, config_value, config_type, category, description) VALUES
    ('portal.welcome.title', 'Welcome to Jira', 'STRING', 'branding', 'Welcome page title'),
    ('portal.logo.url', '/assets/logo.png', 'STRING', 'branding', 'Portal logo URL'),
    ('portal.theme.color', '#0052CC', 'STRING', 'branding', 'Primary theme color'),
    ('portal.footer.text', 'Powered by Jira Platform', 'STRING', 'footer', 'Footer text')
ON CONFLICT (config_key) DO NOTHING;