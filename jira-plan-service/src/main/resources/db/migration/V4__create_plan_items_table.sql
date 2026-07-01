-- V4__create_plan_items_table.sql
-- Backlog items with LexoRank ordering

CREATE TABLE jira_plan.plan_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    issue_key VARCHAR(50),
    issue_title VARCHAR(500),
    issue_type VARCHAR(20) NOT NULL,
    parent_id UUID,
    sort_order VARCHAR(255) NOT NULL,
    target_date DATE,
    status VARCHAR(50),
    status_category VARCHAR(50),
    story_points INTEGER,
    assignee_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_plan_items_plan_issue UNIQUE (plan_id, issue_id)
);

CREATE INDEX idx_plan_items_plan_id ON jira_plan.plan_items(plan_id);
CREATE INDEX idx_plan_items_parent ON jira_plan.plan_items(parent_id);
CREATE INDEX idx_plan_items_sort ON jira_plan.plan_items(plan_id, sort_order);
CREATE INDEX idx_plan_items_issue_type ON jira_plan.plan_items(issue_type);
CREATE INDEX idx_plan_items_assignee ON jira_plan.plan_items(assignee_id);
CREATE INDEX idx_plan_items_status ON jira_plan.plan_items(status);

-- Comments
COMMENT ON TABLE jira_plan.plan_items IS 'Backlog items (Epics, Stories, Subtasks) within a Plan';
COMMENT ON COLUMN jira_plan.plan_items.sort_order IS 'LexoRank string for ordering';
COMMENT ON COLUMN jira_plan.plan_items.issue_type IS 'EPIC, STORY, or SUBTASK';
COMMENT ON COLUMN jira_plan.plan_items.parent_id IS 'Parent item for hierarchy (Story under Epic, Subtask under Story)';