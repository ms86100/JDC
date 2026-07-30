-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;

-- V1__init.sql - Initial schema for jira-audit-service
-- Schema: jira_audit

-- Create extension for UUID generation if not exists

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_audit;

-- Audit logs table
CREATE TABLE jira_audit.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    service_name VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    changes JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX idx_audit_service ON jira_audit.audit_logs(service_name);
CREATE INDEX idx_audit_entity ON jira_audit.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON jira_audit.audit_logs(user_id);
CREATE INDEX idx_audit_created ON jira_audit.audit_logs(created_at DESC);
CREATE INDEX idx_audit_action ON jira_audit.audit_logs(action);