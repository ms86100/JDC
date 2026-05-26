-- Enable UUID extension for all microservices
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Initialize Jira Platform Database Schemas
-- This script creates all schemas needed by microservices

-- Auth schema (for future use if services need separate auth schemas)
-- Note: The current auth-service migration creates users, roles, user_roles in public schema
CREATE SCHEMA IF NOT EXISTS jira_auth;
GRANT ALL ON SCHEMA jira_auth TO jiraadmin;

-- User schema (for future use)
CREATE SCHEMA IF NOT EXISTS jira_user;
GRANT ALL ON SCHEMA jira_user TO jiraadmin;

-- Project schema
CREATE SCHEMA IF NOT EXISTS jira_project;
GRANT ALL ON SCHEMA jira_project TO jiraadmin;

-- Issue schema
CREATE SCHEMA IF NOT EXISTS jira_issue;
GRANT ALL ON SCHEMA jira_issue TO jiraadmin;

-- Workflow schema
CREATE SCHEMA IF NOT EXISTS jira_workflow;
GRANT ALL ON SCHEMA jira_workflow TO jiraadmin;

-- Comment schema
CREATE SCHEMA IF NOT EXISTS jira_comment;
GRANT ALL ON SCHEMA jira_comment TO jiraadmin;

-- Notification schema
CREATE SCHEMA IF NOT EXISTS jira_notification;
GRANT ALL ON SCHEMA jira_notification TO jiraadmin;

-- Search schema
CREATE SCHEMA IF NOT EXISTS jira_search;
GRANT ALL ON SCHEMA jira_search TO jiraadmin;

-- Audit schema
CREATE SCHEMA IF NOT EXISTS jira_audit;
GRANT ALL ON SCHEMA jira_audit TO jiraadmin;

-- Attachment schema
CREATE SCHEMA IF NOT EXISTS jira_attachment;
GRANT ALL ON SCHEMA jira_attachment TO jiraadmin;

-- Sprint schema
CREATE SCHEMA IF NOT EXISTS jira_sprint;
GRANT ALL ON SCHEMA jira_sprint TO jiraadmin;

-- Plan schema
CREATE SCHEMA IF NOT EXISTS jira_plan;
GRANT ALL ON SCHEMA jira_plan TO jiraadmin;

-- Admin schema
CREATE SCHEMA IF NOT EXISTS jira_admin;
GRANT ALL ON SCHEMA jira_admin TO jiraadmin;

-- Version schema
CREATE SCHEMA IF NOT EXISTS jira_version;
GRANT ALL ON SCHEMA jira_version TO jiraadmin;

-- Component schema
CREATE SCHEMA IF NOT EXISTS jira_component;
GRANT ALL ON SCHEMA jira_component TO jiraadmin;

-- Create default admin user for login
-- Password: admin123 (BCrypt encoded)
INSERT INTO public.users (id, username, email, password_hash, active, created_at, updated_at)
VALUES (
    '5ba38176-421f-431c-87f9-3836e4147a8c',
    'admin',
    'admin@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

-- Create default user role
INSERT INTO public.roles (id, name, description, created_at)
VALUES (
    gen_random_uuid(),
    'ROLE_USER',
    'Standard user role',
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Create admin role
INSERT INTO public.roles (id, name, description, created_at)
VALUES (
    gen_random_uuid(),
    'ROLE_ADMIN',
    'Administrator role',
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Assign ROLE_USER to admin
INSERT INTO public.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM public.users u, public.roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_USER'
AND NOT EXISTS (
    SELECT 1 FROM public.user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- Grant schema permissions
GRANT USAGE ON SCHEMA jira_auth TO jiraadmin;
GRANT USAGE ON SCHEMA jira_user TO jiraadmin;
GRANT USAGE ON SCHEMA jira_project TO jiraadmin;
GRANT USAGE ON SCHEMA jira_issue TO jiraadmin;
GRANT USAGE ON SCHEMA jira_workflow TO jiraadmin;
GRANT USAGE ON SCHEMA jira_comment TO jiraadmin;
GRANT USAGE ON SCHEMA jira_notification TO jiraadmin;
GRANT USAGE ON SCHEMA jira_search TO jiraadmin;
GRANT USAGE ON SCHEMA jira_audit TO jiraadmin;
GRANT USAGE ON SCHEMA jira_attachment TO jiraadmin;
GRANT USAGE ON SCHEMA jira_sprint TO jiraadmin;
GRANT USAGE ON SCHEMA jira_plan TO jiraadmin;
GRANT USAGE ON SCHEMA jira_admin TO jiraadmin;
GRANT USAGE ON SCHEMA jira_version TO jiraadmin;
GRANT USAGE ON SCHEMA jira_component TO jiraadmin;
