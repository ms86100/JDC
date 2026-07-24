-- Rollback V16: Development information tables
DROP TABLE IF EXISTS jira_issue.dev_info_builds CASCADE;
DROP TABLE IF EXISTS jira_issue.dev_info_pull_requests CASCADE;
DROP TABLE IF EXISTS jira_issue.dev_info_branches CASCADE;
DROP TABLE IF EXISTS jira_issue.dev_info_commits CASCADE;
