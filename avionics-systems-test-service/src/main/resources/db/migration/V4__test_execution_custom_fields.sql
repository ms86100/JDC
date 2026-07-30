-- V4__test_execution_custom_fields.sql
-- Test Service - Custom fields and attributes for test executions

-- ============================================
-- TEST CUSTOM FIELDS TABLE
-- Store custom field values for test entities
-- ============================================
CREATE TABLE jira_test.test_custom_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_value TEXT,
    field_type VARCHAR(50) DEFAULT 'TEXT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (entity_type, entity_id, field_name)
);

-- ============================================
-- TEST PRIORITY TABLE
-- Test case priority configuration
-- ============================================
CREATE TABLE jira_test.test_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    priority_name VARCHAR(100) NOT NULL UNIQUE,
    priority_order INTEGER NOT NULL,
    color VARCHAR(20) DEFAULT '#6c757d',
    icon VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEST SEVERITY TABLE
-- Test case severity configuration
-- ============================================
CREATE TABLE jira_test.test_severities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    severity_name VARCHAR(100) NOT NULL UNIQUE,
    severity_order INTEGER NOT NULL,
    color VARCHAR(20) DEFAULT '#6c757d',
    icon VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEST TAGS TABLE
-- Tags for organizing test cases
-- ============================================
CREATE TABLE jira_test.test_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag_name VARCHAR(100) NOT NULL UNIQUE,
    tag_color VARCHAR(20) DEFAULT '#6c757d',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TEST TAG ASSIGNMENTS TABLE
-- Many-to-many relationship between tests and tags
-- ============================================
CREATE TABLE jira_test.test_tag_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (test_id, tag_id)
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_test_custom_fields_entity ON jira_test.test_custom_fields(entity_type, entity_id);
CREATE INDEX idx_test_tags_name ON jira_test.test_tags(tag_name);
CREATE INDEX idx_test_tag_assignments_test ON jira_test.test_tag_assignments(test_id);
CREATE INDEX idx_test_tag_assignments_tag ON jira_test.test_tag_assignments(tag_id);

-- ============================================
-- SEED DATA: Default Priorities
-- ============================================
INSERT INTO jira_test.test_priorities (id, priority_name, priority_order, color, icon) VALUES
    (gen_random_uuid(), 'Critical', 1, '#dc3545', '🔴'),
    (gen_random_uuid(), 'High', 2, '#fd7e14', '🟠'),
    (gen_random_uuid(), 'Medium', 3, '#ffc107', '🟡'),
    (gen_random_uuid(), 'Low', 4, '#28a745', '🟢')
ON CONFLICT (priority_name) DO NOTHING;

-- ============================================
-- SEED DATA: Default Severities
-- ============================================
INSERT INTO jira_test.test_severities (id, severity_name, severity_order, color, icon) VALUES
    (gen_random_uuid(), 'Critical', 1, '#dc3545', '🔴'),
    (gen_random_uuid(), 'High', 2, '#fd7e14', '🟠'),
    (gen_random_uuid(), 'Medium', 3, '#ffc107', '🟡'),
    (gen_random_uuid(), 'Low', 4, '#28a745', '🟢')
ON CONFLICT (severity_name) DO NOTHING;