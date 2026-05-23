-- Phase 7: Board card layout (admin-selected custom fields on cards)
CREATE TABLE IF NOT EXISTS jira_migration.board_card_layout_fields (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id UUID NOT NULL,
    project_id UUID,
    field_key VARCHAR(255) NOT NULL,
    display_order INT DEFAULT 0,
    position VARCHAR(20) DEFAULT 'BOTTOM',
    visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_board_card_field UNIQUE (board_id, field_key)
);

CREATE INDEX idx_bclf_board ON jira_migration.board_card_layout_fields(board_id);
CREATE INDEX idx_bclf_project ON jira_migration.board_card_layout_fields(project_id);

-- Phase 9: Dashboard gadget field bindings
CREATE TABLE IF NOT EXISTS jira_migration.dashboard_gadget_field_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dashboard_key VARCHAR(128) NOT NULL DEFAULT 'system',
    gadget_key VARCHAR(128) NOT NULL,
    field_key VARCHAR(255) NOT NULL,
    chart_type VARCHAR(64),
    display_order INT DEFAULT 0,
    config JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_dashboard_gadget_field UNIQUE (dashboard_key, gadget_key, field_key)
);

CREATE INDEX idx_dgfc_gadget ON jira_migration.dashboard_gadget_field_config(gadget_key);
