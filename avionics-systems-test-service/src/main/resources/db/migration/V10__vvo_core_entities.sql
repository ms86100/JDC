CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- VVO Core Entities
-- V10: VVO, HLVVO, Test Request, and link tables

-- ============================================
-- HLVVO DEFINITION
-- ============================================

CREATE TABLE IF NOT EXISTS hlvvo_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'NEW', -- NEW, PLAN, VVO_WRITING_IN_PROGRESS, SUPPLIER_IN_REVIEW, AUTHORIZE

    target_date DATE,
    airbus_reference VARCHAR(100),
    hlvvo_version INTEGER DEFAULT 1,
    proofreading_data JSONB,

    assignee_id UUID,
    specification_reference TEXT,
    component_ids UUID[],
    task_progress INTEGER DEFAULT 0,
    pts_link TEXT,
    mfcl_link TEXT,
    fix_version_id UUID,
    labels TEXT[],

    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hlvvo_definition_project ON hlvvo_definition(project_id);
CREATE INDEX IF NOT EXISTS idx_hlvvo_definition_status ON hlvvo_definition(status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_hlvvo_definition_issue_key ON hlvvo_definition(issue_key);

-- ============================================
-- VVO DEFINITION
-- ============================================

CREATE TABLE IF NOT EXISTS vvo_definition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'NEW', -- NEW, TO_BE_VERIFIED, VERIFIED, RELEASED, CANCELLED, SUPERSEDED

    hlvvo_id UUID REFERENCES hlvvo_definition(id),

    -- Execution
    execution_responsible TEXT[], -- AIRBUS_DO, AIRBUS_LAB, AIRBUS_FLIGHT, AIRBUS_PERF_DO, SUPPLIER
    execution_delegation TEXT[], -- Multi-select from aircraft systems (UUIDs stored as text)

    -- Classification
    vvo_usage TEXT[], -- MATURITY, FORMAL_VERIFICATION, NON_REGRESSION
    vvo_scope VARCHAR(30), -- INTERFACE, FUNCTIONAL

    -- Test Mean
    test_mean_type_requested TEXT[], -- SIB, FIB, SIMULATOR, FLIGHT_TEST

    -- Content
    operational_conditions TEXT,
    expected_results TEXT,

    -- Systems
    real_system_needed TEXT[],

    -- Applicability
    applicability TEXT[],
    supplier_applicability TEXT[],

    -- Requirements & Traceability
    associated_requirements TEXT[],
    id_doors VARCHAR(100),

    -- Versioning
    vvo_version INTEGER DEFAULT 1,
    clone_source_id UUID,

    -- Planning
    fix_version_id UUID,
    milestone_target VARCHAR(255),
    specification_reference TEXT,

    -- Assignment
    assignee_id UUID,
    story_points INTEGER,
    labels TEXT[],
    component_ids UUID[],

    -- Metadata
    archived BOOLEAN DEFAULT false,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- B-tree indexes
CREATE INDEX IF NOT EXISTS idx_vvo_definition_project ON vvo_definition(project_id);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_status ON vvo_definition(status);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_hlvvo ON vvo_definition(hlvvo_id);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_id_doors ON vvo_definition(id_doors);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_fix_version ON vvo_definition(fix_version_id);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_clone_source ON vvo_definition(clone_source_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_vvo_definition_issue_key ON vvo_definition(issue_key);

-- GIN indexes on array columns
CREATE INDEX IF NOT EXISTS idx_vvo_definition_execution_responsible ON vvo_definition USING GIN(execution_responsible);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_execution_delegation ON vvo_definition USING GIN(execution_delegation);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_vvo_usage ON vvo_definition USING GIN(vvo_usage);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_test_mean_type_requested ON vvo_definition USING GIN(test_mean_type_requested);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_real_system_needed ON vvo_definition USING GIN(real_system_needed);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_applicability ON vvo_definition USING GIN(applicability);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_supplier_applicability ON vvo_definition USING GIN(supplier_applicability);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_associated_requirements ON vvo_definition USING GIN(associated_requirements);
CREATE INDEX IF NOT EXISTS idx_vvo_definition_labels ON vvo_definition USING GIN(labels);

-- ============================================
-- TEST REQUEST
-- ============================================

CREATE TABLE IF NOT EXISTS test_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    request_type VARCHAR(20), -- LTR, FTR
    status VARCHAR(30) DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, DONE, CANCELLED

    fix_version_id UUID,
    assignee_id UUID,
    frozen BOOLEAN DEFAULT false,
    labels TEXT[],

    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_test_request_project ON test_request(project_id);
CREATE INDEX IF NOT EXISTS idx_test_request_status ON test_request(status);

-- ============================================
-- VVO <-> TEST REQUEST LINK
-- ============================================

CREATE TABLE IF NOT EXISTS vvo_test_request_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vvo_id UUID NOT NULL REFERENCES vvo_definition(id) ON DELETE CASCADE,
    test_request_id UUID NOT NULL REFERENCES test_request(id) ON DELETE CASCADE,
    link_type VARCHAR(20) DEFAULT 'CONTAIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(vvo_id, test_request_id)
);

CREATE INDEX IF NOT EXISTS idx_vvo_test_request_link_vvo ON vvo_test_request_link(vvo_id);
CREATE INDEX IF NOT EXISTS idx_vvo_test_request_link_test_request ON vvo_test_request_link(test_request_id);

-- ============================================
-- UPDATED_AT TRIGGER
-- ============================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_vvo_definition_updated_at BEFORE UPDATE ON vvo_definition
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_hlvvo_definition_updated_at BEFORE UPDATE ON hlvvo_definition
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_test_request_updated_at BEFORE UPDATE ON test_request
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
