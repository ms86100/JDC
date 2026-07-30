-- Link projects to permission schemes (JPA entity expects this column).

ALTER TABLE jira_project.projects
    ADD COLUMN IF NOT EXISTS permission_scheme_id UUID
        REFERENCES jira_project.permission_schemes(id);

CREATE INDEX IF NOT EXISTS idx_projects_permission_scheme
    ON jira_project.projects(permission_scheme_id);
