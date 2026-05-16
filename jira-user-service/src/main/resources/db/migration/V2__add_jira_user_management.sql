-- V2__add_jira_user_management.sql - Jira-style user management schema
-- Tables for Users, Groups, and Memberships (Atlassian Crowd compatible)

CREATE SCHEMA IF NOT EXISTS jira_admin;

-- User Directories (for managing multiple directory sources)
CREATE TABLE jira_admin.directories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    directory_name VARCHAR(255) NOT NULL,
    directory_type VARCHAR(50) NOT NULL DEFAULT 'INTERNAL',
    is_active BOOLEAN DEFAULT true,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_directories_active ON jira_admin.directories(is_active);
CREATE INDEX idx_directories_order ON jira_admin.directories(order_index);

-- Users table (compatible with Atlassian Crowd structure)
CREATE TABLE jira_admin.cwd_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    directory_id UUID NOT NULL REFERENCES jira_admin.directories(id) ON DELETE CASCADE,
    user_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    email_address VARCHAR(255),
    display_name VARCHAR(255),
    active BOOLEAN DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
   -- Atlassian-style attributes
    lower_user_name VARCHAR(255) NOT NULL,
    external_id VARCHAR(255),
    -- Login tracking
    failed_auth_count INTEGER DEFAULT 0,
    last_auth_date TIMESTAMP WITH TIME ZONE,
    credential_expire_date TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_cwd_user_directory_username ON jira_admin.cwd_user(directory_id, lower_user_name);
CREATE INDEX idx_cwd_user_email ON jira_admin.cwd_user(email_address);
CREATE INDEX idx_cwd_user_active ON jira_admin.cwd_user(active);
CREATE INDEX idx_cwd_user_lower_name ON jira_admin.cwd_user(lower_user_name);

-- Groups table
CREATE TABLE jira_admin.cwd_group (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    directory_id UUID NOT NULL REFERENCES jira_admin.directories(id) ON DELETE CASCADE,
    group_name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN DEFAULT true,
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    lower_group_name VARCHAR(255) NOT NULL,
    -- Group type flags (for Jira-style badges)
    is_global BOOLEAN DEFAULT false,
    is_system BOOLEAN DEFAULT false
);

CREATE UNIQUE INDEX idx_cwd_group_directory_name ON jira_admin.cwd_group(directory_id, lower_group_name);
CREATE INDEX idx_cwd_group_active ON jira_admin.cwd_group(active);
CREATE INDEX idx_cwd_group_name ON jira_admin.cwd_group(group_name);

-- Membership table (user-group relationships)
CREATE TABLE jira_admin.cwd_membership (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID NOT NULL, -- group_id
    child_id UUID NOT NULL, -- user_id or group_id (for nested groups)
    membership_type VARCHAR(20) NOT NULL DEFAULT 'GROUP_USER', -- GROUP_USER, GROUP_GROUP
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_parent FOREIGN KEY (parent_id) REFERENCES jira_admin.cwd_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_child FOREIGN KEY (child_id) REFERENCES jira_admin.cwd_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_membership UNIQUE (parent_id, child_id, membership_type)
);

CREATE INDEX idx_cwd_membership_parent ON jira_admin.cwd_membership(parent_id);
CREATE INDEX idx_cwd_membership_child ON jira_admin.cwd_membership(child_id);
CREATE INDEX idx_cwd_membership_type ON jira_admin.cwd_membership(membership_type);

-- Application access (Jira Software, Confluence, etc.)
CREATE TABLE jira_admin.application_access (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES jira_admin.cwd_user(id) ON DELETE CASCADE,
    application_key VARCHAR(100) NOT NULL DEFAULT 'jira-software',
    active BOOLEAN DEFAULT true,
    granted_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_app_access_user_app ON jira_admin.application_access(user_id, application_key);
CREATE INDEX idx_app_access_app ON jira_admin.application_access(application_key);

-- Login info tracking
CREATE TABLE jira_admin.login_info (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES jira_admin.cwd_user(id) ON DELETE CASCADE,
    login_count INTEGER DEFAULT 0,
    last_login_date TIMESTAMP WITH TIME ZONE,
    last_failed_login TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX idx_login_info_user ON jira_admin.login_info(user_id);

-- Notification schemes (for View Group page)
CREATE TABLE jira_admin.notification_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Permission schemes
CREATE TABLE jira_admin.permission_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Issue Security Schemes
CREATE TABLE jira_admin.issue_security_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Saved Filters
CREATE TABLE jira_admin.saved_filters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    owner_id UUID NOT NULL REFERENCES jira_admin.cwd_user(id) ON DELETE CASCADE,
    jql_query TEXT,
    is_shared BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Group-Scheme associations
CREATE TABLE jira_admin.group_schemes (
    group_id UUID NOT NULL REFERENCES jira_admin.cwd_group(id) ON DELETE CASCADE,
    scheme_type VARCHAR(50) NOT NULL, -- permission, notification, security
    scheme_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, scheme_type, scheme_id)
);

CREATE INDEX idx_group_schemes_group ON jira_admin.group_schemes(group_id);
CREATE INDEX idx_group_schemes_type ON jira_admin.group_schemes(scheme_type);

-- Trigger for updated_date
CREATE OR REPLACE FUNCTION jira_admin.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_date = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_cwd_user_updated_at
    BEFORE UPDATE ON jira_admin.cwd_user
    FOR EACH ROW EXECUTE FUNCTION jira_admin.update_updated_at_column();

CREATE TRIGGER update_cwd_group_updated_at
    BEFORE UPDATE ON jira_admin.cwd_group
    FOR EACH ROW EXECUTE FUNCTION jira_admin.update_updated_at_column();

CREATE TRIGGER update_directories_updated_at
    BEFORE UPDATE ON jira_admin.directories
    FOR EACH ROW EXECUTE FUNCTION jira_admin.update_updated_at_column();

-- Insert default internal directory
INSERT INTO jira_admin.directories (id, directory_name, directory_type, is_active, order_index)
VALUES ('00000000-0000-0000-0000-000000000001', 'Jira Internal Directory', 'INTERNAL', true, 0);

-- Insert default groups (Jira-style)
INSERT INTO jira_admin.cwd_group (id, directory_id, group_name, description, lower_group_name, is_global, is_system)
VALUES
    ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'jira-administrators', 'Jira administrators with full system access', 'jira-administrators', true, true),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001', 'jira-software-users', 'Default group for Jira Software users', 'jira-software-users', true, false),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'jira-system-administrators', 'System administrators with elevated privileges', 'jira-system-administrators', false, true);

-- Insert default admin user (password: admin123)
INSERT INTO jira_admin.cwd_user (id, directory_id, user_name, password_hash, email_address, display_name, active, lower_user_name, first_name, last_name)
VALUES
    ('00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000001', 'admin', '$2b$10$rOzJqQKG5dEJ3d5F5vQ0Oe5RVRh5vY5vY5vY5vY5vY5vY5vY5vY', 'admin@example.com', 'System Administrator', true, 'admin', 'System', 'Administrator'),
    ('90b5c96e-150e-495c-af46-1e8a1c952647', '00000000-0000-0000-0000-000000000001', 'ms86100', '$2b$10$OfGaCyc7YqCq2RnWrxtZOuP0PQRlQR1rGP./vxyFfThoz.5IFhCGG', 'ms86100@gmail.com', 'Sagar Sharma', true, 'ms86100', 'Sagar', 'Sharma');

-- Add users to groups
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type)
VALUES
    ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000100', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000100', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000100', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000010', '90b5c96e-150e-495c-af46-1e8a1c952647', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', '90b5c96e-150e-495c-af46-1e8a1c952647', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000012', '90b5c96e-150e-495c-af46-1e8a1c952647', 'GROUP_USER');

-- Add application access for users
INSERT INTO jira_admin.application_access (user_id, application_key, active)
VALUES
    ('00000000-0000-0000-0000-000000000100', 'jira-software', true),
    ('90b5c96e-150e-495c-af46-1e8a1c952647', 'jira-software', true);

-- Add login info
INSERT INTO jira_admin.login_info (user_id, login_count, last_login_date)
VALUES
    ('00000000-0000-0000-0000-000000000100', 5, CURRENT_TIMESTAMP),
    ('90b5c96e-150e-495c-af46-1e8a1c952647', 23, CURRENT_TIMESTAMP);