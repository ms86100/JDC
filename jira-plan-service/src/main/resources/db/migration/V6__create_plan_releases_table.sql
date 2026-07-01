-- V6__create_plan_releases_table.sql
-- Release/version management within Plans

CREATE TABLE jira_plan.plan_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    description TEXT,
    release_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by UUID,
    approved_at TIMESTAMP,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_plan_releases_plan_id ON jira_plan.plan_releases(plan_id);
CREATE INDEX idx_plan_releases_status ON jira_plan.plan_releases(status);
CREATE INDEX idx_plan_releases_release_date ON jira_plan.plan_releases(release_date);

-- Comments
COMMENT ON TABLE jira_plan.plan_releases IS 'Releases/versions within a Plan';
COMMENT ON COLUMN jira_plan.plan_releases.status IS 'DRAFT, APPROVED, RELEASED';
COMMENT ON COLUMN jira_plan.plan_releases.approved_by IS 'User who approved the release';
COMMENT ON COLUMN jira_plan.plan_releases.approved_at IS 'When the release was approved';