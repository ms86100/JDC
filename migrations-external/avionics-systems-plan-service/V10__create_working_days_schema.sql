-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;


-- Working Days configuration and holiday management
-- Foundation for capacity planning

-- Working days configuration (calendar template)
CREATE TABLE jira_plan.working_days_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,  -- e.g., "Standard Week", "4-Day Week", "24/7"
    monday BOOLEAN DEFAULT TRUE,
    tuesday BOOLEAN DEFAULT TRUE,
    wednesday BOOLEAN DEFAULT TRUE,
    thursday BOOLEAN DEFAULT TRUE,
    friday BOOLEAN DEFAULT TRUE,
    saturday BOOLEAN DEFAULT FALSE,
    sunday BOOLEAN DEFAULT FALSE,
    hours_per_day DECIMAL(4,2) DEFAULT 8.00,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default working days config
INSERT INTO jira_plan.working_days_config (id, name, monday, tuesday, wednesday, thursday, friday, saturday, sunday, hours_per_day, is_default)
VALUES
    (gen_random_uuid(), 'Standard 5-Day Week', TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, 8.00, TRUE),
    (gen_random_uuid(), '4-Day Work Week', TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, 8.00, FALSE);

-- Non-working days (holidays/vacations)
CREATE TABLE jira_plan.non_working_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id UUID REFERENCES jira_plan.working_days_config(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    name VARCHAR(255),  -- e.g., "Christmas", "Company Holiday", "Team Offsite"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(config_id, date)
);

-- Team availability calendar (time off, vacations, sick days)
CREATE TABLE jira_plan.team_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID REFERENCES jira_plan.plan_teams(id) ON DELETE CASCADE,
    user_id UUID,  -- NULL means applies to whole team
    date DATE NOT NULL,
    hours DECIMAL(4,2),  -- Override hours (0 = full day off, 4 = half day)
    reason VARCHAR(255),  -- e.g., "Vacation", "Conference", "Sick Leave", "Public Holiday"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(team_id, user_id, date)
);

-- Indexes
CREATE INDEX idx_non_working_days_config ON jira_plan.non_working_days(config_id);
CREATE INDEX idx_non_working_days_date ON jira_plan.non_working_days(date);
CREATE INDEX idx_team_availability_team ON jira_plan.team_availability(team_id);
CREATE INDEX idx_team_availability_user ON jira_plan.team_availability(user_id);
CREATE INDEX idx_team_availability_date ON jira_plan.team_availability(date);
CREATE INDEX idx_team_availability_range ON jira_plan.team_availability(team_id, date);