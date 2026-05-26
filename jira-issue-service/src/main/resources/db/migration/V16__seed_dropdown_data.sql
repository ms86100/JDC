-- V6__seed_dropdown_data.sql
-- Comprehensive seed data for all dropdown entities used in Create Issue modal
-- Run this migration to populate dropdown data for all services

-- ============================================
-- ISSUE TYPES (jira_issue.issue_types)
-- ============================================
INSERT INTO jira_issue.issue_types (id, name, icon, description) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Bug', 'bug', 'A bug report - something is not working as expected'),
    ('a0000000-0000-0000-0000-000000000002', 'Story', 'book', 'A user story - a feature from the end user perspective'),
    ('a0000000-0000-0000-0000-000000000003', 'Task', 'task', 'A task - work that needs to be done'),
    ('a0000000-0000-0000-0000-000000000004', 'Epic', 'lightning', 'An epic - a large feature that contains multiple stories'),
    ('a0000000-0000-0000-0000-000000000005', 'Subtask', 'subtask', 'A subtask - a smaller piece of work within a parent issue'),
    ('a0000000-0000-0000-0000-000000000006', 'Improvement', 'improvement', 'An improvement to an existing feature'),
    ('a0000000-0000-0000-0000-000000000007', 'New Feature', 'newfeature', 'A new feature request'),
    ('a0000000-0000-0000-0000-000000000008', 'Question', 'question', 'A question about the project or issues'),
    ('a0000000-0000-0000-0000-000000000009', 'Technical Task', 'technical', 'A technical task internal to the team')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- PRIORITIES (jira_issue.issue_priorities)
-- ============================================
INSERT INTO jira_issue.issue_priorities (id, name, icon, color, sequence) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'Highest', 'Highest', '#ff0000', 1),
    ('b0000000-0000-0000-0000-000000000002', 'High', 'High', '#ff6600', 2),
    ('b0000000-0000-0000-0000-000000000003', 'Medium', 'Medium', '#ffcc00', 3),
    ('b0000000-0000-0000-0000-000000000004', 'Low', 'Low', '#0099ff', 4),
    ('b0000000-0000-0000-0000-000000000005', 'Lowest', 'Lowest', '#99cc00', 5);
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- STATUSES (jira_issue.issue_statuses)
-- ============================================
INSERT INTO jira_issue.issue_statuses (id, name, sequence, category) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'To Do', 1, 'TODO'),
    ('c0000000-0000-0000-0000-000000000002', 'In Progress', 2, 'IN_PROGRESS'),
    ('c0000000-0000-0000-0000-000000000003', 'In Review', 3, 'IN_PROGRESS'),
    ('c0000000-0000-0000-0000-000000000004', 'Done', 4, 'DONE'),
    ('c0000000-0000-0000-0000-000000000005', 'Closed', 5, 'DONE'),
    ('c0000000-0000-0000-0000-000000000006', 'Backlog', 0, 'TODO'),
    ('c0000000-0000-0000-0000-000000000007', 'Open', 1, 'TODO'),
    ('c0000000-0000-0000-0000-000000000008', 'Resolved', 4, 'DONE'),
    ('c0000000-0000-0000-0000-000000000009', 'Reopened', 2, 'IN_PROGRESS');
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- ISSUE LINK TYPES (jira_issue.issue_link_types)
-- ============================================
INSERT INTO jira_issue.issue_link_types (id, name, inward, outward, sequence) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'blocks', 'is blocked by', 'blocks', 1),
    ('d0000000-0000-0000-0000-000000000002', 'relates to', 'relates to', 'relates to', 2),
    ('d0000000-0000-0000-0000-000000000003', 'duplicates', 'is duplicated by', 'duplicates', 3),
    ('d0000000-0000-0000-0000-000000000004', 'clones', 'is cloned by', 'clones', 4),
    ('d0000000-0000-0000-0000-000000000005', 'causes', 'is caused by', 'causes', 5),
    ('d0000000-0000-0000-0000-000000000006', 'is required by', 'is required by', 'requires', 6),
    ('d0000000-0000-0000-0000-000000000007', 'depends on', 'is depended on by', 'depends on', 7)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- RESOLUTIONS (jira_issue.resolutions)
-- ============================================
INSERT INTO jira_issue.resolutions (id, name, description, sort_order) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'Fixed', 'The issue has been fixed and verified', 1),
    ('e0000000-0000-0000-0000-000000000002', 'Won''t Fix', 'This issue will not be addressed', 2),
    ('e0000000-0000-0000-0000-000000000003', 'Duplicate', 'This issue is a duplicate of another', 3),
    ('e0000000-0000-0000-0000-000000000004', 'Incomplete', 'The issue cannot be completed as described', 4),
    ('e0000000-0000-0000-0000-000000000005', 'Cannot Reproduce', 'The issue cannot be reproduced', 5),
    ('e0000000-0000-0000-0000-000000000006', 'Done', 'Work is complete', 6)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- DEFAULT LABELS (jira_issue.labels)
-- ============================================
INSERT INTO jira_issue.labels (id, name, color, description, created_at) VALUES
    ('l0000000-0000-0000-0000-000000000001', 'bug', '#ff0000', 'Bug report label', NOW()),
    ('l0000000-0000-0000-0000-000000000002', 'enhancement', '#00cc00', 'Enhancement label', NOW()),
    ('l0000000-0000-0000-0000-000000000003', 'feature', '#0066ff', 'New feature label', NOW()),
    ('l0000000-0000-0000-0000-000000000004', 'documentation', '#660099', 'Documentation label', NOW()),
    ('l0000000-0000-0000-0000-000000000005', 'high-priority', '#ff6600', 'High priority label', NOW()),
    ('l0000000-0000-0000-0000-000000000006', 'low-priority', '#0099ff', 'Low priority label', NOW()),
    ('l0000000-0000-0000-0000-000000000007', 'frontend', '#ff0099', 'Frontend label', NOW()),
    ('l0000000-0000-0000-0000-000000000008', 'backend', '#9900ff', 'Backend label', NOW()),
    ('l0000000-0000-0000-0000-000000000009', 'security', '#ff0000', 'Security label', NOW()),
    ('l0000000-0000-0000-0000-000000000010', 'ui', '#00cccc', 'UI label', NOW()),
    ('l0000000-0000-0000-0000-000000000011', 'api', '#009999', 'API label', NOW()),
    ('l0000000-0000-0000-0000-000000000012', 'testing', '#00ff00', 'Testing label', NOW()),
    ('l0000000-0000-0000-0000-000000000013', 'devops', '#ff9900', 'DevOps label', NOW()),
    ('l0000000-0000-0000-0000-000000000014', 'urgent', '#ff0000', 'Urgent label', NOW()),
    ('l0000000-0000-0000-0000-000000000015', 'blocker', '#cc0000', 'Blocker label', NOW())
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- SECURITY LEVELS (jira_issue.security_levels)
-- ============================================
INSERT INTO jira_issue.security_levels (id, name, description, icon_url, sort_order, created_at) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'All Users', 'Anyone can see this issue', '/icons/security/public', 1, NOW()),
    ('f0000000-0000-0000-0000-000000000002', 'Project Members', 'Only project members can see this issue', '/icons/security/members', 2, NOW()),
    ('f0000000-0000-0000-0000-000000000003', 'Developers', 'Only developers can see this issue', '/icons/security/developers', 3, NOW()),
    ('f0000000-0000-0000-0000-000000000004', 'Managers', 'Only managers can see this issue', '/icons/security/managers', 4, NOW()),
    ('f0000000-0000-0000-0000-000000000005', 'Confidential', 'Highly confidential - very limited access', '/icons/security/confidential', 5, NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- VERSIONS (sample - jira_issue.project_versions)
-- ============================================
-- Note: Versions are project-specific, so they'll be created when projects are created
-- Sample version data structure for reference
/*
INSERT INTO jira_issue.project_versions (id, project_id, name, description, release_date, released, archived, created_at)
VALUES
    ('v0000000-0000-0000-0000-000000000001', '<project_id>', '1.0.0', 'Initial release', NOW() + INTERVAL '30 days', FALSE, FALSE, NOW()),
    ('v0000000-0000-0000-0000-000000000002', '<project_id>', '1.1.0', 'Feature release', NOW() + INTERVAL '60 days', FALSE, FALSE, NOW()),
    ('v0000000-0000-0000-0000-000000000003', '<project_id>', '2.0.0', 'Major release', NOW() + INTERVAL '90 days', FALSE, FALSE, NOW());
*/

-- ============================================
-- COMPONENTS (sample - jira_issue.project_components)
-- ============================================
-- Note: Components are project-specific, so they'll be created when projects are created
-- Sample component data structure for reference
/*
INSERT INTO jira_issue.project_components (id, project_id, name, description, lead_id, created_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', '<project_id>', 'Frontend', 'Frontend UI components', NULL, NOW()),
    ('c0000000-0000-0000-0000-000000000002', '<project_id>', 'Backend', 'Backend API services', NULL, NOW()),
    ('c0000000-0000-0000-0000-000000000003', '<project_id>', 'Database', 'Database and migration scripts', NULL, NOW());
*/

-- ============================================
-- PRINT CONFIRMATION
-- ============================================
DO $$
BEGIN
    RAISE NOTICE 'Seed data migration completed successfully!';
    RAISE NOTICE 'Issue Types: %', (SELECT COUNT(*) FROM jira_issue.issue_types);
    RAISE NOTICE 'Priorities: %', (SELECT COUNT(*) FROM jira_issue.issue_priorities);
    RAISE NOTICE 'Statuses: %', (SELECT COUNT(*) FROM jira_issue.issue_statuses);
    RAISE NOTICE 'Issue Link Types: %', (SELECT COUNT(*) FROM jira_issue.issue_link_types);
    RAISE NOTICE 'Resolutions: %', (SELECT COUNT(*) FROM jira_issue.resolutions);
    RAISE NOTICE 'Labels: %', (SELECT COUNT(*) FROM jira_issue.labels);
    RAISE NOTICE 'Security Levels: %', (SELECT COUNT(*) FROM jira_issue.security_levels);
END $$;