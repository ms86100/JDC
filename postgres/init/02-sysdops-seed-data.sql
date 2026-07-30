-- ============================================================
-- SYSDOPS Aircraft Design System - Cross-Schema Seed Data
-- Seeds project, version, issue, and change card data into
-- jira_project and jira_issue schemas for the nFMS V&V project.
-- ============================================================
-- Well-known IDs (must match V12 seed in avionics-systems-test-service):
--   Project:     10000000-0000-0000-0000-000000000001  (NFMS)
--   Fix Version: 20000000-0000-0000-0000-000000000001  (Baseline 1)
--   Fix Version: 20000000-0000-0000-0000-000000000002  (Baseline 2)
--   User:        30000000-0000-0000-0000-000000000001  (seed user)
-- ============================================================

-- ============================================
-- 0. ENSURE SEED USER EXISTS IN AUTH SCHEMA
-- ============================================

INSERT INTO jira_auth.users (id, username, email, password_hash, display_name, active)
VALUES ('30000000-0000-0000-0000-000000000001', 'sysdops_seed',
        'sysdops@nfms.airbus.local',
        '$2a$12$8pkY60ZxHKszkJLrD9Udh.XlenmSWnl8viUxp0vo2cptX/JgF5QXm',
        'SYSDOPS Seed User', true)
ON CONFLICT (username) DO NOTHING;


-- ============================================
-- 1. SEED PROJECT
-- ============================================

INSERT INTO jira_project.projects (id, project_key, name, description, project_type, lead_user_id, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'NFMS',
    'nFMS System Development & Testing',
    'Next-generation Flight Management System Verification & Validation project. Covers DIR TO, HOLD, OFFSET, and other lateral/vertical guidance functions for SA/CEO/NEO and A350 programmes.',
    'COMPANY_MANAGED',
    '30000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 2. SEED PROJECT VERSIONS (Baselines)
-- ============================================

INSERT INTO jira_issue.project_versions (id, project_id, version_name, description, released, release_date, start_date, sequence) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'Baseline 1', 'First VVO baseline delivery - DIR TO and HOLD core functions. Covers SIB and FIB lab testing.',
 false, NULL, '2026-06-01', 1),

('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'Baseline 2', 'Second VVO baseline delivery - OFFSET functions and non-regression campaign. Includes flight test preparation.',
 false, NULL, '2026-10-01', 2),

('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'Baseline 3', 'Third VVO baseline - A350-specific adaptations and certification evidence package.',
 false, NULL, '2027-01-15', 3)
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 3. SEED PROJECT COMPONENTS (Aircraft Subsystems)
-- ============================================

INSERT INTO jira_issue.project_components (id, project_id, name, description) VALUES
('60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'Lateral Guidance', 'LNAV lateral guidance functions including DIR TO, HOLD, OFFSET, and path computation.'),

('60000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'Vertical Guidance', 'VNAV vertical guidance functions including descent profiles, step climbs, and altitude constraints.'),

('60000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'Navigation Database', 'ARINC 424 navigation database management, loading, and integrity verification.'),

('60000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001',
 'MCDU Interface', 'Multi-function Control Display Unit interface for pilot interaction with FMS.'),

('60000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001',
 'Performance Computation', 'Fuel prediction, performance degradation assessment, and engine model integration.'),

('60000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001',
 'Datalink', 'CPDLC, ADS-C, and ACARS datalink integration for ATC clearance processing.')
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 4. SEED ISSUES (Change Cards for the nFMS project)
-- These represent design changes and anomalies tracked
-- alongside the VVO verification activities.
-- ============================================

-- Issue statuses reference: use VARCHAR status values since the
-- jira_issue.issues.status column is VARCHAR(50) in the 00-init-schema.

INSERT INTO jira_issue.issues (id, project_id, issue_key, title, description, status, priority, issue_type,
    reporter_id, assignee_id, created_at, updated_at) VALUES

-- Change Card 1: ANOMALY from Tech Event TE-1
('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'NFMS-CC-1', 'FADEC interface change for wind shear DIR TO correction',
 'Design change required to improve DIR TO lateral accuracy during wind shear events. Triggered by Tech Event TE-1 showing 0.3 NM deviation exceeding tolerance.',
 'In Progress', 'Highest', 'Bug',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Change Card 2: ANOMALY from magnetic variation issue
('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'NFMS-CC-2', 'WMM2025 magnetic model integration',
 'Integrate WMM2025 magnetic variation model to replace outdated WMM2020. Required for accurate DIR TO course computation above 70N latitude.',
 'Done', 'High', 'Bug',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Change Card 3: EVOLUTION for hold entry improvement
('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'NFMS-CC-3', 'HOLD entry algorithm enhancement for 180-degree ambiguity',
 'Evolution to resolve the 180-degree heading ambiguity in hold entry type selection per ARINC 424 Chapter 7 clarification AM-2026-003.',
 'In Progress', 'High', 'Improvement',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Change Card 4: EVOLUTION for fuel prediction
('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001',
 'NFMS-CC-4', 'LEAP-1A engine model table resolution improvement',
 'Improve CFM LEAP-1A engine model lookup table resolution at FL350 to reduce hold fuel prediction error from 2.4% to within 1% threshold.',
 'In Progress', 'High', 'Improvement',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Change Card 5: Task for CPDLC parser (already resolved)
('40000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001',
 'NFMS-CC-5', 'CPDLC UM-176 revised hold clearance parser update',
 'Update CPDLC parser to correctly recognize and process UM-176 (revised hold clearance) messages from ATC ground systems.',
 'Done', 'Medium', 'Task',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Design Item 1: FRD for lateral guidance
('40000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001',
 'NFMS-DI-1', 'FRD v3.2 - Lateral Guidance Design Item',
 'Functional Requirement Document v3.2 covering all lateral guidance functions: DIR TO, HOLD, OFFSET, leg types, and transition logic.',
 'In Progress', 'High', 'Task',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- DCL 1: Supplier sync for engine model data
('40000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001',
 'NFMS-DCL-1', 'DCL - Thales engine model data package',
 'Design Change Log for engine model data package exchange with Thales for LEAP-1A performance tables.',
 'In Progress', 'Medium', 'Task',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Deliverable 1: SID document
('40000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001',
 'NFMS-DEL-1', 'SID - nFMS Lateral Guidance Software Interface Document',
 'Software Interface Document for nFMS lateral guidance module. Defines ARINC 429, MIL-STD-1553, and Ethernet interfaces.',
 'To Do', 'High', 'Task',
 '30000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 5. SEED CHANGE CARD METADATA
-- ============================================

INSERT INTO jira_issue.change_card_metadata (id, issue_id, change_type, classification, tab_layout_key) VALUES
('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001',
 'ANOMALY', 'TYPE_1A', 'STANDARD'),

('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002',
 'ANOMALY', 'TYPE_1B', 'STANDARD'),

('50000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000003',
 'EVOLUTION', 'TYPE_2', 'STANDARD'),

('50000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000004',
 'EVOLUTION', 'TYPE_2', 'STANDARD'),

('50000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000005',
 'ANOMALY', 'TYPE_0', 'STANDARD')
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 6. SEED DESIGN ITEM METADATA
-- ============================================

INSERT INTO jira_issue.design_item_metadata (id, issue_id, applicability, supplier_sharing) VALUES
('51000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000006',
 ARRAY['SA_CEONEO','A350'], true)
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 7. SEED DCL METADATA
-- ============================================

INSERT INTO jira_issue.dcl_metadata (id, issue_id, action_responsible, requested_by, dcl_abstract,
    description_thales, sync_direction) VALUES
('52000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000007',
 'Thales Avionics', 'Airbus nFMS Team',
 'Request for updated LEAP-1A performance tables with improved resolution at FL350 for hold fuel prediction.',
 'Engine model data package v2.3 with 500ft altitude step resolution (previously 1000ft).',
 'AIRBUS_TO_SUPPLIER')
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 8. SEED DELIVERABLE METADATA
-- ============================================

INSERT INTO jira_issue.deliverable_metadata (id, issue_id, deliverable_type, milestone_type,
    baseline_start_date, baseline_end_date, review_status, domain_leader) VALUES
('53000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000008',
 'SID', 'CRITICAL_DELIVERABLE',
 '2026-08-01', '2026-09-15',
 'TO_DO', '30000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 9. SEED ISSUE VERSIONS (link issues to fix versions)
-- ============================================

INSERT INTO jira_issue.issue_versions (issue_id, version_id, type) VALUES
('40000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'FIX_VERSION'),
('40000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'FIX_VERSION'),
('40000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'FIX_VERSION'),
('40000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 'FIX_VERSION'),
('40000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001', 'FIX_VERSION')
ON CONFLICT DO NOTHING;


-- ============================================
-- 10. SEED ISSUE COMPONENTS (link issues to components)
-- ============================================

INSERT INTO jira_issue.issue_components (issue_id, component_id) VALUES
-- CC-1: Lateral Guidance
('40000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001'),
-- CC-2: Navigation Database
('40000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003'),
-- CC-3: Lateral Guidance
('40000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000001'),
-- CC-4: Performance Computation
('40000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000005'),
-- CC-5: Datalink
('40000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000006'),
-- DI-1: Lateral Guidance
('40000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000001'),
-- DEL-1: MCDU Interface + Lateral Guidance
('40000000-0000-0000-0000-000000000008', '60000000-0000-0000-0000-000000000001'),
('40000000-0000-0000-0000-000000000008', '60000000-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;


-- ============================================
-- 11. SEED PROJECT MEMBER (add seed user to project)
-- ============================================

INSERT INTO jira_project.project_members (id, project_id, member_type, member_id, role_id)
SELECT
    uuid_generate_v4(),
    '10000000-0000-0000-0000-000000000001',
    'USER',
    '30000000-0000-0000-0000-000000000001',
    pr.id
FROM jira_project.project_roles pr
WHERE pr.name = 'PROJECT_ADMIN'
  OR pr.name = 'Administrators'
LIMIT 1
ON CONFLICT DO NOTHING;


-- ============================================
-- 12. SEED VERSION-SERVICE PROJECT VERSIONS
-- (for jira-version-service standalone schema)
-- ============================================

INSERT INTO project_versions (id, project_id, name, description, released, sequence, start_date, release_status, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'Baseline 1', 'First VVO baseline delivery - DIR TO and HOLD core functions.',
 false, 1, '2026-06-01', 'UNRELEASED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'Baseline 2', 'Second VVO baseline delivery - OFFSET functions and non-regression.',
 false, 2, '2026-10-01', 'UNRELEASED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'Baseline 3', 'Third VVO baseline - A350 adaptations and certification evidence.',
 false, 3, '2027-01-15', 'UNRELEASED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
