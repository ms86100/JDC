ALTER TABLE jira_plan.plan_items ADD COLUMN IF NOT EXISTS target_end_date DATE;

COMMENT ON COLUMN jira_plan.plan_items.target_end_date IS 'Planned end date for roadmap timeline (Advanced Roadmaps target end)';
