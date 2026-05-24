-- V2__automation.sql - Phase 5: Notifications & Automation schema
-- Schema: jira_notification

-- Notification Schemes table
CREATE TABLE jira_notification.notification_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    project_id UUID,
    created_by UUID NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_schemes_project_id ON jira_notification.notification_schemes(project_id);
CREATE INDEX idx_notification_schemes_created_by ON jira_notification.notification_schemes(created_by);

-- Notification Scheme Events table
CREATE TABLE jira_notification.notification_scheme_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    recipient_type VARCHAR(50) NOT NULL,
    recipient_id UUID,
    recipient_group VARCHAR(255),
    notification_template_id UUID,
    enabled BOOLEAN DEFAULT TRUE,
    notify_assignee BOOLEAN DEFAULT FALSE,
    notify_reporter BOOLEAN DEFAULT FALSE,
    notify_watchers BOOLEAN DEFAULT FALSE,
    notify_voters BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scheme_events_scheme FOREIGN KEY (scheme_id) REFERENCES jira_notification.notification_schemes(id) ON DELETE CASCADE
);

CREATE INDEX idx_scheme_events_scheme_id ON jira_notification.notification_scheme_events(scheme_id);
CREATE INDEX idx_scheme_events_event_type ON jira_notification.notification_scheme_events(event_type);
CREATE UNIQUE INDEX idx_scheme_events_unique ON jira_notification.notification_scheme_events(scheme_id, event_type, recipient_type);

-- Notification Events table
CREATE TABLE jira_notification.notification_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    category VARCHAR(50),
    icon_url VARCHAR(500),
    is_system_event BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_events_category ON jira_notification.notification_events(category);
CREATE INDEX idx_notification_events_enabled ON jira_notification.notification_events(enabled);

-- Email Templates table
CREATE TABLE jira_notification.email_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    template_type VARCHAR(50) DEFAULT 'THYMELEAF',
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_templates_event_type ON jira_notification.email_templates(event_type);
CREATE INDEX idx_email_templates_enabled ON jira_notification.email_templates(enabled);
CREATE INDEX idx_email_templates_created_by ON jira_notification.email_templates(created_by);

-- Automation Rules table
CREATE TABLE jira_notification.automation_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_id UUID,
    created_by UUID NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    is_system_rule BOOLEAN DEFAULT FALSE,
    execution_count INTEGER DEFAULT 0,
    last_executed_at TIMESTAMP WITH TIME ZONE,
    last_status VARCHAR(50),
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_automation_rules_project_id ON jira_notification.automation_rules(project_id);
CREATE INDEX idx_automation_rules_created_by ON jira_notification.automation_rules(created_by);
CREATE INDEX idx_automation_rules_enabled ON jira_notification.automation_rules(enabled);
CREATE INDEX idx_automation_rules_trigger_type ON jira_notification.automation_rules(trigger_type);

-- Automation Triggers table
CREATE TABLE jira_notification.automation_triggers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    trigger_config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_triggers_rule FOREIGN KEY (rule_id) REFERENCES jira_notification.automation_rules(id) ON DELETE CASCADE
);

CREATE INDEX idx_automation_triggers_rule_id ON jira_notification.automation_triggers(rule_id);
CREATE INDEX idx_automation_triggers_trigger_type ON jira_notification.automation_triggers(trigger_type);

-- Automation Conditions table
CREATE TABLE jira_notification.automation_conditions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID NOT NULL,
    condition_type VARCHAR(100) NOT NULL,
    field_name VARCHAR(255),
    operator VARCHAR(50),
    condition_value VARCHAR,
    condition_config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    logical_group VARCHAR(50) DEFAULT 'ALL',
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conditions_rule FOREIGN KEY (rule_id) REFERENCES jira_notification.automation_rules(id) ON DELETE CASCADE
);

CREATE INDEX idx_automation_conditions_rule_id ON jira_notification.automation_conditions(rule_id);

-- Automation Actions table
CREATE TABLE jira_notification.automation_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    action_config TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    order_index INTEGER DEFAULT 0,
    failure_handling VARCHAR(50) DEFAULT 'CONTINUE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_actions_rule FOREIGN KEY (rule_id) REFERENCES jira_notification.automation_rules(id) ON DELETE CASCADE
);

CREATE INDEX idx_automation_actions_rule_id ON jira_notification.automation_actions(rule_id);
CREATE INDEX idx_automation_actions_action_type ON jira_notification.automation_actions(action_type);

-- Automation Logs table
CREATE TABLE jira_notification.automation_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id UUID NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    trigger_event_id UUID,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    conditions_evaluated INTEGER,
    conditions_passed INTEGER,
    actions_executed INTEGER,
    actions_failed INTEGER,
    execution_time_ms BIGINT,
    error_details TEXT,
    context_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_rule FOREIGN KEY (rule_id) REFERENCES jira_notification.automation_rules(id) ON DELETE CASCADE
);

CREATE INDEX idx_automation_logs_rule_id ON jira_notification.automation_logs(rule_id);
CREATE INDEX idx_automation_logs_status ON jira_notification.automation_logs(status);
CREATE INDEX idx_automation_logs_created_at ON jira_notification.automation_logs(created_at DESC);

-- Insert default notification events
INSERT INTO jira_notification.notification_events (event_type, name, description, category, is_system_event) VALUES
('ISSUE_CREATED', 'Issue Created', 'Triggered when a new issue is created', 'Issue', true),
('ISSUE_UPDATED', 'Issue Updated', 'Triggered when an issue is updated', 'Issue', true),
('ISSUE_DELETED', 'Issue Deleted', 'Triggered when an issue is deleted', 'Issue', true),
('ISSUE_ASSIGNED', 'Issue Assigned', 'Triggered when an issue is assigned to a user', 'Issue', true),
('ISSUE_COMMENTED', 'Issue Commented', 'Triggered when a comment is added to an issue', 'Comment', true),
('ISSUE_RESOLVED', 'Issue Resolved', 'Triggered when an issue is resolved', 'Status', true),
('ISSUE_CLOSED', 'Issue Closed', 'Triggered when an issue is closed', 'Status', true),
('ISSUE_REOPENED', 'Issue Reopened', 'Triggered when an issue is reopened', 'Status', true),
('SPRINT_STARTED', 'Sprint Started', 'Triggered when a sprint begins', 'Sprint', true),
('SPRINT_COMPLETED', 'Sprint Completed', 'Triggered when a sprint ends', 'Sprint', true),
('SPRINT_CANCELLED', 'Sprint Cancelled', 'Triggered when a sprint is cancelled', 'Sprint', true),
('PROJECT_CREATED', 'Project Created', 'Triggered when a new project is created', 'Project', true);

-- Insert default email templates
INSERT INTO jira_notification.email_templates (template_key, name, description, subject_template, body_template, event_type, is_default) VALUES
('issue-assigned', 'Issue Assigned Email', 'Email template for issue assignment notifications', 'You have been assigned to issue: [[${issueKey}]]', '<html><body><h1>Issue Assigned</h1><p>You have been assigned to issue [[${issueKey}]] - [[${title}]] in project [[${projectKey}]].</p><p>Reporter: [[${reporter}]]</p></body></html>', 'ISSUE_ASSIGNED', true),
('issue-commented', 'Issue Commented Email', 'Email template for issue comment notifications', 'New comment on issue: [[${issueKey}]]', '<html><body><h1>New Comment</h1><p>[[${commenter}]] commented on issue [[${issueKey}]].</p><blockquote>[[${commentPreview}]]</blockquote></body></html>', 'ISSUE_COMMENTED', true),
('sprint-started', 'Sprint Started Email', 'Email template for sprint start notifications', 'Sprint Started: [[${sprintName}]]', '<html><body><h1>Sprint Started</h1><p>Sprint [[${sprintName}]] has begun!</p><p>Duration: [[${startDate}]] to [[${endDate}]]</p><p>Goal: [[${goal}]]</p></body></html>', 'SPRINT_STARTED', true),
('sprint-completed', 'Sprint Completed Email', 'Email template for sprint completion notifications', 'Sprint Completed: [[${sprintName}]]', '<html><body><h1>Sprint Completed</h1><p>Sprint [[${sprintName}]] has been completed!</p><p>Completed Issues: [[${completedIssues}]]</p><p>Total Points: [[${totalPoints}]]</p><p>Velocity: [[${velocity}]]</p></body></html>', 'SPRINT_COMPLETED', true);