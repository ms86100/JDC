-- Standalone seed data SQL - Run this directly against the database
-- This populates all dropdown data for the Create Issue modal

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
    ('b0000000-0000-0000-0000-000000000005', 'Lowest', 'Lowest', '#99cc00', 5)
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
    ('c0000000-0000-0000-0000-000000000009', 'Reopened', 2, 'IN_PROGRESS')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- ISSUE LINK TYPES (jira_issue.issue_link_types)
-- ============================================
INSERT INTO jira_issue.issue_link_types (id, name, inward, outward, is_active) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'blocks', 'is blocked by', 'blocks', true),
    ('d0000000-0000-0000-0000-000000000002', 'relates to', 'relates to', 'relates to', true),
    ('d0000000-0000-0000-0000-000000000003', 'duplicates', 'is duplicated by', 'duplicates', true),
    ('d0000000-0000-0000-0000-000000000004', 'clones', 'is cloned by', 'clones', true),
    ('d0000000-0000-0000-0000-000000000005', 'causes', 'is caused by', 'causes', true),
    ('d0000000-0000-0000-0000-000000000006', 'requires', 'is required by', 'requires', true),
    ('d0000000-0000-0000-0000-000000000007', 'depends on', 'is depended on by', 'depends on', true)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- RESOLUTIONS (jira_issue.resolutions)
-- ============================================
INSERT INTO jira_issue.resolutions (id, name, description, sort_order, is_active) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'Fixed', 'The issue has been fixed and verified', 1, true),
    ('e0000000-0000-0000-0000-000000000002', 'Won''t Fix', 'This issue will not be addressed', 2, true),
    ('e0000000-0000-0000-0000-000000000003', 'Duplicate', 'This issue is a duplicate of another', 3, true),
    ('e0000000-0000-0000-0000-000000000004', 'Incomplete', 'The issue cannot be completed as described', 4, true),
    ('e0000000-0000-0000-0000-000000000005', 'Cannot Reproduce', 'The issue cannot be reproduced', 5, true),
    ('e0000000-0000-0000-0000-000000000006', 'Done', 'Work is complete', 6, true)
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- LABELS (jira_issue.labels)
-- ============================================
INSERT INTO jira_issue.labels (id, name, color, description) VALUES
    ('l0000000-0000-0000-0000-000000000001', 'bug', '#ff0000', 'Bug report label'),
    ('l0000000-0000-0000-0000-000000000002', 'enhancement', '#00cc00', 'Enhancement label'),
    ('l0000000-0000-0000-0000-000000000003', 'feature', '#0066ff', 'New feature label'),
    ('l0000000-0000-0000-0000-000000000004', 'documentation', '#660099', 'Documentation label'),
    ('l0000000-0000-0000-0000-000000000005', 'high-priority', '#ff6600', 'High priority label'),
    ('l0000000-0000-0000-0000-000000000006', 'low-priority', '#0099ff', 'Low priority label'),
    ('l0000000-0000-0000-0000-000000000007', 'frontend', '#ff0099', 'Frontend label'),
    ('l0000000-0000-0000-0000-000000000008', 'backend', '#9900ff', 'Backend label'),
    ('l0000000-0000-0000-0000-000000000009', 'security', '#ff0000', 'Security label'),
    ('l0000000-0000-0000-0000-000000000010', 'ui', '#00cccc', 'UI label'),
    ('l0000000-0000-0000-0000-000000000011', 'api', '#009999', 'API label'),
    ('l0000000-0000-0000-0000-000000000012', 'testing', '#00ff00', 'Testing label'),
    ('l0000000-0000-0000-0000-000000000013', 'devops', '#ff9900', 'DevOps label'),
    ('l0000000-0000-0000-0000-000000000014', 'urgent', '#ff0000', 'Urgent label'),
    ('l0000000-0000-0000-0000-000000000015', 'blocker', '#cc0000', 'Blocker label')
ON CONFLICT (name) DO NOTHING;

-- ============================================
-- SECURITY LEVELS (jira_issue.security_levels)
-- ============================================
INSERT INTO jira_issue.security_levels (id, name, description, icon_url, sort_order) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'All Users', 'Anyone can see this issue', '/icons/security/public', 1),
    ('f0000000-0000-0000-0000-000000000002', 'Project Members', 'Only project members can see this issue', '/icons/security/members', 2),
    ('f0000000-0000-0000-0000-000000000003', 'Developers', 'Only developers can see this issue', '/icons/security/developers', 3),
    ('f0000000-0000-0000-0000-000000000004', 'Managers', 'Only managers can see this issue', '/icons/security/managers', 4),
    ('f0000000-0000-0000-0000-000000000005', 'Confidential', 'Highly confidential - very limited access', '/icons/security/confidential', 5)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- PRINT SUMMARY
-- ============================================
SELECT 'Seed Data Summary:' as info;
SELECT 'Issue Types: ' || COUNT(*)::text as count FROM jira_issue.issue_types;
SELECT 'Priorities: ' || COUNT(*)::text as count FROM jira_issue.issue_priorities;
SELECT 'Statuses: ' || COUNT(*)::text as count FROM jira_issue.issue_statuses;
SELECT 'Issue Link Types: ' || COUNT(*)::text as count FROM jira_issue.issue_link_types;
SELECT 'Resolutions: ' || COUNT(*)::text as count FROM jira_issue.resolutions;
SELECT 'Labels: ' || COUNT(*)::text as count FROM jira_issue.labels;
SELECT 'Security Levels: ' || COUNT(*)::text as count FROM jira_issue.security_levels;