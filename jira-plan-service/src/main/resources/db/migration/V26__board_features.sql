-- Board feature toggles for sprint, backlog, estimation, and parallel sprints
ALTER TABLE jira_plan.board_configs ADD COLUMN IF NOT EXISTS feature_sprints BOOLEAN DEFAULT true;
ALTER TABLE jira_plan.board_configs ADD COLUMN IF NOT EXISTS feature_backlog BOOLEAN DEFAULT true;
ALTER TABLE jira_plan.board_configs ADD COLUMN IF NOT EXISTS feature_estimation BOOLEAN DEFAULT true;
ALTER TABLE jira_plan.board_configs ADD COLUMN IF NOT EXISTS feature_parallel_sprints BOOLEAN DEFAULT false;
