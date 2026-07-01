-- Fix for project_schemes unique constraint
-- Run this SQL directly against your PostgreSQL database

-- Check if the constraint already exists
DO $$
BEGIN
    -- Drop existing index if it exists with different name
    IF EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'project_schemes'
        AND indexname = 'project_schemes_project_id_idx'
    ) THEN
        DROP INDEX jira_project.project_schemes_project_id_idx;
    END IF;

    -- Add unique constraint on project_id
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'project_schemes_project_id_key'
    ) THEN
        ALTER TABLE jira_project.project_schemes
        ADD CONSTRAINT project_schemes_project_id_key UNIQUE (project_id);
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'Constraint may already exist: %', SQLERRM;
END $$;

-- Verify the constraint
SELECT
    conname as constraint_name,
    contype as constraint_type
FROM pg_constraint
WHERE conrelid = 'jira_project.project_schemes'::regclass;