-- V3__status_crud_enhancements.sql
-- Enterprise-Grade Status CRUD Implementation
-- Adds missing fields to support full Jira DC Status management

-- ============================================
-- ENHANCE STATUSES TABLE
-- ============================================

-- Add new columns if they don't exist
ALTER TABLE jira_admin.statuses
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS lookup_group VARCHAR(100);

-- Add nullable timestamp columns (Hibernate will handle backfill)
ALTER TABLE jira_admin.statuses
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill NULL values for existing rows
UPDATE jira_admin.statuses SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE jira_admin.statuses SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

-- ============================================
-- SEED DEFAULT STATUSES (Jira DC Standard)
-- ============================================

-- Only insert if no statuses exist
INSERT INTO jira_admin.statuses (name, description, status_category, status_color, icon_url, sequence, is_default, is_active, is_archived, lookup_group, created_at, updated_at)
SELECT * FROM (
    SELECT 'Backlog' as name, 'Issues that are not yet started' as description, 'TODO' as status_category, '#6C757D' as status_color, '' as icon_url, 1 as sequence, FALSE as is_default, TRUE as is_active, FALSE as is_archived, 'open' as lookup_group, CURRENT_TIMESTAMP as created_at, CURRENT_TIMESTAMP as updated_at
    UNION ALL
    SELECT 'To Do', 'Issues ready to be worked on', 'TODO', '#0065FF', '', 2, FALSE, TRUE, FALSE, 'new', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'In Progress', 'Issues currently being worked on', 'IN_PROGRESS', '#FF991F', '', 3, FALSE, TRUE, FALSE, 'indeterminate', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'In Review', 'Issues awaiting review/approval', 'IN_PROGRESS', '#FF5630', '', 4, FALSE, TRUE, FALSE, 'review', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'Done', 'Issues completed successfully', 'DONE', '#00875A', '', 5, FALSE, TRUE, FALSE, 'resolved', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    UNION ALL
    SELECT 'Closed', 'Issues that are closed', 'DONE', '#42526E', '', 6, FALSE, TRUE, FALSE, 'closed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM jira_admin.statuses LIMIT 1);

-- Update first status as default if no default exists
UPDATE jira_admin.statuses
SET is_default = TRUE
WHERE id = (SELECT id FROM jira_admin.statuses WHERE is_default = TRUE LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM jira_admin.statuses WHERE is_default = TRUE LIMIT 1);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS idx_statuses_archived ON jira_admin.statuses(is_archived);
CREATE INDEX IF NOT EXISTS idx_statuses_category ON jira_admin.statuses(status_category);
CREATE INDEX IF NOT EXISTS idx_statuses_sequence ON jira_admin.statuses(sequence);
CREATE INDEX IF NOT EXISTS idx_statuses_active ON jira_admin.statuses(is_active);
CREATE INDEX IF NOT EXISTS idx_statuses_lookup_group ON jira_admin.statuses(lookup_group);