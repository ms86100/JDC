-- V7__test_management_enterprise.sql
-- Phase 4.2: Test Step Engine (Parameterized, Shared Steps)
-- Phase 4.3: Precondition Module
-- Phase 4.7: Test Run Engine (Historical)
-- Phase 4.4: Test Repository Enhancement

-- ============================================
-- SHARED STEPS
-- ============================================
CREATE TABLE jira_issue.shared_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    folder_id UUID,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    tags TEXT[],
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE jira_issue.shared_step_items (
    shared_step_id UUID NOT NULL REFERENCES jira_issue.shared_steps(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    step_type VARCHAR(20) DEFAULT 'ACTION',
    description VARCHAR NOT NULL,
    test_data VARCHAR,
    expected_result VARCHAR,
    attachments TEXT,
    parameters TEXT DEFAULT '{}',
    is_enabled BOOLEAN DEFAULT TRUE,
    linked_shared_step_id UUID,
    condition VARCHAR(500),
    PRIMARY KEY (shared_step_id, item_order)
);

-- ============================================
-- TEST STEPS (Enhanced)
-- ============================================
CREATE TABLE jira_issue.test_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL,
    step_order INTEGER NOT NULL,
    step_type VARCHAR(20) DEFAULT 'ACTION',
    description VARCHAR NOT NULL,
    test_data TEXT,
    expected_result VARCHAR,
    attachments_required BOOLEAN DEFAULT FALSE,
    is_critical BOOLEAN DEFAULT FALSE,
    condition VARCHAR(500),
    estimated_duration_minutes INTEGER DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE jira_issue.test_step_attachments (
    test_step_id UUID NOT NULL REFERENCES jira_issue.test_steps(id) ON DELETE CASCADE,
    attachment TEXT NOT NULL,
    PRIMARY KEY (test_step_id, attachment)
);

CREATE TABLE jira_issue.test_step_data_variants (
    test_step_id UUID NOT NULL REFERENCES jira_issue.test_steps(id) ON DELETE CASCADE,
    variant_order INTEGER NOT NULL,
    data_value VARCHAR,
    PRIMARY KEY (test_step_id, variant_order)
);

-- ============================================
-- PRECONDITIONS
-- ============================================
CREATE TABLE jira_issue.preconditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    precondition_type VARCHAR(50) DEFAULT 'GENERIC',
    category VARCHAR(100) DEFAULT 'PREREQUISITE',
    definition TEXT,
    verification_query TEXT,
    expected_state TEXT,
    is_critical BOOLEAN DEFAULT FALSE,
    estimated_duration_minutes INTEGER DEFAULT 0,
    auto_fulfill BOOLEAN DEFAULT FALSE,
    fulfillment_script TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    folder_id UUID,
    tags TEXT[],
    usage_count INTEGER DEFAULT 0,
    last_verified_at TIMESTAMP,
    last_verified_by UUID,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE jira_issue.test_preconditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL,
    precondition_id UUID NOT NULL REFERENCES jira_issue.preconditions(id) ON DELETE CASCADE,
    execution_order INTEGER DEFAULT 0,
    is_optional BOOLEAN DEFAULT FALSE,
    link_type VARCHAR(50) DEFAULT 'REQUIRES',
    verified BOOLEAN DEFAULT FALSE,
    verified_by UUID,
    verified_at TIMESTAMP,
    UNIQUE (test_id, precondition_id)
);

CREATE TABLE jira_issue.precondition_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    precondition_id UUID NOT NULL REFERENCES jira_issue.preconditions(id) ON DELETE CASCADE,
    execution_id UUID,
    verified_by UUID NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20),
    notes TEXT,
    environment VARCHAR(100)
);

-- ============================================
-- TEST RUNS (Historical Execution Tracking)
-- ============================================
CREATE TABLE jira_issue.test_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID NOT NULL,
    test_id UUID NOT NULL,
    project_id UUID NOT NULL,
    test_version INTEGER,
    test_definition_snapshot TEXT,
    environment VARCHAR(100),
    release VARCHAR(100),
    sprint VARCHAR(100),
    build_url VARCHAR,
    build_number INTEGER,
    branch VARCHAR(255),
    commit_sha VARCHAR(40),
    ci_pipeline_id VARCHAR(255),
    run_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    duration_seconds BIGINT,
    executed_by UUID,
    executed_by_name VARCHAR(200),
    executor_type VARCHAR(20) DEFAULT 'MANUAL',
    test_data_variant VARCHAR(100),
    test_data_index INTEGER,
    total_steps INTEGER DEFAULT 0,
    passed_steps INTEGER DEFAULT 0,
    failed_steps INTEGER DEFAULT 0,
    blocked_steps INTEGER DEFAULT 0,
    skipped_steps INTEGER DEFAULT 0,
    step_results_json TEXT,
    evidence_summary TEXT,
    comment TEXT,
    retry_count INTEGER DEFAULT 0,
    parent_run_id UUID,
    linked_runs TEXT,
    raw_results TEXT,
    test_output_url VARCHAR,
    coverage_report_url VARCHAR,
    custom_properties TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE jira_issue.test_run_evidence (
    test_run_id UUID NOT NULL REFERENCES jira_issue.test_runs(id) ON DELETE CASCADE,
    evidence TEXT NOT NULL,
    PRIMARY KEY (test_run_id, evidence)
);

CREATE TABLE jira_issue.test_run_defects (
    test_run_id UUID NOT NULL REFERENCES jira_issue.test_runs(id) ON DELETE CASCADE,
    defect_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (test_run_id, defect_key)
);

CREATE TABLE jira_issue.test_run_comments (
    test_run_id UUID NOT NULL REFERENCES jira_issue.test_runs(id) ON DELETE CASCADE,
    comment_id VARCHAR(100) NOT NULL,
    comment_text TEXT,
    PRIMARY KEY (test_run_id, comment_id)
);

-- ============================================
-- TEST REPOSITORY ENHANCEMENTS
-- ============================================
CREATE TABLE jira_issue.test_repository_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id UUID NOT NULL,
    principal_type VARCHAR(20) NOT NULL, -- USER, GROUP, PROJECT_ROLE
    principal_id UUID NOT NULL,
    permission_level VARCHAR(20) NOT NULL, -- READ, WRITE, ADMIN
    granted_by UUID,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    UNIQUE (folder_id, principal_type, principal_id)
);

CREATE TABLE jira_issue.test_repository_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id UUID,
    test_id UUID,
    action VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500)
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_test_steps_test_id ON jira_issue.test_steps(test_id);
CREATE INDEX idx_test_steps_order ON jira_issue.test_steps(test_id, step_order);
CREATE INDEX idx_shared_steps_project ON jira_issue.shared_steps(project_id);
CREATE INDEX idx_shared_steps_usage ON jira_issue.shared_steps(usage_count DESC);
CREATE INDEX idx_preconditions_project ON jira_issue.preconditions(project_id);
CREATE INDEX idx_preconditions_type ON jira_issue.preconditions(precondition_type);
CREATE INDEX idx_test_preconditions_test ON jira_issue.test_preconditions(test_id);
CREATE INDEX idx_test_preconditions_precondition ON jira_issue.test_preconditions(precondition_id);
CREATE INDEX idx_test_runs_execution ON jira_issue.test_runs(execution_id);
CREATE INDEX idx_test_runs_test ON jira_issue.test_runs(test_id);
CREATE INDEX idx_test_runs_project ON jira_issue.test_runs(project_id);
CREATE INDEX idx_test_runs_environment ON jira_issue.test_runs(environment);
CREATE INDEX idx_test_runs_release ON jira_issue.test_runs(release);
CREATE INDEX idx_test_runs_status ON jira_issue.test_runs(status);
CREATE INDEX idx_test_runs_created ON jira_issue.test_runs(created_at DESC);
CREATE INDEX idx_test_runs_commit ON jira_issue.test_runs(commit_sha);
CREATE INDEX idx_test_runs_branch ON jira_issue.test_runs(branch);
CREATE INDEX idx_test_repository_audit_folder ON jira_issue.test_repository_audit(folder_id);
CREATE INDEX idx_test_repository_audit_test ON jira_issue.test_repository_audit(test_id);
CREATE INDEX idx_test_repository_audit_performed ON jira_issue.test_repository_audit(performed_at DESC);

-- ============================================
-- COMMENTS
-- ============================================
COMMENT ON TABLE jira_issue.test_runs IS 'CRITICAL: Historical test runs preserved forever. Same test executed 500 times across releases/environments/builds - all history preserved.';
COMMENT ON TABLE jira_issue.test_steps IS 'Enhanced test steps with parameterized data, attachments, and shared step linking.';