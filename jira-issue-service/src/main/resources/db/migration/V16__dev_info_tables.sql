-- ============================================
-- DEVELOPMENT INFORMATION TABLES
-- Tracks commits, branches, PRs, and builds linked to issues
-- ============================================

CREATE TABLE IF NOT EXISTS jira_issue.dev_info_commits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    commit_hash VARCHAR(100) NOT NULL,
    message TEXT,
    author_name VARCHAR(200),
    author_email VARCHAR(300),
    repository VARCHAR(500),
    repository_url TEXT,
    url TEXT,
    committed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dic_issue ON jira_issue.dev_info_commits(issue_id);
CREATE INDEX IF NOT EXISTS idx_dic_hash ON jira_issue.dev_info_commits(commit_hash);
CREATE INDEX IF NOT EXISTS idx_dic_repo ON jira_issue.dev_info_commits(repository);

CREATE TABLE IF NOT EXISTS jira_issue.dev_info_branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    branch_name VARCHAR(500) NOT NULL,
    repository VARCHAR(500),
    url TEXT,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    created_from_issue BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dib_issue ON jira_issue.dev_info_branches(issue_id);

CREATE TABLE IF NOT EXISTS jira_issue.dev_info_pull_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    pr_number INTEGER,
    title VARCHAR(500),
    status VARCHAR(30) DEFAULT 'OPEN',
    source_branch VARCHAR(500),
    target_branch VARCHAR(500),
    repository VARCHAR(500),
    url TEXT,
    author_name VARCHAR(200),
    reviewers TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dipr_issue ON jira_issue.dev_info_pull_requests(issue_id);
CREATE INDEX IF NOT EXISTS idx_dipr_status ON jira_issue.dev_info_pull_requests(status);

CREATE TABLE IF NOT EXISTS jira_issue.dev_info_builds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL REFERENCES jira_issue.issues(id) ON DELETE CASCADE,
    build_number VARCHAR(100),
    plan_key VARCHAR(200),
    status VARCHAR(30) DEFAULT 'IN_PROGRESS',
    url TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dib2_issue ON jira_issue.dev_info_builds(issue_id);
