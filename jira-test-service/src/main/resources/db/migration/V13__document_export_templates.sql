CREATE TABLE IF NOT EXISTS export_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    template_type VARCHAR(20) NOT NULL DEFAULT 'CSV',
    -- CSV, XLSX, DOCX
    output_format VARCHAR(20) NOT NULL DEFAULT 'CSV',
    -- The generated output format

    -- Template definition
    columns JSONB NOT NULL DEFAULT '[]',
    -- Array of: {"key": "issueKey", "header": "Issue Key", "width": 15}
    header_text TEXT,
    footer_text TEXT,
    group_by VARCHAR(100),
    -- Field to group rows by (e.g., "component" for cluster-based reports)
    sort_by VARCHAR(100),
    sort_direction VARCHAR(4) DEFAULT 'ASC',

    -- Filter
    source_type VARCHAR(30) NOT NULL,
    -- VVO, TECH_EVENT, BENCH_DEFECT, PROBLEM_REPORT, TEST_EXECUTION, TEST_RUN
    filter_jql TEXT,
    -- Optional JQL-like filter

    -- Metadata
    is_system BOOLEAN DEFAULT false,
    created_by UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_export_template_name ON export_template(name);
CREATE INDEX IF NOT EXISTS idx_export_template_type ON export_template(template_type);
CREATE INDEX IF NOT EXISTS idx_export_template_source ON export_template(source_type);

-- Seed 5 system templates matching SYSDOPS documentation
INSERT INTO export_template (name, description, template_type, output_format, source_type, columns, group_by, sort_by, is_system) VALUES
(
    'VVO export for Doors',
    'Export VVOs for DOORS import with standard columns',
    'CSV', 'CSV', 'VVO',
    '[{"key":"issueKey","header":"Issue key"},{"key":"summary","header":"Summary"},{"key":"status","header":"Status"},{"key":"vvoVersion","header":"VVO Version"},{"key":"applicability","header":"Applicability"},{"key":"supplierApplicability","header":"Supplier Applicability"},{"key":"operationalConditions","header":"Operational Conditions"},{"key":"expectedResults","header":"Expected Results"}]',
    null, 'issueKey', true
),
(
    'VVOs coverage from TestPlan',
    'VVO coverage report grouped by component/cluster with linked tests, defects, and test execution status',
    'CSV', 'CSV', 'VVO',
    '[{"key":"issueKey","header":"Requirement"},{"key":"idDoors","header":"ID Doors"},{"key":"status","header":"Status"},{"key":"linkedTests","header":"Linked Tests"},{"key":"linkedDefects","header":"Linked Tests Defects"},{"key":"testExecution","header":"Test Execution"}]',
    'component', 'issueKey', true
),
(
    'Light VVOs coverage from TestPlan',
    'Simplified VVO coverage with only test status per VVO',
    'CSV', 'CSV', 'VVO',
    '[{"key":"issueKey","header":"Requirement"},{"key":"summary","header":"Summary"},{"key":"status","header":"Status"},{"key":"coverageStatus","header":"Coverage"}]',
    'component', 'issueKey', true
),
(
    'TechEvent List from TestPlan',
    'List of TechEvents/defects associated with a test plan',
    'CSV', 'CSV', 'TECH_EVENT',
    '[{"key":"issueKey","header":"Issue Key"},{"key":"summary","header":"Summary"},{"key":"status","header":"Status"},{"key":"defectType","header":"Defect Type"},{"key":"defectOrigin","header":"Defect Origin"},{"key":"defectImpact","header":"Defect Impact"},{"key":"priority","header":"Priority"},{"key":"detectedOnProgramId","header":"Detected On Program"},{"key":"systemSupplierId","header":"System Supplier"}]',
    null, 'issueKey', true
),
(
    'Test Runs Detailed from TestExecution',
    'Detailed test run results from test executions, grouped by cluster/component',
    'CSV', 'CSV', 'TEST_EXECUTION',
    '[{"key":"testKey","header":"Test"},{"key":"testName","header":"Test Name"},{"key":"executionStatus","header":"Status"},{"key":"testEnv","header":"Test Environment"},{"key":"startedAt","header":"Started"},{"key":"finishedAt","header":"Finished"},{"key":"defects","header":"Defects"}]',
    'component', 'testKey', true
)
ON CONFLICT (name) DO NOTHING;
