-- ============================================================
-- Component Service Database Schema
-- Enterprise-grade Jira DC parity for component management
-- ============================================================

-- Main components table
CREATE TABLE project_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    lead_user_id UUID,
    assignee_type VARCHAR(50) DEFAULT 'PROJECT_DEFAULT',
    default_assignee UUID,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    color VARCHAR(7),
    icon VARCHAR(50),
    sequence INTEGER DEFAULT 0 NOT NULL,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_project_components_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Issue to Component (Many-to-Many)
CREATE TABLE issue_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    component_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,

    CONSTRAINT fk_issue_components_issue FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_components_component FOREIGN KEY (component_id) REFERENCES project_components(id) ON DELETE CASCADE,
    UNIQUE(issue_id, component_id)
);

-- Component Audit Log
CREATE TABLE component_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    user_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_component_audit_logs_component FOREIGN KEY (component_id) REFERENCES project_components(id) ON DELETE CASCADE
);

-- Component Ownership History
CREATE TABLE component_ownership_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID NOT NULL,
    previous_lead_id UUID,
    new_lead_id UUID,
    transfer_reason TEXT,
    transferred_by UUID,
    transferred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_component_ownership_history_component FOREIGN KEY (component_id) REFERENCES project_components(id) ON DELETE CASCADE
);

-- Component Metrics
CREATE TABLE component_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    total_issues INTEGER DEFAULT 0,
    open_issues INTEGER DEFAULT 0,
    closed_issues INTEGER DEFAULT 0,
    bug_count INTEGER DEFAULT 0,
    story_count INTEGER DEFAULT 0,
    task_count INTEGER DEFAULT 0,
    total_story_points DECIMAL(10,2) DEFAULT 0,
    completed_story_points DECIMAL(10,2) DEFAULT 0,
    avg_resolution_time_hours DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_component_metrics_component FOREIGN KEY (component_id) REFERENCES project_components(id) ON DELETE CASCADE
);

-- Component Auto Assignment Rules
CREATE TABLE component_assignment_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    issue_type_id UUID,
    priority_id UUID,
    assignee_type VARCHAR(50) NOT NULL,
    assignee_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_component_assignment_rules_component FOREIGN KEY (component_id) REFERENCES project_components(id) ON DELETE CASCADE
);

-- ============================================================
-- INDEXES for Performance
-- ============================================================

CREATE INDEX idx_project_components_project_id ON project_components(project_id);
CREATE INDEX idx_project_components_lead_user_id ON project_components(lead_user_id);
CREATE INDEX idx_project_components_archived ON project_components(archived);
CREATE INDEX idx_project_components_deleted ON project_components(deleted);
CREATE INDEX idx_project_components_sequence ON project_components(sequence);

CREATE INDEX idx_issue_components_issue_id ON issue_components(issue_id);
CREATE INDEX idx_issue_components_component_id ON issue_components(component_id);

CREATE INDEX idx_component_audit_logs_component_id ON component_audit_logs(component_id);
CREATE INDEX idx_component_audit_logs_created_at ON component_audit_logs(created_at);
CREATE INDEX idx_component_audit_logs_user_id ON component_audit_logs(user_id);

CREATE INDEX idx_component_ownership_history_component_id ON component_ownership_history(component_id);

CREATE INDEX idx_component_metrics_component_id ON component_metrics(component_id);
CREATE INDEX idx_component_metrics_snapshot_date ON component_metrics(snapshot_date);

CREATE INDEX idx_component_assignment_rules_component_id ON component_assignment_rules(component_id);
CREATE INDEX idx_component_assignment_rules_is_active ON component_assignment_rules(is_active);

-- ============================================================
-- COMMENTS for Documentation
-- ============================================================

COMMENT ON TABLE project_components IS 'Enterprise-grade component management - supports ownership, auto-assignment, and issue linking';
COMMENT ON COLUMN project_components.assignee_type IS 'PROJECT_DEFAULT, COMPONENT_LEAD, PROJECT_LEAD, UNASSIGNED';
COMMENT ON TABLE issue_components IS 'Many-to-many relationship for components - an issue can belong to multiple components';
COMMENT ON TABLE component_audit_logs IS 'Complete audit trail of all component changes';
COMMENT ON TABLE component_ownership_history IS 'Ownership transfer history for accountability';
COMMENT ON TABLE component_metrics IS 'Daily snapshots for component health and reporting';
COMMENT ON TABLE component_assignment_rules IS 'Auto-assignment rules based on issue type, priority, etc.';