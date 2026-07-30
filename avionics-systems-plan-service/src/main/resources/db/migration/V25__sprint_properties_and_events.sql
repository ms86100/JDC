-- Sprint Properties (key-value store for arbitrary sprint metadata)
CREATE TABLE IF NOT EXISTS jira_plan.sprint_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID NOT NULL REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    property_key VARCHAR(255) NOT NULL,
    property_value JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sprint_id, property_key)
);

CREATE INDEX IF NOT EXISTS idx_sprint_properties_sprint ON jira_plan.sprint_properties(sprint_id);

-- Sprint Events (event-based burndown tracking)
CREATE TABLE IF NOT EXISTS jira_plan.sprint_events (
    id BIGSERIAL PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES jira_plan.sprints(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    plan_item_id UUID,
    old_value INTEGER,
    new_value INTEGER,
    points_delta INTEGER,
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID
);

CREATE INDEX IF NOT EXISTS idx_sprint_events_sprint ON jira_plan.sprint_events(sprint_id);
CREATE INDEX IF NOT EXISTS idx_sprint_events_timestamp ON jira_plan.sprint_events(event_timestamp);
