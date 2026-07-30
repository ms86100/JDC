-- V3__create_program_plan_table.sql
-- Many-to-many relationship between Programs and Plans

CREATE TABLE jira_plan.program_plans (
    program_id UUID NOT NULL REFERENCES jira_plan.programs(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (program_id, plan_id)
);

CREATE INDEX idx_program_plans_program ON jira_plan.program_plans(program_id);
CREATE INDEX idx_program_plans_plan ON jira_plan.program_plans(plan_id);

-- Comments
COMMENT ON TABLE jira_plan.program_plans IS 'Links Programs to Plans (many-to-many)';
COMMENT ON COLUMN jira_plan.program_plans.linked_at IS 'When the plan was linked to the program';