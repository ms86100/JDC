-- V1.0.0__add_search_indexes.sql
-- Index optimization for Jira Search Service
-- This migration adds performance indexes for full-text search

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_search;

-- Drop existing table if exists (for clean setup)
DROP TABLE IF EXISTS jira_search.search_index CASCADE;

-- Create search index table with optimized structure
CREATE TABLE jira_search.search_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    search_vector tsvector,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_entity_lookup UNIQUE (entity_type, entity_id)
);

-- Create GIN index for full-text search (primary performance index)
CREATE INDEX idx_search_vector ON jira_search.search_index USING GIN (search_vector);

-- Create B-tree indexes for common lookups
CREATE INDEX idx_entity_type ON jira_search.search_index (entity_type);
CREATE INDEX idx_entity_id ON jira_search.search_index (entity_id);
CREATE INDEX idx_entity_type_entity_id ON jira_search.search_index (entity_type, entity_id);

-- Create index for sorting and filtering by creation date
CREATE INDEX idx_created_at ON jira_search.search_index (created_at DESC);

-- Create index for title searches (prefix matching)
CREATE INDEX idx_title ON jira_search.search_index (LOWER(title));

-- Partial index for active issues (optimizes common queries)
CREATE INDEX idx_active_issues ON jira_search.search_index (entity_type, entity_id)
    WHERE entity_type IN ('issue', 'bug', 'story', 'task', 'epic');

-- Create trigger function to auto-update search_vector
CREATE OR REPLACE FUNCTION jira_search.update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.content, '')), 'B');
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update search_vector on insert/update
DROP TRIGGER IF EXISTS trg_search_index_update ON jira_search.search_index;
CREATE TRIGGER trg_search_index_update
    BEFORE INSERT OR UPDATE ON jira_search.search_index
    FOR EACH ROW
    EXECUTE FUNCTION jira_search.update_search_vector();

-- Create function to rebuild all search vectors (for maintenance)
CREATE OR REPLACE FUNCTION jira_search.rebuild_search_index()
RETURNS INTEGER AS $$
DECLARE
    count INTEGER;
BEGIN
    UPDATE jira_search.search_index
    SET search_vector =
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(content, '')), 'B');
    GET DIAGNOSTICS count = ROW_COUNT;
    RETURN count;
END;
$$ LANGUAGE plpgsql;

-- Create statistics view for index usage monitoring
CREATE OR REPLACE VIEW jira_search.index_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_tup_read AS read_operations,
    idx_tup_fetch AS fetch_operations,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'jira_search'
ORDER BY idx_tup_read DESC;

-- Create table for search query analytics
CREATE TABLE IF NOT EXISTS jira_search.search_query_log (
    id SERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    query_hash VARCHAR(64),
    entity_type VARCHAR(100),
    result_count INTEGER,
    execution_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for query log analysis
CREATE INDEX idx_query_log_created_at ON jira_search.search_query_log (created_at DESC);
CREATE INDEX idx_query_log_hash ON jira_search.search_query_log (query_hash);

-- Create table for tracking reindex operations
CREATE TABLE IF NOT EXISTS jira_search.reindex_history (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_processed INTEGER,
    status VARCHAR(50) DEFAULT 'running'
);

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON SCHEMA jira_search TO jira_app;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA jira_search TO jira_app;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA jira_search TO jira_app;

COMMENT ON TABLE jira_search.search_index IS 'Full-text search index for all Jira entities';
COMMENT ON INDEX idx_search_vector IS 'GIN index for fast full-text search operations';
COMMENT ON TABLE jira_search.search_query_log IS 'Analytics for search query performance';