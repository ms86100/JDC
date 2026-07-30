-- V5: Add unique constraint for project_schemes to prevent duplicate project schemes
-- This prevents the unique constraint violation error when creating projects via wizard

DO $$
BEGIN
    -- Drop the existing unique index if it exists (with different name)
    -- Then create proper unique constraint on project_id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'project_schemes_project_id_unique'
    ) THEN
        ALTER TABLE jira_project.project_schemes
        ADD CONSTRAINT project_schemes_project_id_unique UNIQUE (project_id);
    END IF;
EXCEPTION WHEN duplicate_object THEN
    -- Constraint already exists, ignore
    NULL;
END $$;