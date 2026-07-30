-- V6__native_test_management.sql
-- Native Test Management Integration
-- Tests become first-class Jira issues with test-specific fields
-- This makes test management fully integrated into the core platform

-- ============================================
-- ENHANCE EXISTING ISSUES TABLE WITH TEST FIELDS
-- ============================================

-- Test-specific fields for when issue_type = 'Test'
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_type VARCHAR(50) DEFAULT 'MANUAL'; -- MANUAL, AUTOMATED, BDD
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_status VARCHAR(30) DEFAULT 'DRAFT'; -- DRAFT, READY, APPROVED, DEPRECATED
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_priority VARCHAR(20); -- HIGH, MEDIUM, LOW, CRITICAL
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_owner_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_steps JSONB; -- Array of test steps as JSON
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS requirement_keys TEXT[]; -- Array of linked requirement issue keys
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_feature_key VARCHAR(255); -- Cucumber feature file reference
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_scenario_id VARCHAR(255); -- Cucumber scenario ID
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_set_id UUID; -- Parent test set
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_plan_id UUID; -- Parent test plan
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_execution_id UUID; -- Last execution ID
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_repository_folder_id UUID; -- Repository folder

-- ============================================
-- TEST ISSUE TYPE
-- ============================================

-- Seed the Test issue type (if not exists)
INSERT INTO jira_issue.issue_types (id, name, issue_type_key, icon, description, is_subtask, sequence) VALUES
    ('type-test', 'Test', 'test', 'test', 'A test case with steps, requirements, and execution tracking', FALSE, 4)
ON CONFLICT DO NOTHING;

-- ============================================
-- TEST REPOSITORY FOLDERS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_repository_folders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    parent_folder_id UUID REFERENCES jira_issue.test_repository_folders(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    path VARCHAR, -- Full path like "/Folder1/Folder2/SubFolder"
    depth INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    is_smart_folder BOOLEAN DEFAULT FALSE,
    smart_folder_query TEXT, -- JQL query for smart folders
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_folder_name_per_parent UNIQUE (parent_folder_id, name)
);

CREATE INDEX idx_trf_project ON jira_issue.test_repository_folders(project_id);
CREATE INDEX idx_trf_parent ON jira_issue.test_repository_folders(parent_folder_id);
CREATE INDEX idx_trf_path ON jira_issue.test_repository_folders(path);
CREATE INDEX idx_trf_smart ON jira_issue.test_repository_folders(is_smart_folder);

-- ============================================
-- TEST SETS (Groups of Tests)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_sets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    folder_id UUID REFERENCES jira_issue.test_repository_folders(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    test_type VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, AUTOMATED, MIXED, BDD
    labels TEXT[],
    test_count INTEGER DEFAULT 0,
    requirement_keys TEXT[], -- Requirements covered by this test set
    status VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, READY, ACTIVE, COMPLETED
    owner_id UUID,
    created_by UUID,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_testset_name_per_project UNIQUE (project_id, name)
);

CREATE INDEX idx_ts_project ON jira_issue.test_sets(project_id);
CREATE INDEX idx_ts_folder ON jira_issue.test_sets(folder_id);
CREATE INDEX idx_ts_labels ON jira_issue.test_sets USING GIN(labels);
CREATE INDEX idx_ts_status ON jira_issue.test_sets(status);

-- ============================================
-- TEST PLANS (Containers for Test Set Executions)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    test_type VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, AUTOMATED, MIXED
    labels TEXT[],
    status VARCHAR(30) DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    start_date DATE,
    end_date DATE,
    target_version VARCHAR(100), -- Target release version
    environment VARCHAR(50), -- DEV, STAGING, PROD
    owner_id UUID,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tp_project ON jira_issue.test_plans(project_id);
CREATE INDEX idx_tp_status ON jira_issue.test_plans(status);
CREATE INDEX idx_tp_dates ON jira_issue.test_plans(start_date, end_date);
CREATE INDEX idx_tp_labels ON jira_issue.test_plans USING GIN(labels);

-- ============================================
-- TEST SETS IN PLANS (Many-to-Many)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_plan_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_plan_id UUID NOT NULL REFERENCES jira_issue.test_plans(id) ON DELETE CASCADE,
    test_set_id UUID NOT NULL REFERENCES jira_issue.test_sets(id) ON DELETE CASCADE,
    execution_order INTEGER DEFAULT 0,
    added_by UUID,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_plan_set UNIQUE (test_plan_id, test_set_id)
);

CREATE INDEX idx_tpi_plan ON jira_issue.test_plan_items(test_plan_id);
CREATE INDEX idx_tpi_set ON jira_issue.test_plan_items(test_set_id);

-- ============================================
-- TEST EXECUTIONS (Single Test Run)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_executions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    test_plan_id UUID REFERENCES jira_issue.test_plans(id) ON DELETE SET NULL,
    test_set_id UUID REFERENCES jira_issue.test_sets(id) ON DELETE SET NULL,
    -- test_id can be NULL if running entire test set
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'RUNNING', -- RUNNING, PASSED, FAILED, BLOCKED, ABORTED, SKIPPED
    test_env VARCHAR(50), -- DEV, STAGING, PROD, CUSTOM
    tester_id UUID,
    test_cycle VARCHAR(100), -- e.g., "Sprint 23", "Release 2.1"
    sprint_id UUID,
    -- CI/CD Integration
    ci_build_url VARCHAR(500),
    ci_job_name VARCHAR(255),
    ci_build_number VARCHAR(100),
    ci_job_id VARCHAR(255),
    branch VARCHAR(255),
    commit_sha VARCHAR(100),
    -- Test Results Summary
    total_tests INTEGER DEFAULT 0,
    passed_tests INTEGER DEFAULT 0,
    failed_tests INTEGER DEFAULT 0,
    blocked_tests INTEGER DEFAULT 0,
    skipped_tests INTEGER DEFAULT 0,
    not_run_tests INTEGER DEFAULT 0,
    -- Timing
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    duration_seconds BIGINT,
    -- Traceability
    requirement_keys TEXT[],
    defect_keys TEXT[],
    -- Evidence
    test_report_url VARCHAR(500),
    attachment_ids UUID[],
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_te_project ON jira_issue.test_executions(project_id);
CREATE INDEX idx_te_plan ON jira_issue.test_executions(test_plan_id);
CREATE INDEX idx_te_set ON jira_issue.test_executions(test_set_id);
CREATE INDEX idx_te_tester ON jira_issue.test_executions(tester_id);
CREATE INDEX idx_te_status ON jira_issue.test_executions(status);
CREATE INDEX idx_te_started ON jira_issue.test_executions(started_at);
CREATE INDEX idx_te_ci ON jira_issue.test_executions(ci_build_url);

-- ============================================
-- STEP RESULTS (Per-Step Execution Result)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.step_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL REFERENCES jira_issue.test_executions(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE, -- The test issue
    step_order INTEGER NOT NULL,
    step_type VARCHAR(20) NOT NULL, -- GIVEN, WHEN, THEN, AND, BUT
    step_description TEXT NOT NULL,
    expected_result TEXT,
    -- Execution Result
    status VARCHAR(20) DEFAULT 'NOT_RUN', -- PASSED, FAILED, BLOCKED, SKIPPED, NOT_RUN
    actual_result TEXT,
    -- Defect Linkage
    defect_key VARCHAR(100),
    defect_severity VARCHAR(20), -- CRITICAL, MAJOR, MINOR
    -- Evidence
    evidence_ids UUID[], -- Attachment IDs
    evidence_comments TEXT[],
    -- Timing
    executed_at TIMESTAMP,
    execution_time_ms BIGINT,
    -- Additional Context
    comment TEXT,
    screenshots TEXT[], -- Screenshot URLs
    logs TEXT[], -- Log file URLs
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sr_execution ON jira_issue.step_results(execution_id);
CREATE INDEX idx_sr_issue ON jira_issue.step_results(issue_id);
CREATE INDEX idx_sr_status ON jira_issue.step_results(status);
CREATE INDEX idx_sr_defect ON jira_issue.step_results(defect_key);

-- ============================================
-- TEST ENVIRONMENTS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_environments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    environment_type VARCHAR(50), -- DEV, STAGING, PROD, CUSTOM
    config JSONB, -- Environment configuration as JSON
    url VARCHAR(500), -- Base URL for this environment
    variables JSONB, -- Environment variables
    credentials JSONB, -- Encrypted credentials (only accessible to admins)
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INTEGER DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_env_name_per_project UNIQUE (project_id, name)
);

CREATE INDEX idx_te_project ON jira_issue.test_environments(project_id);
CREATE INDEX idx_te_active ON jira_issue.test_environments(is_active);

-- ============================================
-- TEST DATASETS (Data Tables for Data-Driven Testing)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_datasets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data_format VARCHAR(50) DEFAULT 'TABLE', -- TABLE, CSV, JSON, EXCEL
    column_names TEXT[], -- Column headers
    column_types VARCHAR(50)[], -- Data types: STRING, NUMBER, DATE, BOOLEAN
    rows_count INTEGER DEFAULT 0,
    data JSONB, -- All data as JSON array of objects
    csv_data TEXT, -- Raw CSV data
    -- Usage tracking
    used_in_tests INTEGER DEFAULT 0,
    used_in_automations INTEGER DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_td_project ON jira_issue.test_datasets(project_id);

-- ============================================
-- CUCUMBER/GHERKIN SCENARIOS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.cucumber_scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_key VARCHAR(255) NOT NULL, -- Unique: "filename::feature_name"
    feature_file VARCHAR(500) NOT NULL,
    feature_name VARCHAR(500) NOT NULL,
    scenario_name VARCHAR(500) NOT NULL,
    scenario_key VARCHAR(500) NOT NULL, -- Full key: "project-key::feature_name::scenario_name"
    scenario_type VARCHAR(50) DEFAULT 'Scenario', -- Scenario, Scenario Outline
    background TEXT, -- Shared background steps
    tags TEXT[], -- @smoke @regression etc
    examples JSONB, -- Examples table for Scenario Outline
    line_number INTEGER,
    issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE SET NULL,
    test_set_id UUID REFERENCES jira_issue.test_sets(id) ON DELETE SET NULL,
    imported_at TIMESTAMP NOT NULL DEFAULT NOW(),
    import_batch_id UUID,
    CONSTRAINT unique_scenario_key UNIQUE (scenario_key)
);

CREATE INDEX idx_cs_feature ON jira_issue.cucumber_scenarios(feature_key);
CREATE INDEX idx_cs_issue ON jira_issue.cucumber_scenarios(issue_id);
CREATE INDEX idx_cs_tags ON jira_issue.cucumber_scenarios USING GIN(tags);
CREATE INDEX idx_cs_imported ON jira_issue.cucumber_scenarios(imported_at);

-- ============================================
-- CUCUMBER FEATURES (Feature File Metadata)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.cucumber_features (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_key VARCHAR(255) NOT NULL UNIQUE,
    feature_file VARCHAR(500) NOT NULL,
    feature_name VARCHAR(500) NOT NULL,
    feature_tags TEXT[],
    background TEXT,
    language VARCHAR(10) DEFAULT 'en',
    scenario_count INTEGER DEFAULT 0,
    test_set_id UUID REFERENCES jira_issue.test_sets(id) ON DELETE SET NULL,
    raw_content TEXT, -- Full feature file content
    import_batch_id UUID,
    imported_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cf_key ON jira_issue.cucumber_features(feature_key);

-- ============================================
-- REQUIREMENT LINKS (Traceability)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.requirement_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_key VARCHAR(255) NOT NULL, -- Jira issue key like "PROJ-123"
    requirement_summary TEXT,
    requirement_type VARCHAR(50), -- EPIC, STORY, REQUIREMENT, BUG
    test_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    coverage_status VARCHAR(20) DEFAULT 'COVERED', -- COVERED, PARTIAL, NOT_COVERED
    last_test_execution_id UUID REFERENCES jira_issue.test_executions(id) ON DELETE SET NULL,
    last_execution_status VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT unique_req_test_link UNIQUE (requirement_key, test_issue_id)
);

CREATE INDEX idx_rl_req ON jira_issue.requirement_links(requirement_key);
CREATE INDEX idx_rl_test ON jira_issue.requirement_links(test_issue_id);
CREATE INDEX idx_rl_status ON jira_issue.requirement_links(coverage_status);

-- ============================================
-- DEFECT LINKS (Failed Test to Defect Linkage)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.defect_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    defect_key VARCHAR(100) NOT NULL, -- Jira issue key like "PROJ-456"
    defect_summary TEXT,
    defect_type VARCHAR(50) DEFAULT 'BUG', -- BUG, IMPROVEMENT, STORY
    test_execution_id UUID REFERENCES jira_issue.test_executions(id) ON DELETE CASCADE,
    step_result_id UUID REFERENCES jira_issue.step_results(id) ON DELETE CASCADE,
    test_issue_id UUID REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    severity VARCHAR(20), -- CRITICAL, MAJOR, MINOR, TRIVIAL
    status VARCHAR(30), -- OPEN, IN_PROGRESS, REOPENED, CLOSED
    priority VARCHAR(20), -- P1, P2, P3, P4
    linked_by UUID,
    linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_defect_link UNIQUE (defect_key, test_execution_id, step_result_id)
);

CREATE INDEX idx_dl_defect ON jira_issue.defect_links(defect_key);
CREATE INDEX idx_dl_execution ON jira_issue.defect_links(test_execution_id);
CREATE INDEX idx_dl_status ON jira_issue.defect_links(status);

-- ============================================
-- CI/CD IMPORT BATCHES (Import Audit Trail)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_import_batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    import_type VARCHAR(30) NOT NULL, -- JUNIT, CUCUMBER, TESTNG, NUNIT, ROBOT
    ci_source VARCHAR(100), -- JENKINS, GITHUB_ACTIONS, GITLAB_CI, BAMBOO, CIRCLECI, AZURE_DEVOPS
    ci_build_url VARCHAR(500),
    ci_job_name VARCHAR(255),
    ci_build_number VARCHAR(100),
    ci_job_id VARCHAR(255),
    branch VARCHAR(255),
    commit_sha VARCHAR(100),
    commit_message TEXT,
    -- Import Results
    total_tests INTEGER DEFAULT 0,
    total_passed INTEGER DEFAULT 0,
    total_failed INTEGER DEFAULT 0,
    total_skipped INTEGER DEFAULT 0,
    tests_created INTEGER DEFAULT 0,
    tests_updated INTEGER DEFAULT 0,
    executions_created INTEGER DEFAULT 0,
    -- Status
    status VARCHAR(30) DEFAULT 'PROCESSING', -- QUEUED, PROCESSING, COMPLETED, FAILED, PARTIAL
    error_message TEXT,
    warnings TEXT[],
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP
);

CREATE INDEX idx_tib_type ON jira_issue.test_import_batches(import_type);
CREATE INDEX idx_tib_source ON jira_issue.test_import_batches(ci_source);
CREATE INDEX idx_tib_status ON jira_issue.test_import_batches(status);
CREATE INDEX idx_tib_created ON jira_issue.test_import_batches(created_at);
CREATE INDEX idx_tib_ci ON jira_issue.test_import_batches(ci_build_url);

-- ============================================
-- TEST EXECUTION HISTORY (Historical Tracking)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_execution_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    execution_id UUID NOT NULL REFERENCES jira_issue.test_executions(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL, -- PASSED, FAILED, BLOCKED, SKIPPED, NOT_RUN
    executed_by UUID,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    test_env VARCHAR(50),
    duration_ms BIGINT,
    issue_snapshot JSONB, -- Snapshot of issue state at execution time
    CONSTRAINT unique_exec_history UNIQUE (test_issue_id, execution_id)
);

CREATE INDEX idx_teh_test ON jira_issue.test_execution_history(test_issue_id);
CREATE INDEX idx_teh_execution ON jira_issue.test_execution_history(execution_id);
CREATE INDEX idx_teh_status ON jira_issue.test_execution_history(status);
CREATE INDEX idx_teh_executed ON jira_issue.test_execution_history(executed_at);

-- ============================================
-- TEST EVIDENCE (Attachments/Logs for Executions)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_evidence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    step_result_id UUID REFERENCES jira_issue.step_results(id) ON DELETE CASCADE,
    execution_id UUID REFERENCES jira_issue.test_executions(id) ON DELETE CASCADE,
    evidence_type VARCHAR(50) NOT NULL, -- SCREENSHOT, VIDEO, LOG, REPORT, FILE, COMMENT
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    url VARCHAR(500), -- CDN or storage URL
    content TEXT, -- For inline comments/notes
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_te_step ON jira_issue.test_evidence(step_result_id);
CREATE INDEX idx_te_execution ON jira_issue.test_evidence(execution_id);
CREATE INDEX idx_te_type ON jira_issue.test_evidence(evidence_type);

-- ============================================
-- SHARED STEPS (Reusable Test Steps)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.shared_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    step_type VARCHAR(20) NOT NULL, -- GIVEN, WHEN, THEN, AND, BUT
    description_template TEXT NOT NULL, -- Template with placeholders
    test_data_template TEXT, -- Optional test data template
    expected_result_template TEXT, -- Optional expected result template
    parameters JSONB, -- Parameter definitions
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_shared_step UNIQUE (project_id, name)
);

CREATE INDEX idx_ss_project ON jira_issue.shared_steps(project_id);
CREATE INDEX idx_ss_name ON jira_issue.shared_steps(name);

-- ============================================
-- SHARED STEP USAGE (Which Tests Use Which Shared Steps)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.shared_step_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shared_step_id UUID NOT NULL REFERENCES jira_issue.shared_steps(id) ON DELETE CASCADE,
    test_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_ss_usage UNIQUE (shared_step_id, test_issue_id)
);

CREATE INDEX idx_ssu_shared ON jira_issue.shared_step_usage(shared_step_id);
CREATE INDEX idx_ssu_test ON jira_issue.shared_step_usage(test_issue_id);

-- ============================================
-- TEST VERSIONING (Issue Version History)
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.test_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    test_steps JSONB NOT NULL, -- Snapshot of test steps
    change_summary TEXT,
    changed_by UUID,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_test_version UNIQUE (test_issue_id, version_number)
);

CREATE INDEX idx_tv_test ON jira_issue.test_versions(test_issue_id);
CREATE INDEX idx_tv_version ON jira_issue.test_versions(version_number);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS idx_issues_test_type ON jira_issue.issues(test_type);
CREATE INDEX IF NOT EXISTS idx_issues_test_status ON jira_issue.issues(test_status);
CREATE INDEX IF NOT EXISTS idx_issues_test_set ON jira_issue.issues(test_set_id);
CREATE INDEX IF NOT EXISTS idx_issues_test_plan ON jira_issue.issues(test_plan_id);
CREATE INDEX IF NOT EXISTS idx_issues_test_execution ON jira_issue.issues(test_execution_id);
CREATE INDEX IF NOT EXISTS idx_issues_test_folder ON jira_issue.issues(test_repository_folder_id);
CREATE INDEX IF NOT EXISTS idx_issues_gherkin ON jira_issue.issues(gherkin_feature_key, gherkin_scenario_id);
CREATE INDEX IF NOT EXISTS idx_issues_req_keys ON jira_issue.issues USING GIN(requirement_keys);

-- ============================================
-- SEED DEFAULT TEST SET FOLDER
-- ============================================

INSERT INTO jira_issue.test_repository_folders (id, project_id, name, description, path, depth, sort_order) VALUES
    ('folder-root', NULL, 'Root', 'Root folder for test repository', '/', 0, 0)
ON CONFLICT DO NOTHING;

-- ============================================
-- TRIGGER: Update test_count on test_sets when tests added/removed
-- ============================================

CREATE OR REPLACE FUNCTION jira_issue.update_test_set_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE jira_issue.test_sets SET test_count = test_count + 1 WHERE id = NEW.test_set_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE jira_issue.test_sets SET test_count = GREATEST(0, test_count - 1) WHERE id = OLD.test_set_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Note: We use issue.test_set_id for tracking, no separate junction table needed
-- Test count is calculated: SELECT COUNT(*) FROM issues WHERE test_set_id = ?

-- ============================================
-- COMMENTS
-- ============================================

COMMENT ON TABLE jira_issue.test_repository_folders IS 'Hierarchical folder structure for organizing tests';
COMMENT ON TABLE jira_issue.test_sets IS 'Groups of tests for a release, sprint, or feature';
COMMENT ON TABLE jira_issue.test_plans IS 'Container for test set executions with schedule';
COMMENT ON TABLE jira_issue.test_executions IS 'Single test run with step-level results';
COMMENT ON TABLE jira_issue.step_results IS 'Per-step execution result with evidence';
COMMENT ON TABLE jira_issue.test_environments IS 'Test execution environments (DEV, STAGING, PROD)';
COMMENT ON TABLE jira_issue.test_datasets IS 'Data tables for data-driven testing';
COMMENT ON TABLE jira_issue.cucumber_scenarios IS 'Parsed BDD scenarios from Cucumber feature files';
COMMENT ON TABLE jira_issue.cucumber_features IS 'Cucumber feature file metadata';
COMMENT ON TABLE jira_issue.requirement_links IS 'Traceability: requirement to test mapping';
COMMENT ON TABLE jira_issue.defect_links IS 'Traceability: failed test to defect linkage';
COMMENT ON TABLE jira_issue.test_import_batches IS 'CI/CD import audit trail';
COMMENT ON TABLE jira_issue.test_execution_history IS 'Historical test execution tracking';
COMMENT ON TABLE jira_issue.test_evidence IS 'Evidence attachments for test executions';
COMMENT ON TABLE jira_issue.shared_steps IS 'Reusable test steps library';
COMMENT ON TABLE jira_issue.test_versions IS 'Test version history for audit and rollback';