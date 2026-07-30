-- Test Folder Table - V7
-- Folder hierarchy for organizing tests

-- ============================================
-- TEST FOLDER TABLE
-- ============================================

CREATE TABLE test_folder (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description VARCHAR,
    project_id UUID NOT NULL,
    parent_id UUID REFERENCES test_folder(id),
    folder_type VARCHAR(50) DEFAULT 'FOLDER', -- FOLDER, SMART_FOLDER, TEST_SET_FOLDER
    path TEXT, -- Full path like /parent/child/grandchild
    depth INTEGER DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED
    owner_id UUID,
    icon TEXT, -- emoji or icon class
    color TEXT, -- hex color for UI
    filter_criteria TEXT, -- JSON for smart folders
    is_starred BOOLEAN DEFAULT FALSE,
    is_expanded BOOLEAN DEFAULT TRUE,
    tags TEXT[], -- ['smoke', 'regression']
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_test_folder_project ON test_folder(project_id);
CREATE INDEX idx_test_folder_parent ON test_folder(parent_id);
CREATE INDEX idx_test_folder_path ON test_folder(path);
CREATE INDEX idx_test_folder_type ON test_folder(folder_type);
CREATE INDEX idx_test_folder_starred ON test_folder(project_id, is_starred);
CREATE INDEX idx_test_folder_status ON test_folder(status);
CREATE INDEX idx_test_folder_depth ON test_folder(depth);
CREATE INDEX idx_test_folder_sort ON test_folder(project_id, sort_order);

-- Add folder_id column to test_issue table
ALTER TABLE test_issue ADD COLUMN IF NOT EXISTS folder_id UUID REFERENCES test_folder(id);

-- Index for folder lookups
CREATE INDEX idx_test_issue_folder ON test_issue(folder_id);

-- ============================================
-- SEED DATA
-- ============================================

-- Example folders for testing
-- Note: These use example project IDs, replace with actual project UUIDs in production
-- INSERT INTO test_folder (name, description, project_id, folder_type) VALUES
--     ('Smoke Tests', 'Quick validation tests', gen_random_uuid(), 'FOLDER'),
--     ('Regression Suite', 'Full regression test suite', gen_random_uuid(), 'FOLDER'),
--     ('API Tests', 'API endpoint tests', gen_random_uuid(), 'FOLDER');