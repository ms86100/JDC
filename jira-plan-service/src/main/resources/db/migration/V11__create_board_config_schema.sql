CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Board/RapidView configuration
-- Core board structure with columns, filters, swimlanes, and card styling

-- Main board configuration
CREATE TABLE jira_plan.board_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    board_type VARCHAR(20) NOT NULL,  -- 'SCRUM', 'KANBAN', 'SUSPEND'
    column_config_mode VARCHAR(20) DEFAULT 'DEFAULT',  -- 'DEFAULT', 'LABEL', 'STATUS', 'COMPONENT'
    constraint_source VARCHAR(50),  -- Where column constraints come from: 'STATUS', 'LABEL', 'COMPONENT'
    is_enabled BOOLEAN DEFAULT TRUE,
    card_layout_mode VARCHAR(20) DEFAULT 'COMPACT',  -- 'COMPACT', 'FULL'
    default_swimlane VARCHAR(50) DEFAULT 'NONE',  -- 'NONE', 'EPIC', 'ASSIGNEE', 'PROJECT', 'PRIORITY'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Board columns (status or label based columns)
CREATE TABLE jira_plan.board_columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sequence INTEGER NOT NULL,
    status_mapping JSONB DEFAULT '[]',  -- Array of status IDs that map to this column
    label_values JSONB DEFAULT '[]',  -- Array of label values for LABEL mode
    min_width INTEGER DEFAULT 100,
    max_width INTEGER DEFAULT 600,
    color VARCHAR(7),  -- Hex color for column header
    max_issues INTEGER,  -- WIP limit (NULL = unlimited)
    constraint_status VARCHAR(50),  -- 'TODO', 'IN_PROGRESS', 'DONE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Quick filters (saved JQL queries)
CREATE TABLE jira_plan.board_quick_filters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    filter_query TEXT NOT NULL,  -- JQL query
    sequence INTEGER NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    icon VARCHAR(50),  -- Icon name for quick filter button
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Swimlane configuration (row grouping)
CREATE TABLE jira_plan.board_swimlanes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    grouping_field VARCHAR(50) NOT NULL,  -- 'NONE', 'EPIC', 'ASSIGNEE', 'PROJECT', 'PRIORITY', 'LABEL'
    enabled BOOLEAN DEFAULT TRUE,
    collapsed_by_default BOOLEAN DEFAULT FALSE,
    sequence INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Card color rules (conditional coloring)
CREATE TABLE jira_plan.board_card_colors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(7) NOT NULL,  -- Hex color (e.g., '#ff0000')
    conditions JSONB NOT NULL,  -- Field conditions: [{"field": "priority", "operator": "EQUALS", "value": "High"}]
    -- Supported operators: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, IN, NOT_IN, IS, IS_NOT
    sequence INTEGER NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Detail view field configuration (fields shown on card hover/click)
CREATE TABLE jira_plan.board_detail_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,  -- 'summary', 'priority', 'assignee', 'reporter', 'labels', etc.
    field_label VARCHAR(255),  -- Custom display label
    sequence INTEGER NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE,
    field_type VARCHAR(50) DEFAULT 'STANDARD',  -- 'STANDARD', 'CUSTOM', 'ESCALATION'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Card layout field configuration (fields shown on card face)
CREATE TABLE jira_plan.board_card_layout_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES jira_plan.board_configs(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    sequence INTEGER NOT NULL,
    position VARCHAR(20) DEFAULT 'LEFT',  -- 'LEFT', 'RIGHT', 'BOTTOM'
    is_visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_board_configs_plan ON jira_plan.board_configs(plan_id);
CREATE INDEX idx_board_columns_board ON jira_plan.board_columns(board_id);
CREATE INDEX idx_board_columns_sequence ON jira_plan.board_columns(board_id, sequence);
CREATE INDEX idx_board_quick_filters_board ON jira_plan.board_quick_filters(board_id);
CREATE INDEX idx_board_swimlanes_board ON jira_plan.board_swimlanes(board_id);
CREATE INDEX idx_board_card_colors_board ON jira_plan.board_card_colors(board_id);
CREATE INDEX idx_board_detail_fields_board ON jira_plan.board_detail_fields(board_id);
CREATE INDEX idx_board_card_layout_fields_board ON jira_plan.board_card_layout_fields(board_id);