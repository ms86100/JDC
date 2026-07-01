-- Cumulative Flow Diagram snapshots
CREATE TABLE IF NOT EXISTS jira_plan.cumulative_flow_snapshots (
    id BIGSERIAL PRIMARY KEY,
    board_id UUID NOT NULL,
    sprint_id UUID,
    snapshot_date DATE NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    issue_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(board_id, snapshot_date, column_name)
);

CREATE INDEX IF NOT EXISTS idx_cfs_board_date ON jira_plan.cumulative_flow_snapshots(board_id, snapshot_date);
