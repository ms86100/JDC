-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;


-- V1__init.sql
-- Document Service Database Schema (schema: jira_document)

CREATE SCHEMA IF NOT EXISTS jira_document;

-- ============================================
-- DOCUMENTS TABLE
-- Document/content management
-- ============================================
CREATE TABLE IF NOT EXISTS jira_document.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    content TEXT,
    content_type VARCHAR(100) DEFAULT 'TEXT',
    project_id UUID,
    issue_id UUID,
    created_by UUID,
    updated_by UUID,
    version INTEGER DEFAULT 1,
    parent_document_id UUID,
    tags TEXT[],
    metadata JSONB,
    is_published BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DOCUMENT VERSIONS TABLE
-- Version history for documents
-- ============================================
CREATE TABLE IF NOT EXISTS jira_document.document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES jira_document.documents(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT,
    content_type VARCHAR(100),
    change_summary TEXT,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, version)
);

-- ============================================
-- DOCUMENT ATTACHMENTS TABLE
-- File attachments for documents
-- ============================================
CREATE TABLE IF NOT EXISTS jira_document.document_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES jira_document.documents(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR,
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- DOCUMENT PERMISSIONS TABLE
-- Access control for documents
-- ============================================
CREATE TABLE IF NOT EXISTS jira_document.document_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES jira_document.documents(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- USER, GROUP, PROJECT_ROLE
    entity_id UUID NOT NULL,
    permission_level VARCHAR(50) NOT NULL, -- VIEW, EDIT, ADMIN
    granted_by UUID,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, entity_type, entity_id)
);

-- ============================================
-- DOCUMENT COMMENTS TABLE
-- Comments on documents
-- ============================================
CREATE TABLE IF NOT EXISTS jira_document.document_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES jira_document.documents(id) ON DELETE CASCADE,
    parent_comment_id UUID,
    content TEXT NOT NULL,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_documents_project ON jira_document.documents(project_id);
CREATE INDEX IF NOT EXISTS idx_documents_issue ON jira_document.documents(issue_id);
CREATE INDEX IF NOT EXISTS idx_documents_owner ON jira_document.documents(created_by);
CREATE INDEX IF NOT EXISTS idx_documents_parent ON jira_document.documents(parent_document_id);
CREATE INDEX IF NOT EXISTS idx_document_versions ON jira_document.document_versions(document_id);
CREATE INDEX IF NOT EXISTS idx_document_attachments ON jira_document.document_attachments(document_id);
CREATE INDEX IF NOT EXISTS idx_document_permissions ON jira_document.document_permissions(document_id);
CREATE INDEX IF NOT EXISTS idx_document_comments ON jira_document.document_comments(document_id);

COMMENT ON TABLE jira_document.documents IS 'Document management with versioning and permissions';