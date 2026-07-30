-- ============================================================
-- V11: Defect Management Entities
-- TechEvent, BenchDefect, ProblemReport for SYSDOPS V&V
-- ============================================================

CREATE TABLE IF NOT EXISTS tech_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(40) DEFAULT 'OPEN',
    -- OPEN, UNDER_ORIGINATOR_ANALYSIS, UNDER_RESOLVER_ANALYSIS, UNDER_TEST_MEAN_ANALYSIS,
    -- READY_FOR_REVIEW, CLASSIFIED, TO_BE_ASSESSED, RESOLVED_CORRECTED, RESOLVED_CONTAINED,
    -- PROPOSED_FOR_CANCELLATION, CANCELLED, CLOSED, TO_BE_REFINED, UNRESOLVED

    -- Reporter/Team
    reporter_id UUID,
    reporter_team_id UUID,
    team_for_analysis_id UUID,

    -- Detection context (cascading from detected_on_program_id)
    detected_on_program_id UUID,
    detected_on_date TIMESTAMP,
    detected_on_test_mean_id UUID,

    -- Impact (cascading from detected_on_program_id)
    impacted_ac_system_id UUID,
    impacted_ata_chapter_id UUID,
    impacted_msf VARCHAR(255),
    impacted_function_id UUID,
    impacted_partition VARCHAR(100),
    system_supplier_id UUID,

    -- Classification
    defect_type VARCHAR(50),
    -- Values: HARDWARE, SOFTWARE
    defect_origin VARCHAR(50),
    -- Values: NONE, FLIGHT_OPS_DOC, LTR, FUNCTION, MAINTENANCE_DOC, SYSTEM, TEST_MEANS, TEST_PROCEDURE, TEST_REQUEST, VIRTUAL_SYSTEM
    defect_impact VARCHAR(50),
    -- Values: IMPROVEMENT, FLIGHT_CLEARANCE, LAB_CLEARANCE, FLIGHT_TEST_CAMPAIGN, CERTIFICATION, ENTRY_INTO_SERVICE, OPERATION, POWER_ON_CLEARANCE
    defect_impact_rationale TEXT,

    -- Versions
    affects_version_id UUID,
    fix_version_id UUID,

    -- Program applicability
    applicable_to_program_ids TEXT[],

    -- Analysis
    public_analysis TEXT,
    abstract_text TEXT,
    test_configuration TEXT,
    recording_reference VARCHAR(255),
    operational_impact TEXT,
    requirement_impact TEXT,
    workaround TEXT,

    -- Rejection
    rejection_rationale TEXT,
    rejection_type VARCHAR(50),
    -- Values: DUPLICATE, NO_ISSUE_AIRBUS_DESIGN, NO_ISSUE_SUPPLIER_DESIGN, NO_ISSUE_SCENARIO_NOT_OPERATIONAL, NOT_REPRODUCED, NO_CORRECTION, TEST_PROCEDURE_ISSUE

    -- Supplier sync
    supplier_analysis TEXT,
    supplier_response VARCHAR(50),
    -- Values: NO_ACTION, ANOMALY_DETECTED, EVOLUTION_NEEDED
    supplier_status VARCHAR(100),
    final_airbus_response TEXT,
    supplier_sync_project_id UUID,
    supplier_sync_issue_id UUID,

    -- Linked items
    linked_change_card_id UUID,
    linked_problem_report_id UUID,

    -- Assignment
    assignee_id UUID,
    resolved_by UUID,
    priority VARCHAR(20),
    labels TEXT[],
    vv_activity VARCHAR(50),
    detected_by VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bench_defect (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'OPEN',
    -- OPEN, UNDER_ANALYSIS, TO_BE_CORRECTED, CORRECTED, CLOSED, CANCELLED

    severity VARCHAR(20),
    -- Values: BLOCKING, HIGH, LOW
    criticality VARCHAR(10),
    -- Values: P0, P1, P2, P3 (only when severity=HIGH)

    defect_type VARCHAR(50),
    defect_origin VARCHAR(50),
    defect_impact VARCHAR(50),
    defect_impact_rationale TEXT,
    ltm_defect_type VARCHAR(50),

    -- Origin category (cascading)
    defect_origin_category_id UUID,
    defect_origin_sub_item_id UUID,

    -- Detection
    detected_on_program_id UUID,
    detected_on_date TIMESTAMP,
    detected_on_test_mean_id UUID,

    -- Applicability
    applicable_to_program_ids TEXT[],
    applicable_to_test_means TEXT[],
    affected_ata VARCHAR(50),

    -- Versions
    affects_version_id UUID,
    fix_version_id UUID,

    -- Analysis
    test_configuration TEXT,
    workaround TEXT,
    change_reference VARCHAR(255),

    -- Dates
    objective_date_analysis DATE,
    objective_date_closure DATE,

    -- Source
    source_tech_event_id UUID,

    -- Assignment
    reporter_id UUID,
    assignee_id UUID,
    priority VARCHAR(20),
    labels TEXT[],

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS problem_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    issue_key VARCHAR(20),
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'OPEN',
    -- OPEN, UNDER_ANALYSIS, CLOSED, REJECTED

    pr_origin VARCHAR(30),
    -- Values: DESIGN_REVIEW, SAFETY_REVIEW, VV_ACTIVITY
    pr_type VARCHAR(50),
    -- Values: SIGNIFICANT_CAT_HAZ, SIGNIFICANT_MAJ, FUNCTIONAL, FUNCTIONAL_INTERNAL, PROCESS, LIFECYCLE_DATA
    pr_type_rationale TEXT,

    potential_effects TEXT,
    justification_mitigation TEXT,

    -- Detection
    detected_on_program_id UUID,
    detected_on_ac_system_id UUID,
    applicable_to_program_ids TEXT[],

    -- Rejection
    rejection_type VARCHAR(50),
    rejection_rationale TEXT,

    -- Linked
    linked_tech_event_id UUID,

    -- Versions
    affects_version_id UUID,
    fix_version_id UUID,

    -- Classification
    classification VARCHAR(50),

    -- Assignment
    reporter_id UUID,
    assignee_id UUID,
    system_supplier_id UUID,
    priority VARCHAR(20),
    labels TEXT[],

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for tech_event
CREATE INDEX IF NOT EXISTS idx_tech_event_project ON tech_event(project_id);
CREATE INDEX IF NOT EXISTS idx_tech_event_status ON tech_event(status);
CREATE INDEX IF NOT EXISTS idx_tech_event_issue_key ON tech_event(issue_key);
CREATE INDEX IF NOT EXISTS idx_tech_event_program ON tech_event(detected_on_program_id);
CREATE INDEX IF NOT EXISTS idx_tech_event_system ON tech_event(impacted_ac_system_id);
CREATE INDEX IF NOT EXISTS idx_tech_event_supplier ON tech_event(system_supplier_id);
CREATE INDEX IF NOT EXISTS idx_tech_event_reporter_team ON tech_event(reporter_team_id);
CREATE INDEX IF NOT EXISTS idx_tech_event_applicable_programs ON tech_event USING GIN(applicable_to_program_ids);
CREATE INDEX IF NOT EXISTS idx_tech_event_labels ON tech_event USING GIN(labels);

-- Indexes for bench_defect
CREATE INDEX IF NOT EXISTS idx_bench_defect_project ON bench_defect(project_id);
CREATE INDEX IF NOT EXISTS idx_bench_defect_status ON bench_defect(status);
CREATE INDEX IF NOT EXISTS idx_bench_defect_issue_key ON bench_defect(issue_key);
CREATE INDEX IF NOT EXISTS idx_bench_defect_severity ON bench_defect(severity);
CREATE INDEX IF NOT EXISTS idx_bench_defect_source_te ON bench_defect(source_tech_event_id);
CREATE INDEX IF NOT EXISTS idx_bench_defect_applicable ON bench_defect USING GIN(applicable_to_program_ids);
CREATE INDEX IF NOT EXISTS idx_bench_defect_labels ON bench_defect USING GIN(labels);

-- Indexes for problem_report
CREATE INDEX IF NOT EXISTS idx_problem_report_project ON problem_report(project_id);
CREATE INDEX IF NOT EXISTS idx_problem_report_status ON problem_report(status);
CREATE INDEX IF NOT EXISTS idx_problem_report_issue_key ON problem_report(issue_key);
CREATE INDEX IF NOT EXISTS idx_problem_report_pr_type ON problem_report(pr_type);
CREATE INDEX IF NOT EXISTS idx_problem_report_linked_te ON problem_report(linked_tech_event_id);
CREATE INDEX IF NOT EXISTS idx_problem_report_applicable ON problem_report USING GIN(applicable_to_program_ids);
CREATE INDEX IF NOT EXISTS idx_problem_report_labels ON problem_report USING GIN(labels);

-- Update triggers
CREATE TRIGGER trg_tech_event_updated_at BEFORE UPDATE ON tech_event
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_bench_defect_updated_at BEFORE UPDATE ON bench_defect
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_problem_report_updated_at BEFORE UPDATE ON problem_report
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
