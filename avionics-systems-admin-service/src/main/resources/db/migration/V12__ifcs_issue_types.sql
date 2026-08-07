-- V12__ifcs_issue_types.sql
-- Seed IFCS issue types and link types for VVM Card, IVV Card, Group, and Sub-Change.

-- ============================================
-- 1. ISSUE TYPES
-- ============================================

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color) VALUES
    ('VVM Card',   'vvm-card',    'vvm-card',    'V&V Management strategy card',           false, 30, '#6554C0'),
    ('IVV Card',   'ivv-card',    'ivv-card',    'Formal validation/verification item',    false, 31, '#FF5630'),
    ('Group',      'group',       'group',       'Aircraft functionality grouping',         false, 32, '#253858'),
    ('Sub-Change', 'sub-change',  'sub-change',  'Detailed change card breakdown',          false, 33, '#00B8D9')
ON CONFLICT DO NOTHING;

-- ============================================
-- 2. ISSUE LINK TYPES
-- ============================================

INSERT INTO jira_issue.issue_link_types (name, inward, outward, style, sequence) VALUES
    ('originates from', 'originated by',  'originates from', 'origin',  14),
    ('covers',          'covered by',     'covers',          'cover',   15)
ON CONFLICT DO NOTHING;
