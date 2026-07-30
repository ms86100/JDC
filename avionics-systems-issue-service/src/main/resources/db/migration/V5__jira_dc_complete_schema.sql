-- V5__jira_dc_complete_schema.sql
-- Complete Jira DC Issue Schema - All Mandatory Fields
-- Reverse engineered from Jira Data Center 11.3.0

-- ============================================
-- ADD ALL MISSING COLUMNS TO ISSUES TABLE
-- ============================================

-- Creator field (who actually created the issue)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS creator_id UUID;

-- Environment field
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS environment TEXT;

-- Aggregate fields for time tracking
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS aggregate_time_estimate BIGINT;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS aggregate_time_spent BIGINT;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS work_ratio DECIMAL(5,2);

-- Last viewed timestamp
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS last_viewed_at TIMESTAMP;

-- External issue key (for migrations)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS external_issue_key VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS external_priority VARCHAR(50);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS external_type VARCHAR(50);

-- Team field (for agile)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS team_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS team_name VARCHAR(100);

-- Target start/end dates (for agile)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS target_start DATE;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS target_end DATE;

-- Original story points (for epic children)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS original_story_points INTEGER;

-- Rank (for ordering)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS rank VARCHAR(255);

-- Issue color (for epics)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS issue_color VARCHAR(7);

-- Security level name
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS security_level_name VARCHAR(100);

-- ============================================
-- ENHANCED WORKLOGS TABLE
-- ============================================
DROP TABLE IF EXISTS jira_issue.worklogs;
CREATE TABLE jira_issue.worklogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    author_name VARCHAR(200),
    started_at TIMESTAMP NOT NULL,
    time_spent_seconds BIGINT NOT NULL,
    time_spent_display VARCHAR(30),  -- e.g., "1d 2h 30m"
    work_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_worklogs_issue ON jira_issue.worklogs(issue_id);
CREATE INDEX idx_worklogs_author ON jira_issue.worklogs(author_id);
CREATE INDEX idx_worklogs_started ON jira_issue.worklogs(started_at);

-- ============================================
-- ENHANCED LABELS TABLE
-- ============================================
DROP TABLE IF EXISTS jira_issue.labels;
CREATE TABLE jira_issue.labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7),
    description TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- ISSUE LABEL ASSOCIATIONS
-- ============================================
CREATE TABLE jira_issue.issue_labels (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    label_id UUID NOT NULL REFERENCES jira_issue.labels(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, label_id)
);

CREATE INDEX idx_issue_labels_issue ON jira_issue.issue_labels(issue_id);
CREATE INDEX idx_issue_labels_label ON jira_issue.issue_labels(label_id);

-- ============================================
-- ENHANCED ISSUE LINKS TABLE
-- ============================================
DROP TABLE IF EXISTS jira_issue.issue_links;
CREATE TABLE jira_issue.issue_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    link_type_id UUID NOT NULL,
    source_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT unique_issue_link UNIQUE (link_type_id, source_issue_id, target_issue_id)
);

CREATE INDEX idx_issue_links_source ON jira_issue.issue_links(source_issue_id);
CREATE INDEX idx_issue_links_target ON jira_issue.issue_links(target_issue_id);
CREATE INDEX idx_issue_links_type ON jira_issue.issue_links(link_type_id);

-- ============================================
-- SECURITY LEVELS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.security_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default security levels
INSERT INTO jira_issue.security_levels (id, name, description, sort_order) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'All Users', 'All users can view this issue', 1),
    ('c0000000-0000-0000-0000-000000000002', 'Project Role', 'Users with specific project role', 2)
ON CONFLICT DO NOTHING;

-- ============================================
-- PROJECT VERSIONS TABLE
-- ============================================
DROP TABLE IF EXISTS jira_issue.project_versions;
CREATE TABLE jira_issue.project_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE,
    release_date DATE,
    is_released BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    released_by UUID,
    released_at TIMESTAMP,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_version_name_per_project UNIQUE (project_id, name)
);

CREATE INDEX idx_versions_project ON jira_issue.project_versions(project_id);
CREATE INDEX idx_versions_released ON jira_issue.project_versions(is_released);

-- ============================================
-- PROJECT COMPONENTS TABLE
-- ============================================
DROP TABLE IF EXISTS jira_issue.project_components;
CREATE TABLE jira_issue.project_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    lead_id UUID,
    assignee_type VARCHAR(20) DEFAULT 'PROJECT_LEAD',
    default_assignee_id UUID,
    is_assignee_type_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_component_name_per_project UNIQUE (project_id, name)
);

CREATE INDEX idx_components_project ON jira_issue.project_components(project_id);

-- ============================================
-- ISSUE VERSIONS (Affects/Fixes)
-- ============================================
CREATE TABLE jira_issue.issue_versions (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES jira_issue.project_versions(id) ON DELETE CASCADE,
    version_type VARCHAR(20) NOT NULL,  -- 'AFFECTS' or 'FIXES'
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, version_id, version_type)
);

CREATE INDEX idx_issue_versions_issue ON jira_issue.issue_versions(issue_id);
CREATE INDEX idx_issue_versions_version ON jira_issue.issue_versions(version_id);

-- ============================================
-- ISSUE COMPONENTS
-- ============================================
CREATE TABLE jira_issue.issue_components (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES jira_issue.project_components(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, component_id)
);

CREATE INDEX idx_issue_components_issue ON jira_issue.issue_components(issue_id);
CREATE INDEX idx_issue_components_component ON jira_issue.issue_components(component_id);

-- ============================================
-- CUSTOM FIELDS DEFINITION TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    field_type VARCHAR(50) NOT NULL,  -- TEXT, NUMBER, DATE, SINGLE_SELECT, MULTI_SELECT, USER, ETC.
    default_value JSONB,
    options JSONB,  -- For select types
    is_required BOOLEAN DEFAULT FALSE,
    is_searchable BOOLEAN DEFAULT TRUE,
    is_sortable BOOLEAN DEFAULT TRUE,
    screen_region VARCHAR(50),  -- LEFT_COL, RIGHT_COL, DETAILS_TAB
    renderer VARCHAR(100),  -- Custom renderer component
    searcher VARCHAR(100),  -- Custom searcher
    plugin_source VARCHAR(100),  -- For marketplace plugin fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cfd_key ON jira_issue.custom_field_definitions(field_key);
CREATE INDEX idx_cfd_type ON jira_issue.custom_field_definitions(field_type);
CREATE INDEX idx_cfd_plugin ON jira_issue.custom_field_definitions(plugin_source);

-- ============================================
-- CUSTOM FIELDS VALUES TABLE
-- ============================================
CREATE TABLE jira_issue.custom_field_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    field_id UUID NOT NULL REFERENCES jira_issue.custom_field_definitions(id) ON DELETE CASCADE,
    value JSONB NOT NULL,  -- Flexible value storage
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_issue_field UNIQUE (issue_id, field_id)
);

CREATE INDEX idx_cfv_issue ON jira_issue.custom_field_values(issue_id);
CREATE INDEX idx_cfv_field ON jira_issue.custom_field_values(field_id);

-- ============================================
-- SCREEN CONFIGURATIONS
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.screen_tabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL REFERENCES jira_issue.screens(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE jira_issue.screen_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_tab_id UUID NOT NULL REFERENCES jira_issue.screen_tabs(id) ON DELETE CASCADE,
    field_key VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) DEFAULT 'standard',  -- standard, custom
    position INT DEFAULT 0,
    is_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- SEED DEFAULT SCREEN
-- ============================================
INSERT INTO jira_issue.screens (id, name, description, is_default) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'Default Issue Screen', 'Default screen for all issue types', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO jira_issue.screen_tabs (id, screen_id, name, position) VALUES
    ('f1000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'Description', 1),
    ('f1000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', 'Details', 2),
    ('f1000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', 'Comments', 3)
ON CONFLICT DO NOTHING;

-- ============================================
-- CHANGE HISTORY (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.change_groups CASCADE;
CREATE TABLE jira_issue.change_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    author_id UUID,
    author_name VARCHAR(200),
    change_type VARCHAR(50) NOT NULL DEFAULT 'EDIT',  -- EDIT, CREATE, DELETE, TRANSITION, COMMENT
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_change_groups_issue ON jira_issue.change_groups(issue_id);
CREATE INDEX idx_change_groups_author ON jira_issue.change_groups(author_id);

DROP TABLE IF EXISTS jira_issue.change_items;
CREATE TABLE jira_issue.change_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_group_id UUID NOT NULL REFERENCES jira_issue.change_groups(id) ON DELETE CASCADE,
    field_type VARCHAR(50) DEFAULT 'jira',
    field VARCHAR(100) NOT NULL,
    old_value TEXT,
    old_string TEXT,
    new_value TEXT,
    new_string TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_change_items_group ON jira_issue.change_items(change_group_id);

-- ============================================
-- VOTES TABLE (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.votes;
CREATE TABLE jira_issue.votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_votes_issue ON jira_issue.votes(issue_id);
CREATE INDEX idx_votes_user ON jira_issue.votes(user_id);

-- ============================================
-- WATCHERS TABLE (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.watchers;
CREATE TABLE jira_issue.watchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (issue_id, user_id)
);

CREATE INDEX idx_watchers_issue ON jira_issue.watchers(issue_id);
CREATE INDEX idx_watchers_user ON jira_issue.watchers(user_id);

-- ============================================
-- ATTACHMENTS TABLE (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.attachments;
CREATE TABLE jira_issue.attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    thumbnail_path VARCHAR(500),
    uploader_id UUID NOT NULL,
    uploader_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_issue ON jira_issue.attachments(issue_id);
CREATE INDEX idx_attachments_uploader ON jira_issue.attachments(uploader_id);

-- ============================================
-- COMMENTS TABLE (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.comments;
CREATE TABLE jira_issue.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    author_name VARCHAR(200),
    content TEXT NOT NULL,
    parent_comment_id UUID REFERENCES jira_issue.comments(id),
    internal BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_issue ON jira_issue.comments(issue_id);
CREATE INDEX idx_comments_author ON jira_issue.comments(author_id);
CREATE INDEX idx_comments_parent ON jira_issue.comments(parent_comment_id);

-- ============================================
-- SPRINTS TABLE (ENHANCED)
-- ============================================
DROP TABLE IF EXISTS jira_issue.sprints;
CREATE TABLE jira_issue.sprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    board_id UUID,
    project_id UUID,
    goal TEXT,
    start_date DATE,
    end_date DATE,
    state VARCHAR(20) DEFAULT 'FUTURE',  -- FUTUR, ACTIVE, CLOSED
    completed_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sprints_board ON jira_issue.sprints(board_id);
CREATE INDEX idx_sprints_state ON jira_issue.sprints(state);

-- ============================================
-- ISSUE-SPRINT ASSOCIATIONS
-- ============================================
CREATE TABLE jira_issue.issue_sprints (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    sprint_id UUID NOT NULL REFERENCES jira_issue.sprints(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, sprint_id)
);

CREATE INDEX idx_issue_sprints_issue ON jira_issue.issue_sprints(issue_id);
CREATE INDEX idx_issue_sprints_sprint ON jira_issue.issue_sprints(sprint_id);

-- ============================================
-- SEED EPIC ISSUE TYPE
-- ============================================
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
    ('f2000000-0000-0000-0000-000000000001', 'Epic', 'epic', 'An epic that contains stories')
ON CONFLICT DO NOTHING;

-- ============================================
-- SEED SUBTASK ISSUE TYPE
-- ============================================
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
    ('f2000000-0000-0000-0000-000000000002', 'Sub-task', 'subtask', 'A sub-task of a parent issue')
ON CONFLICT DO NOTHING;

-- ============================================
-- TRIGGER: Update vote_count on issues
-- ============================================
CREATE OR REPLACE FUNCTION jira_issue.update_vote_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE jira_issue.issues SET vote_count = COALESCE(vote_count, 0) + 1 WHERE id = NEW.issue_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE jira_issue.issues SET vote_count = GREATEST(0, COALESCE(vote_count, 1) - 1) WHERE id = OLD.issue_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_vote_count ON jira_issue.votes;
CREATE TRIGGER trigger_vote_count
AFTER INSERT OR DELETE ON jira_issue.votes
FOR EACH ROW EXECUTE FUNCTION jira_issue.update_vote_count();

-- ============================================
-- TRIGGER: Update watcher_count on issues
-- ============================================
CREATE OR REPLACE FUNCTION jira_issue.update_watcher_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE jira_issue.issues SET watcher_count = COALESCE(watcher_count, 0) + 1 WHERE id = NEW.issue_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE jira_issue.issues SET watcher_count = GREATEST(0, COALESCE(watcher_count, 1) - 1) WHERE id = OLD.issue_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_watcher_count ON jira_issue.watchers;
CREATE TRIGGER trigger_watcher_count
AFTER INSERT OR DELETE ON jira_issue.watchers
FOR EACH ROW EXECUTE FUNCTION jira_issue.update_watcher_count();

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_issues_creator ON jira_issue.issues(creator_id);
CREATE INDEX IF NOT EXISTS idx_issues_team ON jira_issue.issues(team_id);
CREATE INDEX IF NOT EXISTS idx_issues_rank ON jira_issue.issues(rank);
-- issues has no sprint_id column; sprint membership is tracked via the
-- issue_sprints join table (indexed above), so no index on issues(sprint_id).
CREATE INDEX IF NOT EXISTS idx_issues_epic ON jira_issue.issues(epic_id);
CREATE INDEX IF NOT EXISTS idx_issues_parent ON jira_issue.issues(parent_issue_id);
CREATE INDEX IF NOT EXISTS idx_issues_security ON jira_issue.issues(security_level_id);
CREATE INDEX IF NOT EXISTS idx_issues_due_date ON jira_issue.issues(due_date);
CREATE INDEX IF NOT EXISTS idx_issues_last_viewed ON jira_issue.issues(last_viewed_at);
CREATE INDEX IF NOT EXISTS idx_issues_story_points ON jira_issue.issues(story_points);
CREATE INDEX IF NOT EXISTS idx_issues_created_at ON jira_issue.issues(created_at);
CREATE INDEX IF NOT EXISTS idx_issues_updated_at ON jira_issue.issues(updated_at);

-- ============================================
-- ADD COLUMNS TO EXISTING ISSUES TABLE
-- ============================================
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS vote_count INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS watcher_count INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS creator_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS environment TEXT;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS team_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS team_name VARCHAR(100);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS target_start DATE;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS target_end DATE;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS last_viewed_at TIMESTAMP;

COMMENT ON TABLE jira_issue.issues IS 'Complete issue table with all Jira DC mandatory fields';
COMMENT ON COLUMN jira_issue.issues.creator_id IS 'User who originally created the issue';
COMMENT ON COLUMN jira_issue.issues.environment IS 'Environment where the issue was found';
COMMENT ON COLUMN jira_issue.issues.vote_count IS 'Denormalized count of votes for performance';
COMMENT ON COLUMN jira_issue.issues.watcher_count IS 'Denormalized count of watchers for performance';
COMMENT ON COLUMN jira_issue.issues.team_id IS 'Agile team associated with this issue';
COMMENT ON COLUMN jira_issue.issues.target_start IS 'Target start date for agile planning';
COMMENT ON COLUMN jira_issue.issues.target_end IS 'Target end date for agile planning';
COMMENT ON COLUMN jira_issue.issues.last_viewed_at IS 'When this issue was last viewed';