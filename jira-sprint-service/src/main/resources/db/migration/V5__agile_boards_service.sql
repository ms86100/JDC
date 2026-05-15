-- V5__agile_boards_service.sql
-- Enhanced Agile Board Service - Quick Filters, Swimlanes, WIP Limits

-- ============================================
-- AGILE BOARDS TABLE
-- Stores board configurations for Scrum/Kanban
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.agile_boards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_id UUID NOT NULL,
    board_type VARCHAR(50) NOT NULL DEFAULT 'SCRUM',
    filter_id UUID,
    jql_query TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    allow_all_issues BOOLEAN DEFAULT TRUE,
    card_layout VARCHAR(50) DEFAULT 'FULL',
    estimation_statistic VARCHAR(100),
    days_on_board INTEGER DEFAULT 5,
    last_viewed TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- BOARD COLUMNS TABLE
-- Configurable columns with WIP limits
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.board_columns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sequence INTEGER NOT NULL DEFAULT 0,
    status_category VARCHAR(50) DEFAULT 'TODO',
    is_done BOOLEAN DEFAULT FALSE,
    max_issues INTEGER,
    color VARCHAR(20) DEFAULT '#6c757d',
    is_collapsible BOOLEAN DEFAULT TRUE,
    is_hidden BOOLEAN DEFAULT FALSE
);

-- ============================================
-- BOARD SPRINTS TABLE
-- Links sprints to boards with state tracking
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.board_sprints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    sprint_id UUID NOT NULL REFERENCES jira_sprint.sprints(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL DEFAULT 0,
    state VARCHAR(20) DEFAULT 'FUTURE',
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    complete_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (board_id, sprint_id)
);

-- ============================================
-- QUICK FILTER PRESETS TABLE
-- User-defined quick filters for boards
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.quick_filter_presets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    user_id UUID,
    name VARCHAR(100) NOT NULL,
    jql_query TEXT NOT NULL,
    icon VARCHAR(50),
    is_system BOOLEAN DEFAULT FALSE,
    sequence INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- BOARD CONFIGURATIONS TABLE
-- Stores user preferences for boards
-- ============================================
CREATE TABLE IF NOT EXISTS jira_sprint.board_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL REFERENCES jira_sprint.agile_boards(id) ON DELETE CASCADE,
    user_id UUID,
    swimlane_field VARCHAR(50) DEFAULT 'none',
    collapsed_swimlanes TEXT[],  -- Array of swimlane keys
    card_color_field VARCHAR(50) DEFAULT 'none',
    show_work_vs_capacity BOOLEAN DEFAULT TRUE,
    default_view VARCHAR(20) DEFAULT 'board',
    column_order TEXT[],  -- Ordered column IDs
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (board_id, user_id)
);

-- ============================================
-- SEED DATA: Default Quick Filters
-- ============================================
INSERT INTO jira_sprint.quick_filter_presets (id, name, jql_query, icon, is_system, sequence) VALUES
    ('00000000-0000-0001-0001-000000000001', 'Assigned to Me', 'assignee = currentUser()', '👤', TRUE, 1),
    ('00000000-0000-0001-0001-000000000002', 'Reported by Me', 'reporter = currentUser()', '📝', TRUE, 2),
    ('00000000-0000-0001-0001-000000000003', 'Recently Updated', 'updated >= -1d', '🔄', TRUE, 3),
    ('00000000-0000-0001-0001-000000000004', 'Unassigned', 'assignee is empty', '❓', TRUE, 4),
    ('00000000-0000-0001-0001-000000000005', 'Has Due Date', 'duedate is not empty', '📅', TRUE, 5),
    ('00000000-0000-0001-0001-000000000006', 'High Priority', 'priority in (High, Highest)', '🔺', TRUE, 6),
    ('00000000-0000-0001-0001-000000000007', 'Blocked', 'status = "Blocked"', '🚧', TRUE, 7)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SEED DATA: Default Scrum Board Columns
-- ============================================
INSERT INTO jira_sprint.board_columns (id, board_id, name, sequence, status_category, is_done, max_issues, color) VALUES
    ('00000000-0000-0002-0001-000000000001', '00000000-0000-0002-0001-000000000000', 'Backlog', 0, 'TODO', FALSE, NULL, '#6c757d'),
    ('00000000-0000-0002-0001-000000000002', '00000000-0000-0002-0001-000000000000', 'To Do', 1, 'TODO', FALSE, NULL, '#6c757d'),
    ('00000000-0000-0002-0001-000000000003', '00000000-0000-0002-0001-000000000000', 'In Progress', 2, 'IN_PROGRESS', FALSE, 5, '#0066ff'),
    ('00000000-0000-0002-0001-000000000004', '00000000-0000-0002-0001-000000000000', 'In Review', 3, 'IN_REVIEW', FALSE, 3, '#ff9200'),
    ('00000000-0000-0002-0001-000000000005', '00000000-0000-0002-0001-000000000000', 'Done', 4, 'DONE', TRUE, NULL, '#28a745')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- SEED DATA: Default Kanban Board Columns
-- ============================================
INSERT INTO jira_sprint.board_columns (id, board_id, name, sequence, status_category, is_done, max_issues, color) VALUES
    ('00000000-0000-0002-0002-000000000001', '00000000-0000-0002-0002-000000000000', 'To Do', 0, 'TODO', FALSE, NULL, '#6c757d'),
    ('00000000-0000-0002-0002-000000000002', '00000000-0000-0002-0002-000000000000', 'In Progress', 1, 'IN_PROGRESS', FALSE, 10, '#0066ff'),
    ('00000000-0000-0002-0002-000000000003', '00000000-0000-0002-0002-000000000000', 'Done', 2, 'DONE', TRUE, NULL, '#28a745')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_agile_boards_project ON jira_sprint.agile_boards(project_id);
CREATE INDEX IF NOT EXISTS idx_agile_boards_type ON jira_sprint.agile_boards(board_type);
CREATE INDEX IF NOT EXISTS idx_board_columns_board ON jira_sprint.board_columns(board_id);
CREATE INDEX IF NOT EXISTS idx_board_columns_sequence ON jira_sprint.board_columns(board_id, sequence);
CREATE INDEX IF NOT EXISTS idx_board_sprints_board ON jira_sprint.board_sprints(board_id);
CREATE INDEX IF NOT EXISTS idx_board_sprints_sprint ON jira_sprint.board_sprints(sprint_id);
CREATE INDEX IF NOT EXISTS idx_quick_filters_board ON jira_sprint.quick_filter_presets(board_id);
CREATE INDEX IF NOT EXISTS idx_quick_filters_user ON jira_sprint.quick_filter_presets(user_id);
CREATE INDEX IF NOT EXISTS idx_board_configs_board ON jira_sprint.board_configs(board_id);

-- ============================================
-- FUNCTION: Get WIP Status for Column
-- ============================================
CREATE OR REPLACE FUNCTION jira_sprint.get_wip_status(
    p_board_id UUID,
    p_status_category VARCHAR
) RETURNS TABLE(max_issues INTEGER, current_count BIGINT, status VARCHAR) AS $$
BEGIN
    RETURN QUERY
    SELECT
        bc.max_issues,
        COUNT(i.id)::BIGINT as current_count,
        CASE
            WHEN bc.max_issues IS NULL THEN 'ok'
            WHEN COUNT(i.id) >= bc.max_issues THEN 'exceeded'
            WHEN COUNT(i.id) >= bc.max_issues * 0.8 THEN 'warning'
            ELSE 'ok'
        END as status
    FROM jira_sprint.board_columns bc
    LEFT JOIN jira_issue.issues i ON i.status ILIKE '%' || LOWER(bc.status_category) || '%'
    WHERE bc.board_id = p_board_id
    AND bc.status_category = p_status_category
    GROUP BY bc.max_issues;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE jira_sprint.agile_boards IS 'Agile boards for Scrum and Kanban management';
COMMENT ON TABLE jira_sprint.board_columns IS 'Configurable columns with optional WIP limits';
COMMENT ON TABLE jira_sprint.board_sprints IS 'Links sprints to boards for Scrum planning';
COMMENT ON TABLE jira_sprint.quick_filter_presets IS 'User-defined JQL quick filters';
COMMENT ON TABLE jira_sprint.board_configs IS 'User preferences for board display';