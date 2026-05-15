-- Sprint management with full lifecycle
-- Mirrors Jira GreenHopper sprint implementation

-- Sprint states enum
CREATE TYPE sprint_state AS ENUM ('FUTURE', 'ACTIVE', 'CLOSED', 'ABANDONED');

-- Main sprint entity
CREATE TABLE jira_plan.sprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    goal TEXT,  -- Sprint goal description
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    complete_date TIMESTAMP,  -- When sprint was closed
    state VARCHAR(20) DEFAULT 'FUTURE',
    sequence INTEGER,
    velocity INTEGER DEFAULT 0,  -- Story points completed
    committed_points INTEGER DEFAULT 0,  -- Points committed at start
    completed_points INTEGER DEFAULT 0,  -- Points completed at close
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint issue linking
CREATE TABLE jira_plan.sprint_issues (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    plan_item_id UUID REFERENCES jira_plan.plan_items(id) ON DELETE CASCADE,
    issue_id UUID NOT NULL,
    rank_value VARCHAR(255),  -- LexoRank within sprint
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by UUID,
    removed_at TIMESTAMP,
    removed_by UUID,
    completion_status VARCHAR(50),  -- 'UNCOMPLETED', 'COMPLETED', 'DROPPED'
    completed_at TIMESTAMP,
    UNIQUE(sprint_id, plan_item_id)
);

-- Sprint audit log for tracking changes
CREATE TABLE jira_plan.sprint_audit_log (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,  -- 'CREATED', 'STARTED', 'CLOSED', 'ABANDONED', 'ISSUE_ADDED', 'ISSUE_REMOVED', 'UPDATED'
    user_id UUID,
    details JSONB,  -- Additional event details
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sprint burndown snapshot (daily data points)
CREATE TABLE jira_plan.sprint_burndown (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    total_issues INTEGER NOT NULL DEFAULT 0,
    completed_issues INTEGER NOT NULL DEFAULT 0,
    remaining_points INTEGER NOT NULL DEFAULT 0,
    ideal_remaining INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sprint_id, snapshot_date)
);

-- Sprint goal change history
CREATE TABLE jira_plan.sprint_goal_history (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    old_goal TEXT,
    new_goal TEXT,
    changed_by UUID,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_sprints_board ON jira_plan.sprints(board_id);
CREATE INDEX idx_sprints_state ON jira_plan.sprints(state);
CREATE INDEX idx_sprints_board_state ON jira_plan.sprints(board_id, state);
CREATE INDEX idx_sprints_start_date ON jira_plan.sprints(start_date);
CREATE INDEX idx_sprint_issues_sprint ON jira_plan.sprint_issues(sprint_id);
CREATE INDEX idx_sprint_issues_issue ON jira_plan.sprint_issues(issue_id);
CREATE INDEX idx_sprint_audit_sprint ON jira_plan.sprint_audit_log(sprint_id);
CREATE INDEX idx_sprint_audit_created ON jira_plan.sprint_audit_log(created_at);
CREATE INDEX idx_sprint_burndown_sprint ON jira_plan.sprint_burndown(sprint_id);
CREATE INDEX idx_sprint_burndown_date ON jira_plan.sprint_burndown(snapshot_date);