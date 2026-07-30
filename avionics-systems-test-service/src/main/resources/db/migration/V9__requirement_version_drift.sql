-- Requirement Version & Drift Module - V9
-- Comprehensive versioning and coverage drift tracking

-- ============================================
-- REQUIREMENT VERSIONS TABLE
-- ============================================

DROP TABLE IF EXISTS requirement_versions CASCADE;
CREATE TABLE requirement_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id UUID NOT NULL,
    version VARCHAR(50) NOT NULL, -- Semantic version like "1.0.0", "1.1.0"
    version_number INTEGER, -- Numeric version for ordering and comparison

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED

    -- Content snapshots
    content TEXT, -- Full requirement content
    changelog TEXT, -- Description of changes from previous version

    -- Title/description extraction
    title_snapshot VARCHAR(500),
    description_snapshot TEXT,
    acceptance_criteria_snapshot JSONB,
    linked_tests_snapshot JSONB,

    -- Version lineage
    previous_version_id UUID,

    -- Publishing metadata
    published_at TIMESTAMP,
    published_by UUID,

    -- Change classification
    change_magnitude VARCHAR(20) DEFAULT 'MINOR', -- MINOR, MAJOR, CRITICAL

    -- Audit fields
    changed_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for requirement_versions
CREATE INDEX idx_rv_requirement ON requirement_versions(requirement_id);
CREATE INDEX idx_rv_status ON requirement_versions(status);
CREATE INDEX idx_rv_version_number ON requirement_versions(requirement_id, version_number);
CREATE INDEX idx_rv_created_at ON requirement_versions(requirement_id, created_at DESC);
CREATE INDEX idx_rv_previous_version ON requirement_versions(previous_version_id);

-- ============================================
-- COVERAGE DRIFT RECORDS TABLE
-- ============================================

CREATE TABLE coverage_drift_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id UUID NOT NULL,
    project_id UUID NOT NULL,

    -- Coverage metrics
    previous_coverage DECIMAL(5,2),
    current_coverage DECIMAL(5,2),
    drift DECIMAL(5,2), -- Difference between previous and current

    -- Drift classification
    drift_type VARCHAR(20) NOT NULL DEFAULT 'STABLE', -- IMPROVED, DEGRADED, STABLE

    -- Test count changes
    previous_test_count INTEGER,
    current_test_count INTEGER,

    -- Detailed change information (JSON)
    affected_tests JSONB, -- Array of affected test objects
    missing_coverage JSONB, -- Array of missing test identifiers
    stale_coverage JSONB, -- Array of stale test identifiers

    -- Action tracking
    action_required BOOLEAN DEFAULT FALSE,

    -- Timestamps
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Additional metadata
    metadata JSONB -- Additional context about the drift
);

-- Indexes for coverage_drift_records
CREATE INDEX idx_cdr_requirement ON coverage_drift_records(requirement_id);
CREATE INDEX idx_cdr_project ON coverage_drift_records(project_id);
CREATE INDEX idx_cdr_drift_type ON coverage_drift_records(drift_type);
CREATE INDEX idx_cdr_action_required ON coverage_drift_records(action_required);
CREATE INDEX idx_cdr_detected_at ON coverage_drift_records(detected_at DESC);
CREATE INDEX idx_cdr_project_detected ON coverage_drift_records(project_id, detected_at DESC);

-- Composite index for alert queries
CREATE INDEX idx_cdr_alert_query ON coverage_drift_records(project_id, action_required, detected_at DESC);

-- ============================================
-- REQUIREMENT CHANGE EVENTS TABLE
-- ============================================

CREATE TABLE requirement_change_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id UUID NOT NULL,
    version_id UUID,

    -- Change classification
    change_type VARCHAR(50), -- ADDED, MODIFIED, DELETED, ROLLBACK
    impact_level VARCHAR(20), -- LOW, MEDIUM, HIGH, CRITICAL

    -- Version tracking
    from_version INTEGER,
    to_version INTEGER,

    -- Change details
    affected_fields JSONB, -- Array of field names that changed
    change_summary TEXT,
    change_details TEXT, -- Detailed description of changes

    -- Context
    triggered_by UUID, -- User who triggered the change
    trigger_source VARCHAR(100), -- API, WEBHOOK, AUTOMATED, etc.

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- External references
    external_event_id VARCHAR(200), -- For tracking webhook/external events
    correlation_id UUID -- For tracing related events
);

-- Indexes for requirement_change_events
CREATE INDEX idx_rce_requirement ON requirement_change_events(requirement_id);
CREATE INDEX idx_rce_version ON requirement_change_events(version_id);
CREATE INDEX idx_rce_change_type ON requirement_change_events(change_type);
CREATE INDEX idx_rce_impact_level ON requirement_change_events(impact_level);
CREATE INDEX idx_rce_created_at ON requirement_change_events(created_at DESC);
CREATE INDEX idx_rce_correlation ON requirement_change_events(correlation_id);

-- ============================================
-- DATA MIGRATION: Populate version_number from existing data
-- ============================================

-- Migration: Set version_number for existing requirement_versions
UPDATE requirement_versions rv
SET version_number = subquery.version_num
FROM (
    SELECT id,
           requirement_id,
           ROW_NUMBER() OVER (PARTITION BY requirement_id ORDER BY created_at ASC) AS version_num
    FROM requirement_versions
) AS subquery
WHERE rv.id = subquery.id;

-- Migration: Set status based on existing flags (if any)
UPDATE requirement_versions
SET status = COALESCE(
    (SELECT status FROM requirement_versions WHERE id = requirement_versions.id AND status IS NOT NULL),
    'DRAFT'
);

-- ============================================
-- VIEWS FOR REPORTING
-- ============================================

-- View: Active versions summary
CREATE OR REPLACE VIEW v_requirement_version_summary AS
SELECT
    rv.requirement_id,
    COUNT(*) as total_versions,
    COUNT(CASE WHEN rv.status = 'PUBLISHED' THEN 1 END) as published_count,
    COUNT(CASE WHEN rv.status = 'DRAFT' THEN 1 END) as draft_count,
    COUNT(CASE WHEN rv.status = 'ARCHIVED' THEN 1 END) as archived_count,
    MAX(rv.created_at) as latest_version_date,
    MAX(CASE WHEN rv.status = 'PUBLISHED' THEN rv.created_at END) as last_published_date
FROM requirement_versions rv
GROUP BY rv.requirement_id;

-- View: Coverage drift summary by project
CREATE OR REPLACE VIEW v_drift_summary_by_project AS
SELECT
    cdr.project_id,
    COUNT(*) as total_records,
    COUNT(CASE WHEN cdr.drift_type = 'IMPROVED' THEN 1 END) as improved_count,
    COUNT(CASE WHEN cdr.drift_type = 'DEGRADED' THEN 1 END) as degraded_count,
    COUNT(CASE WHEN cdr.drift_type = 'STABLE' THEN 1 END) as stable_count,
    AVG(cdr.drift) as average_drift,
    COUNT(CASE WHEN cdr.action_required = TRUE THEN 1 END) as action_required_count,
    MAX(cdr.detected_at) as last_detection
FROM coverage_drift_records cdr
GROUP BY cdr.project_id;

-- View: Requirements needing attention
CREATE OR REPLACE VIEW v_requirements_needing_attention AS
SELECT
    cdr.requirement_id,
    cdr.project_id,
    cdr.drift,
    cdr.drift_type,
    cdr.current_coverage,
    cdr.action_required,
    cdr.detected_at,
    rv.version as latest_version,
    rv.change_magnitude
FROM coverage_drift_records cdr
LEFT JOIN LATERAL (
    SELECT version, change_magnitude
    FROM requirement_versions
    WHERE requirement_id = cdr.requirement_id
    ORDER BY created_at DESC
    LIMIT 1
) rv ON TRUE
WHERE cdr.action_required = TRUE
   OR cdr.drift_type = 'DEGRADED'
ORDER BY cdr.detected_at DESC;

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================

-- Uncomment for testing purposes:
/*
INSERT INTO requirement_versions (requirement_id, version, version_number, status, content, changelog, change_magnitude)
VALUES
    ('11111111-1111-1111-1111-111111111111', '1.0.0', 1, 'PUBLISHED', 'Initial requirement content', 'Initial version', 'MINOR'),
    ('11111111-1111-1111-1111-111111111111', '1.1.0', 2, 'PUBLISHED', 'Updated requirement with new features', 'Added new acceptance criteria', 'MAJOR'),
    ('11111111-1111-1111-1111-111111111111', '1.2.0', 3, 'DRAFT', 'Draft version with proposed changes', 'Minor content update', 'MINOR');

INSERT INTO coverage_drift_records (requirement_id, project_id, previous_coverage, current_coverage, drift, drift_type, action_required)
VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 75.00, 80.00, 5.00, 'IMPROVED', FALSE),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 80.00, 65.00, -15.00, 'DEGRADED', TRUE);
*/