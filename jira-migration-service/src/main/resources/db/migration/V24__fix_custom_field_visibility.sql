-- Fix the field version trigger that references non-existent updated_by column
CREATE OR REPLACE FUNCTION jira_migration.increment_field_version()
RETURNS TRIGGER AS $$
BEGIN
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
        CURRENT_TIMESTAMP, OLD.created_by, 'Field definition updated'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Un-hide all custom fields so they appear on the issue detail view
UPDATE jira_migration.field_definitions
SET hidden = false
WHERE custom = true AND hidden = true;
