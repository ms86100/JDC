-- V28__ifcs_entities.sql
-- IFCS Issue Type Entities: VVM Card, IVV Card, Group, Sub-Change metadata tables

SET search_path TO jira_issue;

-- VVM Card (V&V Management) metadata
CREATE TABLE IF NOT EXISTS vvm_card_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    scope VARCHAR(100),
    pipeline_status VARCHAR(200),
    ltr_reference VARCHAR(100),
    expert_review_status VARCHAR(20) DEFAULT 'To do',
    testing_review_status VARCHAR(20) DEFAULT 'To do',
    safety_review_status VARCHAR(20) DEFAULT 'To do',
    validation_count INTEGER DEFAULT 0,
    verification_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- IVV Card (formal validation/verification item) metadata
CREATE TABLE IF NOT EXISTS ivv_card_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    vvm_card_id UUID REFERENCES vvm_card_metadata(id),
    ivv_type VARCHAR(20) NOT NULL DEFAULT 'VALIDATION',
    requirement_impact VARCHAR(100),
    level VARCHAR(10),
    statement TEXT,
    path TEXT,
    change_tag VARCHAR(50),
    change_rationale TEXT,
    partition_name VARCHAR(100),
    product TEXT,
    equivalence TEXT,
    category_level INTEGER DEFAULT 0,
    mvv INTEGER DEFAULT 0,
    vvo_reference VARCHAR(100),
    sha VARCHAR(100),
    ivv_priority VARCHAR(10),
    test_case_impact VARCHAR(100),
    test_procedure_impact VARCHAR(100),
    evidence VARCHAR(100),
    tests_status VARCHAR(20) DEFAULT 'UNCOVERED',
    ac_variant VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Group metadata
CREATE TABLE IF NOT EXISTS group_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    impacted_team VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Sub-Change Card metadata
CREATE TABLE IF NOT EXISTS sub_change_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL UNIQUE,
    parent_change_card_id UUID,
    git_branch VARCHAR(200),
    pr_status VARCHAR(20),
    pr_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ivv_vvm_card ON ivv_card_metadata(vvm_card_id);
CREATE INDEX IF NOT EXISTS idx_sub_change_parent ON sub_change_metadata(parent_change_card_id);
