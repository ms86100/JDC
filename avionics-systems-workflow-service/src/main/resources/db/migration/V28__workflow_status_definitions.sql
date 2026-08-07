-- V28__workflow_status_definitions.sql
-- Creates a local status definitions table so the workflow-service can resolve
-- custom status UUIDs (b0000001-* through b0000008-*) to display names without
-- depending on upstream issue-service or admin-service catalogs.

CREATE TABLE IF NOT EXISTS jira_workflow.workflow_status_definitions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status_id  UUID NOT NULL UNIQUE,
    name       VARCHAR(200) NOT NULL,
    category   VARCHAR(50),
    color      VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wsd_status_id
    ON jira_workflow.workflow_status_definitions(status_id);

-- ============================================================================
-- VVO Workflow (b0000001-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000001-0000-0000-0000-000000000001', 'New',             'TODO'),
    ('b0000001-0000-0000-0000-000000000002', 'To be verified',  'IN_PROGRESS'),
    ('b0000001-0000-0000-0000-000000000003', 'Verified',        'IN_PROGRESS'),
    ('b0000001-0000-0000-0000-000000000004', 'Released',        'DONE'),
    ('b0000001-0000-0000-0000-000000000005', 'Cancelled',       'DONE'),
    ('b0000001-0000-0000-0000-000000000006', 'Superseded',      'DONE')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- HLVVO Workflow (b0000002-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000002-0000-0000-0000-000000000001', 'New',                      'TODO'),
    ('b0000002-0000-0000-0000-000000000002', 'Plan',                     'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000003', 'VVO Writing in Progress',  'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000004', 'Supplier in Review',       'IN_PROGRESS'),
    ('b0000002-0000-0000-0000-000000000005', 'Authorize',                'DONE')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- Change Card Workflow (b0000003-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000003-0000-0000-0000-000000000001', 'In Analysis',           'IN_PROGRESS'),
    ('b0000003-0000-0000-0000-000000000002', 'In Progress',           'IN_PROGRESS'),
    ('b0000003-0000-0000-0000-000000000003', 'Closed',                'DONE'),
    ('b0000003-0000-0000-0000-000000000004', 'No Change',             'DONE'),
    ('b0000003-0000-0000-0000-000000000005', 'Temporary Acceptance',  'IN_PROGRESS')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- TechEvent Workflow (b0000004-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000004-0000-0000-0000-000000000001', 'Open',                       'TODO'),
    ('b0000004-0000-0000-0000-000000000002', 'Under Originator Analysis',  'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000003', 'Under Resolver Analysis',    'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000004', 'Under Test Mean Analysis',   'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000005', 'Ready for Review',           'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000006', 'Classified',                 'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000007', 'To be Assessed',             'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-000000000008', 'Resolved Corrected',         'DONE'),
    ('b0000004-0000-0000-0000-000000000009', 'Resolved Contained',         'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-00000000000a', 'Proposed for Cancellation',  'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-00000000000b', 'Cancelled',                  'DONE'),
    ('b0000004-0000-0000-0000-00000000000c', 'Closed',                     'DONE'),
    ('b0000004-0000-0000-0000-00000000000d', 'To be Refined',              'IN_PROGRESS'),
    ('b0000004-0000-0000-0000-00000000000e', 'Unresolved',                 'IN_PROGRESS')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- Problem Report Workflow (b0000005-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000005-0000-0000-0000-000000000001', 'Open',            'TODO'),
    ('b0000005-0000-0000-0000-000000000002', 'Under Analysis',  'IN_PROGRESS'),
    ('b0000005-0000-0000-0000-000000000003', 'Closed',          'DONE'),
    ('b0000005-0000-0000-0000-000000000004', 'Rejected',        'DONE')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- Bench Defect Workflow (b0000006-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000006-0000-0000-0000-000000000001', 'Open',              'TODO'),
    ('b0000006-0000-0000-0000-000000000002', 'Under Analysis',    'IN_PROGRESS'),
    ('b0000006-0000-0000-0000-000000000003', 'To be Corrected',   'IN_PROGRESS'),
    ('b0000006-0000-0000-0000-000000000004', 'Corrected',         'IN_PROGRESS'),
    ('b0000006-0000-0000-0000-000000000005', 'Closed',            'DONE'),
    ('b0000006-0000-0000-0000-000000000006', 'Cancelled',         'DONE')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- M1659.2 System Standard Workflow (b0000007-*)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000007-0000-0000-0000-000000000001', 'Backlog',             'TODO'),
    ('b0000007-0000-0000-0000-000000000002', 'Internal KoM',        'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000003', 'Common KoM',          'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000004', 'Plans Review',        'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000005', 'FCR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000006', 'PDR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000007', 'DDR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000008', 'CDR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-000000000009', 'LAR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000a', 'FAR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000b', 'FFR',                 'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000c', 'CR',                  'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000d', 'In Service Release',  'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000e', 'In Service',          'IN_PROGRESS'),
    ('b0000007-0000-0000-0000-00000000000f', 'Certified',           'DONE'),
    ('b0000007-0000-0000-0000-000000000010', 'Closed',              'DONE'),
    ('b0000007-0000-0000-0000-000000000011', 'Cancelled',           'DONE')
ON CONFLICT (status_id) DO NOTHING;

-- ============================================================================
-- MOD Workflow (b0000008-*) — V22 names (latest intent)
-- ============================================================================
INSERT INTO jira_workflow.workflow_status_definitions (status_id, name, category) VALUES
    ('b0000008-0000-0000-0000-000000000001', 'Open',                  'TODO'),
    ('b0000008-0000-0000-0000-000000000002', 'Impact Analysis',       'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000003', 'Design Review',         'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000004', 'Safety Review',         'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000005', 'Certification Review',  'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000006', 'Quick Review',          'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000007', 'Approved',              'IN_PROGRESS'),
    ('b0000008-0000-0000-0000-000000000008', 'Implemented',           'DONE'),
    ('b0000008-0000-0000-0000-000000000009', 'Closed',                'DONE')
ON CONFLICT (status_id) DO NOTHING;
