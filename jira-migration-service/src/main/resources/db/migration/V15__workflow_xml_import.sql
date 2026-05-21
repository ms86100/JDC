CREATE TABLE IF NOT EXISTS jira_migration.migration_workflow_imports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id) ON DELETE SET NULL,
    workflow_name VARCHAR(255) NOT NULL,
    scheme_name VARCHAR(255),
    source_format VARCHAR(50) NOT NULL DEFAULT 'WORKFLOW_DESCRIPTOR',
    target_workflow_id VARCHAR(100),
    target_scheme_id VARCHAR(100),
    import_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    descriptor_json JSONB NOT NULL,
    scheme_json JSONB,
    graph_json JSONB,
    validation_report JSONB,
    simulation_trace JSONB,
    snapshot_before JSONB,
    unsupported_features JSONB,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    rolled_back_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mwi_job ON jira_migration.migration_workflow_imports(job_id);
CREATE INDEX IF NOT EXISTS idx_mwi_workflow_name ON jira_migration.migration_workflow_imports(workflow_name);
