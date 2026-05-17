-- ============================================================
-- Version Service Database Schema
-- Enterprise-grade Jira DC parity for version management
-- ============================================================

-- Main versions table
CREATE TABLE project_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    released BOOLEAN DEFAULT FALSE NOT NULL,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    sequence INTEGER DEFAULT 0 NOT NULL,
    start_date TIMESTAMP,
    release_date TIMESTAMP,
    actual_release_date TIMESTAMP,
    semantic_version VARCHAR(50),
    build_number VARCHAR(100),
    branch_name VARCHAR(255),
    release_train VARCHAR(100),
    deployment_status VARCHAR(50) DEFAULT 'PLANNED',
    release_status VARCHAR(50) DEFAULT 'UNRELEASED',
    release_notes_url TEXT,
    release_notes_generated BOOLEAN DEFAULT FALSE,
    color VARCHAR(7),
    created_by UUID,
    updated_by UUID,
    released_by UUID,
    archived_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,

    CONSTRAINT fk_project_versions_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Issue to Fix Versions (Many-to-Many)
CREATE TABLE issue_fix_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    version_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,

    CONSTRAINT fk_issue_fix_versions_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_fix_versions_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE,
    UNIQUE(issue_id, version_id)
);

-- Issue to Affects Versions (Many-to-Many)
CREATE TABLE issue_affects_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    version_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,

    CONSTRAINT fk_issue_affects_versions_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_affects_versions_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE,
    UNIQUE(issue_id, version_id)
);

-- Version Release Notes
CREATE TABLE version_release_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    content TEXT NOT NULL,
    generated_at TIMESTAMP,
    generated_by UUID,
    content_hash VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_version_release_notes_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE
);

-- Version Audit Log
CREATE TABLE version_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    user_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_version_audit_logs_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE
);

-- Version Metric Snapshots (for tracking progress over time)
CREATE TABLE version_metric_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    total_issues INTEGER DEFAULT 0,
    open_issues INTEGER DEFAULT 0,
    closed_issues INTEGER DEFAULT 0,
    resolved_issues INTEGER DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0,
    total_story_points DECIMAL(10,2) DEFAULT 0,
    completed_story_points DECIMAL(10,2) DEFAULT 0,
    velocity_points DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_version_metric_snapshots_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE
);

-- Version Deployments (DevOps integration)
CREATE TABLE version_deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    deployment_id VARCHAR(255) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    deployment_url TEXT,
    build_number VARCHAR(100),
    build_url TEXT,
    commit_sha VARCHAR(40),
    deployed_by UUID,
    deployed_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_version_deployments_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE
);

-- Version Build References (CI/CD integration)
CREATE TABLE version_build_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id UUID NOT NULL,
    build_number VARCHAR(100) NOT NULL,
    build_url TEXT,
    build_status VARCHAR(50),
    branch_name VARCHAR(255),
    commit_sha VARCHAR(40),
    commit_message TEXT,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    triggered_by UUID,
    triggered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_version_build_references_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE
);

-- Version Release Train (for release train methodology)
CREATE TABLE release_trains (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cadence VARCHAR(50),
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Version Train Membership
CREATE TABLE release_train_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    train_id UUID NOT NULL,
    version_id UUID NOT NULL,
    sequence INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_release_train_versions_train FOREIGN KEY (train_id) REFERENCES release_trains(id) ON DELETE CASCADE,
    CONSTRAINT fk_release_train_versions_version FOREIGN KEY (version_id) REFERENCES project_versions(id) ON DELETE CASCADE,
    UNIQUE(train_id, version_id)
);

-- ============================================================
-- INDEXES for Performance
-- ============================================================

CREATE INDEX idx_project_versions_project_id ON project_versions(project_id);
CREATE INDEX idx_project_versions_released ON project_versions(released);
CREATE INDEX idx_project_versions_archived ON project_versions(archived);
CREATE INDEX idx_project_versions_release_date ON project_versions(release_date);
CREATE INDEX idx_project_versions_sequence ON project_versions(sequence);
CREATE INDEX idx_project_versions_deleted ON project_versions(deleted);
CREATE INDEX idx_project_versions_release_train ON project_versions(release_train);

CREATE INDEX idx_issue_fix_versions_issue_id ON issue_fix_versions(issue_id);
CREATE INDEX idx_issue_fix_versions_version_id ON issue_fix_versions(version_id);

CREATE INDEX idx_issue_affects_versions_issue_id ON issue_affects_versions(issue_id);
CREATE INDEX idx_issue_affects_versions_version_id ON issue_affects_versions(version_id);

CREATE INDEX idx_version_release_notes_version_id ON version_release_notes(version_id);

CREATE INDEX idx_version_audit_logs_version_id ON version_audit_logs(version_id);
CREATE INDEX idx_version_audit_logs_created_at ON version_audit_logs(created_at);
CREATE INDEX idx_version_audit_logs_user_id ON version_audit_logs(user_id);

CREATE INDEX idx_version_metric_snapshots_version_id ON version_metric_snapshots(version_id);
CREATE INDEX idx_version_metric_snapshots_date ON version_metric_snapshots(snapshot_date);

CREATE INDEX idx_version_deployments_version_id ON version_deployments(version_id);
CREATE INDEX idx_version_deployments_environment ON version_deployments(environment);
CREATE INDEX idx_version_deployments_status ON version_deployments(status);

CREATE INDEX idx_version_build_references_version_id ON version_build_references(version_id);
CREATE INDEX idx_version_build_references_build_number ON version_build_references(build_number);

CREATE INDEX idx_release_train_versions_train_id ON release_train_versions(train_id);
CREATE INDEX idx_release_train_versions_version_id ON release_train_versions(version_id);

-- ============================================================
-- COMMENTS for Documentation
-- ============================================================

COMMENT ON TABLE project_versions IS 'Enterprise-grade version management - supports fix versions, affects versions, release tracking';
COMMENT ON COLUMN project_versions.semantic_version IS 'Semantic version format: major.minor.patch';
COMMENT ON COLUMN project_versions.release_train IS 'Release train name for synchronized releases';
COMMENT ON COLUMN project_versions.deployment_status IS 'PLANNED, BUILDING, DEPLOYING, DEPLOYED, FAILED';
COMMENT ON COLUMN project_versions.release_status IS 'UNRELEASED, PRE_RELEASE, RELEASE_CANDIDATE, RELEASED';

COMMENT ON TABLE issue_fix_versions IS 'Many-to-many relationship for fix versions - an issue can be fixed in multiple versions';
COMMENT ON TABLE issue_affects_versions IS 'Many-to-many relationship for affected versions - an issue can affect multiple released versions';
COMMENT ON TABLE version_release_notes IS 'Auto-generated or manually curated release notes per version';
COMMENT ON TABLE version_audit_logs IS 'Complete audit trail of all version changes';
COMMENT ON TABLE version_metric_snapshots IS 'Daily snapshots for burndown and velocity tracking';
COMMENT ON TABLE version_deployments IS 'DevOps integration for deployment tracking per version';
COMMENT ON TABLE version_build_references IS 'CI/CD build linkage for traceability';
COMMENT ON TABLE release_trains IS 'Release train management for coordinated releases';
COMMENT ON TABLE release_train_versions IS 'Many-to-many linking versions to release trains';