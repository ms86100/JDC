-- V3__add_cascade_delete_constraints.sql
-- Add proper cascade delete constraints for dynamic field architecture

-- ========================================================================
-- Add CASCADE DELETE for issue_field_values -> field_definitions
-- ========================================================================

-- Drop existing FK constraint (will be recreated with ON DELETE CASCADE)
ALTER TABLE jira_migration.issue_field_values
DROP CONSTRAINT IF EXISTS issue_field_values_field_definition_id_fkey;

-- Recreate FK with ON DELETE CASCADE
ALTER TABLE jira_migration.issue_field_values
ADD CONSTRAINT issue_field_values_field_definition_id_fkey
FOREIGN KEY (field_definition_id)
REFERENCES jira_migration.field_definitions(id)
ON DELETE CASCADE;

-- ========================================================================
-- Add CASCADE DELETE for custom_field_contexts -> custom_field_definitions
-- ========================================================================

ALTER TABLE jira_migration.custom_field_contexts
DROP CONSTRAINT IF EXISTS custom_field_contexts_custom_field_id_fkey;

ALTER TABLE jira_migration.custom_field_contexts
ADD CONSTRAINT custom_field_contexts_custom_field_id_fkey
FOREIGN KEY (custom_field_id)
REFERENCES jira_migration.custom_field_definitions(id)
ON DELETE CASCADE;

-- ========================================================================
-- Add CASCADE DELETE for custom_field_options -> custom_field_definitions
-- ========================================================================

ALTER TABLE jira_migration.custom_field_options
DROP CONSTRAINT IF EXISTS custom_field_options_custom_field_id_fkey;

ALTER TABLE jira_migration.custom_field_options
ADD CONSTRAINT custom_field_options_custom_field_id_fkey
FOREIGN KEY (custom_field_id)
REFERENCES jira_migration.custom_field_definitions(id)
ON DELETE CASCADE;

-- ========================================================================
-- Add CASCADE DELETE for custom_field_options -> custom_field_options (parent)
-- ========================================================================

ALTER TABLE jira_migration.custom_field_options
DROP CONSTRAINT IF EXISTS custom_field_options_parent_option_id_fkey;

ALTER TABLE jira_migration.custom_field_options
ADD CONSTRAINT custom_field_options_parent_option_id_fkey
FOREIGN KEY (parent_option_id)
REFERENCES jira_migration.custom_field_options(id)
ON DELETE CASCADE;

-- ========================================================================
-- Verify constraints are properly set
-- ========================================================================

SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
    ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'jira_migration'
    AND tc.table_name IN ('issue_field_values', 'custom_field_contexts', 'custom_field_options');

-- ========================================================================
-- Log constraint updates
-- ========================================================================

DO $$
BEGIN
    RAISE NOTICE 'Cascade delete constraints added successfully:';
    RAISE NOTICE '  - issue_field_values -> field_definitions (ON DELETE CASCADE)';
    RAISE NOTICE '  - custom_field_contexts -> custom_field_definitions (ON DELETE CASCADE)';
    RAISE NOTICE '  - custom_field_options -> custom_field_definitions (ON DELETE CASCADE)';
    RAISE NOTICE '  - custom_field_options -> custom_field_options parent (ON DELETE CASCADE)';
END $$;