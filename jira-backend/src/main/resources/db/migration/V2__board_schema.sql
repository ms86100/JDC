-- Board Schema
CREATE TABLE boards (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL DEFAULT 'kanban',
    card_layout VARCHAR(20) NOT NULL DEFAULT 'FULL',
    owner_id VARCHAR(36) NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_board_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_board_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX idx_boards_project ON boards(project_id);
CREATE INDEX idx_boards_owner ON boards(owner_id);

-- Board Columns
CREATE TABLE board_columns (
    id VARCHAR(36) PRIMARY KEY,
    board_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) DEFAULT '#0065ff',
    status_category VARCHAR(20) NOT NULL,
    status VARCHAR(100),
    position INTEGER NOT NULL DEFAULT 0,
    max_issues INTEGER,
    is_hidden BOOLEAN DEFAULT FALSE,
    is_done BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_column_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE
);

CREATE INDEX idx_columns_board ON board_columns(board_id);
CREATE INDEX idx_columns_position ON board_columns(board_id, position);

-- Board Issues (junction for board-column relationship)
CREATE TABLE board_issues (
    id VARCHAR(36) PRIMARY KEY,
    board_id VARCHAR(36) NOT NULL,
    issue_id VARCHAR(36) NOT NULL,
    column_id VARCHAR(36),
    position INTEGER NOT NULL DEFAULT 0,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_board_issue_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT fk_board_issue_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_board_issue_column FOREIGN KEY (column_id) REFERENCES board_columns(id) ON DELETE SET NULL
);

CREATE INDEX idx_board_issues_board ON board_issues(board_id);
CREATE INDEX idx_board_issues_issue ON board_issues(issue_id);
CREATE UNIQUE INDEX idx_board_issue_unique ON board_issues(board_id, issue_id);

-- Board Permissions
CREATE TABLE board_permissions (
    id VARCHAR(36) PRIMARY KEY,
    board_id VARCHAR(36) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    principal_id VARCHAR(36) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_permission_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT chk_principal_type CHECK (principal_type IN ('user', 'group'))
);

CREATE INDEX idx_permissions_board ON board_permissions(board_id);
CREATE UNIQUE INDEX idx_permissions_unique ON board_permissions(board_id, principal_type, principal_id);

-- Board Favorites (quick access)
CREATE TABLE board_favorites (
    user_id VARCHAR(36) NOT NULL,
    board_id VARCHAR(36) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, board_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE
);