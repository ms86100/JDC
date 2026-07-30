-- ============================================================
-- V12: SYSDOPS Aircraft Design System - Comprehensive Test Data Seed
-- Seeds VVO, HLVVO, TechEvent, BenchDefect, ProblemReport,
-- TestRequest, TestIssue, and traceability data so all UI pages
-- show meaningful data on first load.
-- ============================================================
-- Well-known IDs:
--   Project:     10000000-0000-0000-0000-000000000001  (NFMS)
--   Fix Version: 20000000-0000-0000-0000-000000000001  (Baseline 1)
--   Fix Version: 20000000-0000-0000-0000-000000000002  (Baseline 2)
--   User:        30000000-0000-0000-0000-000000000001  (seed user)
-- ============================================================

-- ============================================
-- 1. HLVVO DEFINITIONS (2 records)
-- ============================================

INSERT INTO hlvvo_definition (id, project_id, issue_key, summary, description, status, target_date, airbus_reference, hlvvo_version, task_progress, created_by) VALUES
('c1000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'HLVVO-1',
 'DIR TO Verification Package',
 'High-level verification package for DIR TO functions covering lateral guidance, waypoint sequencing, and course intercept logic for the nFMS.',
 'AUTHORIZE', '2026-09-30', 'HLVVO-DIRTO-001', 1, 100, '30000000-0000-0000-0000-000000000001'),

('c1000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'HLVVO-2',
 'HOLD Pattern Verification Package',
 'High-level verification package for HOLD pattern functions including entry type selection, timing legs, and protected-area computations.',
 'VVO_WRITING_IN_PROGRESS', '2026-12-31', 'HLVVO-HOLD-001', 1, 40, '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;


-- ============================================
-- 2. VVO DEFINITIONS (15 records)
-- ============================================

-- --- 3 VVOs in NEW status ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, created_by) VALUES

('c2000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'VVO-1',
 'DIR TO: Verify ABEAM waypoint maximum capacity',
 'Verify that the FMS correctly handles the maximum number of ABEAM waypoints in a DIR TO scenario. Ensure no buffer overflow and proper sequencing.',
 'NEW', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_1.0', 1, '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'VVO-2',
 'DIR TO: Course intercept convergence angle limits',
 'Verify that course intercept is correctly computed for convergence angles between 0 and 90 degrees, and that the FMS rejects angles outside this range.',
 'NEW', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_2.0', 1, '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'VVO-3',
 'DIR TO: Lateral guidance transition from NAV to HDG',
 'Verify smooth lateral guidance transition when pilot activates DIR TO while in HDG mode. Check roll-steering commands and FD bar behavior.',
 'NEW', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_DO'], ARRAY['MATURITY'], 'INTERFACE', ARRAY['SIMULATOR'],
 ARRAY['SA_CEONEO','A350'], NULL, 1, '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- --- 3 VVOs in TO_BE_VERIFIED status ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, created_by) VALUES

('c2000001-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'VVO-4',
 'DIR TO: Fly-by waypoint overshooting protection',
 'Verify that the FMS path computation prevents overshooting on fly-by waypoints during DIR TO. Check bank angle constraints and turn anticipation distance.',
 'TO_BE_VERIFIED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB','AIRBUS_DO'], ARRAY['FORMAL_VERIFICATION','NON_REGRESSION'], 'FUNCTIONAL', ARRAY['SIB','FIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_3.0', 1, '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'VVO-5',
 'DIR TO: RNAV approach path re-computation after DIR TO',
 'Verify that after a DIR TO to a waypoint on an RNAV approach, the FMS correctly re-sequences the approach procedure and final approach path.',
 'TO_BE_VERIFIED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO','A350'], 'VVO_nFMS_DIRTO_4.0', 1, '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'VVO-6',
 'DIR TO: Performance degradation under high waypoint count',
 'Verify FMS performance (computation time, display refresh rate) when DIR TO is activated with a flight plan containing 250+ waypoints.',
 'TO_BE_VERIFIED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['NON_REGRESSION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], NULL, 1, '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- --- 3 VVOs in VERIFIED status (with fix version) ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, fix_version_id, created_by) VALUES

('c2000001-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'VVO-7',
 'DIR TO: Correct track-to-fix leg insertion',
 'Verify that a track-to-fix leg is correctly inserted in the flight plan when DIR TO targets a waypoint not on the current active leg. Verified on SIB with Baseline 1.',
 'VERIFIED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_5.0', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'VVO-8',
 'DIR TO: Wind model update during intercept',
 'Verify that the FMS correctly updates the wind model when wind data changes during a DIR TO intercept. Confirm path prediction adjusts accordingly.',
 'VERIFIED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_DO'], ARRAY['FORMAL_VERIFICATION'], 'INTERFACE', ARRAY['FIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_6.0', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000001', 'VVO-9',
 'HOLD: Standard entry type selection (direct/teardrop/parallel)',
 'Verify that the FMS selects the correct hold entry type based on aircraft heading relative to inbound course. All three entry types validated.',
 'VERIFIED', 'c1000001-0000-0000-0000-000000000002',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO','A350'], 'VVO_nFMS_HOLD_43.1', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- --- 3 VVOs in RELEASED status (with fix version) ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, fix_version_id, created_by) VALUES

('c2000001-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000001', 'VVO-10',
 'HOLD: Timing leg computation for outbound leg',
 'Verify outbound leg timing computation based on altitude (1 min below FL140, 1.5 min at or above FL140). Released in Baseline 1.',
 'RELEASED', 'c1000001-0000-0000-0000-000000000002',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_HOLD_44.0', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'VVO-11',
 'HOLD: Protected area computation per ARINC 424',
 'Verify that the hold protected area dimensions conform to ARINC 424 Chapter 7 specifications including wind corrections and buffer zones.',
 'RELEASED', 'c1000001-0000-0000-0000-000000000002',
 ARRAY['AIRBUS_LAB','AIRBUS_DO'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB','FLIGHT_TEST'],
 ARRAY['SA_CEONEO','A350'], 'VVO_nFMS_HOLD_45.0', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'VVO-12',
 'OFFSET: Lateral offset path computation accuracy',
 'Verify that the FMS computes a parallel offset path within +/-0.01 NM accuracy for offsets between 1 and 20 NM. Released after flight test campaign.',
 'RELEASED', NULL,
 ARRAY['AIRBUS_LAB','AIRBUS_DO'], ARRAY['FORMAL_VERIFICATION','NON_REGRESSION'], 'FUNCTIONAL', ARRAY['SIB','FLIGHT_TEST'],
 ARRAY['SA_CEONEO','A350'], 'VVO_nFMS_OFFSET_10.0', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- --- 2 VVOs in CANCELLED status (with fix version) ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, fix_version_id, created_by) VALUES

('c2000001-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000001', 'VVO-13',
 'HOLD: Manual hold speed entry validation (CANCELLED - superseded by VVO-9)',
 'Originally planned to verify manual hold speed entry. Cancelled because the scope was merged into VVO-9 hold entry type selection.',
 'CANCELLED', 'c1000001-0000-0000-0000-000000000002',
 ARRAY['AIRBUS_LAB'], ARRAY['MATURITY'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], NULL, 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c2000001-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000001', 'VVO-14',
 'OFFSET: Step-climb offset re-engagement (CANCELLED)',
 'Cancelled: requirement removed from FRD v3.2. Step-climb during offset no longer a certified scenario.',
 'CANCELLED', NULL,
 ARRAY['AIRBUS_DO'], ARRAY['NON_REGRESSION'], 'INTERFACE', ARRAY['SIMULATOR'],
 ARRAY['A350'], NULL, 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- --- 1 VVO in SUPERSEDED status ---

INSERT INTO vvo_definition (id, project_id, issue_key, summary, description, status, hlvvo_id,
    execution_responsible, vvo_usage, vvo_scope, test_mean_type_requested,
    applicability, id_doors, vvo_version, fix_version_id, created_by) VALUES

('c2000001-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000001', 'VVO-15',
 'DIR TO: Legacy intercept algorithm (SUPERSEDED by VVO-2)',
 'Superseded by VVO-2 which covers the updated intercept algorithm after FRD amendment 4.1.',
 'SUPERSEDED', 'c1000001-0000-0000-0000-000000000001',
 ARRAY['AIRBUS_LAB'], ARRAY['FORMAL_VERIFICATION'], 'FUNCTIONAL', ARRAY['SIB'],
 ARRAY['SA_CEONEO'], 'VVO_nFMS_DIRTO_0.9', 1, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;


-- ============================================
-- 3. TEST REQUESTS (2 records)
-- ============================================

INSERT INTO test_request (id, project_id, issue_key, summary, description, request_type, status, fix_version_id, created_by) VALUES
('c3000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'LTR-1',
 'Lab Test Request - Baseline 1 DIR TO',
 'Lab test request covering all DIR TO VVOs planned for Baseline 1 execution on SIB bench.',
 'LTR', 'IN_PROGRESS', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),

('c3000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'FTR-1',
 'Flight Test Request - HOLD Pattern',
 'Flight test request for HOLD pattern entry and protected area validation on A350 aircraft.',
 'FTR', 'OPEN', NULL, '30000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;


-- ============================================
-- 4. VVO <-> TEST REQUEST LINKS
-- Link VVOs 4-8 (DIR TO, TO_BE_VERIFIED/VERIFIED) to LTR-1
-- ============================================

INSERT INTO vvo_test_request_link (id, vvo_id, test_request_id, link_type) VALUES
('c4000001-0000-0000-0000-000000000001', 'c2000001-0000-0000-0000-000000000004', 'c3000001-0000-0000-0000-000000000001', 'CONTAIN'),
('c4000001-0000-0000-0000-000000000002', 'c2000001-0000-0000-0000-000000000005', 'c3000001-0000-0000-0000-000000000001', 'CONTAIN'),
('c4000001-0000-0000-0000-000000000003', 'c2000001-0000-0000-0000-000000000006', 'c3000001-0000-0000-0000-000000000001', 'CONTAIN'),
('c4000001-0000-0000-0000-000000000004', 'c2000001-0000-0000-0000-000000000007', 'c3000001-0000-0000-0000-000000000001', 'CONTAIN'),
('c4000001-0000-0000-0000-000000000005', 'c2000001-0000-0000-0000-000000000008', 'c3000001-0000-0000-0000-000000000001', 'CONTAIN')
ON CONFLICT DO NOTHING;


-- ============================================
-- 5. TECH EVENTS (8 records across M1668 states)
-- ============================================

INSERT INTO tech_event (id, project_id, issue_key, summary, description, status,
    reporter_id, detected_on_date, defect_type, defect_origin, defect_impact, priority,
    public_analysis, workaround, labels) VALUES

-- 2 OPEN
('c5000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'TE-1',
 'DIR TO: Unexpected lateral deviation during wind shear event',
 'During SIB test with 40kt wind shear injection, the DIR TO intercept path showed a 0.3 NM lateral deviation exceeding the 0.1 NM tolerance.',
 'OPEN', '30000000-0000-0000-0000-000000000001', '2026-06-15',
 'SOFTWARE', 'FUNCTION', 'FLIGHT_CLEARANCE', 'HIGH',
 NULL, NULL, ARRAY['DIR_TO','wind_shear','SIB']),

('c5000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'TE-2',
 'HOLD: Entry type incorrectly computed for 180-degree heading',
 'When aircraft heading equals the inbound course (180-degree ambiguity), the FMS selects direct entry instead of teardrop. ARINC 424 specifies teardrop.',
 'OPEN', '30000000-0000-0000-0000-000000000001', '2026-06-20',
 'SOFTWARE', 'FUNCTION', 'LAB_CLEARANCE', 'HIGH',
 NULL, NULL, ARRAY['HOLD','entry_type','boundary_case']),

-- 1 UNDER_ORIGINATOR_ANALYSIS
('c5000001-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'TE-3',
 'DIR TO: MCDU display freeze during rapid DIR TO/HDG toggle',
 'MCDU display freezes for ~2 seconds when rapidly toggling between DIR TO and HDG modes. System recovers automatically but pilot loses situational awareness.',
 'UNDER_ORIGINATOR_ANALYSIS', '30000000-0000-0000-0000-000000000001', '2026-05-28',
 'SOFTWARE', 'SYSTEM', 'LAB_CLEARANCE', 'MEDIUM',
 'Investigating MCDU rendering thread priority during mode transitions.', NULL, ARRAY['DIR_TO','MCDU','display']),

-- 1 UNDER_RESOLVER_ANALYSIS
('c5000001-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'TE-4',
 'HOLD: Fuel prediction error in hold pattern exceeds 2% threshold',
 'Fuel consumption prediction during hold shows 2.4% error vs actual on A320neo engine model. Within tolerance for A350 model.',
 'UNDER_RESOLVER_ANALYSIS', '30000000-0000-0000-0000-000000000001', '2026-05-10',
 'SOFTWARE', 'FUNCTION', 'CERTIFICATION', 'HIGH',
 'Root cause analysis points to engine model lookup table resolution for CFM LEAP-1A at FL350.', NULL, ARRAY['HOLD','fuel_prediction','A320neo']),

-- 1 CLASSIFIED
('c5000001-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'TE-5',
 'OFFSET: Path computation timeout on complex procedure turns',
 'FMS computation exceeds 500ms timeout when computing offset path through more than 3 consecutive procedure turns. Affects SID with multiple turns.',
 'CLASSIFIED', '30000000-0000-0000-0000-000000000001', '2026-04-22',
 'SOFTWARE', 'FUNCTION', 'OPERATION', 'MEDIUM',
 'Classified as design limitation. Algorithm complexity is O(n^2) for consecutive turns.', 'Limit offset activation to segments with <= 3 turns.', ARRAY['OFFSET','timeout','procedure_turn']),

-- 1 RESOLVED_CORRECTED
('c5000001-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001', 'TE-6',
 'DIR TO: Incorrect magnetic variation applied at high latitudes',
 'DIR TO course computation uses outdated WMM2020 magnetic model above 70N latitude. Should use WMM2025 per FRD v3.1 requirement NAVDB-MAG-001.',
 'RESOLVED_CORRECTED', '30000000-0000-0000-0000-000000000001', '2026-03-15',
 'SOFTWARE', 'FUNCTION', 'FLIGHT_CLEARANCE', 'HIGH',
 'Corrected by updating WMM model to 2025 epoch. Patch applied in Build 14.2.1.', NULL,
 ARRAY['DIR_TO','magnetic_variation','WMM','resolved']),

-- 1 CLOSED
('c5000001-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001', 'TE-7',
 'HOLD: ATC revision of hold clearance not reflected in FMS',
 'When ATC revises hold clearance (new fix or different inbound course), FMS did not update the active hold. Fixed in Build 14.1.0.',
 'CLOSED', '30000000-0000-0000-0000-000000000001', '2026-02-08',
 'SOFTWARE', 'FUNCTION', 'OPERATION', 'MEDIUM',
 'Root cause: datalink parser did not recognize CPDLC UM-176 (revised hold) message. Parser updated.', NULL,
 ARRAY['HOLD','CPDLC','ATC','closed']),

-- 1 CANCELLED
('c5000001-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001', 'TE-8',
 'DIR TO: Duplicate waypoint name handling (CANCELLED - test procedure error)',
 'Reported lateral jump when DIR TO to a waypoint with duplicate name. Investigation revealed test procedure used wrong waypoint database version.',
 'CANCELLED', '30000000-0000-0000-0000-000000000001', '2026-01-20',
 'SOFTWARE', 'TEST_PROCEDURE', NULL, 'LOW',
 'Cancelled: root cause is test procedure error, not a software defect. NavDB version mismatch in SIB configuration.',
 NULL, ARRAY['DIR_TO','cancelled','procedure_error'])
ON CONFLICT DO NOTHING;


-- ============================================
-- 6. BENCH DEFECTS (4 records)
-- ============================================

INSERT INTO bench_defect (id, project_id, issue_key, summary, description, status,
    severity, criticality, defect_type, defect_origin, defect_impact,
    detected_on_date, source_tech_event_id, reporter_id, priority, labels) VALUES

-- BLOCKING severity
('c6000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'BD-1',
 'SIB-3 bench: ARINC 429 bus failure during DIR TO execution',
 'ARINC 429 label 314 (lateral deviation) stops transmitting after DIR TO activation on SIB-3. Hardware interface board suspected. Bench is unusable for DIR TO testing.',
 'OPEN', 'BLOCKING', NULL,
 'HARDWARE', 'TEST_MEANS', 'LAB_CLEARANCE',
 '2026-06-25', NULL, '30000000-0000-0000-0000-000000000001', 'HIGHEST', ARRAY['SIB-3','ARINC429','blocking']),

-- HIGH severity with P1 criticality, linked to TE-1
('c6000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'BD-2',
 'FIB-2 bench: Wind model injection interface timing jitter',
 'Wind model injection via MIL-STD-1553 shows 15ms jitter (spec: <5ms). Causes non-deterministic test results for wind-dependent VVOs on FIB-2.',
 'UNDER_ANALYSIS', 'HIGH', 'P1',
 'HARDWARE', 'TEST_MEANS', 'LAB_CLEARANCE',
 '2026-06-18', 'c5000001-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'HIGH', ARRAY['FIB-2','1553','timing']),

-- LOW severity
('c6000001-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'BD-3',
 'SIB-1 bench: MCDU display brightness inconsistency',
 'MCDU display brightness on SIB-1 differs from aircraft specification by 15%. Does not affect test validity but noted for fidelity tracking.',
 'OPEN', 'LOW', NULL,
 'HARDWARE', 'TEST_MEANS', 'IMPROVEMENT',
 '2026-05-30', NULL, '30000000-0000-0000-0000-000000000001', 'LOW', ARRAY['SIB-1','MCDU','cosmetic']),

-- CLOSED
('c6000001-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'BD-4',
 'SIB-2 bench: NavDB loading timeout (RESOLVED)',
 'Navigation database loading exceeded 120s timeout on SIB-2 bench during cold start. Root cause: USB controller firmware. Updated to v2.1, loading time now 45s.',
 'CLOSED', 'HIGH', 'P2',
 'SOFTWARE', 'TEST_MEANS', 'LAB_CLEARANCE',
 '2026-04-10', NULL, '30000000-0000-0000-0000-000000000001', 'MEDIUM', ARRAY['SIB-2','NavDB','resolved'])
ON CONFLICT DO NOTHING;


-- ============================================
-- 7. PROBLEM REPORTS (3 records)
-- ============================================

INSERT INTO problem_report (id, project_id, issue_key, summary, description, status,
    pr_origin, pr_type, pr_type_rationale, potential_effects,
    justification_mitigation, linked_tech_event_id, reporter_id, priority, labels) VALUES

-- OPEN with SIGNIFICANT_CAT_HAZ
('c7000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'PR-1',
 'Lateral deviation during DIR TO may exceed RNAV containment',
 'During wind shear conditions, the DIR TO lateral deviation exceeded 0.3 NM. If this occurs during an RNP approach, the aircraft may exit the containment area, which is a CAT HAZ condition.',
 'OPEN', 'VV_ACTIVITY', 'SIGNIFICANT_CAT_HAZ',
 'Failure to maintain RNP containment during DIR TO in wind shear may lead to controlled flight into terrain if in mountainous approach environment.',
 'RNP containment alerting system provides visual and aural warning. Crew procedure mandates go-around if RNP alert triggers. Probability assessment pending.',
 'Wind shear magnitude threshold to be determined. Affects DIR TO below FL100 in terminal area.', 'c5000001-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000001', 'HIGHEST', ARRAY['CAT_HAZ','DIR_TO','RNP','safety']),

-- UNDER_ANALYSIS with FUNCTIONAL
('c7000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'PR-2',
 'HOLD fuel prediction inaccuracy may cause insufficient fuel warning',
 'The 2.4% fuel prediction error in hold patterns could lead to premature LOW FUEL warnings or, conversely, mask actual fuel shortfall conditions.',
 'UNDER_ANALYSIS', 'VV_ACTIVITY', 'FUNCTIONAL',
 'Functional impact: possible nuisance LOW FUEL advisory during extended holds, leading to unnecessary crew workload.',
 'Crew training addresses fuel management during holds. Dispatcher fuel planning includes hold contingency per ICAO Annex 6.',
 'Quantitative analysis of error propagation over 30-minute hold duration in progress.', 'c5000001-0000-0000-0000-000000000004',
 '30000000-0000-0000-0000-000000000001', 'HIGH', ARRAY['HOLD','fuel','functional']),

-- CLOSED
('c7000001-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'PR-3',
 'WMM magnetic model outdated for high-latitude operations (CLOSED)',
 'Problem report raised for the use of WMM2020 above 70N. Closed after Tech Event TE-6 resolution confirmed WMM2025 integration in Build 14.2.1.',
 'CLOSED', 'DESIGN_REVIEW', 'FUNCTIONAL',
 'Outdated magnetic model could cause up to 3-degree heading error during DIR TO above 70N latitude.',
 'Corrected by WMM2025 update. Verified through regression testing on SIB-1.',
 NULL, 'c5000001-0000-0000-0000-000000000006',
 '30000000-0000-0000-0000-000000000001', 'MEDIUM', ARRAY['WMM','magnetic','closed'])
ON CONFLICT DO NOTHING;


-- ============================================
-- 8. TEST ISSUES (test cases for traceability)
-- ============================================

INSERT INTO test_issue (id, project_id, name, description, test_type, status, priority, labels) VALUES
('d1000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'TC-DIRTO-001: ABEAM waypoint maximum capacity',
 'Verify DIR TO handles max ABEAM waypoints. Steps: 1) Load flight plan with 99 ABEAM waypoints, 2) Activate DIR TO to last ABEAM, 3) Verify sequencing.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['DIR_TO','formal_verification']),

('d1000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'TC-DIRTO-002: Course intercept angle validation',
 'Verify convergence angle limits for DIR TO course intercept. Steps: 1) Set intercept at 0/45/90/91 degrees, 2) Verify acceptance/rejection.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['DIR_TO','formal_verification']),

('d1000001-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001',
 'TC-DIRTO-003: NAV to HDG lateral guidance transition',
 'Verify smooth transition from NAV to HDG during DIR TO. Steps: 1) Engage DIR TO in NAV mode, 2) Switch to HDG, 3) Verify roll commands.',
 'MANUAL', 'APPROVED', 'MEDIUM', ARRAY['DIR_TO','interface']),

('d1000001-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001',
 'TC-HOLD-001: Entry type selection verification',
 'Verify correct hold entry type selection. Steps: 1) Set heading to test direct/teardrop/parallel entry conditions, 2) Verify entry type computed by FMS.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['HOLD','formal_verification']),

('d1000001-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001',
 'TC-HOLD-002: Outbound timing leg computation',
 'Verify hold timing per altitude rules. Steps: 1) Set hold at FL100 (expect 1 min), 2) Set hold at FL200 (expect 1.5 min), 3) Compare with computed values.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['HOLD','formal_verification']),

('d1000001-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000001',
 'TC-HOLD-003: Protected area ARINC 424 compliance',
 'Verify hold protected area dimensions. Steps: 1) Configure standard hold, 2) Measure computed protected area, 3) Compare against ARINC 424 Chapter 7.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['HOLD','formal_verification','ARINC424']),

('d1000001-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000001',
 'TC-OFFSET-001: Lateral offset path accuracy',
 'Verify offset path accuracy. Steps: 1) Set offsets at 1, 5, 10, 20 NM, 2) Measure deviation from expected parallel path, 3) Verify <0.01 NM tolerance.',
 'MANUAL', 'APPROVED', 'HIGH', ARRAY['OFFSET','formal_verification']),

('d1000001-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000001',
 'TC-DIRTO-004: Wind model update during intercept',
 'Automated test for wind model refresh. Steps: 1) Start DIR TO, 2) Inject wind change via FIB interface, 3) Verify path prediction adjusts within 2 cycles.',
 'AUTOMATED', 'APPROVED', 'MEDIUM', ARRAY['DIR_TO','automated','wind_model'])
ON CONFLICT DO NOTHING;


-- ============================================
-- 9. REQUIREMENT LINKS (VVO issue keys -> test cases)
-- Links VVOs to their corresponding test cases
-- ============================================

INSERT INTO requirement_link (id, requirement_key, requirement_type, test_id, coverage_status) VALUES
('e1000001-0000-0000-0000-000000000001', 'VVO-1', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000001', 'COVERED'),
('e1000001-0000-0000-0000-000000000002', 'VVO-2', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000002', 'COVERED'),
('e1000001-0000-0000-0000-000000000003', 'VVO-3', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000003', 'COVERED'),
('e1000001-0000-0000-0000-000000000004', 'VVO-9', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000004', 'COVERED'),
('e1000001-0000-0000-0000-000000000005', 'VVO-10', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000005', 'COVERED'),
('e1000001-0000-0000-0000-000000000006', 'VVO-11', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000006', 'COVERED'),
('e1000001-0000-0000-0000-000000000007', 'VVO-12', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000007', 'COVERED'),
('e1000001-0000-0000-0000-000000000008', 'VVO-8',  'REQUIREMENT', 'd1000001-0000-0000-0000-000000000008', 'COVERED'),
-- Partial coverage: VVO-4 needs more test cases
('e1000001-0000-0000-0000-000000000009', 'VVO-4', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000001', 'PARTIAL'),
-- Not covered: VVO-5 and VVO-6 still need test cases
('e1000001-0000-0000-0000-000000000010', 'VVO-5', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000002', 'NOT_COVERED'),
('e1000001-0000-0000-0000-000000000011', 'VVO-6', 'REQUIREMENT', 'd1000001-0000-0000-0000-000000000003', 'NOT_COVERED')
ON CONFLICT DO NOTHING;


-- ============================================
-- 10. TEST SET (group the VVO test cases)
-- ============================================

INSERT INTO test_set (id, project_id, name, description, test_type, test_count, labels) VALUES
('d2000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'Baseline 1 - DIR TO VVO Tests',
 'Test set containing all DIR TO verification test cases for Baseline 1.',
 'MANUAL', 4, ARRAY['baseline_1','DIR_TO']),
('d2000001-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001',
 'Baseline 1 - HOLD VVO Tests',
 'Test set containing all HOLD pattern verification test cases for Baseline 1.',
 'MANUAL', 3, ARRAY['baseline_1','HOLD'])
ON CONFLICT DO NOTHING;

INSERT INTO test_set_item (id, test_set_id, test_id) VALUES
('d3000001-0000-0000-0000-000000000001', 'd2000001-0000-0000-0000-000000000001', 'd1000001-0000-0000-0000-000000000001'),
('d3000001-0000-0000-0000-000000000002', 'd2000001-0000-0000-0000-000000000001', 'd1000001-0000-0000-0000-000000000002'),
('d3000001-0000-0000-0000-000000000003', 'd2000001-0000-0000-0000-000000000001', 'd1000001-0000-0000-0000-000000000003'),
('d3000001-0000-0000-0000-000000000004', 'd2000001-0000-0000-0000-000000000001', 'd1000001-0000-0000-0000-000000000008'),
('d3000001-0000-0000-0000-000000000005', 'd2000001-0000-0000-0000-000000000002', 'd1000001-0000-0000-0000-000000000004'),
('d3000001-0000-0000-0000-000000000006', 'd2000001-0000-0000-0000-000000000002', 'd1000001-0000-0000-0000-000000000005'),
('d3000001-0000-0000-0000-000000000007', 'd2000001-0000-0000-0000-000000000002', 'd1000001-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;


-- ============================================
-- 11. TEST PLAN (Baseline 1 overall plan)
-- ============================================

INSERT INTO test_plan (id, project_id, name, description, start_date, end_date, status, created_by, labels) VALUES
('d4000001-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
 'Baseline 1 Verification Campaign',
 'Complete verification campaign for nFMS Baseline 1 covering DIR TO, HOLD, and OFFSET functions.',
 '2026-06-01', '2026-09-30', 'IN_PROGRESS', '30000000-0000-0000-0000-000000000001', ARRAY['baseline_1','campaign'])
ON CONFLICT DO NOTHING;

INSERT INTO test_plan_item (id, test_plan_id, test_set_id, execution_order) VALUES
('d5000001-0000-0000-0000-000000000001', 'd4000001-0000-0000-0000-000000000001', 'd2000001-0000-0000-0000-000000000001', 1),
('d5000001-0000-0000-0000-000000000002', 'd4000001-0000-0000-0000-000000000001', 'd2000001-0000-0000-0000-000000000002', 2)
ON CONFLICT DO NOTHING;


-- ============================================
-- 12. TEST EXECUTIONS (show some execution history)
-- ============================================

INSERT INTO test_execution (id, test_plan_id, test_set_id, name, description, status,
    test_env, tester_id, test_cycle, total_tests, passed_tests, failed_tests, blocked_tests, not_run_tests,
    started_at, finished_at) VALUES

('d6000001-0000-0000-0000-000000000001', 'd4000001-0000-0000-0000-000000000001', 'd2000001-0000-0000-0000-000000000001',
 'DIR TO Tests - Run 1 (SIB-1)',
 'First execution of DIR TO test set on SIB-1 bench. One test blocked due to bench defect BD-1.',
 'FAILED', 'STAGING', '30000000-0000-0000-0000-000000000001', 'Baseline 1 - Sprint 3',
 4, 2, 1, 1, 0,
 '2026-06-15 09:00:00', '2026-06-15 17:30:00'),

('d6000001-0000-0000-0000-000000000002', 'd4000001-0000-0000-0000-000000000001', 'd2000001-0000-0000-0000-000000000002',
 'HOLD Tests - Run 1 (SIB-2)',
 'First execution of HOLD test set on SIB-2 bench. All tests passed.',
 'PASSED', 'STAGING', '30000000-0000-0000-0000-000000000001', 'Baseline 1 - Sprint 4',
 3, 3, 0, 0, 0,
 '2026-07-01 08:00:00', '2026-07-01 16:00:00')
ON CONFLICT DO NOTHING;
