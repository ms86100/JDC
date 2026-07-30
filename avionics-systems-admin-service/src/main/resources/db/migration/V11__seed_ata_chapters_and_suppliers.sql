-- V11__seed_ata_chapters_and_suppliers.sql
-- Seed ATA chapters and system suppliers per aircraft program.
-- Uses ON CONFLICT DO NOTHING for idempotency.

-- ============================================
-- 1. ATA CHAPTERS (standard aviation chapters, per program)
-- ============================================

WITH programs AS (
    SELECT id FROM jira_admin.aircraft_programs
     WHERE code IN ('SA_CEONEO','SA_NAX','LR_CEONEO','A350','A380')
)
INSERT INTO jira_admin.ata_chapters (chapter_number, title, program_id, display_order)
SELECT ch.num, ch.title, p.id, ch.ord
FROM programs p
CROSS JOIN (VALUES
    ('21', 'Air Conditioning',                          1),
    ('22', 'Auto Flight',                               2),
    ('23', 'Communications',                            3),
    ('24', 'Electrical Power',                          4),
    ('25', 'Equipment / Furnishings',                   5),
    ('26', 'Fire Protection',                           6),
    ('27', 'Flight Controls',                           7),
    ('28', 'Fuel',                                      8),
    ('29', 'Hydraulic Power',                           9),
    ('30', 'Ice and Rain Protection',                  10),
    ('31', 'Indicating / Recording Systems',           11),
    ('32', 'Landing Gear',                             12),
    ('33', 'Lights',                                   13),
    ('34', 'Navigation',                               14),
    ('35', 'Oxygen',                                   15),
    ('36', 'Pneumatic',                                16),
    ('38', 'Water / Waste',                            17),
    ('42', 'Integrated Modular Avionics',              18),
    ('44', 'Cabin Systems',                            19),
    ('45', 'Central Maintenance System',               20),
    ('46', 'Information Systems',                      21),
    ('49', 'Auxiliary Power Unit',                      22),
    ('52', 'Doors',                                    23),
    ('71', 'Power Plant',                              24),
    ('73', 'Engine Fuel and Control',                  25),
    ('76', 'Engine Controls',                          26),
    ('77', 'Engine Indicating',                        27),
    ('78', 'Exhaust',                                  28),
    ('79', 'Oil',                                      29),
    ('80', 'Starting',                                 30)
) AS ch(num, title, ord)
ON CONFLICT (program_id, chapter_number) DO NOTHING;

-- ============================================
-- 2. SYSTEM SUPPLIERS (avionics/system suppliers, linked to systems per program)
-- ============================================

WITH sys_data AS (
    SELECT s.id as system_id, s.program_id, s.code as sys_code
    FROM jira_admin.aircraft_systems s
)
INSERT INTO jira_admin.system_suppliers (code, name, program_id, system_id, display_order)
SELECT sup.code, sup.name, sd.program_id, sd.system_id, sup.ord
FROM sys_data sd
JOIN (VALUES
    ('FCS',   'THALES',         'Thales Avionics',              1),
    ('FMS',   'THALES',         'Thales Avionics',              2),
    ('DIS',   'THALES',         'Thales Avionics',              3),
    ('AP',    'HONEYWELL',      'Honeywell Aerospace',          4),
    ('ADIRS', 'HONEYWELL',      'Honeywell Aerospace',          5),
    ('FWS',   'HONEYWELL',      'Honeywell Aerospace',          6),
    ('FADEC', 'SAFRAN',         'Safran Electronics & Defense', 7),
    ('EPDS',  'SAFRAN',         'Safran Electronics & Defense', 8),
    ('AMS',   'COLLINS',        'Collins Aerospace',            9),
    ('CMS',   'COLLINS',        'Collins Aerospace',           10),
    ('AFDX',  'AIRBUS',         'Airbus Defence & Space',      11),
    ('ACR',   'AIRBUS',         'Airbus Defence & Space',      12),
    ('FDR',   'L3HARRIS',       'L3Harris Technologies',       13),
    ('ATC',   'TRANSPONDER_CO', 'Transponder Technologies',    14),
    ('AGS',   'LIEBHERR',       'Liebherr-Aerospace',          15),
    ('CPCS',  'LIEBHERR',       'Liebherr-Aerospace',          16),
    ('CCS',   'DIEHL',          'Diehl Aerospace',             17),
    ('CLK',   'THALES',         'Thales Avionics',             18),
    ('DLS',   'TELEDYNE',       'Teledyne Technologies',       19),
    ('CP',    'ESTERLINE',      'Esterline Technologies',      20)
) AS sup(sys_code, code, name, ord) ON sd.sys_code = sup.sys_code
ON CONFLICT DO NOTHING;
