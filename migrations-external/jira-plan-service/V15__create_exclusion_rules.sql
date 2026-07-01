
-- V15__create_exclusion_rules.sql
-- Creates the exclusion_rules table for filtering issues from plans

CREATE TABLE jira_plan.exclusion_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    operator VARCHAR(50) NOT NULL,
    field_value VARCHAR(500) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_exclusion_rules_plan_id ON jira_plan.exclusion_rules(plan_id);
CREATE INDEX idx_exclusion_rules_field_name ON jira_plan.exclusion_rules(field_name);
CREATE INDEX idx_exclusion_rules_active ON jira_plan.exclusion_rules(plan_id, is_active);

-- Comments
COMMENT ON TABLE jira_plan.exclusion_rules IS 'Stores exclusion rules to filter issues from plans';
COMMENT ON COLUMN jira_plan.exclusion_rules.field_name IS 'Jira field name to filter on (issuetype, status, labels, etc.)';
COMMENT ON COLUMN jira_plan.exclusion_rules.operator IS 'Comparison operator: EQUALS, NOT_EQUALS, CONTAINS, IN, etc.';
COMMENT ON COLUMN jira_plan.exclusion_rules.field_value IS 'Value(s) to filter by';
COMMENT ON COLUMN jira_plan.exclusion_rules.is_active IS 'Soft delete flag';