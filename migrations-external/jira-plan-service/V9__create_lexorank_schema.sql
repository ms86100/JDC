-- LexoRank: Gap-based string ordering algorithm
-- Jira's patent-pending ranking system for issue ordering
-- Uses buckets for performance and concurrent edit handling

-- Bucket management for LexoRank
CREATE TABLE jira_plan.lexorank_buckets (
    id BIGSERIAL PRIMARY KEY,
    bucket_index INTEGER NOT NULL UNIQUE,
    name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default buckets (0, 1, 2 as used by Jira)
INSERT INTO jira_plan.lexorank_buckets (bucket_index, name) VALUES
    (0, 'Default'),
    (1, 'Archive'),
    (2, 'Suspended');

-- Core LexoRank entries table
CREATE TABLE jira_plan.lexorank_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,  -- 'PLAN_ITEM', 'ISSUE', 'EPIC'
    entity_id UUID NOT NULL,
    bucket_id BIGINT REFERENCES jira_plan.lexorank_buckets(id) DEFAULT 0,
    rank_value VARCHAR(255) NOT NULL,
    locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP,
    locked_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_type, entity_id)
);

-- Balancer state for rank rebalancing
CREATE TABLE jira_plan.lexorank_balancer (
    id BIGSERIAL PRIMARY KEY,
    bucket_index INTEGER NOT NULL UNIQUE,
    last_rank VARCHAR(255),
    balance_threshold INTEGER DEFAULT 5,
    last_balanced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initialize balancer for bucket 0
INSERT INTO jira_plan.lexorank_balancer (bucket_index, balance_threshold) VALUES (0, 5);

-- Rank operation audit log
CREATE TABLE jira_plan.lexorank_audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    operation_type VARCHAR(20) NOT NULL,  -- 'RANK', 'LOCK', 'UNLOCK', 'REBALANCE'
    old_rank VARCHAR(255),
    new_rank VARCHAR(255),
    user_id UUID,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_lexorank_entity ON jira_plan.lexorank_entries(entity_type, entity_id);
CREATE INDEX idx_lexorank_rank ON jira_plan.lexorank_entries(rank_value);
CREATE INDEX idx_lexorank_bucket ON jira_plan.lexorank_entries(bucket_id);
CREATE INDEX idx_lexorank_entity_bucket ON jira_plan.lexorank_entries(entity_type, bucket_id);
CREATE INDEX idx_lexorank_audit_entity ON jira_plan.lexorank_audit_log(entity_type, entity_id);
CREATE INDEX idx_lexorank_audit_created ON jira_plan.lexorank_audit_log(created_at);