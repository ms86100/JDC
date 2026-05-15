-- V1__create_program_schema.sql
-- Create the jira_plan schema and programs table

CREATE SCHEMA IF NOT EXISTS jira_plan;

CREATE TABLE jira_plan.programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    access_type VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_programs_owner_id ON jira_plan.programs(owner_id);
CREATE INDEX idx_programs_access_type ON jira_plan.programs(access_type);
CREATE INDEX idx_programs_is_active ON jira_plan.programs(is_active);

-- Comments
COMMENT ON TABLE jira_plan.programs IS 'Programs - Top-level containers grouping multiple Plans';
COMMENT ON COLUMN jira_plan.programs.access_type IS 'OPEN (anyone can view) or RESTRICTED';
COMMENT ON COLUMN jira_plan.programs.owner_id IS 'User who created the program';