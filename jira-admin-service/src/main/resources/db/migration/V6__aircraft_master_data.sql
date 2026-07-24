-- V6__aircraft_master_data.sql
-- Aircraft Design System Master Data Tables
-- Creates 8 reference/master data tables for aircraft programs,
-- test means, systems, ATA chapters, suppliers, functions, teams, and defect origins.

-- ============================================
-- TRIGGER FUNCTION: auto-update updated_at
-- ============================================

CREATE OR REPLACE FUNCTION jira_admin.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 1. AIRCRAFT PROGRAMS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.aircraft_programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_program_id UUID REFERENCES jira_admin.aircraft_programs(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aircraft_programs_code ON jira_admin.aircraft_programs(code);
CREATE INDEX IF NOT EXISTS idx_aircraft_programs_parent ON jira_admin.aircraft_programs(parent_program_id);
CREATE INDEX IF NOT EXISTS idx_aircraft_programs_active ON jira_admin.aircraft_programs(is_active);

CREATE OR REPLACE TRIGGER trg_aircraft_programs_updated_at
    BEFORE UPDATE ON jira_admin.aircraft_programs
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 2. TEST MEANS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.test_means (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    program_id UUID NOT NULL REFERENCES jira_admin.aircraft_programs(id) ON DELETE CASCADE,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(program_id, code)
);

CREATE INDEX IF NOT EXISTS idx_test_means_code ON jira_admin.test_means(code);
CREATE INDEX IF NOT EXISTS idx_test_means_program ON jira_admin.test_means(program_id);
CREATE INDEX IF NOT EXISTS idx_test_means_category ON jira_admin.test_means(category);
CREATE INDEX IF NOT EXISTS idx_test_means_active ON jira_admin.test_means(is_active);

CREATE OR REPLACE TRIGGER trg_test_means_updated_at
    BEFORE UPDATE ON jira_admin.test_means
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 3. AIRCRAFT SYSTEMS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.aircraft_systems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    program_id UUID NOT NULL REFERENCES jira_admin.aircraft_programs(id) ON DELETE CASCADE,
    ata_chapter_code VARCHAR(10),
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(program_id, code)
);

CREATE INDEX IF NOT EXISTS idx_aircraft_systems_code ON jira_admin.aircraft_systems(code);
CREATE INDEX IF NOT EXISTS idx_aircraft_systems_program ON jira_admin.aircraft_systems(program_id);
CREATE INDEX IF NOT EXISTS idx_aircraft_systems_ata ON jira_admin.aircraft_systems(ata_chapter_code);
CREATE INDEX IF NOT EXISTS idx_aircraft_systems_active ON jira_admin.aircraft_systems(is_active);

CREATE OR REPLACE TRIGGER trg_aircraft_systems_updated_at
    BEFORE UPDATE ON jira_admin.aircraft_systems
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 4. ATA CHAPTERS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.ata_chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_number VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    program_id UUID NOT NULL REFERENCES jira_admin.aircraft_programs(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(program_id, chapter_number)
);

CREATE INDEX IF NOT EXISTS idx_ata_chapters_chapter ON jira_admin.ata_chapters(chapter_number);
CREATE INDEX IF NOT EXISTS idx_ata_chapters_program ON jira_admin.ata_chapters(program_id);
CREATE INDEX IF NOT EXISTS idx_ata_chapters_active ON jira_admin.ata_chapters(is_active);

CREATE OR REPLACE TRIGGER trg_ata_chapters_updated_at
    BEFORE UPDATE ON jira_admin.ata_chapters
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 5. SYSTEM SUPPLIERS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.system_suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    program_id UUID NOT NULL REFERENCES jira_admin.aircraft_programs(id) ON DELETE CASCADE,
    system_id UUID NOT NULL REFERENCES jira_admin.aircraft_systems(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(program_id, system_id, code)
);

CREATE INDEX IF NOT EXISTS idx_system_suppliers_code ON jira_admin.system_suppliers(code);
CREATE INDEX IF NOT EXISTS idx_system_suppliers_program ON jira_admin.system_suppliers(program_id);
CREATE INDEX IF NOT EXISTS idx_system_suppliers_system ON jira_admin.system_suppliers(system_id);
CREATE INDEX IF NOT EXISTS idx_system_suppliers_active ON jira_admin.system_suppliers(is_active);

CREATE OR REPLACE TRIGGER trg_system_suppliers_updated_at
    BEFORE UPDATE ON jira_admin.system_suppliers
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 6. SYSTEM FUNCTIONS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.system_functions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    system_id UUID NOT NULL REFERENCES jira_admin.aircraft_systems(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(system_id, code)
);

CREATE INDEX IF NOT EXISTS idx_system_functions_code ON jira_admin.system_functions(code);
CREATE INDEX IF NOT EXISTS idx_system_functions_system ON jira_admin.system_functions(system_id);
CREATE INDEX IF NOT EXISTS idx_system_functions_active ON jira_admin.system_functions(is_active);

CREATE OR REPLACE TRIGGER trg_system_functions_updated_at
    BEFORE UPDATE ON jira_admin.system_functions
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 7. REPORTER TEAMS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.reporter_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    program_id UUID REFERENCES jira_admin.aircraft_programs(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reporter_teams_code ON jira_admin.reporter_teams(code);
CREATE INDEX IF NOT EXISTS idx_reporter_teams_program ON jira_admin.reporter_teams(program_id);
CREATE INDEX IF NOT EXISTS idx_reporter_teams_active ON jira_admin.reporter_teams(is_active);

CREATE OR REPLACE TRIGGER trg_reporter_teams_updated_at
    BEFORE UPDATE ON jira_admin.reporter_teams
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();

-- ============================================
-- 8. TEST MEAN DEFECT ORIGINS
-- ============================================

CREATE TABLE IF NOT EXISTS jira_admin.test_mean_defect_origins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    sub_item VARCHAR(255),
    parent_id UUID REFERENCES jira_admin.test_mean_defect_origins(id) ON DELETE CASCADE,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_defect_origins_category ON jira_admin.test_mean_defect_origins(category);
CREATE INDEX IF NOT EXISTS idx_defect_origins_parent ON jira_admin.test_mean_defect_origins(parent_id);
CREATE INDEX IF NOT EXISTS idx_defect_origins_active ON jira_admin.test_mean_defect_origins(is_active);

CREATE OR REPLACE TRIGGER trg_defect_origins_updated_at
    BEFORE UPDATE ON jira_admin.test_mean_defect_origins
    FOR EACH ROW EXECUTE FUNCTION jira_admin.set_updated_at();
