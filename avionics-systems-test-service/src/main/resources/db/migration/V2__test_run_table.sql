-- V2: Test Run table for individual test execution instances
-- Tracks each historically-persisted run of a test

CREATE TABLE test_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES test_issue(id) ON DELETE CASCADE,
    execution_id UUID REFERENCES test_execution(id) ON DELETE SET NULL,
    project_id UUID NOT NULL,
    status VARCHAR(100) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, PASSED, FAILED, BLOCKED, SKIPPED

    executed_by UUID,
    executed_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    duration INTEGER, -- in seconds

    comment TEXT,
    defect_keys TEXT, -- comma-separated Jira issue keys

    -- Step results for this run as array
    step_statuses TEXT[], -- Array of PASSED/FAILED/BLOCKED per step
    passed_steps INTEGER DEFAULT 0,
    failed_steps INTEGER DEFAULT 0,
    blocked_steps INTEGER DEFAULT 0,
    total_steps INTEGER DEFAULT 0,

    -- Environment info
    environment VARCHAR(200),
    browser VARCHAR(200),
    platform VARCHAR(200),
    test_data TEXT,

    -- Links to evidence
    evidence_links TEXT[],

    logs TEXT,
    error_message TEXT,

    -- Whether this run is a retry
    is_retry BOOLEAN DEFAULT FALSE,
    parent_run_id UUID REFERENCES test_run(id) ON DELETE SET NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_test_run_test ON test_run(test_id);
CREATE INDEX idx_test_run_execution ON test_run(execution_id);
CREATE INDEX idx_test_run_project ON test_run(project_id);
CREATE INDEX idx_test_run_status ON test_run(status);
CREATE INDEX idx_test_run_executed_by ON test_run(executed_by);
CREATE INDEX idx_test_run_executed_at ON test_run(executed_at DESC);
CREATE INDEX idx_test_run_env ON test_run(environment);

-- Composite indexes for common query patterns
CREATE INDEX idx_test_run_test_status ON test_run(test_id, status);
CREATE INDEX idx_test_run_test_executed ON test_run(test_id, executed_at DESC);
CREATE INDEX idx_test_run_project_env ON test_run(project_id, environment);