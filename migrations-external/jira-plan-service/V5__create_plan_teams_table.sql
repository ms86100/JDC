-- V5__create_plan_teams_table.sql
-- Team management within Plans

CREATE TABLE jira_plan.plan_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plan_teams_plan_id ON jira_plan.plan_teams(plan_id);
CREATE INDEX idx_plan_teams_is_active ON jira_plan.plan_teams(is_active);

-- Team members table
CREATE TABLE jira_plan.plan_team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES jira_plan.plan_teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    user_name VARCHAR(255),
    capacity_hours DECIMAL(5,2) DEFAULT 40.00,
    role VARCHAR(50),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_team_member_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_plan_team_members_team_id ON jira_plan.plan_team_members(team_id);
CREATE INDEX idx_plan_team_members_user_id ON jira_plan.plan_team_members(user_id);

-- Comments
COMMENT ON TABLE jira_plan.plan_teams IS 'Teams working on a Plan';
COMMENT ON TABLE jira_plan.plan_team_members IS 'Members of Plan teams with capacity';
COMMENT ON COLUMN jira_plan.plan_team_members.capacity_hours IS 'Weekly capacity in hours';