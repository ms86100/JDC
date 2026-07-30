-- V6__add_field_versioning.sql
-- Add field schema versioning support for migration and history

-- ========================================================================
-- Field Version History Table
-- ========================================================================

CREATE TABLE jira_migration.field_version_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_definition_id UUID NOT NULL,

    -- Version info
    version INT NOT NULL,
    change_type VARCHAR(20) NOT NULL, -- CREATED, UPDATED, DELETED

    -- Field snapshot
    field_key VARCHAR(255),
    display_name VARCHAR(255),
    description TEXT,
    field_type VARCHAR(50),
    renderer VARCHAR(100),
    screen_region VARCHAR(50),

    -- Configuration snapshot
    schema_definition JSONB,
    renderer_config JSONB,
    validation_rules JSONB,
    options JSONB,

    -- Metadata
    searchable BOOLEAN,
    sortable BOOLEAN,
    filterable BOOLEAN,
    required BOOLEAN,
    read_only BOOLEAN,
    hidden BOOLEAN,

    -- Audit
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID,
    change_reason VARCHAR(500),

    -- Constraint
    CONSTRAINT fk_field_version_field_def
        FOREIGN KEY (field_definition_id)
        REFERENCES jira_migration.field_definitions(id)
        ON DELETE CASCADE
);

-- ========================================================================
-- Field Schema Migration Table
-- ========================================================================

CREATE TABLE jira_migration.field_schema_migrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_definition_id UUID NOT NULL,

    from_version INT NOT NULL,
    to_version INT NOT NULL,

    migration_type VARCHAR(50) NOT NULL, -- RENAME, RETYPE, ADD_OPTION, REMOVE_OPTION, etc.
    migration_script JSONB,
    rollback_script JSONB,

    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK
    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_schema_migration_field_def
        FOREIGN KEY (field_definition_id)
        REFERENCES jira_migration.field_definitions(id)
        ON DELETE CASCADE
);

-- ========================================================================
-- Indexes
-- ========================================================================

-- Field version history lookup by field
CREATE INDEX idx_field_version_history_field
ON jira_migration.field_version_history(field_definition_id, changed_at DESC);

-- Field version lookup by version
CREATE INDEX idx_field_version_history_version
ON jira_migration.field_version_history(field_definition_id, version);

-- Schema migration by status
CREATE INDEX idx_field_schema_migrations_status
ON jira_migration.field_schema_migrations(status, created_at)
WHERE status IN ('PENDING', 'IN_PROGRESS');

-- Schema migration by field
CREATE INDEX idx_field_schema_migrations_field
ON jira_migration.field_schema_migrations(field_definition_id, created_at DESC);

-- ========================================================================
-- Function to auto-increment field version on update
-- ========================================================================

CREATE OR REPLACE FUNCTION jira_migration.increment_field_version()
RETURNS TRIGGER AS $$
BEGIN
    -- Increment version on update
    IF TG_OP = 'UPDATE' THEN
        NEW.version = OLD.version + 1;

        -- Insert into version history
        INSERT INTO jira_migration.field_version_history (
            field_definition_id, version, change_type,
            field_key, display_name, description, field_type, renderer, screen_region,
            schema_definition, renderer_config, validation_rules, options,
            searchable, sortable, filterable, required, read_only, hidden,
            changed_at, changed_by, change_reason
        ) VALUES (
            OLD.id, OLD.version, 'UPDATED',
            OLD.field_key, OLD.display_name, OLD.description, OLD.field_type::TEXT, OLD.renderer::TEXT, OLD.screen_region::TEXT,
            OLD.schema_definition, OLD.renderer_config, OLD.validation_rules, OLD.options,
            OLD.searchable, OLD.sortable, OLD.filterable, OLD.required, OLD.read_only, OLD.hidden,
            CURRENT_TIMESTAMP, NEW.updated_by, 'Field definition updated'
        );
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO jira_migration.field_version_history (
            field_definition_id, version, change_type,
            field_key, display_name, description, field_type, renderer, screen_region,
            schema_definition, renderer_config, validation_rules, options,
            searchable, sortable, filterable, required, read_only, hidden,
            changed_at, changed_by, change_reason
        ) VALUES (
            NEW.id, 1, 'CREATED',
            NEW.field_key, NEW.display_name, NEW.description, NEW.field_type::TEXT, NEW.renderer::TEXT, NEW.screen_region::TEXT,
            NEW.schema_definition, NEW.renderer_config, NEW.validation_rules, NEW.options,
            NEW.searchable, NEW.sortable, NEW.filterable, NEW.required, NEW.read_only, NEW.hidden,
            CURRENT_TIMESTAMP, NEW.created_by, 'Field definition created'
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for auto-versioning
CREATE TRIGGER trg_field_definition_version
AFTER INSERT OR UPDATE ON jira_migration.field_definitions
FOR EACH ROW EXECUTE FUNCTION jira_migration.increment_field_version();

DO $$
BEGIN
    RAISE NOTICE 'Field versioning tables created successfully:';
    RAISE NOTICE '  - field_version_history: tracks all field changes';
    RAISE NOTICE '  - field_schema_migrations: manages schema migrations';
    RAISE NOTICE '  - Auto-versioning trigger installed';
END $$;