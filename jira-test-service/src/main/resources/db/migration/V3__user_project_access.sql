-- User Project Access table for permission management
-- V2: Add user_project_access table for project-level permissions

CREATE TABLE user_project_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    project_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL, -- ADMIN, TEST_MANAGER, TESTER, VIEWER, DEVELOPER
    has_create_permission BOOLEAN DEFAULT false,
    has_update_permission BOOLEAN DEFAULT false,
    has_delete_permission BOOLEAN DEFAULT false,
    has_execute_permission BOOLEAN DEFAULT false,
    has_import_permission BOOLEAN DEFAULT false,
    has_report_permission BOOLEAN DEFAULT false,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID,
    CONSTRAINT uk_user_project UNIQUE (user_id, project_id)
);

CREATE INDEX idx_user_project_access_user ON user_project_access(user_id);
CREATE INDEX idx_user_project_access_project ON user_project_access(project_id);
CREATE INDEX idx_user_project_access_role ON user_project_access(role);

-- Seed default roles for common project access patterns
-- Admin role with all permissions
INSERT INTO user_project_access (id, user_id, project_id, role, has_create_permission, has_update_permission,
    has_delete_permission, has_execute_permission, has_import_permission, has_report_permission)
VALUES
    (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 'ADMIN', true, true, true, true, true, true);