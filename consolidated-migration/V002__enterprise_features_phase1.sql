-- =====================================================
-- Migration: Enterprise Feature Gap - Phase 1 & 2
-- Features: Dataset Engine, Shared Steps, Impact Analysis, Flaky Detection, Quarantine
-- =====================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- DATASET ENGINE TABLES
-- =====================================================

-- Test Datasets table (already exists, adding missing columns)
CREATE TABLE IF NOT EXISTS test_datasets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data_format VARCHAR(50) DEFAULT 'TABULAR',
    column_names TEXT[],
    column_types TEXT[],
    data JSONB,
    csv_data TEXT,
    row_count INTEGER DEFAULT 0,
    version INTEGER DEFAULT 1,
    is_immutable BOOLEAN DEFAULT FALSE,
    created_by UUID,
    folder_id UUID,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_test_datasets_project ON test_datasets(project_id);
CREATE INDEX IF NOT EXISTS idx_test_datasets_name ON test_datasets(name);
CREATE INDEX IF NOT EXISTS idx_test_datasets_archived ON test_datasets(archived);

-- Dataset Versions
CREATE TABLE IF NOT EXISTS dataset_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dataset_id UUID NOT NULL REFERENCES test_datasets(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    data JSONB NOT NULL,
    column_names TEXT[],
    column_types TEXT[],
    row_count INTEGER,
    change_summary TEXT,
    created_by UUID,
    is_immutable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dataset_versions_dataset ON dataset_versions(dataset_id);
CREATE INDEX IF NOT EXISTS idx_dataset_versions_version ON dataset_versions(dataset_id, version_number DESC);

-- Dataset Variables
CREATE TABLE IF NOT EXISTS dataset_variables (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dataset_id UUID NOT NULL REFERENCES test_datasets(id) ON DELETE CASCADE,
    variable_name VARCHAR(255) NOT NULL,
    variable_type VARCHAR(50) DEFAULT 'STRING',
    is_required BOOLEAN DEFAULT TRUE,
    default_value VARCHAR,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_dataset_variables_dataset ON dataset_variables(dataset_id);

-- Test Dataset Bindings
CREATE TABLE IF NOT EXISTS test_dataset_bindings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL,
    dataset_id UUID NOT NULL REFERENCES test_datasets(id) ON DELETE CASCADE,
    dataset_version_id UUID REFERENCES dataset_versions(id),
    column_mappings JSONB,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_test_dataset_bindings_test ON test_dataset_bindings(test_id);
CREATE INDEX IF NOT EXISTS idx_test_dataset_bindings_dataset ON test_dataset_bindings(dataset_id);

-- =====================================================
-- SHARED STEP LIBRARY TABLES
-- =====================================================

-- Shared Steps
CREATE TABLE IF NOT EXISTS shared_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    steps JSONB,
    current_version INTEGER DEFAULT 1,
    usage_count INTEGER DEFAULT 0,
    folder_id UUID,
    created_by UUID,
    archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shared_steps_project ON shared_steps(project_id);
CREATE INDEX IF NOT EXISTS idx_shared_steps_name ON shared_steps(name);
CREATE INDEX IF NOT EXISTS idx_shared_steps_usage ON shared_steps(usage_count DESC);

-- Shared Step Versions
CREATE TABLE IF NOT EXISTS shared_step_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shared_step_id UUID NOT NULL REFERENCES shared_steps(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    steps JSONB NOT NULL,
    change_summary TEXT,
    created_by UUID,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shared_step_versions_step ON shared_step_versions(shared_step_id);
CREATE INDEX IF NOT EXISTS idx_shared_step_versions_current ON shared_step_versions(shared_step_id, is_current) WHERE is_current = TRUE;

-- Test Shared Step Mappings
CREATE TABLE IF NOT EXISTS test_shared_step_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL,
    test_step_index INTEGER NOT NULL,
    shared_step_id UUID NOT NULL REFERENCES shared_steps(id) ON DELETE CASCADE,
    shared_step_version_id UUID REFERENCES shared_step_versions(id),
    embedded_snapshot JSONB,
    parameters JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_test_shared_step_mapping_test ON test_shared_step_mapping(test_id);
CREATE INDEX IF NOT EXISTS idx_test_shared_step_mapping_shared ON test_shared_step_mapping(shared_step_id);

-- Shared Step Dependencies
CREATE TABLE IF NOT EXISTS shared_step_dependencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_shared_step_id UUID NOT NULL REFERENCES shared_steps(id) ON DELETE CASCADE,
    child_shared_step_id UUID NOT NULL REFERENCES shared_steps(id) ON DELETE CASCADE,
    dependency_type VARCHAR(50) DEFAULT 'CONTAINS'
);

CREATE INDEX IF NOT EXISTS idx_shared_step_dependencies_parent ON shared_step_dependencies(parent_shared_step_id);
CREATE INDEX IF NOT EXISTS idx_shared_step_dependencies_child ON shared_step_dependencies(child_shared_step_id);

-- =====================================================
-- IMPACT ANALYSIS TABLES
-- =====================================================

-- Component Registry
CREATE TABLE IF NOT EXISTS component_registry (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    component_name VARCHAR(255) NOT NULL,
    component_path VARCHAR(500),
    ownership_team VARCHAR(255),
    ownership_contact VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(project_id, component_name)
);

CREATE INDEX IF NOT EXISTS idx_component_registry_project ON component_registry(project_id);

-- Test Component Mapping
CREATE TABLE IF NOT EXISTS test_component_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL,
    component_id UUID NOT NULL REFERENCES component_registry(id) ON DELETE CASCADE,
    confidence_score DECIMAL(3,2) DEFAULT 1.00,
    mapping_type VARCHAR(50) DEFAULT 'direct',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(test_id, component_id)
);

CREATE INDEX IF NOT EXISTS idx_test_component_mapping_test ON test_component_mapping(test_id);
CREATE INDEX IF NOT EXISTS idx_test_component_mapping_component ON test_component_mapping(component_id);

-- Code Change Events
CREATE TABLE IF NOT EXISTS code_change_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    commit_sha VARCHAR(40) NOT NULL,
    commit_message TEXT,
    author VARCHAR(255),
    changed_files JSONB NOT NULL,
    pr_id VARCHAR(100),
    branch VARCHAR(255),
    timestamp TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_code_change_events_project ON code_change_events(project_id);
CREATE INDEX IF NOT EXISTS idx_code_change_events_commit ON code_change_events(commit_sha);

-- Impact Analysis Results
CREATE TABLE IF NOT EXISTS impact_analysis_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_id UUID,
    analysis_payload JSONB NOT NULL,
    suggested_suite JSONB,
    risk_score DECIMAL(5,2),
    confidence_score DECIMAL(3,2),
    analyzed_by VARCHAR(100) DEFAULT 'rule-based',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_impact_analysis_project ON impact_analysis_results(project_id, created_at DESC);

-- =====================================================
-- FLAKY TEST DETECTION TABLES
-- =====================================================

-- Flaky Test Analysis
CREATE TABLE IF NOT EXISTS flaky_test_analysis (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL UNIQUE,
    total_executions INTEGER DEFAULT 0,
    total_failures INTEGER DEFAULT 0,
    total_passes INTEGER DEFAULT 0,
    flaky_score DECIMAL(5,2) DEFAULT 0.00,
    pass_rate_trend VARCHAR(20) DEFAULT 'stable',
    first_flaky_occurrence TIMESTAMP,
    last_flaky_occurrence TIMESTAMP,
    current_status VARCHAR(50) DEFAULT 'stable',
    confidence_level DECIMAL(3,2) DEFAULT 0.00,
    analysis_window_days INTEGER DEFAULT 30,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_flaky_test_analysis_score ON flaky_test_analysis(flaky_score DESC);
CREATE INDEX IF NOT EXISTS idx_flaky_test_analysis_status ON flaky_test_analysis(current_status);

-- Flaky Test Patterns
CREATE TABLE IF NOT EXISTS flaky_test_patterns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL REFERENCES flaky_test_analysis(id) ON DELETE CASCADE,
    pattern_type VARCHAR(100) NOT NULL,
    pattern_description TEXT,
    frequency_score DECIMAL(5,2),
    affected_environments JSONB,
    affected_builds JSONB,
    root_cause_category VARCHAR(100),
    suggested_fix TEXT,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_flaky_test_patterns_test ON flaky_test_patterns(test_id);

-- Execution Flakiness Record
CREATE TABLE IF NOT EXISTS execution_flakiness_record (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL,
    test_id UUID NOT NULL,
    is_flaky_execution BOOLEAN,
    failure_reason VARCHAR(255),
    environment_id UUID,
    execution_duration_ms INTEGER,
    retry_attempt INTEGER DEFAULT 0,
    analyzed_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_execution_flakiness_test ON execution_flakiness_record(test_id);
CREATE INDEX IF NOT EXISTS idx_execution_flakiness_date ON execution_flakiness_record(analyzed_at DESC);

-- =====================================================
-- QUARANTINE SYSTEM TABLES
-- =====================================================

-- Test Quarantine
CREATE TABLE IF NOT EXISTS test_quarantine (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'candidate',
    quarantine_reason TEXT,
    trigger_type VARCHAR(50),
    triggered_by UUID,
    triggered_at TIMESTAMP DEFAULT NOW(),
    auto_restore_enabled BOOLEAN DEFAULT TRUE,
    auto_restore_conditions JSONB,
    current_execution_count INTEGER DEFAULT 0,
    current_pass_count INTEGER DEFAULT 0,
    last_execution_at TIMESTAMP,
    last_status VARCHAR(50),
    restored_at TIMESTAMP,
    restored_by UUID,
    restore_reason TEXT,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_test_quarantine_status ON test_quarantine(status);
CREATE INDEX IF NOT EXISTS idx_test_quarantine_test ON test_quarantine(test_id);

-- Quarantine Transitions
CREATE TABLE IF NOT EXISTS quarantine_transitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quarantine_id UUID NOT NULL REFERENCES test_quarantine(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    transition_reason TEXT,
    transitioned_by UUID,
    transitioned_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quarantine_transitions_quarantine ON quarantine_transitions(quarantine_id);

-- Quarantine Metrics
CREATE TABLE IF NOT EXISTS quarantine_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quarantine_id UUID NOT NULL REFERENCES test_quarantine(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    quarantine_age_days INTEGER,
    execution_count INTEGER DEFAULT 0,
    pass_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    flaky_score DECIMAL(5,2),
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_quarantine_metrics_quarantine ON quarantine_metrics(quarantine_id);
CREATE INDEX IF NOT EXISTS idx_quarantine_metrics_date ON quarantine_metrics(metric_date);

-- Quarantine Rules
CREATE TABLE IF NOT EXISTS quarantine_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    conditions JSONB NOT NULL,
    auto_quarantine BOOLEAN DEFAULT TRUE,
    notify_on_trigger BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quarantine_rules_project ON quarantine_rules(project_id);
CREATE INDEX IF NOT EXISTS idx_quarantine_rules_active ON quarantine_rules(is_active);

-- =====================================================
-- EXECUTION TIMELINE TABLES
-- =====================================================

-- Execution Timeline Events
CREATE TABLE IF NOT EXISTS execution_timeline_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    step_index INTEGER,
    event_data JSONB,
    screenshot_path VARCHAR(500),
    log_entries JSONB,
    metadata JSONB,
    sequence_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_execution_timeline_execution ON execution_timeline_events(execution_id);
CREATE INDEX IF NOT EXISTS idx_execution_timeline_timestamp ON execution_timeline_events(execution_id, event_timestamp);
CREATE INDEX IF NOT EXISTS idx_execution_timeline_sequence ON execution_timeline_events(execution_id, sequence_order);

-- =====================================================
-- VERSION DIFF TABLES
-- =====================================================

-- Version Diff Cache
CREATE TABLE IF NOT EXISTS version_diff_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    version_a INTEGER NOT NULL,
    version_b INTEGER NOT NULL,
    diff_data JSONB NOT NULL,
    computed_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(entity_type, entity_id, version_a, version_b)
);

CREATE INDEX IF NOT EXISTS idx_version_diff_cache_entity ON version_diff_cache(entity_type, entity_id);

-- =====================================================
-- REQUIREMENT CHANGE IMPACT TABLES
-- =====================================================

-- Requirement Versions
CREATE TABLE IF NOT EXISTS requirement_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    title_snapshot VARCHAR(500),
    description_snapshot TEXT,
    acceptance_criteria_snapshot JSONB,
    linked_tests_snapshot JSONB,
    change_type VARCHAR(50),
    change_description TEXT,
    changed_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(requirement_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_requirement_versions_req ON requirement_versions(requirement_id);
CREATE INDEX IF NOT EXISTS idx_requirement_versions_number ON requirement_versions(requirement_id, version_number DESC);

-- Requirement Change Events
CREATE TABLE IF NOT EXISTS requirement_change_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_id UUID NOT NULL,
    from_version INTEGER NOT NULL,
    to_version INTEGER NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    field_changes JSONB,
    impact_assessment JSONB,
    affected_tests JSONB,
    notified_stakeholders JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_requirement_change_events_req ON requirement_change_events(requirement_id);

-- Coverage Drift Records
CREATE TABLE IF NOT EXISTS coverage_drift_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    requirement_id UUID NOT NULL,
    analysis_timestamp TIMESTAMP DEFAULT NOW(),
    previous_coverage_score DECIMAL(5,2),
    current_coverage_score DECIMAL(5,2),
    drift_type VARCHAR(50),
    missing_coverage JSONB,
    stale_coverage JSONB,
    action_required BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_coverage_drift_req ON coverage_drift_records(requirement_id);

-- =====================================================
-- Add foreign key constraints if tables exist
-- =====================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'issues') THEN
        -- Add foreign key from test_dataset_bindings to issues(test_id)
        ALTER TABLE IF EXISTS test_dataset_bindings
            ADD CONSTRAINT fk_test_dataset_bindings_test
            FOREIGN KEY (test_id) REFERENCES issues(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Migration complete
COMMENT ON TABLE test_datasets IS 'Stores test data datasets for parameterization';
COMMENT ON TABLE shared_steps IS 'Stores reusable shared test steps';
COMMENT ON TABLE component_registry IS 'Registry of code components for impact analysis';
COMMENT ON TABLE flaky_test_analysis IS 'Tracks flaky test detection metrics';
COMMENT ON TABLE test_quarantine IS 'Manages quarantined tests that are excluded from release metrics';

-- =====================================================
-- PHASE 3.3: EVIDENCE MANAGEMENT SYSTEM TABLES
-- =====================================================

-- Evidence Records
CREATE TABLE IF NOT EXISTS evidence_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID NOT NULL,
    step_result_id UUID,
    evidence_type VARCHAR(50) NOT NULL,
    classification_level VARCHAR(50),
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    content TEXT,
    metadata JSONB,
    retention_policy_id UUID,
    is_archived BOOLEAN DEFAULT FALSE,
    archived_at TIMESTAMP,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evidence_records_execution ON evidence_records(execution_id);
CREATE INDEX IF NOT EXISTS idx_evidence_records_step ON evidence_records(step_result_id);
CREATE INDEX IF NOT EXISTS idx_evidence_records_type ON evidence_records(evidence_type);
CREATE INDEX IF NOT EXISTS idx_evidence_records_classification ON evidence_records(classification_level);

-- Evidence Metadata
CREATE TABLE IF NOT EXISTS evidence_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evidence_id UUID NOT NULL,
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value TEXT,
    metadata_type VARCHAR(50) DEFAULT 'STRING',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evidence_metadata_evidence ON evidence_metadata(evidence_id);

-- Evidence Retention Policies
CREATE TABLE IF NOT EXISTS evidence_retention_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID,
    policy_name VARCHAR(255) NOT NULL,
    description TEXT,
    evidence_type VARCHAR(50),
    retention_days INTEGER DEFAULT 365,
    compression_enabled BOOLEAN DEFAULT FALSE,
    auto_archive BOOLEAN DEFAULT TRUE,
    move_to_cold_storage BOOLEAN DEFAULT FALSE,
    cold_storage_after_days INTEGER DEFAULT 90,
    permanent_delete BOOLEAN DEFAULT FALSE,
    delete_after_days INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evidence_retention_project ON evidence_retention_policies(project_id);
CREATE INDEX IF NOT EXISTS idx_evidence_retention_active ON evidence_retention_policies(is_active);

-- =====================================================
-- PHASE 3.4: ENVIRONMENT MATRIX ENGINE TABLES
-- =====================================================

-- Environment Matrix
CREATE TABLE IF NOT EXISTS environment_matrix (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    dimension_configs JSONB NOT NULL,
    filter_rules JSONB,
    conflict_rules JSONB,
    total_combinations INTEGER DEFAULT 0,
    valid_combinations INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_env_matrix_project ON environment_matrix(project_id);

-- Environment Combinations
CREATE TABLE IF NOT EXISTS environment_combinations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    matrix_id UUID NOT NULL REFERENCES environment_matrix(id) ON DELETE CASCADE,
    combination_index INTEGER DEFAULT 0,
    combination_data JSONB NOT NULL,
    is_valid BOOLEAN DEFAULT TRUE,
    validation_errors JSONB,
    provisioned_config JSONB,
    provisioning_status VARCHAR(50) DEFAULT 'PENDING',
    provisioned_at TIMESTAMP,
    provisioning_error TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_env_comb_matrix ON environment_combinations(matrix_id);
CREATE INDEX IF NOT EXISTS idx_env_comb_status ON environment_combinations(provisioning_status);

-- Environment Provisioning Rules
CREATE TABLE IF NOT EXISTS environment_provisioning_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    description TEXT,
    provider_type VARCHAR(50) NOT NULL,
    provider_config JSONB NOT NULL,
    provisioning_script TEXT,
    capabilities_template JSONB,
    environment_template JSONB,
    max_concurrent INTEGER DEFAULT 5,
    timeout_seconds INTEGER DEFAULT 300,
    retry_count INTEGER DEFAULT 3,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_by UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_env_prov_project ON environment_provisioning_rules(project_id);
CREATE INDEX IF NOT EXISTS idx_env_prov_provider ON environment_provisioning_rules(provider_type);
CREATE INDEX IF NOT EXISTS idx_env_prov_active ON environment_provisioning_rules(is_active);