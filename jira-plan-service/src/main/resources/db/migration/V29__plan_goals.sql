-- V29__plan_goals.sql
-- BigPicture Goals Module: hierarchical goals linked to plans and epics

CREATE TABLE IF NOT EXISTS jira_plan.plan_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    -- Values: NOT_STARTED, IN_PROGRESS, AT_RISK, ON_TRACK, COMPLETED
    target_date DATE,
    progress INTEGER NOT NULL DEFAULT 0,
    parent_goal_id UUID,
    linked_epic_ids TEXT[],
    color VARCHAR(20),
    owner_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_plan_goals_plan FOREIGN KEY (plan_id) REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_goals_parent FOREIGN KEY (parent_goal_id) REFERENCES jira_plan.plan_goals(id) ON DELETE SET NULL,
    CONSTRAINT chk_plan_goals_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'AT_RISK', 'ON_TRACK', 'COMPLETED')),
    CONSTRAINT chk_plan_goals_progress CHECK (progress >= 0 AND progress <= 100)
);

CREATE INDEX IF NOT EXISTS idx_plan_goals_plan_id ON jira_plan.plan_goals(plan_id);
CREATE INDEX IF NOT EXISTS idx_plan_goals_parent ON jira_plan.plan_goals(parent_goal_id);
CREATE INDEX IF NOT EXISTS idx_plan_goals_status ON jira_plan.plan_goals(status);
CREATE INDEX IF NOT EXISTS idx_plan_goals_owner ON jira_plan.plan_goals(owner_user_id);
