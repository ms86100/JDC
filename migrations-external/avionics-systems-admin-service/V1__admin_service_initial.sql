-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;


-- V1__admin_service_initial.sql
-- Admin Service Database Schema

-- System Settings Table
CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    data_type VARCHAR(50),
    is_sensitive BOOLEAN DEFAULT FALSE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_settings_category ON system_settings(category);
CREATE INDEX IF NOT EXISTS idx_settings_key ON system_settings(setting_key);

-- Admin Users Table
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    avatar_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT FALSE,
    timezone VARCHAR(100) DEFAULT 'UTC',
    language VARCHAR(20) DEFAULT 'en-US',
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON admin_users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON admin_users(status);
CREATE INDEX IF NOT EXISTS idx_users_role ON admin_users(role);

-- User Preferences Table
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    preference_key VARCHAR(255) NOT NULL,
    preference_value TEXT,
    PRIMARY KEY (user_id, preference_key)
);

-- Projects Admin Table
CREATE TABLE IF NOT EXISTS projects_admin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) DEFAULT 'SOFTWARE',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    lead_user_id UUID,
    default_assignee VARCHAR(255),
    default_priority VARCHAR(50),
    default_issue_type VARCHAR(50),
    allow_sub_tasks BOOLEAN DEFAULT TRUE,
    allow_attachments BOOLEAN DEFAULT TRUE,
    allow_comments BOOLEAN DEFAULT TRUE,
    max_attachments INTEGER DEFAULT 10,
    workflow_scheme VARCHAR(255),
    issue_type_scheme VARCHAR(255),
    field_config_scheme VARCHAR(255),
    project_level VARCHAR(50) DEFAULT 'PROJECT',
    enable_notifications BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_projects_key ON projects_admin(project_key);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects_admin(status);
CREATE INDEX IF NOT EXISTS idx_projects_type ON projects_admin(type);

-- Project Notification Events Table
CREATE TABLE IF NOT EXISTS project_notification_events (
    project_id UUID NOT NULL REFERENCES projects_admin(id) ON DELETE CASCADE,
    event VARCHAR(100) NOT NULL,
    PRIMARY KEY (project_id, event)
);

-- Appearance Settings Table
CREATE TABLE IF NOT EXISTS appearance_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    app_name VARCHAR(255) DEFAULT 'Jira Clone',
    login_page_message TEXT,
    footer_message TEXT,
    theme VARCHAR(50) DEFAULT 'light',
    theme_config TEXT,
    color_scheme VARCHAR(100),
    fonts TEXT,
    use_system_font BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Licenses Table
CREATE TABLE IF NOT EXISTS licenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    license_type VARCHAR(100),
    license_key TEXT,
    max_users INTEGER,
    max_projects INTEGER,
    purchase_date TIMESTAMP,
    expiry_date TIMESTAMP,
    support_entitlement VARCHAR(255),
    metadata TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Seed Data: Default Settings
INSERT INTO system_settings (setting_key, setting_value, description, category, data_type, is_sensitive) VALUES
    ('application.title', 'Jira Clone', 'Application Title', 'general', 'string', FALSE),
    ('application.baseUrl', 'http://localhost:3000', 'Base URL', 'general', 'string', FALSE),
    ('application.adminEmail', 'admin@example.com', 'Admin Email', 'general', 'string', TRUE),
    ('application.dateFormat', 'MMM dd, yyyy', 'Date Format', 'general', 'string', FALSE),
    ('application.timeZone', 'UTC', 'Time Zone', 'general', 'string', FALSE),
    ('security.allowSignUp', 'true', 'Allow User Registration', 'security', 'boolean', FALSE),
    ('security.requireEmailVerification', 'false', 'Require Email Verification', 'security', 'boolean', FALSE),
    ('security.enableTwoFactor', 'false', 'Enable 2FA', 'security', 'boolean', FALSE),
    ('security.passwordMinLength', '8', 'Minimum Password Length', 'security', 'number', FALSE),
    ('security.sessionTimeout', '30', 'Session Timeout (minutes)', 'security', 'number', FALSE),
    ('email.enabled', 'true', 'Enable Email', 'email', 'boolean', FALSE),
    ('email.smtpHost', 'smtp.example.com', 'SMTP Host', 'email', 'string', TRUE),
    ('email.smtpPort', '587', 'SMTP Port', 'email', 'number', FALSE),
    ('email.smtpUsername', '', 'SMTP Username', 'email', 'string', TRUE),
    ('email.smtpPassword', '', 'SMTP Password', 'email', 'password', TRUE),
    ('email.from', 'noreply@example.com', 'From Address', 'email', 'string', TRUE),
    ('email.ssl', 'true', 'Use SSL/TLS', 'email', 'boolean', FALSE),
    ('attachments.maxSize', '10485760', 'Max Attachment Size (bytes)', 'attachments', 'number', FALSE),
    ('attachments.allowedTypes', 'jpg,png,pdf,doc,docx,xls,xlsx', 'Allowed File Types', 'attachments', 'string', FALSE),
    ('api.enabled', 'true', 'Enable API', 'api', 'boolean', FALSE),
    ('api.rateLimit', '1000', 'API Rate Limit (per hour)', 'api', 'number', FALSE),
    ('logging.level', 'INFO', 'Log Level', 'logging', 'string', FALSE),
    ('logging.audit', 'true', 'Enable Audit Logging', 'logging', 'boolean', FALSE)
ON CONFLICT (setting_key) DO NOTHING;

-- Seed Data: Default Appearance
INSERT INTO appearance_settings (id, logo_url, favicon_url, app_name, login_page_message, footer_message, theme, theme_config, color_scheme, fonts, use_system_font)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '/assets/logo.png',
    '/assets/favicon.ico',
    'Jira Clone',
    'Welcome to Jira Clone - Your Project Management Solution',
    'Powered by Jira Clone Platform',
    'light',
    '{"primaryColor":"#0052CC","secondaryColor":"#6C757D","accentColor":"#00B8D9"}',
    'default',
    '{"primaryFont":"Inter","monospaceFont":"JetBrains Mono","baseFontSize":"14px"}',
    FALSE
) ON CONFLICT (id) DO NOTHING;

-- Seed Data: Default License
INSERT INTO licenses (license_type, max_users, max_projects, purchase_date, expiry_date, support_entitlement)
VALUES (
    'Standard',
    100,
    50,
    NOW() - INTERVAL '1 year',
    NOW() + INTERVAL '6 months',
    'Standard Support'
) ON CONFLICT DO NOTHING;

-- Seed Data: Admin User (password: admin123)
INSERT INTO admin_users (id, username, email, display_name, password_hash, status, role, email_verified)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@example.com',
    'Administrator',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzjGQz8wTdLQnMQ.xQ9h7z7dCq',
    'ACTIVE',
    'ADMIN',
    TRUE
) ON CONFLICT (username) DO NOTHING;