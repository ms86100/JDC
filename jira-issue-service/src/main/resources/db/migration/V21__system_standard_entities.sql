-- ============================================================
-- V21: System Standard + Review Sub-Task Metadata Entities
-- Adds System Standard (M1659.2) and Review Sub-Task metadata
-- ============================================================

CREATE TABLE IF NOT EXISTS jira_issue.system_standard_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    standard_type VARCHAR(20),
    -- Values: LAB, LAB_AND_FLIGHT
    spec_freeze_date DATE,
    delivery_to_lab_date DATE,
    requested_lab_clearance_date DATE,
    planned_flight_clearance_date DATE,
    target_flight_date DATE,
    applicability TEXT[],
    component_ids TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jira_issue.review_sub_task_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    parent_system_standard_id UUID,
    review_type VARCHAR(50),
    -- Values: INTERNAL_KOM, COMMON_KOM, PLANS_REVIEW, FCR, PDR, DDR, CDR, LAR, FAR, FFR, CR
    review_status VARCHAR(20) DEFAULT 'BACKLOG',
    -- Values: NOT_REQUIRED, BACKLOG, PLANNED, PASSED_GREEN, PASSED_AMBER, PASSED_RED
    baseline_start_date DATE,
    baseline_end_date DATE,
    follow_up_review_id UUID,
    -- Set when auto-cloned on PASSED_RED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sys_std_issue_id ON jira_issue.system_standard_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_sys_std_standard_type ON jira_issue.system_standard_metadata(standard_type);

CREATE INDEX IF NOT EXISTS idx_review_st_issue_id ON jira_issue.review_sub_task_metadata(issue_id);
CREATE INDEX IF NOT EXISTS idx_review_st_parent_id ON jira_issue.review_sub_task_metadata(parent_system_standard_id);
CREATE INDEX IF NOT EXISTS idx_review_st_review_type ON jira_issue.review_sub_task_metadata(review_type);
CREATE INDEX IF NOT EXISTS idx_review_st_review_status ON jira_issue.review_sub_task_metadata(review_status);
CREATE INDEX IF NOT EXISTS idx_review_st_follow_up ON jira_issue.review_sub_task_metadata(follow_up_review_id);

-- Update triggers (reuse existing set_updated_at function from V19)
CREATE TRIGGER trg_sys_std_updated_at BEFORE UPDATE ON jira_issue.system_standard_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();
CREATE TRIGGER trg_review_st_updated_at BEFORE UPDATE ON jira_issue.review_sub_task_metadata
    FOR EACH ROW EXECUTE FUNCTION jira_issue.set_updated_at();

-- Register issue types
INSERT INTO jira_issue.issue_types (id, name, issue_type_key, description, is_subtask) VALUES
(gen_random_uuid(), 'System Standard', 'system_standard', 'System version development per M1659.2', false),
(gen_random_uuid(), 'Review Sub-Task', 'review_sub_task', 'System standard milestone review with RAG status', true)
ON CONFLICT (issue_type_key) DO NOTHING;
