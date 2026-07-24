-- ============================================
-- BOARD CONFIGURATION — JIRA DC PARITY
-- Tasks 2.2-2.7: Release Hub, Parallel Sprints, Kanban Backlog,
-- Days in Column, Simplified Workflow, Board Administrators,
-- Swimlanes, Card Colors, Card Fields, Issue Detail View,
-- Working Days, Sprint Scope Change
-- ============================================

-- Task 2.4: Kanban backlog, sub-filter, hide completed
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS kanban_backlog_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS sub_filter TEXT;
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS hide_completed_after_days INTEGER DEFAULT 14;

-- Task 2.6: Simplified workflow
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS use_simplified_workflow BOOLEAN DEFAULT FALSE;

-- Task 3.1: Estimation tracking
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS time_tracking VARCHAR(50) DEFAULT 'NONE';

-- Task 3.6: Working days
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS timezone VARCHAR(100);
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS working_days VARCHAR(50) DEFAULT 'MON,TUE,WED,THU,FRI';
ALTER TABLE agile_boards ADD COLUMN IF NOT EXISTS non_working_dates TEXT;

-- Task 2.5: Days in column indicator
ALTER TABLE board_columns ADD COLUMN IF NOT EXISTS show_days_in_column BOOLEAN DEFAULT FALSE;

-- Task 3.8: Sprint scope change tracking
ALTER TABLE sprint_issues ADD COLUMN IF NOT EXISTS added_at TIMESTAMP DEFAULT NOW();
ALTER TABLE sprint_issues ADD COLUMN IF NOT EXISTS removed_at TIMESTAMP;
ALTER TABLE sprint_issues ADD COLUMN IF NOT EXISTS removed_reason VARCHAR(200);

-- Task 2.7: Board administrators
CREATE TABLE IF NOT EXISTS board_administrators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    holder_id UUID NOT NULL,
    holder_type VARCHAR(20) NOT NULL DEFAULT 'USER',
    added_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ba_board ON board_administrators(board_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ba_board_holder ON board_administrators(board_id, holder_id, holder_type);

-- Task 3.2: Query-based swimlanes
CREATE TABLE IF NOT EXISTS board_swimlanes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    jql_query TEXT,
    description TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bsw_board ON board_swimlanes(board_id);

-- Task 3.3: Card color rules
CREATE TABLE IF NOT EXISTS board_card_color_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    color_method VARCHAR(30) NOT NULL,
    match_value TEXT,
    color VARCHAR(10) DEFAULT '#6c757d',
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bccr_board ON board_card_color_rules(board_id);

-- Task 3.4: Card additional fields (max 3)
CREATE TABLE IF NOT EXISTS board_card_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    field_id VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL CHECK (position BETWEEN 1 AND 3),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bcf_board ON board_card_fields(board_id);

-- Task 3.5: Issue detail view fields
CREATE TABLE IF NOT EXISTS board_issue_detail_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    field_id VARCHAR(100) NOT NULL,
    field_group VARCHAR(30) DEFAULT 'GENERAL',
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_bidf_board ON board_issue_detail_fields(board_id);

-- Task 4.2: CFD snapshots
CREATE TABLE IF NOT EXISTS board_cfd_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    column_id UUID,
    column_name VARCHAR(100),
    status_category VARCHAR(30),
    issue_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(board_id, snapshot_date, column_id)
);
CREATE INDEX IF NOT EXISTS idx_bcfd_board_date ON board_cfd_snapshots(board_id, snapshot_date);

-- Task 4.4: Filter subscriptions
CREATE TABLE IF NOT EXISTS filter_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filter_id UUID NOT NULL,
    user_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    email_address VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_fs_filter ON filter_subscriptions(filter_id);
CREATE INDEX IF NOT EXISTS idx_fs_user ON filter_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_fs_next_run ON filter_subscriptions(next_run_at);
