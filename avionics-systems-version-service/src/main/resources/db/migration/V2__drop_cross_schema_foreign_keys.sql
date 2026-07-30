-- Microservice owns version data; project/issue rows live in other schemas/services.
-- Drop FK constraints that reference projects/issues tables not present in version_db.

ALTER TABLE IF EXISTS project_versions
    DROP CONSTRAINT IF EXISTS fk_project_versions_project;

ALTER TABLE IF EXISTS issue_fix_versions
    DROP CONSTRAINT IF EXISTS fk_issue_fix_versions_issue;

ALTER TABLE IF EXISTS issue_fix_versions
    DROP CONSTRAINT IF EXISTS fk_issue_fix_versions_version;

ALTER TABLE IF EXISTS issue_affects_versions
    DROP CONSTRAINT IF EXISTS fk_issue_affects_versions_issue;

ALTER TABLE IF EXISTS issue_affects_versions
    DROP CONSTRAINT IF EXISTS fk_issue_affects_versions_version;
