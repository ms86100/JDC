-- V4__sprint_metrics_and_stats.sql
-- Sprint Service Database Extensions - Metrics, Statistics, and Reporting

CREATE SCHEMA IF NOT EXISTS jira_sprint;

-- ============================================
-- SPRINT METRICS TABLE
-- Historical sprint performance metrics
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.sprint_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    metric_value DECIMAL(15,2),
    metric_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sprint_id, metric_date, metric_type)
);

-- ============================================
-- SPRINT SNAPSHOTS TABLE
-- Point-in-time snapshots for reporting
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.sprint_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    snapshot_type VARCHAR(50) DEFAULT 'DAILY',
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEAM VELOCITY TABLE
-- Track team velocity over sprints
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.team_velocity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    velocity_type VARCHAR(50) DEFAULT 'STORY_POINTS',
    velocity_value DECIMAL(10,2) NOT NULL,
    issues_completed INTEGER DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, sprint_id)
);

-- ============================================
-- SPRINT PARTICIPANTS TABLE
-- Track who's involved in each sprint
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.sprint_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'DEVELOPER',
    is_active BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (sprint_id, user_id)
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_sprint_metrics_sprint ON jira_sprint.sprint_metrics(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_metrics_type ON jira_sprint.sprint_metrics(metric_type);
CREATE INDEX IF NOT EXISTS idx_sprint_snapshots_sprint ON jira_sprint.sprint_snapshots(sprint_id);
CREATE INDEX IF NOT EXISTS idx_team_velocity_project ON jira_sprint.team_velocity(project_id);
CREATE INDEX IF NOT EXISTS idx_sprint_participants_sprint ON jira_sprint.sprint_participants(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_participants_user ON jira_sprint.sprint_participants(user_id);

-- ============================================
-- FUNCTION: Calculate Sprint Velocity
-- ============================================
CREATE OR REPLACE FUNCTION jira_sprint.calculate_sprint_velocity(p_sprint_id UUID)
RETURNS DECIMAL(10,2) AS $$
DECLARE
    v_velocity DECIMAL(10,2);
BEGIN
    SELECT COALESCE(SUM(i.story_points), 0) INTO v_velocity
    FROM jira_issue.issues i
    JOIN jira_sprint.sprint_issues si ON si.issue_id = i.id
    WHERE si.sprint_id = p_sprint_id
    AND i.status IN ('DONE', 'CLOSED', 'RESOLVED');

    RETURN v_velocity;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- FUNCTION: Get Sprint Burndown Data
-- ============================================
CREATE OR REPLACE FUNCTION jira_sprint.get_sprint_burndown_data(p_sprint_id UUID)
RETURNS TABLE(snapshot_date DATE, total_story_points DECIMAL, completed_story_points DECIMAL, remaining DECIMAL) AS $$
BEGIN
    RETURN QUERY
    SELECT
        sd.snapshot_date,
        sd.total_story_points,
        sd.completed_story_points,
        sd.total_story_points - sd.completed_story_points as remaining
    FROM jira_sprint.sprint_burndown sd
    WHERE sd.sprint_id = p_sprint_id
    ORDER BY sd.snapshot_date;
END;
$$ LANGUAGE plpgsql;