-- V20__add_board_config_audit_log.sql
-- Add board configuration audit log table for tracking board changes

CREATE TABLE jira_plan.board_config_audit_log (
    id BIGSERIAL PRIMARY KEY,
    board_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_board_audit_board ON jira_plan.board_config_audit_log(board_id);
CREATE INDEX idx_board_audit_created ON jira_plan.board_config_audit_log(created_at);
CREATE INDEX idx_board_audit_board_event ON jira_plan.board_config_audit_log(board_id, event_type);

COMMENT ON TABLE jira_plan.board_config_audit_log IS 'Audit log for board configuration changes';
COMMENT ON COLUMN jira_plan.board_config_audit_log.board_id IS 'Reference to the board being audited';
COMMENT ON COLUMN jira_plan.board_config_audit_log.event_type IS 'Type of event: BOARD_CREATED, BOARD_UPDATED, BOARD_DELETED, COLUMN_ADDED, COLUMN_UPDATED, COLUMN_DELETED, COLUMNS_REORDERED, FILTER_ADDED, FILTER_DELETED, SWIMLANE_ADDED, SWIMLANE_DELETED, COLOR_ADDED, COLOR_DELETED';
COMMENT ON COLUMN jira_plan.board_config_audit_log.details IS 'JSON containing additional event details';