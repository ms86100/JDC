-- ============================================================
-- V19: Change Management Metadata Entities
-- Adds ChangeCard, DesignItem, DCL, and Deliverable metadata
-- ============================================================

CREATE TABLE IF NOT EXISTS jira_issue.change_card_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    change_type VARCHAR(20),
    -- Values: ANOMALY, EVOLUTION
    classification VARCHAR(50),
    -- Values: TYPE_0, TYPE_1A, TYPE_1B, TYPE_2, TYPE_3, SIGNIFICANT_CAT_HAZ, SIGNIFICANT_MAJ, FUNCTIONAL, FUNCTIONAL_INTERNAL, PROCESS, LIFECYCLE_DATA
    parent_design_item_id UUID,
    tab_layout_key VARCHAR(50) DEFAULT 'STANDARD',
    closure_rationale TEXT,
    resolved_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.design_item_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    applicability TEXT[],
    supplier_sharing BOOLEAN DEFAULT false,
    shared_supplier_ids TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.dcl_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    action_responsible VARCHAR(255),
    requested_by VARCHAR(255),
    dcl_abstract TEXT,
    description_thales TEXT,
    description_honeywell TEXT,
    supplier_sync_project_id UUID,
    supplier_sync_issue_id UUID,
    sync_direction VARCHAR(30),
    -- Values: AIRBUS_TO_SUPPLIER, SUPPLIER_TO_AIRBUS
    last_synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.deliverable_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    deliverable_type VARCHAR(20),
    -- Values: SID, MSID, FRD, FDD, FRD_FDD, ICD
    milestone_type VARCHAR(30),
    -- Values: EVM, CRITICAL_EVM, OTHER_DELIVERABLE, CRITICAL_DELIVERABLE
    baseline_start_date DATE,
    baseline_end_date DATE,
    external_end_date DATE,
    delivery_date DATE,
    program_rebaselining VARCHAR(10),
    -- Values: YES, NO
    source_of_delay VARCHAR(50),
    risk_probability VARCHAR(20),
    -- Values: ALMOST_NONE, LOW, MEDIUM, HIGH, VERY_HIGH
    risk_consequence VARCHAR(20),
    -- Values: TRIVIAL, LOW, MEDIUM, HIGH, SEVERE
    risk_description TEXT,
    risk_owner UUID,
    risk_mitigation VARCHAR(20),
    -- Values: MITIGATION, RCA_PPS
    review_status VARCHAR(20),
    -- Values: TO_DO, OK, KO
    review_assignee UUID,
    review_comment TEXT,
    review_start_date DATE,
    review_deadline DATE,
    domain_leader UUID,
    computer VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_change_card_issue_id ON jira_issue.change_card_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_change_card_parent_di ON jira_issue.change_card_metadata(parent_design_item_id);
CREATE INDEX IF NOT EXISTS idx_change_card_change_type ON jira_issue.change_card_metadata(change_type);
CREATE INDEX IF NOT EXISTS idx_change_card_classification ON jira_issue.change_card_metadata(classification);

CREATE INDEX IF NOT EXISTS idx_design_item_issue_id ON jira_issue.design_item_metadata(issue_id);

CREATE INDEX IF NOT EXISTS idx_dcl_issue_id ON jira_issue.dcl_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_dcl_sync_project ON jira_issue.dcl_metadata(supplier_sync_project_id);

CREATE INDEX IF NOT EXISTS idx_deliverable_issue_id ON jira_issue.deliverable_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_deliverable_type ON jira_issue.deliverable_metadata(deliverable_type);
CREATE INDEX IF NOT EXISTS idx_deliverable_milestone ON jira_issue.deliverable_metadata(milestone_type);

-- Update triggers
CREATE OR REPLACE FUNCTION jira_issue.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_change_card_updated_at BEFORE UPDATE ON jira_issue.change_card_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();
CREATE TRIGGER trg_design_item_updated_at BEFORE UPDATE ON jira_issue.design_item_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();
CREATE TRIGGER trg_dcl_updated_at BEFORE UPDATE ON jira_issue.dcl_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();
CREATE TRIGGER trg_deliverable_updated_at BEFORE UPDATE ON jira_issue.deliverable_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();
