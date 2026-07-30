-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;


-- Xray Test Management Clone - Full Schema
-- V1: Initial test management tables

-- ============================================
-- CORE TEST ENTITIES
-- ============================================

-- Test Issue - represents a single test case
CREATE TABLE test_issue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    test_type VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, AUTOMATED, CUKE
    status VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, READY, APPROVED, DEPRECATED
    labels TEXT[], -- ['smoke', 'regression', 'e2e']
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    owner_id UUID,
    requirement_keys TEXT[], -- linked requirement IDs (Jira issue keys)
    gherkin_feature_key VARCHAR(255), -- cucumber feature file this came from
    gherkin_scenario_id VARCHAR(255), -- cucumber scenario id in feature file
    test_set_id UUID,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_issue_project ON test_issue(project_id);
CREATE INDEX idx_test_issue_test_set ON test_issue(test_set_id);
CREATE INDEX idx_test_issue_gherkin ON test_issue(gherkin_feature_key, gherkin_scenario_id);
CREATE INDEX idx_test_issue_labels ON test_issue USING GIN(labels);

-- Test Step - individual step within a test
CREATE TABLE test_step (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES test_issue(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    step_type VARCHAR(20) NOT NULL, -- GIVEN, WHEN, THEN, AND, BUT
    description TEXT NOT NULL,
    test_data TEXT, -- parameterised test data
    expected_result TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_step_test ON test_step(test_id);
CREATE INDEX idx_test_step_order ON test_step(test_id, step_order);

-- ============================================
-- TEST SETS AND PLANS
-- ============================================

-- Test Set - group of tests for a release/sprint
CREATE TABLE test_set (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    test_type VARCHAR(50) DEFAULT 'MANUAL', -- MANUAL, AUTOMATED, MIXED
    labels TEXT[],
    test_count INTEGER DEFAULT 0,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_set_project ON test_set(project_id);
CREATE INDEX idx_test_set_labels ON test_set USING GIN(labels);

-- Test Plan - container for test set executions
CREATE TABLE test_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(30) DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    labels TEXT[],
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_plan_project ON test_plan(project_id);
CREATE INDEX idx_test_plan_dates ON test_plan(start_date, end_date);

-- Test Set Items - many-to-many for tests in sets
CREATE TABLE test_set_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_set_id UUID NOT NULL REFERENCES test_set(id) ON DELETE CASCADE,
    test_id UUID NOT NULL REFERENCES test_issue(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(test_set_id, test_id)
);

CREATE INDEX idx_test_set_item_set ON test_set_item(test_set_id);
CREATE INDEX idx_test_set_item_test ON test_set_item(test_id);

-- Test Plan Items - many-to-many for test sets in plans
CREATE TABLE test_plan_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_plan_id UUID NOT NULL REFERENCES test_plan(id) ON DELETE CASCADE,
    test_set_id UUID NOT NULL REFERENCES test_set(id) ON DELETE CASCADE,
    execution_order INTEGER,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(test_plan_id, test_set_id)
);

CREATE INDEX idx_test_plan_item_plan ON test_plan_item(test_plan_id);
CREATE INDEX idx_test_plan_item_set ON test_plan_item(test_set_id);

-- ============================================
-- TEST EXECUTIONS
-- ============================================

-- Test Execution - one run of a test (or test set)
CREATE TABLE test_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_plan_id UUID REFERENCES test_plan(id),
    test_set_id UUID REFERENCES test_set(id),
    test_id UUID REFERENCES test_issue(id), -- null if running whole test set
    name VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'RUNNING', -- RUNNING, PASSED, FAILED, BLOCKED, CANCELLED
    test_env VARCHAR(50), -- DEV, STAGING, PROD
    tester_id UUID,
    test_cycle VARCHAR(100), -- e.g. "Sprint 23", "Release 2.1"
    ci_build_url VARCHAR, -- link back to CI job
    ci_job_id VARCHAR(255),
    total_tests INTEGER DEFAULT 0,
    passed_tests INTEGER DEFAULT 0,
    failed_tests INTEGER DEFAULT 0,
    blocked_tests INTEGER DEFAULT 0,
    not_run_tests INTEGER DEFAULT 0,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_test_execution_plan ON test_execution(test_plan_id);
CREATE INDEX idx_test_execution_set ON test_execution(test_set_id);
CREATE INDEX idx_test_execution_test ON test_execution(test_id);
CREATE INDEX idx_test_execution_tester ON test_execution(tester_id);
CREATE INDEX idx_test_execution_status ON test_execution(status);

-- Step Result - result for each step in an execution
CREATE TABLE step_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID NOT NULL REFERENCES test_execution(id) ON DELETE CASCADE,
    step_id UUID NOT NULL REFERENCES test_step(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'NOT_RUN', -- PASSED, FAILED, BLOCKED, NOT_RUN
    actual_result TEXT,
    evidence_urls TEXT[], -- screenshots, logs, attachments
    defect_key VARCHAR(100), -- linked Jira issue for the failure
    comment TEXT,
    executed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_step_result_execution ON step_result(execution_id);
CREATE INDEX idx_step_result_step ON step_result(step_id);
CREATE INDEX idx_step_result_defect ON step_result(defect_key);

-- ============================================
-- TRACEABILITY
-- ============================================

-- Requirement Link - trace requirement to tests
CREATE TABLE requirement_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_key VARCHAR(255) NOT NULL, -- Jira issue key like "PROJ-123"
    requirement_type VARCHAR(50), -- EPIC, STORY, REQUIREMENT
    test_id UUID NOT NULL REFERENCES test_issue(id) ON DELETE CASCADE,
    coverage_status VARCHAR(20) DEFAULT 'COVERED', -- COVERED, PARTIAL, NOT_COVERED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_req_link_req ON requirement_link(requirement_key);
CREATE INDEX idx_req_link_test ON requirement_link(test_id);
CREATE UNIQUE INDEX idx_req_link_unique ON requirement_link(requirement_key, test_id);

-- Defect Link - failed test linked to defect
CREATE TABLE defect_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    defect_key VARCHAR(100) NOT NULL, -- Jira issue key like "PROJ-456"
    execution_id UUID REFERENCES test_execution(id) ON DELETE CASCADE,
    step_result_id UUID REFERENCES step_result(id) ON DELETE CASCADE,
    severity VARCHAR(20), -- CRITICAL, MAJOR, MINOR
    status VARCHAR(20), -- OPEN, IN_PROGRESS, CLOSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_defect_link_defect ON defect_link(defect_key);
CREATE INDEX idx_defect_link_execution ON defect_link(execution_id);

-- ============================================
-- CUCUMBER/GHERKIN IMPORT
-- ============================================

-- Cucumber Scenario - parsed BDD scenarios
CREATE TABLE cucumber_scenario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_key VARCHAR(255) NOT NULL, -- unique key: filename::scenario_name
    feature_file VARCHAR(500) NOT NULL,
    feature_name VARCHAR(500) NOT NULL,
    scenario_name VARCHAR(500) NOT NULL,
    scenario_key VARCHAR(500) NOT NULL, -- full gherkin key
    scenario_type VARCHAR(50) DEFAULT 'Scenario', -- Scenario, Scenario Outline
    background TEXT, -- shared background steps
    tags TEXT[], -- @smoke @regression etc
    line_number INTEGER,
    test_id UUID REFERENCES test_issue(id) ON DELETE SET NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cucumber_feature ON cucumber_scenario(feature_key);
CREATE INDEX idx_cucumber_test ON cucumber_scenario(test_id);
CREATE INDEX idx_cucumber_tags ON cucumber_scenario USING GIN(tags);

-- ============================================
-- CI/CD IMPORT
-- ============================================

-- Test Import Batch - audit of CI imports
CREATE TABLE test_import_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_type VARCHAR(30) NOT NULL, -- JUNIT, CUCUMBER, TESTNG
    ci_source VARCHAR(100), -- JENKINS, GITHUB_ACTIONS, GITLAB_CI, BAMBOO
    ci_build_url VARCHAR,
    ci_job_name VARCHAR(255),
    ci_build_number VARCHAR(100),
    branch VARCHAR(255),
    commit_sha VARCHAR(100),
    total_tests INTEGER DEFAULT 0,
    total_passed INTEGER DEFAULT 0,
    total_failed INTEGER DEFAULT 0,
    total_skipped INTEGER DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PROCESSING', -- PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX idx_import_batch_type ON test_import_batch(import_type);
CREATE INDEX idx_import_batch_source ON test_import_batch(ci_source);
CREATE INDEX idx_import_batch_status ON test_import_batch(status);
CREATE INDEX idx_import_batch_created ON test_import_batch(created_at);

-- ============================================
-- SEED DATA
-- ============================================

-- Default test types
INSERT INTO test_issue (id, project_id, name, description, test_type) VALUES
    (gen_random_uuid(), gen_random_uuid(), 'Example Test', 'This is an example test case', 'MANUAL');