DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'jira_migration')
       AND NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'avionics_systems_migration') THEN
        ALTER SCHEMA jira_migration RENAME TO avionics_systems_migration;
    END IF;
END $$;
