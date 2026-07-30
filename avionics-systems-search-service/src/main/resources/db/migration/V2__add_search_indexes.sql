-- V2__add_search_indexes.sql
-- Index optimization and additional tables for Jira Search Service

CREATE INDEX IF NOT EXISTS idx_search_vector ON jira_search.search_index USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS idx_entity_type ON jira_search.search_index (entity_type);
CREATE INDEX IF NOT EXISTS idx_entity_id ON jira_search.search_index (entity_id);
CREATE INDEX IF NOT EXISTS idx_entity_type_entity_id ON jira_search.search_index (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON jira_search.search_index (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_title ON jira_search.search_index (LOWER(title));

CREATE TABLE IF NOT EXISTS jira_search.search_query_log (
    id SERIAL PRIMARY KEY,
    query_text TEXT NOT NULL,
    query_hash VARCHAR(64),
    entity_type VARCHAR(100),
    result_count INTEGER,
    execution_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_query_log_created_at ON jira_search.search_query_log (created_at DESC);

CREATE TABLE IF NOT EXISTS jira_search.reindex_history (
    id SERIAL PRIMARY KEY,
    entity_type VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_processed INTEGER,
    status VARCHAR(50) DEFAULT 'running'
);
