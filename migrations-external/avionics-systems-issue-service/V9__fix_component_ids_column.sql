-- Fix missing column for component_ids in issues table
-- This column is required by IssueService to store component references

ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS component_ids UUID[];