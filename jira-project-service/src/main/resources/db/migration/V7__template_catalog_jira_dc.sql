-- V7: Jira Data Center-style template catalog
-- Adds template categories, capabilities, enriched metadata, and unified scheme mappings

-- ============================================
-- TEMPLATE CATEGORIES
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    icon_emoji VARCHAR(10),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO jira_project.template_categories (id, category_key, name, description, icon, icon_emoji, sort_order)
VALUES
    ('00000000-0000-0000-00a1-000000000001', 'PROJECT_MANAGEMENT',
     'Project Management',
     'Plan, track, and report on all of your work within a project.',
     'project-management', '📊', 1),
    ('00000000-0000-0000-00a1-000000000002', 'SOFTWARE_DEVELOPMENT',
     'Software Development',
     'Build, ship, and maintain software with agile and defect-tracking workflows.',
     'software-development', '💻', 2),
    ('00000000-0000-0000-00a1-000000000003', 'PROCESS_MANAGEMENT',
     'Process Management',
     'Track work activity through structured transitions and approvals.',
     'process-management', '⚙️', 3)
ON CONFLICT (category_key) DO NOTHING;

-- ============================================
-- TEMPLATE CAPABILITIES (enabled modules/features)
-- ============================================
CREATE TABLE IF NOT EXISTS jira_project.template_capabilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES jira_project.project_templates(id) ON DELETE CASCADE,
    capability_key VARCHAR(80) NOT NULL,
    capability_label VARCHAR(120) NOT NULL,
    capability_group VARCHAR(50) DEFAULT 'MODULE',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(template_id, capability_key)
);

CREATE INDEX IF NOT EXISTS idx_template_capabilities_template
ON jira_project.template_capabilities(template_id);

-- ============================================
-- ENHANCE PROJECT TEMPLATES
-- ============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'category_id') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN category_id UUID REFERENCES jira_project.template_categories(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'workflow_type') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN workflow_type VARCHAR(50);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'short_description') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN short_description VARCHAR(255);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'icon_emoji') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN icon_emoji VARCHAR(10);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'is_recommended') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN is_recommended BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'use_cases') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN use_cases TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'jira_project' AND table_name = 'project_templates'
                   AND column_name = 'preview_accent') THEN
        ALTER TABLE jira_project.project_templates
        ADD COLUMN preview_accent VARCHAR(7);
    END IF;
END $$;

-- ============================================
-- ASSIGN CATEGORIES & METADATA TO TEMPLATES
-- ============================================
UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000001',
    workflow_type = 'AGILE_SCRUM',
    template_type = 'AGILE',
    icon_emoji = '🏃',
    short_description = 'Agile software development with sprints',
    is_recommended = TRUE,
    use_cases = 'Software teams running two-week sprints with backlog grooming and release planning.',
    preview_accent = '#0066FF',
    description = 'Agile software development with sprints. Includes sprint planning, backlog, velocity tracking, story points, and scrum reports.',
    instructions = 'Use Scrum when your team plans work in time-boxed sprints and needs backlog, sprint board, and burndown reporting.'
WHERE id = '00000000-0000-0000-0002-000000000001';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000001',
    workflow_type = 'AGILE_KANBAN',
    template_type = 'AGILE',
    icon_emoji = '📋',
    short_description = 'Visual workflow with continuous delivery',
    is_recommended = TRUE,
    use_cases = 'Teams optimizing flow, WIP limits, and cycle time across a continuous board.',
    preview_accent = '#FF9200',
    description = 'Visual workflow with continuous delivery. Includes WIP limits, continuous flow, board-based tracking, and cycle time analytics.',
    instructions = 'Use Kanban when work flows continuously without fixed sprints and you need WIP and cycle-time visibility.'
WHERE id = '00000000-0000-0000-0002-000000000002';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000002',
    workflow_type = 'DEFECT_TRACKING',
    template_type = 'SUPPORT',
    icon_emoji = '🐛',
    short_description = 'Track and manage software bugs',
    is_recommended = FALSE,
    use_cases = 'QA and engineering teams managing defect lifecycle, severity, and reproducibility.',
    preview_accent = '#DC3545',
    description = 'Track and manage software bugs. Includes defect lifecycle, severity/priority, QA workflows, and reproducibility tracking.',
    instructions = 'Use Bug Tracking when the primary work item is defects with QA states and resolution analytics.'
WHERE id = '00000000-0000-0000-0002-000000000003';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000001',
    workflow_type = 'TASK',
    template_type = 'MANAGEMENT',
    icon_emoji = '✓',
    short_description = 'Manage tasks and action items',
    is_recommended = FALSE,
    use_cases = 'Cross-functional teams tracking assignees, due dates, and lightweight task collaboration.',
    preview_accent = '#28A745',
    description = 'Manage tasks and action items. Lightweight workflow with assignee tracking, due dates, and team collaboration.',
    instructions = 'Use Task Management for simple to-do style work without agile ceremonies.'
WHERE id = '00000000-0000-0000-0002-000000000004';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000001',
    workflow_type = 'PORTFOLIO',
    template_type = 'MANAGEMENT',
    icon_emoji = '📊',
    short_description = 'Track multiple projects and initiatives',
    is_recommended = FALSE,
    use_cases = 'PMOs and program managers tracking initiatives, dependencies, and cross-project roadmaps.',
    preview_accent = '#6C757D',
    description = 'Track multiple projects and initiatives. Cross-project roadmap, dependencies, initiative tracking, and reporting dashboards.',
    instructions = 'Use Portfolio when you need epics spanning teams and executive-level reporting.'
WHERE id = '00000000-0000-0000-0002-000000000005';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000001',
    workflow_type = 'TEAM_MANAGED',
    template_type = 'BASIC',
    icon_emoji = '👥',
    short_description = 'Simple project for autonomous teams',
    is_recommended = FALSE,
    use_cases = 'Small teams wanting a lightweight, team-managed space with minimal configuration.',
    preview_accent = '#17A2B8',
    description = 'Lightweight next-gen project for autonomous teams with simplified boards and permissions.',
    instructions = 'Use Basic for team-managed projects that do not require full scheme administration.'
WHERE id = '00000000-0000-0000-0002-000000000006';

UPDATE jira_project.project_templates SET
    category_id = '00000000-0000-0000-00a1-000000000003',
    workflow_type = 'PROCESS',
    template_type = 'PROCESS',
    icon_emoji = '⚙️',
    short_description = 'Structured transitions and approvals',
    is_recommended = FALSE,
    use_cases = 'Operations, finance, and compliance teams with approval gates and SLA-driven lifecycles.',
    preview_accent = '#6554C0',
    description = 'Track work activity through structured transitions. Approval workflows, SLA tracking, status-driven lifecycle, and operational workflows.',
    instructions = 'Use Process Management when items move through review and approval states before completion.'
WHERE id = '00000000-0000-0000-0002-000000000009';

-- Deactivate duplicate V4 business templates that overlap V2 catalog
UPDATE jira_project.project_templates SET is_active = FALSE
WHERE id IN ('00000000-0000-0000-0002-000000000007', '00000000-0000-0000-0002-000000000008');

-- ============================================
-- SEED CAPABILITIES PER TEMPLATE
-- ============================================
INSERT INTO jira_project.template_capabilities (template_id, capability_key, capability_label, capability_group, sort_order)
VALUES
-- Scrum
('00000000-0000-0000-0002-000000000001', 'backlog', 'Backlog', 'MODULE', 1),
('00000000-0000-0000-0002-000000000001', 'sprint_planning', 'Sprint Planning', 'MODULE', 2),
('00000000-0000-0000-0002-000000000001', 'velocity', 'Velocity Tracking', 'REPORT', 3),
('00000000-0000-0000-0002-000000000001', 'story_points', 'Story Points', 'FIELD', 4),
('00000000-0000-0000-0002-000000000001', 'burndown', 'Burndown Charts', 'REPORT', 5),
('00000000-0000-0000-0002-000000000001', 'scrum_reports', 'Scrum Reports', 'REPORT', 6),
('00000000-0000-0000-0002-000000000001', 'epic_management', 'Epic Management', 'MODULE', 7),
-- Kanban
('00000000-0000-0000-0002-000000000002', 'kanban_board', 'Kanban Board', 'MODULE', 1),
('00000000-0000-0000-0002-000000000002', 'wip_limits', 'WIP Limits', 'BOARD', 2),
('00000000-0000-0000-0002-000000000002', 'swimlanes', 'Swimlanes', 'BOARD', 3),
('00000000-0000-0000-0002-000000000002', 'cycle_time', 'Cycle Time Metrics', 'REPORT', 4),
('00000000-0000-0000-0002-000000000002', 'flow_reports', 'Continuous Flow Reports', 'REPORT', 5),
-- Bug Tracking
('00000000-0000-0000-0002-000000000003', 'defect_lifecycle', 'Defect Lifecycle Workflow', 'WORKFLOW', 1),
('00000000-0000-0000-0002-000000000003', 'severity_priority', 'Severity / Priority Fields', 'FIELD', 2),
('00000000-0000-0000-0002-000000000003', 'qa_states', 'QA States', 'WORKFLOW', 3),
('00000000-0000-0000-0002-000000000003', 'reproducibility', 'Reproducibility Tracking', 'FIELD', 4),
('00000000-0000-0000-0002-000000000003', 'resolution_analytics', 'Resolution Analytics', 'REPORT', 5),
-- Task Management
('00000000-0000-0000-0002-000000000004', 'lightweight_workflow', 'Lightweight Workflow', 'WORKFLOW', 1),
('00000000-0000-0000-0002-000000000004', 'assignee_tracking', 'Assignee Tracking', 'FIELD', 2),
('00000000-0000-0000-0002-000000000004', 'due_dates', 'Due Dates', 'FIELD', 3),
('00000000-0000-0000-0002-000000000004', 'team_collaboration', 'Team Collaboration', 'MODULE', 4),
-- Portfolio
('00000000-0000-0000-0002-000000000005', 'cross_project_roadmap', 'Cross-Project Roadmap', 'MODULE', 1),
('00000000-0000-0000-0002-000000000005', 'dependencies', 'Dependencies', 'MODULE', 2),
('00000000-0000-0000-0002-000000000005', 'initiative_tracking', 'Initiative Tracking', 'MODULE', 3),
('00000000-0000-0000-0002-000000000005', 'reporting_dashboards', 'Reporting Dashboards', 'REPORT', 4),
-- Process Management
('00000000-0000-0000-0002-000000000009', 'approval_workflows', 'Approval Workflows', 'WORKFLOW', 1),
('00000000-0000-0000-0002-000000000009', 'sla_tracking', 'SLA Tracking', 'AUTOMATION', 2),
('00000000-0000-0000-0002-000000000009', 'status_lifecycle', 'Status-Driven Lifecycle', 'WORKFLOW', 3),
('00000000-0000-0000-0002-000000000009', 'operational_workflows', 'Operational Workflows', 'WORKFLOW', 4)
ON CONFLICT (template_id, capability_key) DO NOTHING;

-- ============================================
-- SCRUM / KANBAN / BUG WORKFLOW VISUALIZATION SEEDS
-- ============================================
INSERT INTO jira_project.template_workflow_statuses
(id, template_id, status_name, status_key, status_color, status_category, sequence, description, icon)
VALUES
('00000000-0000-0000-0008-000000000020', '00000000-0000-0000-0002-000000000001', 'Backlog', 'BACKLOG', '#6B778C', 'TODO', 0, 'Unprioritized work', 'backlog'),
('00000000-0000-0000-0008-000000000021', '00000000-0000-0000-0002-000000000001', 'To Do', 'TODO', '#6B778C', 'TODO', 1, 'Ready for sprint', 'todo'),
('00000000-0000-0000-0008-000000000022', '00000000-0000-0000-0002-000000000001', 'In Progress', 'IN_PROGRESS', '#0052CC', 'IN_PROGRESS', 2, 'Active sprint work', 'progress'),
('00000000-0000-0000-0008-000000000023', '00000000-0000-0000-0002-000000000001', 'In Review', 'IN_REVIEW', '#00B8D9', 'IN_PROGRESS', 3, 'Awaiting review', 'review'),
('00000000-0000-0000-0008-000000000024', '00000000-0000-0000-0002-000000000001', 'Done', 'DONE', '#00875A', 'DONE', 4, 'Sprint complete', 'done')
ON CONFLICT DO NOTHING;

INSERT INTO jira_project.template_issue_types
(id, template_id, issue_type_name, issue_type_icon, is_default, is_subtask, sequence)
VALUES
('00000000-0000-0000-0010-000000000010', '00000000-0000-0000-0002-000000000001', 'Epic', 'epic', TRUE, FALSE, 0),
('00000000-0000-0000-0010-000000000011', '00000000-0000-0000-0002-000000000001', 'Story', 'story', FALSE, FALSE, 1),
('00000000-0000-0000-0010-000000000012', '00000000-0000-0000-0002-000000000001', 'Task', 'task', FALSE, FALSE, 2),
('00000000-0000-0000-0010-000000000013', '00000000-0000-0000-0002-000000000001', 'Bug', 'bug', FALSE, FALSE, 3),
('00000000-0000-0000-0010-000000000014', '00000000-0000-0000-0002-000000000001', 'Sub-task', 'subtask', FALSE, TRUE, 4)
ON CONFLICT DO NOTHING;

-- ============================================
-- UNIFIED TEMPLATE SCHEME MAPPINGS (from template_scheme_defaults)
-- ============================================
-- Distinct rows only (avoids duplicate-key within single INSERT for shared scheme names)
INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, scheme_id, is_default)
SELECT DISTINCT ON (tsd.template_id, its.name)
    tsd.template_id, 'ISSUE_TYPE', its.name, tsd.issue_type_scheme_id, TRUE
FROM jira_project.template_scheme_defaults tsd
JOIN jira_project.issue_type_schemes its ON its.id = tsd.issue_type_scheme_id
WHERE tsd.issue_type_scheme_id IS NOT NULL
ORDER BY tsd.template_id, its.name
ON CONFLICT (template_id, scheme_type, scheme_name) DO UPDATE SET scheme_id = EXCLUDED.scheme_id;

INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, scheme_id, is_default)
SELECT DISTINCT ON (tsd.template_id, ws.name)
    tsd.template_id, 'WORKFLOW', ws.name, tsd.workflow_scheme_id, TRUE
FROM jira_project.template_scheme_defaults tsd
JOIN jira_project.workflow_schemes ws ON ws.id = tsd.workflow_scheme_id
WHERE tsd.workflow_scheme_id IS NOT NULL
ORDER BY tsd.template_id, ws.name
ON CONFLICT (template_id, scheme_type, scheme_name) DO UPDATE SET scheme_id = EXCLUDED.scheme_id;

INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, scheme_id, is_default)
SELECT DISTINCT ON (tsd.template_id, ps.name)
    tsd.template_id, 'PERMISSION', ps.name, tsd.permission_scheme_id, TRUE
FROM jira_project.template_scheme_defaults tsd
JOIN jira_project.permission_schemes ps ON ps.id = tsd.permission_scheme_id
WHERE tsd.permission_scheme_id IS NOT NULL
ORDER BY tsd.template_id, ps.name
ON CONFLICT (template_id, scheme_type, scheme_name) DO UPDATE SET scheme_id = EXCLUDED.scheme_id;

INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, scheme_id, is_default)
SELECT DISTINCT ON (tsd.template_id, ns.name)
    tsd.template_id, 'NOTIFICATION', ns.name, tsd.notification_scheme_id, TRUE
FROM jira_project.template_scheme_defaults tsd
JOIN jira_project.notification_schemes ns ON ns.id = tsd.notification_scheme_id
WHERE tsd.notification_scheme_id IS NOT NULL
ORDER BY tsd.template_id, ns.name
ON CONFLICT (template_id, scheme_type, scheme_name) DO UPDATE SET scheme_id = EXCLUDED.scheme_id;

INSERT INTO jira_project.template_scheme_mappings (template_id, scheme_type, scheme_name, scheme_id, is_default)
SELECT DISTINCT ON (tsd.template_id, ss.name)
    tsd.template_id, 'SCREEN', ss.name, tsd.screen_scheme_id, TRUE
FROM jira_project.template_scheme_defaults tsd
JOIN jira_project.screen_schemes ss ON ss.id = tsd.screen_scheme_id
WHERE tsd.screen_scheme_id IS NOT NULL
ORDER BY tsd.template_id, ss.name
ON CONFLICT (template_id, scheme_type, scheme_name) DO UPDATE SET scheme_id = EXCLUDED.scheme_id;
