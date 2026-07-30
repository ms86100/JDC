-- Component service: logical references to project/issue IDs without cross-schema FKs.

ALTER TABLE IF EXISTS project_components
    DROP CONSTRAINT IF EXISTS fk_project_components_project;

ALTER TABLE IF EXISTS issue_components
    DROP CONSTRAINT IF EXISTS fk_issue_components_issue;

ALTER TABLE IF EXISTS issue_components
    DROP CONSTRAINT IF EXISTS fk_issue_components_component;
