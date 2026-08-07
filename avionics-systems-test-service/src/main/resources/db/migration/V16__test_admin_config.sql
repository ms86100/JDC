-- Test Status Configuration
CREATE TABLE IF NOT EXISTS test_status_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID,
    name VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#6B7280',
    icon VARCHAR(50),
    category VARCHAR(20) DEFAULT 'TODO',
    is_default BOOLEAN DEFAULT false,
    is_final BOOLEAN DEFAULT false,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name)
);

-- Execution Status Configuration
CREATE TABLE IF NOT EXISTS execution_status_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID,
    name VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#6B7280',
    icon VARCHAR(50),
    is_pass BOOLEAN DEFAULT false,
    is_fail BOOLEAN DEFAULT false,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name)
);

-- Test Type Configuration
CREATE TABLE IF NOT EXISTS test_type_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID,
    name VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(50),
    color VARCHAR(20) DEFAULT '#6B7280',
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name)
);

-- Exploratory Session (Gap 7)
CREATE TABLE IF NOT EXISTS exploratory_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    charter TEXT,
    charter_goal VARCHAR(500),
    session_type VARCHAR(30) DEFAULT 'CHARTER_BASED',
    time_box_minutes INT DEFAULT 60,
    actual_duration_minutes INT,
    status VARCHAR(30) DEFAULT 'PLANNED',
    tester_id UUID,
    environment VARCHAR(200),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    bugs TEXT[],
    ideas TEXT[],
    questions TEXT[],
    evidence_links TEXT[],
    defect_keys TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test Project Settings (Gap 12)
CREATE TABLE IF NOT EXISTS test_project_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL UNIQUE,
    settings JSONB DEFAULT '{}',
    default_test_type VARCHAR(50) DEFAULT 'MANUAL',
    default_priority VARCHAR(50) DEFAULT 'MEDIUM',
    default_test_status VARCHAR(50) DEFAULT 'DRAFT',
    auto_create_execution BOOLEAN DEFAULT false,
    require_approval BOOLEAN DEFAULT false,
    retention_days INT DEFAULT 365,
    max_steps_per_test INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test Run Iteration (Gap 11)
CREATE TABLE IF NOT EXISTS test_run_iteration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_run_id UUID NOT NULL REFERENCES test_run(id),
    iteration_index INT NOT NULL DEFAULT 0,
    data_row JSONB,
    status VARCHAR(30) DEFAULT 'PENDING',
    step_statuses TEXT[],
    passed_steps INT DEFAULT 0,
    failed_steps INT DEFAULT 0,
    comment TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_test_status_config_active ON test_status_config(is_active);
CREATE INDEX IF NOT EXISTS idx_execution_status_config_active ON execution_status_config(is_active);
CREATE INDEX IF NOT EXISTS idx_test_type_config_active ON test_type_config(is_active);
CREATE INDEX IF NOT EXISTS idx_exploratory_session_project ON exploratory_session(project_id);
CREATE INDEX IF NOT EXISTS idx_exploratory_session_status ON exploratory_session(status);
CREATE INDEX IF NOT EXISTS idx_test_run_iteration_run ON test_run_iteration(test_run_id);

-- Seed default test statuses
INSERT INTO test_status_config (name, display_name, color, category, is_default, is_final, sort_order) VALUES
    ('DRAFT', 'Draft', '#9CA3AF', 'TODO', true, false, 1),
    ('READY', 'Ready', '#3B82F6', 'TODO', false, false, 2),
    ('APPROVED', 'Approved', '#10B981', 'DONE', false, true, 3),
    ('DEPRECATED', 'Deprecated', '#EF4444', 'DONE', false, true, 4)
ON CONFLICT (name) DO NOTHING;

-- Seed default execution statuses
INSERT INTO execution_status_config (name, display_name, color, is_pass, is_fail, sort_order) VALUES
    ('PASSED', 'Passed', '#10B981', true, false, 1),
    ('FAILED', 'Failed', '#EF4444', false, true, 2),
    ('BLOCKED', 'Blocked', '#F59E0B', false, false, 3),
    ('SKIPPED', 'Skipped', '#6B7280', false, false, 4),
    ('UNTESTED', 'Untested', '#D1D5DB', false, false, 5)
ON CONFLICT (name) DO NOTHING;

-- Seed default test types
INSERT INTO test_type_config (name, display_name, description, sort_order) VALUES
    ('MANUAL', 'Manual Test', 'Manual step-by-step execution', 1),
    ('AUTOMATED', 'Automated Test', 'CI/CD integrated automated test', 2),
    ('BDD', 'BDD / Cucumber', 'Behavior-driven development scenario', 3)
ON CONFLICT (name) DO NOTHING;
