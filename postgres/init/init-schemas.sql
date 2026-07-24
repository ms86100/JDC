-- Enable UUID extension for all microservices
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Fix: field_version_history trigger must skip INSERT (only log on UPDATE)
CREATE OR REPLACE FUNCTION jira_migration.increment_field_version()
RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'UPDATE' THEN
    INSERT INTO jira_migration.field_version_history 
      (field_definition_id, version, change_type, field_key, display_name, description, field_type, 
       renderer, screen_region, schema_definition, renderer_config, validation_rules, options,
       searchable, sortable, filterable, required, read_only, hidden, changed_at, changed_by, change_reason)
    VALUES 
      (OLD.id, OLD.version, 'UPDATED', OLD.field_key, OLD.display_name, OLD.description, 
       OLD.field_type::TEXT, OLD.renderer::TEXT, OLD.screen_region::TEXT, OLD.schema_definition, 
       OLD.renderer_config, OLD.validation_rules, OLD.options, OLD.searchable, OLD.sortable, 
       OLD.filterable, OLD.required, OLD.read_only, OLD.hidden, CURRENT_TIMESTAMP, OLD.created_by, 
       'Field definition updated');
    NEW.version := COALESCE(OLD.version, 0) + 1;
  END IF;
  IF TG_OP = 'INSERT' THEN
    NEW.version := 1;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
