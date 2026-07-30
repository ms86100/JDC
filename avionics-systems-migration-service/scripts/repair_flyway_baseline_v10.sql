-- Repair Flyway metadata when jira_migration schema exists but flyway_schema_history is missing.
-- After repair at v10, run: mvn flyway:migrate (applies V11+ only).

CREATE TABLE IF NOT EXISTS jira_migration.flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INT NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);

-- Only baseline if empty (idempotent)
INSERT INTO jira_migration.flyway_schema_history (
    installed_rank, version, description, type, script, checksum,
    installed_by, installed_on, execution_time, success
)
SELECT 1, '10', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL,
       'repair-script', NOW(), 0, true
WHERE NOT EXISTS (SELECT 1 FROM jira_migration.flyway_schema_history);
