-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;

-- Extend wizard step values for 8-step enterprise flow
ALTER TABLE jira_migration.wizard_sessions DROP CONSTRAINT IF EXISTS valid_wizard_step;
ALTER TABLE jira_migration.wizard_sessions ADD CONSTRAINT valid_wizard_step CHECK (current_step IN (
    'UPLOAD', 'TARGET_PROJECT', 'VALIDATE', 'MAP_FIELDS', 'MAP_USERS',
    'CONFIGURE', 'REVIEW', 'EXECUTE', 'COMPLETED'
));
