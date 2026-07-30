-- Enable UUID extension for all microservices
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schemas used by existing Flyway migrations.
-- Schema names remain as jira_* internally (an implementation detail invisible to users).
CREATE SCHEMA IF NOT EXISTS jira_auth;
CREATE SCHEMA IF NOT EXISTS jira_user;
CREATE SCHEMA IF NOT EXISTS jira_project;
CREATE SCHEMA IF NOT EXISTS jira_issue;
CREATE SCHEMA IF NOT EXISTS jira_workflow;
CREATE SCHEMA IF NOT EXISTS jira_comment;
CREATE SCHEMA IF NOT EXISTS jira_notification;
CREATE SCHEMA IF NOT EXISTS jira_search;
CREATE SCHEMA IF NOT EXISTS jira_audit;
CREATE SCHEMA IF NOT EXISTS jira_attachment;
CREATE SCHEMA IF NOT EXISTS jira_sprint;
CREATE SCHEMA IF NOT EXISTS jira_plan;
CREATE SCHEMA IF NOT EXISTS jira_admin;
CREATE SCHEMA IF NOT EXISTS jira_migration;
CREATE SCHEMA IF NOT EXISTS jira_version;
CREATE SCHEMA IF NOT EXISTS jira_component;
CREATE SCHEMA IF NOT EXISTS jira_test;
CREATE SCHEMA IF NOT EXISTS jira_dashboard;
CREATE SCHEMA IF NOT EXISTS jira_document;
CREATE SCHEMA IF NOT EXISTS jira_portal;
CREATE SCHEMA IF NOT EXISTS jira_report;
