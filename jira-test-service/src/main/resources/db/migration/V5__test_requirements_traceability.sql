-- V5__test_requirements_traceability.sql
-- Test Service - Requirements traceability and test coverage

CREATE SCHEMA IF NOT EXISTS jira_test;

-- ============================================
-- REQUIREMENTS TABLE
-- Map requirements to test cases for coverage tracking
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_key VARCHAR(100) NOT NULL UNIQUE,
    requirement_title VARCHAR(500) NOT NULL,
    requirement_description TEXT,
    requirement_type VARCHAR(50) DEFAULT 'FUNCTIONAL',
    source VARCHAR(100),
    priority VARCHAR(50),
    status VARCHAR(50) DEFAULT 'APPROVED',
    parent_requirement_id UUID,
    project_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REQUIREMENT VERSIONS TABLE
-- Track version history for requirements
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.requirement_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id UUID REFERENCES jira_test.requirements(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    change_description TEXT,
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (requirement_id, version_number)
);

-- ============================================
-- TEST REQUIREMENT COVERAGE TABLE
-- Many-to-many relationship for traceability
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.test_requirement_coverage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL,
    requirement_id UUID NOT NULL,
    coverage_status VARCHAR(50) DEFAULT 'COVERED', -- COVERED, PARTIAL, NOT_COVERED
    notes TEXT,
    last_verified_at TIMESTAMP,
    verified_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (test_id, requirement_id)
);

-- ============================================
-- TEST COVERAGE METRICS TABLE
-- Aggregate coverage data for reporting
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.test_coverage_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    coverage_percentage DECIMAL(5,2),
    total_requirements INTEGER,
    covered_requirements INTEGER,
    untested_requirements INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, metric_date)
);

-- ============================================
-- CHANGE LOG TABLE
-- Track changes to test entities for audit
-- ============================================
CREATE TABLE jira_test. IF NOT EXISTS jira_test.requirement_change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    field_changed VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX jira_test. IF NOT EXISTS idx_requirements_key ON jira_test.requirements(requirement_key);
CREATE INDEX jira_test. IF NOT EXISTS idx_requirements_project ON jira_test.requirements(project_id);
CREATE INDEX jira_test. IF NOT EXISTS idx_coverage_test ON jira_test.test_requirement_coverage(test_id);
CREATE INDEX jira_test. IF NOT EXISTS idx_coverage_requirement ON jira_test.test_requirement_coverage(requirement_id);
CREATE INDEX jira_test. IF NOT EXISTS idx_coverage_metrics_project ON jira_test.test_coverage_metrics(project_id);