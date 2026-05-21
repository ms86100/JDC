-- Audit Log Table - V8
-- Comprehensive audit logging for compliance and tracking

-- ============================================
-- AUDIT LOG TABLE
-- ============================================

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(50) NOT NULL, -- AuditAction enum value
    entity_type VARCHAR(50), -- TEST, TEST_EXECUTION, USER, IMPORT, etc.
    entity_id UUID, -- ID of the affected entity
    entity_name VARCHAR(500), -- Name of the affected entity
    project_id UUID, -- Project context
    user_id UUID, -- User who performed the action
    user_name VARCHAR(200), -- User display name
    ip_address VARCHAR(45), -- Client IP address
    old_value TEXT, -- Previous value (for updates)
    new_value TEXT, -- New value (for updates)
    change_description TEXT, -- Human-readable change description
    metadata TEXT, -- JSON for additional context
    status VARCHAR(50) DEFAULT 'SUCCESS', -- SUCCESS, FAILURE
    error_message TEXT, -- Error message if failed
    action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_project ON audit_log(project_id);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_timestamp ON audit_log(action_timestamp);
CREATE INDEX idx_audit_status ON audit_log(status);

-- Composite indexes for common queries
CREATE INDEX idx_audit_project_action ON audit_log(project_id, action);
CREATE INDEX idx_audit_project_timestamp ON audit_log(project_id, action_timestamp DESC);
CREATE INDEX idx_audit_entity_timestamp ON audit_log(entity_type, entity_id, action_timestamp DESC);

-- ============================================
-- RETENTION POLICY (Optional)
-- ============================================

-- Note: In production, consider implementing a retention policy
-- to automatically purge old audit logs based on compliance requirements.
-- For example:
-- DELETE FROM audit_log WHERE action_timestamp < NOW() - INTERVAL '2 years';

-- ============================================
-- SEED DATA (for testing)
-- ============================================

-- Example audit entries (commented out for production)
-- INSERT INTO audit_log (action, entity_type, entity_id, entity_name, project_id, user_name, status)
-- VALUES ('TEST_CREATED', 'TEST', gen_random_uuid(), 'Example Test', gen_random_uuid(), 'admin', 'SUCCESS');