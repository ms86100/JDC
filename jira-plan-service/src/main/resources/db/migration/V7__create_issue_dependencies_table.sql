-- V7__create_issue_dependencies_table.sql
-- Issue dependency tracking

CREATE TABLE jira_plan.issue_dependencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    blocking_issue_id UUID NOT NULL,
    blocking_issue_key VARCHAR(50),
    blocked_issue_id UUID NOT NULL,
    blocked_issue_key VARCHAR(50),
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_issue_dependency UNIQUE (plan_id, blocking_issue_id, blocked_issue_id)
);

CREATE INDEX idx_issue_dependencies_plan_id ON jira_plan.issue_dependencies(plan_id);
CREATE INDEX idx_issue_dependencies_blocking ON jira_plan.issue_dependencies(blocking_issue_id);
CREATE INDEX idx_issue_dependencies_blocked ON jira_plan.issue_dependencies(blocked_issue_id);
CREATE INDEX idx_issue_dependencies_type ON jira_plan.issue_dependencies(dependency_type);

-- Comments
COMMENT ON TABLE jira_plan.issue_dependencies IS 'Tracks dependencies between issues in a Plan';
COMMENT ON COLUMN jira_plan.issue_dependencies.dependency_type IS 'BLOCKS, IS_BLOCKED_BY, RELATES_TO, etc.';