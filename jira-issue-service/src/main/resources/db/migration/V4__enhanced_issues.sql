-- V4__enhanced_issues.sql
-- Enhanced Issue Fields - Jira DC Compatible
-- Adds all missing Jira DC fields to the issues table

-- ============================================
-- ADD MISSING COLUMNS TO ISSUES
-- ============================================

-- Epic/Story hierarchy
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS epic_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS epic_name VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS epic_color VARCHAR(7);

-- Sub-task support
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS parent_issue_id UUID;

-- Security level
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS security_level_id UUID;

-- Versions (affects/fixes)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS affects_versions UUID[];
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS fix_versions UUID[];

-- Story points and rank
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS story_points INTEGER;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS rank VARCHAR(255);

-- Time tracking
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS original_estimate BIGINT;  -- in seconds
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS remaining_estimate BIGINT;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS time_spent BIGINT;

-- Resolution
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS resolution_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS resolution_date TIMESTAMP;

-- Due date
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS due_date DATE;

-- Votes and watchers count (denormalized for performance)
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS vote_count INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS watcher_count INTEGER DEFAULT 0;

-- ============================================
-- WATCHERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.watchers (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, user_id)
);

-- ============================================
-- VOTES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.votes (
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (issue_id, user_id)
);

-- ============================================
-- ISSUE LINK TYPES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.issue_link_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    inward VARCHAR(50) NOT NULL,  -- "blocks", "is blocked by"
    outward VARCHAR(50) NOT NULL,  -- "blocks", "is blocked by"
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- ISSUE LINKS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.issue_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    link_type_id UUID NOT NULL REFERENCES jira_issue.issue_link_types(id),
    source_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT unique_issue_link UNIQUE (link_type_id, source_issue_id, target_issue_id)
);

-- ============================================
-- SEED DATA: Issue Link Types (Jira DC Compatible)
-- ============================================
INSERT INTO jira_issue.issue_link_types (id, name, inward, outward) VALUES
    ('link-blocks', 'Blocks', 'is blocked by', 'blocks'),
    ('link-is-blocked-by', 'Is blocked by', 'blocks', 'is blocked by'),
    ('link-duplicates', 'Duplicates', 'is duplicated by', 'duplicates'),
    ('link-is-duplicated-by', 'Is duplicated by', 'duplicates', 'is duplicated by'),
    ('link-relates-to', 'Relates to', 'relates to', 'relates to'),
    ('link- 원인- duplicates', 'Duplicates', 'is duplicated by', 'duplicates'),
    ('link-causes', 'Causes', 'is caused by', 'causes'),
    ('link-is-caused-by', 'Is caused by', 'causes', 'is caused by'),
    ('link-depends-on', 'Depends on', 'is depended upon by', 'depends on'),
    ('link-is-depended-upon-by', 'Is depended upon by', 'depends on', 'is depended upon by'),
    ('link-clones', 'Clones', 'is cloned by', 'clones'),
    ('link-is-cloned-by', 'Is cloned by', 'clones', 'is cloned by'),
    ('link-splits-into', 'Splits into', 'is split from', 'splits into'),
    ('link-is-split-from', 'Is split from', 'splits into', 'is split from'),
    ('link-原因', '原因', '是由于', '导致'),
    ('link-supercedes', 'Supercedes', 'is superseded by', 'supercedes'),
    ('link-is-superceded-by', 'Is superseded by', 'supercedes', 'is superseded by'),
    ('link-progress', 'Progress', 'is progressed by', 'progresses'),
    ('link-is-progressed-by', 'Is progressed by', 'progress', 'is progressed by')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- RESOLUTIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.resolutions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon_url VARCHAR(255),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default resolutions
INSERT INTO jira_issue.resolutions (id, name, description, sort_order) VALUES
    ('res-fixed', 'Fixed', 'The issue has been fixed', 1),
    ('res-wont-fix', 'Won''t Fix', 'The issue will not be fixed', 2),
    ('res-duplicate', 'Duplicate', 'The issue is a duplicate', 3),
    ('res-incomplete', 'Incomplete', 'The issue cannot be completed', 4),
    ('res-cannot-reproduce', 'Cannot Reproduce', 'The issue cannot be reproduced', 5),
    ('res-done', 'Done', 'The issue has been completed', 6)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- VERSION TABLE (if not exists)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.project_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE,
    release_date DATE,
    is_released BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    released_by UUID,
    released_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_version_name_per_project UNIQUE (project_id, name)
);

-- ============================================
-- COMPONENT TABLE (if not exists)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.project_components (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES jira_project.projects(id) ON DELETE CASCADE,
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

-- ============================================
-- ISSUE WORKLOGS TABLE (enhanced)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_issue.worklogs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    author_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL,
    time_spent_seconds BIGINT NOT NULL,
    time_spent_display VARCHAR(30),  -- e.g., "1d 2h"
    work_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- LABELS TABLE (enhanced with colors)
-- ============================================
ALTER TABLE jira_issue.labels ADD COLUMN IF NOT EXISTS color VARCHAR(7);
ALTER TABLE jira_issue.labels ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE jira_issue.labels ADD COLUMN IF NOT EXISTS created_by UUID;

-- ============================================
-- ISSUE ATTACHMENTS (existing table enhancement)
-- ============================================
ALTER TABLE jira_issue.attachments ADD COLUMN IF NOT EXISTS thumbnail_path VARCHAR(500);
ALTER TABLE jira_issue.attachments ADD COLUMN IF NOT EXISTS mime_type_detected VARCHAR(100);
ALTER TABLE jira_issue.attachments ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================
CREATE INDEX IF NOT EXISTS idx_issues_epic ON jira_issue.issues(epic_id);
CREATE INDEX IF NOT EXISTS idx_issues_parent ON jira_issue.issues(parent_issue_id);
CREATE INDEX IF NOT EXISTS idx_issues_security ON jira_issue.issues(security_level_id);
CREATE INDEX IF NOT EXISTS idx_issues_resolution ON jira_issue.issues(resolution_id);
CREATE INDEX IF NOT EXISTS idx_issues_due_date ON jira_issue.issues(due_date);
CREATE INDEX IF NOT EXISTS idx_issues_rank ON jira_issue.issues(rank);
CREATE INDEX IF NOT EXISTS idx_issues_story_points ON jira_issue.issues(story_points);
CREATE INDEX IF NOT EXISTS idx_issues_created_at ON jira_issue.issues(created_at);
CREATE INDEX IF NOT EXISTS idx_issues_updated_at ON jira_issue.issues(updated_at);

CREATE INDEX IF NOT EXISTS idx_watchers_user ON jira_issue.watchers(user_id);
CREATE INDEX IF NOT EXISTS idx_votes_user ON jira_issue.votes(user_id);
CREATE INDEX IF NOT EXISTS idx_worklogs_author ON jira_issue.worklogs(author_id);
CREATE INDEX IF NOT EXISTS idx_worklogs_started ON jira_issue.worklogs(started_at);
CREATE INDEX IF NOT EXISTS idx_issue_links_source ON jira_issue.issue_links(source_issue_id);
CREATE INDEX IF NOT EXISTS idx_issue_links_target ON jira_issue.issue_links(target_issue_id);
CREATE INDEX IF NOT EXISTS idx_versions_project ON jira_issue.project_versions(project_id);
CREATE INDEX IF NOT EXISTS idx_components_project ON jira_issue.project_components(project_id);

-- ============================================
-- FUNCTION: Update issue counters
-- Automatically updates vote_count and watcher_count
-- ============================================
CREATE OR REPLACE FUNCTION jira_issue.update_issue_counters()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF TG_TABLE_NAME = 'watchers' THEN
            UPDATE jira_issue.issues SET watcher_count = watcher_count + 1 WHERE id = NEW.issue_id;
        ELSIF TG_TABLE_NAME = 'votes' THEN
            UPDATE jira_issue.issues SET vote_count = vote_count + 1 WHERE id = NEW.issue_id;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF TG_TABLE_NAME = 'watchers' THEN
            UPDATE jira_issue.issues SET watcher_count = watcher_count - 1 WHERE id = OLD.issue_id;
        ELSIF TG_TABLE_NAME = 'votes' THEN
            UPDATE jira_issue.issues SET vote_count = vote_count - 1 WHERE id = OLD.issue_id;
        END IF;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Create triggers for counter updates
DROP TRIGGER IF EXISTS trigger_watchers_counter ON jira_issue.watchers;
CREATE TRIGGER trigger_watchers_counter
AFTER INSERT OR DELETE ON jira_issue.watchers
FOR EACH ROW EXECUTE FUNCTION jira_issue.update_issue_counters();

DROP TRIGGER IF EXISTS trigger_votes_counter ON jira_issue.votes;
CREATE TRIGGER trigger_votes_counter
AFTER INSERT OR DELETE ON jira_issue.votes
FOR EACH ROW EXECUTE FUNCTION jira_issue.update_issue_counters();

-- ============================================
-- COMMENTS: Add threaded support
-- ============================================
ALTER TABLE jira_issue.comments ADD COLUMN IF NOT EXISTS parent_comment_id UUID;
ALTER TABLE jira_issue.comments ADD COLUMN IF NOT EXISTS internal BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.comments ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_comments_parent ON jira_issue.comments(parent_comment_id);

COMMENT ON TABLE jira_issue.issues IS 'Enhanced issue table with all Jira DC compatible fields';
COMMENT ON TABLE jira_issue.watchers IS 'Users watching an issue for updates';
COMMENT ON TABLE jira_issue.votes IS 'Users who have voted for an issue';
COMMENT ON TABLE jira_issue.issue_links IS 'Links between issues (blocks, relates to, etc.)';
COMMENT ON TABLE jira_issue.worklogs IS 'Time spent working on issues';