-- V5__add_performance_indexes.sql
-- Add missing indexes for production performance optimization

-- ========================================================================
-- Migration Jobs Indexes
-- ========================================================================

-- Composite index for filtering active jobs by status and time
CREATE INDEX IF NOT EXISTS idx_migration_jobs_status_initiated
ON jira_migration.migration_jobs(job_status, initiated_at DESC)
WHERE job_status IN ('PENDING', 'IN_PROGRESS');

-- Index for completed jobs with rollback capability
CREATE INDEX IF NOT EXISTS idx_migration_jobs_can_rollback
ON jira_migration.migration_jobs(can_rollback, job_status)
WHERE can_rollback = true;

-- Index for job lookup by user
CREATE INDEX IF NOT EXISTS idx_migration_jobs_initiated_by
ON jira_migration.migration_jobs(initiated_by, initiated_at DESC);

-- Index for project-scoped job lookup
CREATE INDEX IF NOT EXISTS idx_migration_jobs_source_project
ON jira_migration.migration_jobs(source_project_id, initiated_at DESC);

CREATE INDEX IF NOT EXISTS idx_migration_jobs_target_project
ON jira_migration.migration_jobs(target_project_id, initiated_at DESC);

-- JSONB index for config queries
CREATE INDEX IF NOT EXISTS idx_migration_jobs_config
ON jira_migration.migration_jobs USING GIN (config)
WHERE config IS NOT NULL;

-- Index for rollback chain traversal
CREATE INDEX IF NOT EXISTS idx_migration_jobs_rollback_id
ON jira_migration.migration_jobs(rollback_job_id)
WHERE rollback_job_id IS NOT NULL;

-- ========================================================================
-- Entity Status Indexes
-- ========================================================================

-- Primary query pattern: job entity lookup
CREATE INDEX IF NOT EXISTS idx_entity_status_job
ON jira_migration.entity_status(job_id, entity_type, processing_order);

-- Status filter index for failed entities
CREATE INDEX IF NOT EXISTS idx_entity_status_status
ON jira_migration.entity_status(status, completed_at DESC)
WHERE status = 'FAILED';

-- DLQ entry cleanup index
CREATE INDEX IF NOT EXISTS idx_entity_status_dlq
ON jira_migration.entity_status(status, started_at)
WHERE status = 'DLQ';

-- Entity lookup by key
CREATE INDEX IF NOT EXISTS idx_entity_status_source_id
ON jira_migration.entity_status(entity_key, job_id);

-- ========================================================================
-- Field Definitions Indexes
-- ========================================================================

-- Plugin field lookup by source
CREATE INDEX IF NOT EXISTS idx_field_definitions_plugin
ON jira_migration.field_definitions(plugin_source, plugin_namespace)
WHERE plugin_source IS NOT NULL;

-- Deprecated field filtering
CREATE INDEX IF NOT EXISTS idx_field_definitions_deprecated
ON jira_migration.field_definitions(deprecated, updated_at)
WHERE deprecated = true;

-- Custom field fast lookup
CREATE INDEX IF NOT EXISTS idx_field_definitions_custom
ON jira_migration.field_definitions(custom, field_key)
WHERE custom = true;

-- Screen region queries
CREATE INDEX IF NOT EXISTS idx_field_definitions_screen
ON jira_migration.field_definitions(screen_region, field_type)
WHERE screen_region IS NOT NULL;

-- Searchable field filtering
CREATE INDEX IF NOT EXISTS idx_field_definitions_searchable
ON jira_migration.field_definitions(searchable, search_weight DESC)
WHERE searchable = true;

-- ========================================================================
-- Issue Field Values Indexes
-- ========================================================================

-- Issue field value lookup (most common query)
CREATE INDEX IF NOT EXISTS idx_issue_field_values_issue
ON jira_migration.issue_field_values(issue_id, field_definition_id);

-- Validation status for DLQ cleanup
CREATE INDEX IF NOT EXISTS idx_issue_field_values_validation
ON jira_migration.issue_field_values(validation_status, created_at)
WHERE validation_status != 'VALID';

-- Search text index (for full-text search)
CREATE INDEX IF NOT EXISTS idx_issue_field_values_searchable
ON jira_migration.issue_field_values(searchable_text)
WHERE searchable_text IS NOT NULL;

-- ========================================================================
-- Custom Field Options Indexes
-- ========================================================================

-- Active options lookup
CREATE INDEX IF NOT EXISTS idx_custom_field_options_active
ON jira_migration.custom_field_options(custom_field_id, disabled, sequence)
WHERE disabled = false;

-- Parent option hierarchy
CREATE INDEX IF NOT EXISTS idx_custom_field_options_parent
ON jira_migration.custom_field_options(parent_option_id, sequence)
WHERE parent_option_id IS NOT NULL;

-- ========================================================================
-- Field Mappings Indexes
-- ========================================================================

-- Mapping type lookup
CREATE INDEX IF NOT EXISTS idx_field_mappings_type
ON jira_migration.field_mappings(mapping_type, created_at DESC);
