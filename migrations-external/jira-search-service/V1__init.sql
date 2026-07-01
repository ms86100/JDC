-- V1__init.sql - Initial schema for jira-search-service
-- Schema: jira_search

-- Create extension for UUID generation if not exists

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_search;

-- Search index table
CREATE TABLE jira_search.search_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    search_vector tsvector,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

-- GIN index for full-text search
CREATE INDEX idx_search_index_search_vector ON jira_search.search_index USING GIN(search_vector);

-- Indexes for entity lookup
CREATE INDEX idx_search_index_entity_type ON jira_search.search_index(entity_type);
CREATE INDEX idx_search_index_entity_id ON jira_search.search_index(entity_id);
CREATE INDEX idx_search_index_created_at ON jira_search.search_index(created_at DESC);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION jira_search.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for search_index updated_at
CREATE TRIGGER update_search_index_updated_at
    BEFORE UPDATE ON jira_search.search_index
    FOR EACH ROW
    EXECUTE FUNCTION jira_search.update_updated_at_column();

-- Trigger to auto-update search_vector on insert/update
CREATE OR REPLACE FUNCTION jira_search.update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_search_index_vector
    BEFORE INSERT OR UPDATE ON jira_search.search_index
    FOR EACH ROW
    EXECUTE FUNCTION jira_search.update_search_vector();