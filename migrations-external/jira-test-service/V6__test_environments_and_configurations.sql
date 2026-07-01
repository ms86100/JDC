-- V6__test_environments_and_configurations.sql
-- Test Service - Test environments and execution configurations

CREATE SCHEMA IF NOT EXISTS jira_test;

-- ============================================
-- TEST ENVIRONMENTS TABLE
-- Define test execution environments
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.test_environments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    environment_name VARCHAR(100) NOT NULL UNIQUE,
    environment_type VARCHAR(50) DEFAULT 'TEST', -- DEV, TEST, STAGING, PRODUCTION
    base_url VARCHAR(500),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    configuration JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEST ENVIRONMENT CONFIGS TABLE
-- Environment-specific configurations
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.environment_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    environment_id UUID REFERENCES jira_test.test_environments(id) ON DELETE CASCADE,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    is_secret BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (environment_id, config_key)
);

-- ============================================
-- TEST ENVIRONMENT ASSIGNMENTS TABLE
-- Assign environments to test executions
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.test_environment_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL,
    environment_id UUID REFERENCES jira_test.test_environments(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (test_id, environment_id)
);

-- ============================================
-- BROWSER CONFIGS TABLE
-- Browser configurations for UI test execution
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.browser_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    browser_name VARCHAR(50) NOT NULL,
    browser_version VARCHAR(20),
    platform VARCHAR(50),
    is_headless BOOLEAN DEFAULT FALSE,
    screen_resolution VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    configuration JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DEVICE CONFIGS TABLE
-- Mobile device configurations
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.device_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) DEFAULT 'ANDROID', -- ANDROID, IOS, TABLET
    os_version VARCHAR(50),
    screen_resolution VARCHAR(20),
    manufacturer VARCHAR(100),
    is_emulator BOOLEAN DEFAULT FALSE,
    is_default BOOLEAN DEFAULT FALSE,
    configuration JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEST EXECUTION MATRICES TABLE
-- Grid execution configurations
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.execution_matrices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matrix_name VARCHAR(100) NOT NULL,
    project_id UUID NOT NULL,
    environments JSONB,
    browsers JSONB,
    devices JSONB,
    is_parallel BOOLEAN DEFAULT TRUE,
    max_threads INTEGER DEFAULT 5,
    retry_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX jira_test. IF NOT EXISTS idx_test_environments_active ON jira_test.test_environments(is_active);
CREATE INDEX jira_test. IF NOT EXISTS idx_environment_configs_env ON jira_test.environment_configs(environment_id);
CREATE INDEX jira_test. IF NOT EXISTS idx_test_environment_assignments_test ON jira_test.test_environment_assignments(test_id);
CREATE INDEX jira_test. IF NOT EXISTS idx_browser_configs_default ON jira_test.browser_configs(is_default);
CREATE INDEX jira_test. IF NOT EXISTS idx_device_configs_default ON jira_test.device_configs(is_default);

-- ============================================
-- SEED DATA: Default Environments
-- ============================================
INSERT INTO jira_test..test_environments (id, environment_name, environment_type, base_url, description) VALUES
    (gen_random_uuid(), 'Development', 'DEV', 'http://dev.example.com', 'Development environment'),
    (gen_random_uuid(), 'QA Environment', 'TEST', 'http://test.example.com', 'QA testing environment'),
    (gen_random_uuid(), 'Staging', 'STAGING', 'http://staging.example.com', 'Pre-production staging')
ON CONFLICT (environment_name) DO NOTHING;

-- ============================================
-- SEED DATA: Default Browsers
-- ============================================
INSERT INTO jira_test..browser_configs (id, browser_name, browser_version, platform, is_default, is_active) VALUES
    (gen_random_uuid(), 'Chrome', 'latest', 'Windows', TRUE, TRUE),
    (gen_random_uuid(), 'Firefox', 'latest', 'Windows', FALSE, TRUE),
    (gen_random_uuid(), 'Safari', 'latest', 'macOS', FALSE, TRUE),
    (gen_random_uuid(), 'Edge', 'latest', 'Windows', FALSE, TRUE)
ON CONFLICT DO NOTHING;