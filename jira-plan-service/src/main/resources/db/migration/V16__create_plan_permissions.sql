-- V16__create_plan_permissions.sql
-- Creates the plan_permissions table for granular access control

CREATE TABLE jira_plan.plan_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES jira_plan.plans(id) ON DELETE CASCADE,
    permission_type VARCHAR(50) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,  -- 'USER', 'GROUP', 'PROJECT_ROLE'
    principal_id UUID NOT NULL,
    granted_by UUID,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, permission_type, principal_type, principal_id)
);

-- Indexes
CREATE INDEX idx_plan_permissions_plan_id ON jira_plan.plan_permissions(plan_id);
CREATE INDEX idx_plan_permissions_principal ON jira_plan.plan_permissions(principal_type, principal_id);
CREATE INDEX idx_plan_permissions_type ON jira_plan.plan_permissions(permission_type);

-- Comments
COMMENT ON TABLE jira_plan.plan_permissions IS 'Granular permissions for plan access';
COMMENT ON COLUMN jira_plan.plan_permissions.permission_type IS 'Permission type: VIEW, EDIT, ADMIN, MANAGE_MEMBERS, etc.';
COMMENT ON COLUMN jira_plan.plan_permissions.principal_type IS 'Type of principal: USER, GROUP, or PROJECT_ROLE';
COMMENT ON COLUMN jira_plan.plan_permissions.principal_id IS 'ID of the user, group, or project role';