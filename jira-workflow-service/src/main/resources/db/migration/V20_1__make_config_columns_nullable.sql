-- V22.1: Make condition_config and validator_config nullable
-- V14 added split columns (field_name, operator, value, etc.) as alternatives
-- to the original JSONB config columns. New seed data uses the split columns,
-- so the original JSONB columns must be nullable.

ALTER TABLE jira_workflow.workflow_conditions
  ALTER COLUMN condition_config DROP NOT NULL;

ALTER TABLE jira_workflow.workflow_validators
  ALTER COLUMN validator_config DROP NOT NULL;
