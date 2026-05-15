-- V2__create_plan_schema.sql
-- Create plans table with JSONB settings

CREATE TABLE jira_plan.plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    settings JSONB DEFAULT '{}',
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plans_owner_id ON jira_plan.plans(owner_id);
CREATE INDEX idx_plans_dates ON jira_plan.plans(start_date, end_date);
CREATE INDEX idx_plans_settings ON jira_plan.plans USING GIN(settings);
CREATE INDEX idx_plans_is_active ON jira_plan.plans(is_active);

-- Comments
COMMENT ON TABLE jira_plan.plans IS 'Plans - Roadmap containers with timeline and scheduling';
COMMENT ON COLUMN jira_plan.plans.settings IS 'JSONB containing warnings, scheduling config, group-by mode';
COMMENT ON COLUMN jira_plan.plans.start_date IS 'Planned start date for the plan';
COMMENT ON COLUMN jira_plan.plans.end_date IS 'Planned end date for the plan';