-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_issue;

-- Enable UUID extension

-- Worklogs table
CREATE TABLE IF NOT EXISTS jira_issue.worklogs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(200),
    time_worked_minutes INT NOT NULL,
    description TEXT,
    started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_worklogs_issue ON jira_issue.worklogs(issue_id);
CREATE INDEX IF NOT EXISTS idx_worklogs_author ON jira_issue.worklogs(author_id);

-- Issue links table
CREATE TABLE IF NOT EXISTS jira_issue.issue_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_issue_id UUID NOT NULL,
    destination_issue_id UUID NOT NULL,
    link_type VARCHAR(50) NOT NULL,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- If issue_links already existed (legacy schema), ensure expected columns exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'jira_issue'
          AND table_name = 'issue_links'
          AND column_name = 'source_issue_id'
    ) THEN
        ALTER TABLE jira_issue.issue_links ADD COLUMN source_issue_id UUID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'jira_issue'
          AND table_name = 'issue_links'
          AND column_name = 'destination_issue_id'
    ) THEN
        ALTER TABLE jira_issue.issue_links ADD COLUMN destination_issue_id UUID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'jira_issue'
          AND table_name = 'issue_links'
          AND column_name = 'link_type'
    ) THEN
        ALTER TABLE jira_issue.issue_links ADD COLUMN link_type VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'jira_issue'
          AND table_name = 'issue_links'
          AND column_name = 'sequence'
    ) THEN
        ALTER TABLE jira_issue.issue_links ADD COLUMN sequence INT NOT NULL DEFAULT 0;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_issue_links_source ON jira_issue.issue_links(source_issue_id);
CREATE INDEX IF NOT EXISTS idx_issue_links_dest ON jira_issue.issue_links(destination_issue_id);

-- Labels table
CREATE TABLE IF NOT EXISTS jira_issue.labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- If labels already existed (legacy schema), ensure expected columns exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'jira_issue' AND table_name = 'labels' AND column_name = 'issue_id'
    ) THEN
        ALTER TABLE jira_issue.labels ADD COLUMN issue_id UUID;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'jira_issue' AND table_name = 'labels' AND column_name = 'name'
    ) THEN
        ALTER TABLE jira_issue.labels ADD COLUMN name VARCHAR(100);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_labels_issue ON jira_issue.labels(issue_id);
CREATE INDEX IF NOT EXISTS idx_labels_name ON jira_issue.labels(name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_labels_issue_name ON jira_issue.labels(issue_id, LOWER(name));

-- Change groups table (for audit trail)
CREATE TABLE IF NOT EXISTS jira_issue.change_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    author_id UUID,
    author_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- If change_groups already existed (legacy schema), ensure expected columns exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'jira_issue' AND table_name = 'change_groups' AND column_name = 'issue_id'
    ) THEN
        ALTER TABLE jira_issue.change_groups ADD COLUMN issue_id UUID;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_change_groups_issue ON jira_issue.change_groups(issue_id);

-- Change items table
CREATE TABLE IF NOT EXISTS jira_issue.change_items (
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

CREATE INDEX IF NOT EXISTS idx_change_items_group ON jira_issue.change_items(change_group_id);

-- Seed default issue link types
CREATE TABLE IF NOT EXISTS jira_issue.issue_link_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    inward VARCHAR(100) NOT NULL,
    outward VARCHAR(100) NOT NULL,
    style VARCHAR(20),
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO jira_issue.issue_link_types (name, inward, outward, style, sequence) VALUES
  ('blocks', 'is blocked by', 'blocks', 'blocks', 1),
  ('relates to', 'relates to', 'relates to', 'relates', 2),
  ('duplicates', 'is duplicated by', 'duplicates', 'duplicate', 3),
  ('is cloned by', 'is cloned by', 'clones', 'clone', 4),
  ('is parent of', 'has sub-task', 'is sub-task of', 'parent', 5),
  ('causes', 'is caused by', 'causes', 'causes', 6)
ON CONFLICT (name) DO NOTHING;
