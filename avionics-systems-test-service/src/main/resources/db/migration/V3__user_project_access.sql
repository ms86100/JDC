-- User Project Access table for permission management
-- V3: Ensure user_project_access table exists (idempotent, may already exist from V2_1)

CREATE TABLE IF NOT EXISTS user_project_access (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    project_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_user_project_access_user ON user_project_access(user_id);
CREATE INDEX IF NOT EXISTS idx_user_project_access_project ON user_project_access(project_id);
CREATE INDEX IF NOT EXISTS idx_user_project_access_role ON user_project_access(role);