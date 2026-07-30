-- V19__add_wip_limit_to_sprints.sql
-- Add WIP limit column to sprints table for sprint capacity management

ALTER TABLE jira_plan.sprints ADD COLUMN wip_limit INTEGER;

COMMENT ON COLUMN jira_plan.sprints.wip_limit IS 'Work-in-progress limit for the sprint';