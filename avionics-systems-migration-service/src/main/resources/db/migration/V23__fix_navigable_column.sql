-- Fix missing navigable column in field_definitions table
-- The JPA entity FieldDefinition expects this column at line 78, but it was never added
-- plugin_field_registry and custom_field_definitions already have this column

ALTER TABLE jira_migration.field_definitions
    ADD COLUMN IF NOT EXISTS navigable BOOLEAN DEFAULT TRUE;