-- V7__aircraft_master_data_seed.sql
-- Seed data for Aircraft Design System master data tables created in V6.
-- All INSERTs use ON CONFLICT DO NOTHING for idempotency.

-- ============================================
-- 1. AIRCRAFT PROGRAMS (5 top-level programs)
-- ============================================

INSERT INTO jira_admin.aircraft_programs (code, name, description, display_order) VALUES
    ('SA_CEONEO', 'Single Aisle CEO/NEO', 'Airbus A320 family CEO and NEO variants', 1),
    ('SA_NAX',    'Single Aisle New Avionics', 'Next-generation avionics for Single Aisle', 2),
    ('LR_CEONEO', 'Long Range CEO/NEO', 'Airbus A330 family CEO and NEO variants', 3),
    ('A350',      'A350 XWB', 'Airbus A350 extra wide body program', 4),
    ('A380',      'A380', 'Airbus A380 super jumbo program', 5)
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 2. TEST MEANS (per program via CTE)
-- ============================================

WITH programs AS (
    SELECT id FROM jira_admin.aircraft_programs
     WHERE code IN ('SA_CEONEO','SA_NAX','LR_CEONEO','A350','A380')
)
INSERT INTO jira_admin.test_means (code, name, program_id, category, display_order)
SELECT tm.code, tm.name, p.id, tm.category, tm.ord
FROM programs p
CROSS JOIN (VALUES
    ('SIB',         'System Integration Bench',                        'BENCH',       1),
    ('FIB',         'Full Integration Bench',                          'BENCH',       2),
    ('SIMULATOR',   'Flight Simulator',                                'SIMULATOR',   3),
    ('S23',         'S23 Test Bench',                                  'BENCH',       4),
    ('CGIB',        'CGIB Bench',                                      'BENCH',       5),
    ('IVP',         'IVP Test Bench',                                  'BENCH',       6),
    ('CSB',         'CSB Bench',                                       'BENCH',       7),
    ('vFiB',        'Virtual FIB',                                     'SIMULATOR',   8),
    ('HLSVE',       'High Level Simulation & Verification Environment','SIMULATOR',   9),
    ('FLIGHT_TEST', 'Flight Test',                                     'FLIGHT_TEST', 10)
) AS tm(code, name, category, ord)
ON CONFLICT (program_id, code) DO NOTHING;

-- ============================================
-- 3. AIRCRAFT SYSTEMS (37 systems, per program via CTE)
-- ============================================

WITH programs AS (
    SELECT id FROM jira_admin.aircraft_programs
     WHERE code IN ('SA_CEONEO','SA_NAX','LR_CEONEO','A350','A380')
)
INSERT INTO jira_admin.aircraft_systems (code, name, description, program_id, display_order)
SELECT s.code, s.name, s.descr, p.id, s.ord
FROM programs p
CROSS JOIN (VALUES
    ('ACR',   'A/C Communications Routing (ACR, ACARS)',     'ACR / ACARS communications routing',                 1),
    ('AFDX',  'A/C Network (AFDX)',                          'Avionics Full-Duplex Switched Ethernet network',     2),
    ('ADIRS', 'Air Data / Inertial Reference System',        'Air data and inertial reference system',              3),
    ('AGS',   'Air Generation System',                       'Air generation / environmental control system',       4),
    ('AESS',  'Aircraft Surveillance System',                'Aircraft electronic surveillance system',             5),
    ('ANS',   'Airport Navigation System',                   'Airport navigation system',                           6),
    ('AOC',   'AOC Application',                             'Airline operational control application',              7),
    ('ATC',   'ATC Application',                             'Air traffic control transponder application',          8),
    ('AMS',   'Audio Management System',                     'Audio management system',                             9),
    ('AP',    'Autopilot System',                            'Autopilot system',                                   10),
    ('APU',   'Auxiliary Power Unit',                         'Auxiliary power unit',                                11),
    ('AVS',   'Avionics Ventilation System',                 'Avionics ventilation and cooling',                    12),
    ('CCS',   'Cabin Core System',                           'Cabin core system',                                   13),
    ('CPCS',  'Cabin Pressure Control and Monitoring',       'Cabin pressure control and monitoring system',        14),
    ('CMS',   'Central Maintenance System',                  'Central maintenance system',                          15),
    ('CLK',   'Clock',                                       'Aircraft clock system',                               16),
    ('CP',    'Control Panels',                              'Cockpit control panels',                              17),
    ('DLS',   'Dataloading System',                          'Software dataloading system',                         18),
    ('DIS',   'Display System (EIS2, EIS, DGWP)',            'Electronic instrument / display system',              19),
    ('EPDS',  'Electrical Power Distribution',               'Electrical power distribution system',                20),
    ('FADEC', 'Engine Fuel & Control',                       'Full authority digital engine control',               21),
    ('FCS',   'Flight Control System',                       'Flight control system (primary + secondary)',         22),
    ('FCU',   'Flight Control Unit',                         'Flight control unit',                                 23),
    ('FDR',   'Flight Data Recorders',                       'Flight data recorder (FDR / CVR)',                    24),
    ('FMS',   'Flight Management System',                    'Flight management system',                            25),
    ('FWS',   'Flight Warning System',                       'Flight warning system',                               26),
    ('FMSS',  'FMS Source Selector Switch',                  'FMS source selector switch',                          27),
    ('FMGMT', 'Fuel Management System',                      'Fuel quantity and management system',                 28),
    ('HUD',   'Head Up Display',                             'Head up display system',                              29),
    ('HLS',   'High Lift System',                            'High lift / slat and flap system',                    30),
    ('IGS',   'Inerting Gas System',                         'Fuel tank inerting gas system',                       31),
    ('LGMS',  'Landing Gear Management System',              'Landing gear management system',                      32),
    ('MFD',   'Multi Function Display',                      'Multi function display',                              33),
    ('MCDU',  'Multipurpose Control & Display Unit',         'Multipurpose control and display unit',               34),
    ('NAV',   'Navaids (MMR, VOR, ADF, DME)',                'Navigation aids (MMR, VOR, ADF, DME)',                35),
    ('EFB',   'OIS / Flight Crew Application (EFB)',         'Onboard information system / electronic flight bag', 36),
    ('SPP',   'Software Pin Programming',                    'Software pin programming system',                     37)
) AS s(code, name, descr, ord)
ON CONFLICT (program_id, code) DO NOTHING;

-- ============================================
-- 4. SYSTEM FUNCTIONS (26 functions linked to FMS)
--    Uses a CTE to look up the FMS system_id per program.
-- ============================================

WITH fms_systems AS (
    SELECT asys.id AS system_id
    FROM jira_admin.aircraft_systems asys
    WHERE asys.code = 'FMS'
)
INSERT INTO jira_admin.system_functions (code, name, system_id, display_order)
SELECT f.code, f.name, fs.system_id, f.ord
FROM fms_systems fs
CROSS JOIN (VALUES
    ('00', 'Non Functional',                                0),
    ('01', 'A/C Position',                                  1),
    ('02', 'BITE Interactive',                               2),
    ('03', 'BITE Normal',                                    3),
    ('04', 'Data Entry Plug / Pin Prog',                     4),
    ('05', 'Elec Power Supply',                              5),
    ('06', 'Engine Controls',                                6),
    ('07', 'Engine Indication',                              7),
    ('08', 'Engine Monitoring',                              8),
    ('09', 'Engine Operability',                             9),
    ('10', 'Engine Protection',                             10),
    ('11', 'Engine Start & Shutdown',                       11),
    ('12', 'Engine Status',                                 12),
    ('13', 'Engine Systems',                                13),
    ('14', 'Fuel Measurement',                              14),
    ('15', 'Interfaces',                                    15),
    ('16', 'Nacelle Anti Ice Control & Monitoring',         16),
    ('17', 'NAIADS',                                        17),
    ('18', 'Non Volatile Memory',                           18),
    ('19', 'Reverse Thrust Control & Monitoring',           19),
    ('20', 'Software Dataloading',                          20),
    ('21', 'Thrust Control',                                21),
    ('22', 'Thrust Management - Air Data',                  22),
    ('23', 'Thrust Management - Bleed',                     23),
    ('24', 'Thrust Rating & Power Management',              24),
    ('25', 'Thrust Settings',                               25),
    ('26', 'Automated Operations',                          26)
) AS f(code, name, ord)
ON CONFLICT (system_id, code) DO NOTHING;

-- ============================================
-- 5. REPORTER TEAMS (20 teams, program_id NULL)
-- ============================================

INSERT INTO jira_admin.reporter_teams (code, name, program_id, display_order) VALUES
    ('ACTUATORS',            'Actuators',              NULL,  1),
    ('AFS',                  'AFS',                    NULL,  2),
    ('AIRCRAFT_OPERATIONS',  'Aircraft Operations',    NULL,  3),
    ('AUTOPILOT',            'Autopilot',              NULL,  4),
    ('DISPLAY_A350',         'Display A350',           NULL,  5),
    ('DISPLAY_LR',           'Display LR',             NULL,  6),
    ('DISPLAY_SA',           'Display SA',             NULL,  7),
    ('ESG',                  'ESG',                    NULL,  8),
    ('ETOC',                 'ETOC',                   NULL,  9),
    ('FLIGHT_MANAGEMENT',    'Flight Management',      NULL, 10),
    ('FSA_NG',               'FSA-NG',                 NULL, 11),
    ('FWS_SA',               'FWS SA',                 NULL, 12),
    ('HLS',                  'HLS',                    NULL, 13),
    ('HG',                   'HG',                     NULL, 14),
    ('LAWS_22',              'LAWS 22',                NULL, 15),
    ('LAWS_27',              'LAWS 27',                NULL, 16),
    ('NAIADS',               'NAIADS',                 NULL, 17),
    ('PFCS',                 'PFCS',                   NULL, 18),
    ('PRIMARY_FLIGHT_CTRL',  'Primary Flight Control', NULL, 19),
    ('TEST_MEANS',           'Test Means',             NULL, 20)
ON CONFLICT (code) DO NOTHING;

-- ============================================
-- 6. TEST MEAN DEFECT ORIGIN CATEGORIES
--    Parent categories first, then sub-items
--    with CTEs to resolve parent_id.
-- ============================================

-- 6a. Parent categories
INSERT INTO jira_admin.test_mean_defect_origins (category, sub_item, parent_id, display_order) VALUES
    ('ARCHITECTURE',                 NULL, NULL, 1),
    ('FACILITIES',                   NULL, NULL, 2),
    ('HYDRAULIC',                    NULL, NULL, 3),
    ('INSTRUMENTATION_AND_TOOLS',    NULL, NULL, 4),
    ('MECHANIC',                     NULL, NULL, 5),
    ('OTHER_ORIGIN',                 NULL, NULL, 6),
    ('SIMULATION',                   NULL, NULL, 7),
    ('WIRING',                       NULL, NULL, 8),
    ('WORK_REQUEST_HYDRAU_MECA',     NULL, NULL, 9)
ON CONFLICT DO NOTHING;

-- 6b. Sub-items for INSTRUMENTATION_AND_TOOLS
WITH parent AS (
    SELECT id FROM jira_admin.test_mean_defect_origins
     WHERE category = 'INSTRUMENTATION_AND_TOOLS' AND parent_id IS NULL
     LIMIT 1
)
INSERT INTO jira_admin.test_mean_defect_origins (category, sub_item, parent_id, display_order)
SELECT 'INSTRUMENTATION_AND_TOOLS', si.name, parent.id, si.ord
FROM parent
CROSS JOIN (VALUES
    ('ADIS',                     1),
    ('ANETO',                    2),
    ('ARMOS',                    3),
    ('CUB',                      4),
    ('DAMS',                     5),
    ('GCONF',                    6),
    ('GFIB',                     7),
    ('GTA',                      8),
    ('IDEFIX',                   9),
    ('IEV',                     10),
    ('IS_CABINET',              11),
    ('OSCAR',                   12),
    ('OTHER_INSTRUMENTATION',   13),
    ('POWER_SUPPLY',            14),
    ('RAR',                     15),
    ('RAVISU',                  16),
    ('SANDRA',                  17),
    ('SEFRAM',                  18),
    ('SYGAM',                   19),
    ('TMS',                     20),
    ('TOCATA',                  21),
    ('VIP5',                    22)
) AS si(name, ord)
ON CONFLICT DO NOTHING;

-- 6c. Sub-items for SIMULATION
WITH parent AS (
    SELECT id FROM jira_admin.test_mean_defect_origins
     WHERE category = 'SIMULATION' AND parent_id IS NULL
     LIMIT 1
)
INSERT INTO jira_admin.test_mean_defect_origins (category, sub_item, parent_id, display_order)
SELECT 'SIMULATION', si.name, parent.id, si.ord
FROM parent
CROSS JOIN (VALUES
    ('E_EDP',           1),
    ('ELECTRONICS',     2),
    ('ISSM_RSSM_DSSM', 3),
    ('MODELS',          4),
    ('PL_REF_FGL',      5),
    ('RSG',             6),
    ('RSH',             7),
    ('VIP',             8)
) AS si(name, ord)
ON CONFLICT DO NOTHING;

-- ============================================
-- 7. ISSUE TYPES
--    Inserted into jira_issue.issue_types (the central issue-type table).
--    Uses issue_type_key for idempotent ON CONFLICT.
-- ============================================

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color) VALUES
    ('VVO',            'vvo',            'vvo',            'Verification & Validation Objective',           false, 20, '#0052CC'),
    ('HLVVO',          'hlvvo',          'hlvvo',          'High Level Verification & Validation Objective', false, 21, '#0747A6'),
    ('Change Card',    'change-card',    'change-card',    'Change card for design modifications',          false, 22, '#FF991F'),
    ('Design Item',    'design-item',    'design-item',    'Design item entry',                             false, 23, '#00B8D9'),
    ('DCL',            'dcl',            'dcl',            'Design Change Log',                             false, 24, '#36B37E'),
    ('TechEvent',      'techevent',      'techevent',      'Technical event record',                        false, 25, '#6554C0'),
    ('Bench Defect',   'bench-defect',   'bench-defect',   'Defect found during bench testing',             false, 26, '#FF5630'),
    ('Problem Report', 'problem-report', 'problem-report', 'Formal problem report',                        false, 27, '#DE350B'),
    ('Test Request',   'test-request',   'test-request',   'Request for test execution',                    false, 28, '#00875A'),
    ('Deliverable',    'deliverable',    'deliverable',    'A deliverable artifact',                        false, 29, '#253858')
ON CONFLICT DO NOTHING;

-- ============================================
-- 8. ISSUE LINK TYPES
-- ============================================

INSERT INTO jira_issue.issue_link_types (name, inward, outward, style, sequence) VALUES
    ('is parent of',  'is child of',        'is parent of',     'parent',  10),
    ('contains',      'is contained by',    'contains',         'contain', 11),
    ('Change',        'changed by',         'Change',           'change',  12),
    ('Defect',        'defect of',          'Defect',           'defect',  13)
ON CONFLICT DO NOTHING;
