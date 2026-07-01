-- V1__init_migration_service.sql
-- Migration Service Database Schema
-- Supports project import/export and CSV-based data migration

-- Extensions required by later migrations (gen_random_uuid is built-in on PG 13+)

-- Create schema
CREATE SCHEMA IF NOT EXISTS jira_migration;

-- ============================================
-- MIGRATION JOBS TABLE
-- Tracks all import/export operations
-- ============================================
CREATE TABLE jira_migration.migration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(20) NOT NULL,  -- IMPORT, EXPORT
    job_status VARCHAR(20) NOT NULL,  -- PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    import_source VARCHAR(50),  -- JIRA_DC, CSV, BACKUP, OTHER

    -- Progress tracking
    total_entities INT DEFAULT 0,
    processed_entities INT DEFAULT 0,
    failed_entities INT DEFAULT 0,
    progress_percentage DECIMAL(5,2) DEFAULT 0,

    -- Configuration
    config JSONB,  -- Import/export settings
    options JSONB,  -- Additional options

    -- User who initiated
    initiated_by UUID,
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    -- Error tracking
    error_message TEXT,
    error_details JSONB,

    -- Source/target info
    source_project_id UUID,
    target_project_id UUID,
    file_path VARCHAR,

    -- Rollback support
    can_rollback BOOLEAN DEFAULT FALSE,
    rollback_job_id UUID,

    -- Result metadata
    result_metadata JSONB,

    CONSTRAINT valid_job_type CHECK (job_type IN ('IMPORT', 'EXPORT')),
    CONSTRAINT valid_job_status CHECK (job_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- ============================================
-- MIGRATION ENTITY STATUS
-- Per-entity status within a job
-- ============================================
CREATE TABLE jira_migration.entity_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,

    entity_type VARCHAR(50) NOT NULL,  -- PROJECT, ISSUE, WORKFLOW, USER, etc.
    entity_key VARCHAR(255),  -- PROJECT-1, user email, etc.
    entity_id UUID,  -- Internal ID if created

    -- Processing status
    status VARCHAR(20) NOT NULL,  -- PENDING, PROCESSING, COMPLETED, FAILED, SKIPPED
    processing_order INT,  -- Order of processing

    -- Error details
    error_code VARCHAR(50),
    error_message TEXT,
    error_row INT,  -- For CSV: row number with error
    error_field VARCHAR(100),  -- Field that caused the error
    error_context JSONB,  -- Additional context

    -- Timestamps
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms INT,

    -- Validation results
    validation_errors JSONB,
    warnings JSONB,

    CONSTRAINT valid_entity_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED'))
);

-- ============================================
-- CSV TEMPLATES TABLE
-- Stores template definitions for import
-- ============================================
CREATE TABLE jira_migration.csv_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,  -- PROJECT, ISSUE, USER, etc.
    version VARCHAR(20) DEFAULT '1.0',

    -- Template definition
    columns JSONB NOT NULL,  -- Column definitions with types, required flags
    header_row INT DEFAULT 1,
    data_start_row INT DEFAULT 2,

    -- Validation rules
    validation_rules JSONB,
    custom_validators JSONB,

    -- Mapping to Jira DC fields
    field_mapping JSONB,  -- Maps CSV columns to target fields

    -- Options
    supports_bulk_import BOOLEAN DEFAULT TRUE,
    max_rows_per_file INT DEFAULT 50000,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,

    CONSTRAINT unique_template_version UNIQUE (template_name, version)
);

-- ============================================
-- FIELD MAPPINGS TABLE
-- Stores user-created field mappings for import
-- ============================================
CREATE TABLE jira_migration.field_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mapping_name VARCHAR(100) NOT NULL,
    mapping_type VARCHAR(30) NOT NULL,  -- IMPORT, EXPORT

    -- Source/target
    source_type VARCHAR(50) NOT NULL,  -- JIRA_DC, CSV, etc.
    target_type VARCHAR(50) NOT NULL,   -- JIRA_PLATFORM

    -- Mapping definition
    mappings JSONB NOT NULL,  -- Array of {source_field, target_field, default_value, transformer}

    -- Preview data
    sample_data JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID,
    is_shared BOOLEAN DEFAULT FALSE
);

-- ============================================
-- VALIDATION RULES TABLE
-- Predefined and custom validation rules
-- ============================================
CREATE TABLE jira_migration.validation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    rule_type VARCHAR(30) NOT NULL,  -- REQUIRED, FORMAT, RANGE, UNIQUE, FK, CUSTOM
    rule_config JSONB NOT NULL,  -- Rule configuration
    error_message_template VARCHAR(500),
    severity VARCHAR(10) DEFAULT 'ERROR',  -- ERROR, WARNING
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- BACKUP ENTITIES TABLE
-- Holds exported entity snapshots
-- ============================================
CREATE TABLE jira_migration.backup_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,

    entity_type VARCHAR(50) NOT NULL,
    entity_key VARCHAR(255),
    entity_data JSONB NOT NULL,  -- Full entity snapshot

    -- Dependencies
    dependencies JSONB,  -- Entity keys this depends on
    parent_key VARCHAR(255),  -- For hierarchy support

    -- Position in export
    sequence_order INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- AUDIT LOG TABLE
-- Migration audit trail
-- ============================================
CREATE TABLE jira_migration.migration_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jira_migration.migration_jobs(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_key VARCHAR(255),
    details JSONB,

    performed_by UUID,
    performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45)
);

-- ============================================
-- USER MAPPING TABLE
-- Maps source users to target users
-- ============================================
CREATE TABLE jira_migration.user_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,

    source_identifier VARCHAR(255) NOT NULL,  -- username, email, user_id from source
    source_type VARCHAR(20) NOT NULL,  -- JIRA_DC, EXTERNAL

    target_user_id UUID,  -- Mapped to existing user
    target_username VARCHAR(150),
    target_email VARCHAR(255),

    -- Mapping resolution
    mapping_type VARCHAR(20) NOT NULL,  -- EXACT_MATCH, EMAIL_MATCH, CREATE_NEW, MANUAL
    confidence_score DECIMAL(5,2),  -- 0-100 for fuzzy matches

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- PROJECT MAPPING TABLE
-- Maps source projects to target projects
-- ============================================
CREATE TABLE jira_migration.project_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jira_migration.migration_jobs(id) ON DELETE CASCADE,

    source_key VARCHAR(10) NOT NULL,
    target_key VARCHAR(10),
    target_id UUID,

    -- Issue key offset tracking
    issue_key_sequence INT DEFAULT 0,  -- Current issue number for this project

    -- Component/version mappings
    component_mappings JSONB,
    version_mappings JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_migration_jobs_status ON jira_migration.migration_jobs(job_status);
CREATE INDEX idx_migration_jobs_type ON jira_migration.migration_jobs(job_type);
CREATE INDEX idx_migration_jobs_initiated_by ON jira_migration.migration_jobs(initiated_by);
CREATE INDEX idx_migration_jobs_initiated_at ON jira_migration.migration_jobs(initiated_at DESC);
CREATE INDEX idx_migration_jobs_source_project ON jira_migration.migration_jobs(source_project_id);
CREATE INDEX idx_migration_jobs_target_project ON jira_migration.migration_jobs(target_project_id);

CREATE INDEX idx_entity_status_job ON jira_migration.entity_status(job_id);
CREATE INDEX idx_entity_status_type ON jira_migration.entity_status(entity_type);
CREATE INDEX idx_entity_status_status ON jira_migration.entity_status(status);
CREATE INDEX idx_entity_status_key ON jira_migration.entity_status(entity_key);

CREATE INDEX idx_csv_templates_entity_type ON jira_migration.csv_templates(entity_type);
CREATE INDEX idx_field_mappings_type ON jira_migration.field_mappings(mapping_type);
CREATE INDEX idx_validation_rules_entity_type ON jira_migration.validation_rules(entity_type);
CREATE INDEX idx_backup_entities_backup_id ON jira_migration.backup_entities(backup_id);
CREATE INDEX idx_backup_entities_type ON jira_migration.backup_entities(entity_type);
CREATE INDEX idx_audit_job_id ON jira_migration.migration_audit(job_id);
CREATE INDEX idx_user_mappings_job_id ON jira_migration.user_mappings(job_id);
CREATE INDEX idx_project_mappings_job_id ON jira_migration.project_mappings(job_id);

-- ============================================
-- SEED DATA: Default CSV Templates
-- ============================================
INSERT INTO jira_migration.csv_templates (id, template_name, entity_type, version, columns, header_row, data_start_row, validation_rules, field_mapping, supports_bulk_import, max_rows_per_file) VALUES
-- Project Template
(gen_random_uuid(), 'Project Import', 'PROJECT', '1.0',
'[
    {"column_name": "project_key", "display_name": "Project Key", "data_type": "VARCHAR", "max_length": 10, "required": true, "pattern": "^[A-Z][A-Z0-9]{0,9}$"},
    {"column_name": "name", "display_name": "Project Name", "data_type": "VARCHAR", "max_length": 200, "required": true},
    {"column_name": "description", "display_name": "Description", "data_type": "TEXT", "required": false},
    {"column_name": "lead_username", "display_name": "Project Lead", "data_type": "VARCHAR", "max_length": 150, "required": false},
    {"column_name": "category", "display_name": "Category", "data_type": "VARCHAR", "max_length": 50, "required": false},
    {"column_name": "project_type", "display_name": "Project Type", "data_type": "VARCHAR", "max_length": 20, "required": false, "allowed_values": ["COMPANY_MANAGED", "TEAM_MANAGED"]}
]',
1, 2,
'[
    {"field": "project_key", "rules": ["required", "unique", "pattern"]},
    {"field": "name", "rules": ["required", "max_length_200"]},
    {"field": "lead_username", "rules": ["user_exists"]}
]',
'{
    "project_key": "projectKey",
    "name": "name",
    "description": "description",
    "lead_username": "leadUsername",
    "category": "category"
}',
true, 1000),

-- Issue Template
(gen_random_uuid(), 'Issue Import', 'ISSUE', '1.0',
'[
    {"column_name": "issue_key", "display_name": "Issue Key", "data_type": "VARCHAR", "max_length": 50, "required": false},
    {"column_name": "project_key", "display_name": "Project Key", "data_type": "VARCHAR", "max_length": 10, "required": true},
    {"column_name": "issue_type", "display_name": "Issue Type", "data_type": "VARCHAR", "max_length": 50, "required": true, "allowed_values": ["Epic", "Story", "Task", "Bug", "Subtask"]},
    {"column_name": "summary", "display_name": "Summary", "data_type": "VARCHAR", "max_length": 500, "required": true},
    {"column_name": "description", "display_name": "Description", "data_type": "TEXT", "required": false},
    {"column_name": "priority", "display_name": "Priority", "data_type": "VARCHAR", "max_length": 30, "required": false, "allowed_values": ["Highest", "High", "Medium", "Low", "Lowest"]},
    {"column_name": "status", "display_name": "Status", "data_type": "VARCHAR", "max_length": 30, "required": true},
    {"column_name": "assignee", "display_name": "Assignee", "data_type": "VARCHAR", "max_length": 150, "required": false},
    {"column_name": "reporter", "display_name": "Reporter", "data_type": "VARCHAR", "max_length": 150, "required": false},
    {"column_name": "parent_key", "display_name": "Parent Issue", "data_type": "VARCHAR", "max_length": 50, "required": false},
    {"column_name": "labels", "display_name": "Labels", "data_type": "VARCHAR", "max_length": 500, "required": false},
    {"column_name": "created", "display_name": "Created Date", "data_type": "DATETIME", "required": false},
    {"column_name": "updated", "display_name": "Updated Date", "data_type": "DATETIME", "required": false},
    {"column_name": "due_date", "display_name": "Due Date", "data_type": "DATE", "required": false},
    {"column_name": "story_points", "display_name": "Story Points", "data_type": "INTEGER", "required": false},
    {"column_name": "resolution", "display_name": "Resolution", "data_type": "VARCHAR", "max_length": 30, "required": false}
]',
1, 2,
'[
    {"field": "project_key", "rules": ["required", "project_exists"]},
    {"field": "issue_type", "rules": ["required", "valid_issue_type"]},
    {"field": "summary", "rules": ["required", "max_length_500"]},
    {"field": "status", "rules": ["required", "valid_status"]},
    {"field": "parent_key", "rules": ["fk_issue"]},
    {"field": "assignee", "rules": ["user_exists"]},
    {"field": "reporter", "rules": ["user_exists"]}
]',
'{
    "project_key": "projectKey",
    "issue_type": "issueType",
    "summary": "summary",
    "description": "description",
    "priority": "priority",
    "status": "status",
    "assignee": "assigneeUsername",
    "reporter": "reporterUsername",
    "parent_key": "parentIssueKey",
    "labels": "labels",
    "created": "createdAt",
    "updated": "updatedAt",
    "due_date": "dueDate",
    "story_points": "storyPoints",
    "resolution": "resolution"
}',
true, 50000),

-- User Template
(gen_random_uuid(), 'User Import', 'USER', '1.0',
'[
    {"column_name": "username", "display_name": "Username", "data_type": "VARCHAR", "max_length": 150, "required": true},
    {"column_name": "email", "display_name": "Email", "data_type": "VARCHAR", "max_length": 255, "required": true},
    {"column_name": "display_name", "display_name": "Display Name", "data_type": "VARCHAR", "max_length": 200, "required": false},
    {"column_name": "active", "display_name": "Active", "data_type": "BOOLEAN", "required": false},
    {"column_name": "groups", "display_name": "Groups", "data_type": "VARCHAR", "max_length": 500, "required": false}
]',
1, 2,
'[
    {"field": "username", "rules": ["required", "unique", "pattern"]},
    {"field": "email", "rules": ["required", "email_format", "unique"]}
]',
'{
    "username": "username",
    "email": "email",
    "display_name": "displayName",
    "active": "active",
    "groups": "groups"
}',
true, 10000);

-- ============================================
-- SEED DATA: Default Validation Rules
-- ============================================
INSERT INTO jira_migration.validation_rules (id, rule_name, entity_type, field_name, rule_type, rule_config, error_message_template, severity, is_active, display_order) VALUES
(gen_random_uuid(), 'project_key_required', 'PROJECT', 'project_key', 'REQUIRED',
'{}', 'Project key is required', 'ERROR', true, 1),
(gen_random_uuid(), 'project_key_pattern', 'PROJECT', 'project_key', 'FORMAT',
'{"pattern": "^[A-Z][A-Z0-9]{0,9}$"}', 'Project key must be uppercase letters and numbers, starting with a letter (max 10 chars)', 'ERROR', true, 2),
(gen_random_uuid(), 'project_key_unique', 'PROJECT', 'project_key', 'UNIQUE',
'{"table": "projects", "column": "project_key"}', 'Project key already exists', 'ERROR', true, 3),
(gen_random_uuid(), 'project_name_required', 'PROJECT', 'name', 'REQUIRED',
'{}', 'Project name is required', 'ERROR', true, 4),

(gen_random_uuid(), 'issue_project_required', 'ISSUE', 'project_key', 'REQUIRED',
'{}', 'Project key is required', 'ERROR', true, 10),
(gen_random_uuid(), 'issue_type_required', 'ISSUE', 'issue_type', 'REQUIRED',
'{}', 'Issue type is required', 'ERROR', true, 11),
(gen_random_uuid(), 'issue_summary_required', 'ISSUE', 'summary', 'REQUIRED',
'{}', 'Issue summary is required', 'ERROR', true, 12),
(gen_random_uuid(), 'issue_summary_max_length', 'ISSUE', 'summary', 'RANGE',
'{"max": 500}', 'Summary cannot exceed 500 characters', 'ERROR', true, 13),
(gen_random_uuid(), 'issue_status_required', 'ISSUE', 'status', 'REQUIRED',
'{}', 'Status is required', 'ERROR', true, 14),
(gen_random_uuid(), 'issue_parent_fk', 'ISSUE', 'parent_key', 'FK',
'{"entity": "ISSUE", "field": "issue_key"}', 'Parent issue does not exist', 'ERROR', false, 15),

(gen_random_uuid(), 'user_username_required', 'USER', 'username', 'REQUIRED',
'{}', 'Username is required', 'ERROR', true, 20),
(gen_random_uuid(), 'user_email_required', 'USER', 'email', 'REQUIRED',
'{}', 'Email is required', 'ERROR', true, 21),
(gen_random_uuid(), 'user_email_format', 'USER', 'email', 'FORMAT',
'{"pattern": "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"}', 'Invalid email format', 'ERROR', true, 22);

COMMENT ON TABLE jira_migration.migration_jobs IS 'Tracks all import/export migration operations';
COMMENT ON TABLE jira_migration.entity_status IS 'Per-entity processing status within a migration job';
COMMENT ON TABLE jira_migration.csv_templates IS 'CSV template definitions for bulk data import';
COMMENT ON TABLE jira_migration.field_mappings IS 'User-created field mappings for import/export';
COMMENT ON TABLE jira_migration.validation_rules IS 'Validation rules for data import';
COMMENT ON TABLE jira_migration.backup_entities IS 'Exported entity snapshots for rollback';
COMMENT ON TABLE jira_migration.migration_audit IS 'Audit trail for migration operations';
COMMENT ON TABLE jira_migration.user_mappings IS 'Maps source users to target users during import';
COMMENT ON TABLE jira_migration.project_mappings IS 'Maps source projects to target projects during import';