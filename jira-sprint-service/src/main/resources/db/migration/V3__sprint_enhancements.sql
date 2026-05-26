-- V3__sprint_enhancements.sql
-- Sprint Service Database Enhancements - Sprint Goals, Capacity, Burndown Support

CREATE SCHEMA IF NOT EXISTS jira_sprint;

-- ============================================
-- SPRINT CAPACITY TABLE
-- Track team member capacity per sprint
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.sprint_capacity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    capacity_hours DECIMAL(10,2) DEFAULT 40.00,
    committed_hours DECIMAL(10,2) DEFAULT 0.00,
    completed_hours DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sprint_id, user_id)
);

-- ============================================
-- SPRINT BURNDOWN TRACKING
-- Daily snapshots for burndown charts
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.sprint_burndown (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    total_issues INTEGER DEFAULT 0,
    total_story_points DECIMAL(10,2) DEFAULT 0,
    completed_issues INTEGER DEFAULT 0,
    completed_story_points DECIMAL(10,2) DEFAULT 0,
    remaining_hours DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sprint_id, snapshot_date)
);

-- ============================================
-- SPRINT GOALS TABLE
-- Extended sprint goal tracking with status
-- ============================================
ALTER TABLE jira_sprint.sprints
    ADD COLUMN IF NOT EXISTS goal TEXT,
    ADD COLUMN IF NOT EXISTS goal_status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    ADD COLUMN IF NOT EXISTS commitment_level DECIMAL(5,2) DEFAULT 100.00,
    ADD COLUMN IF NOT EXISTS velocity_history DECIMAL(10,2)[];

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_sprint_capacity_sprint ON jira_sprint.sprint_capacity(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_capacity_user ON jira_sprint.sprint_capacity(user_id);
CREATE INDEX IF NOT EXISTS idx_sprint_burndown_sprint ON jira_sprint.sprint_burndown(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_burndown_date ON jira_sprint.sprint_burndown(snapshot_date);

-- ============================================
-- FUNCTION: Update Sprint Burndown
-- ============================================
CREATE OR REPLACE FUNCTION jira_sprint.update_sprint_burndown(p_sprint_id UUID)
RETURNS VOID AS $$
BEGIN
    INSERT INTO jira_sprint.sprint_burndown (sprint_id, snapshot_date, total_issues, completed_issues)
    SELECT
        p_sprint_id,
        CURRENT_DATE,
        COUNT(*) as total,
        COUNT(*) FILTER (WHERE i.status IN ('DONE', 'CLOSED', 'RESOLVED')) as completed
    FROM jira_issue.issues i
    JOIN jira_sprint.sprint_issues si ON si.issue_id = i.id
    WHERE si.sprint_id = p_sprint_id
    ON CONFLICT (sprint_id, snapshot_date) DO UPDATE SET
        total_issues = EXCLUDED.total_issues,
        completed_issues = EXCLUDED.completed_issues,
        total_story_points = COALESCE(i.story_points, 0),
        completed_story_points = COALESCE(i.story_points, 0) FILTER (WHERE i.status IN ('DONE', 'CLOSED', 'RESOLVED'));
END;
$$ LANGUAGE plpgsql;