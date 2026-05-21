-- Extend wizard step values for 8-step enterprise flow
ALTER TABLE jira_migration.wizard_sessions DROP CONSTRAINT IF EXISTS valid_wizard_step;
ALTER TABLE jira_migration.wizard_sessions ADD CONSTRAINT valid_wizard_step CHECK (current_step IN (
    'UPLOAD', 'TARGET_PROJECT', 'VALIDATE', 'MAP_FIELDS', 'MAP_USERS',
    'CONFIGURE', 'REVIEW', 'EXECUTE', 'COMPLETED'
));
