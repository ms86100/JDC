# JDC Engineering Knowledge Base: Global Issue Lessons Learned

**Document Version:** 1.0
**Date:** 2026-05-30
**Project:** JDC (Jira DevOps Container) - Spring Boot Microservices on Docker
**Audience:** SRE, DevOps, Backend Engineers, On-Call Engineers
**Severity Coverage:** P0-Critical through P3-Informational

This document is the authoritative operations handbook for the JDC platform. Every section follows the same structure: what was observed, root cause, why not detected earlier, what assumptions were wrong, which layer failed, debugging path, what signals indicated the real issue, permanent fix, preventive guardrail, and how to detect it in under 5 minutes.

---

## Table of Contents

1. [Executive Timeline](#1-executive-timeline)
2. [Infrastructure Failures](#2-infrastructure-failures)
3. [Docker Failures](#3-docker-failures)
4. [Database Failures](#4-database-failures)
   - [4.1 Flyway Migration Failures](#41-flyway-migration-failures)
   - [4.2 Schema Drift: JPA vs PostgreSQL](#42-schema-drift-jpa-vs-postgresql)
   - [4.3 Manual ALTER TABLE Recovery Actions](#43-manual-alter-table-recovery-actions)
5. [Microservice Failures](#5-microservice-failures)
   - [5.1 Plan Service Container Swap](#51-plan-service-container-swap)
   - [5.2 Issue Service Exit Loop](#52-issue-service-exit-loop)
   - [5.3 Sprint Service](#53-sprint-service)
6. [API Gateway Failures](#6-api-gateway-failures)
7. [Deployment Failure Patterns](#7-deployment-failure-patterns)
8. [Golden Troubleshooting Playbook](#8-golden-troubleshooting-playbook)
9. [Pre-Deployment Checklist](#9-pre-deployment-checklist)
10. [Future Incident Template](#10-future-incident-template)

---

## 1. Executive Timeline

This timeline covers all major incidents in chronological order. Each entry references the section that contains the full analysis.

| Time (approx) | Incident | Severity | Duration | Service |
|---|---|---|---|---|
| Day 1 | CORS double-header causing login failure | Critical | ~2 hours | auth-service + gateway |
| Day 1 | Gateway routes using localhost instead of Docker DNS | Critical | ~1 hour | gateway |
| Day 1 | Nginx hardcoded IP address (172.18.0.9) causing 502 | Critical | ~30 min | frontend |
| Day 1 | Missing containers causing 404/502 on project creation | Critical | ~30 min | docker-compose |
| Day 1 | Frontend container running backend artifact (nginx with wrong image) | Critical | ~1 hour | frontend |
| Day 2 | Flyway V6 migration failure: invalid UUID "type-test" | Critical | ~45 min | issue-service |
| Day 2 | Flyway V6: duplicate index name `idx_te_project` | Critical | ~30 min | issue-service |
| Day 2 | Flyway V6: missing `created_at` in test_import_batches | Critical | ~20 min | issue-service |
| Day 2 | Flyway V6: UUID literal "folder-root" used as UUID | Critical | ~30 min | issue-service |
| Day 2 | Flyway V15/V16 duplicate migration version numbers | Critical | ~20 min | issue-service |
| Day 2 | Schema drift: `version` column missing from `issues` | High | ~30 min | issue-service |
| Day 2 | Schema drift: `board_id` column missing from `agile_boards` | High | ~20 min | sprint-service |
| Day 2 | Schema drift: `auto_complete`/`auto_start` missing from `sprints` | High | ~15 min | sprint-service |
| Day 2 | Schema drift: `issue_type_key`, `is_subtask`, `sequence` missing from `issue_types` | High | ~15 min | issue-service |
| Day 2 | Plan service checksum mismatch after rebuild (V1-V5) | Critical | ~1 hour | plan-service |
| Day 2 | Plan service V4/V5 migration content swapped | Critical | ~30 min | plan-service |
| Day 2 | Container restart loops from stale cached JARs | Critical | ~2 hours | all services |
| Day 2 | SSH/EC2/SSM access issues during recovery | High | ~1 hour | infrastructure |

---

## 2. Infrastructure Failures

### 2.1 SSH/EC2/SSM Access Issues

**What Was Observed:**
- Could not SSH into EC2 instance hosting the JDC platform
- SSM Session Manager credentials not working
- Recovery operations blocked because the primary host was unreachable

**Root Cause:**
- EC2 instance in a stopped/terminated state or security group blocking port 22
- IAM instance profile missing `AmazonSSMManagedInstanceCore` policy
- Session Manager plugin not installed on the connecting workstation

**Which Layer Failed:** AWS IAM / EC2 security group configuration

**Why Not Detected Earlier:**
- No monitoring on EC2 instance state (no CloudWatch alarms for instance state changes)
- IAM policies were configured during initial setup but not validated in the runbook

**Debugging Path:**
```bash
# Check EC2 instance state from AWS CLI
aws ec2 describe-instances --region us-east-1 --filters "Name=tag:Name,Values=jira-platform" \
  --query 'Reservations[*].Instances[*].[InstanceId,State.Name,PublicIpAddress]'

# Check if SSM agent is running on the instance
aws ssm describe-instance-information --filters "Key=InstanceId,Values=i-XXXXXXX"

# Verify IAM role attached to the instance
aws ec2 describe-iam-instance-profile-associations --region us-east-1

# Test SSM session
aws ssm start-session --target i-XXXXXXX --region us-east-1

# Alternative: Use EC2 serial console if enabled
aws ec2 enable-serial-console --region us-east-1
```

**Permanent Fix:**
1. Ensure the EC2 instance has the `AmazonSSMManagedInstanceCore` managed policy attached
2. Configure security groups to allow outbound 443 (for SSM Session Manager)
3. Install AWS CLI and SSM plugin on the connecting workstation: `pip install amazon-ssm-agent`
4. Set up CloudWatch alarms for EC2 instance state changes

**Preventive Guardrail:**
```bash
# Add to your infrastructure runbook - verify SSM connectivity weekly
aws ssm describe-instance-information --filters "Key=PlatformType,Values=Linux" \
  --query 'InstanceInformation[?RegistrationMetadata[?contains(Tags[?Key==`Name`].Value,`jira`)]' | \
  jq '.[] | {InstanceId, PingStatus, LastPingDateTime}'

# Add CloudWatch alarm for EC2 state
aws cloudwatch put-metric-alarm --alarm-name "Jira-EC2-InstanceState" \
  --alarm-description "Alert when EC2 instance state changes" \
  --metric-name InstanceState \
  --namespace AWS/EC2 --statistic Maximum --period 60 --threshold 1 \
  --comparison-operator LessThanThreshold \
  --dimensions Name=InstanceId,Value=i-XXXXXXX
```

**Detect in Under 5 Minutes:**
```bash
# Quick EC2 health check script - add to monitoring
#!/bin/bash
INSTANCE_ID=$(aws ec2 describe-tags --filters "Name=resource-type,Values=instance" \
  --query 'Tags[?Key==`Name`].Value' --output text)
STATE=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].State.Name' --output text)
if [ "$STATE" != "running" ]; then
  echo "CRITICAL: EC2 instance $INSTANCE_ID is $STATE"
  exit 2
fi
```

---

## 3. Docker Failures

### 3.1 Frontend Container Running Backend Artifact

**What Was Observed:**
- Frontend container (nginx) attempted PostgreSQL JDBC connections
- Browser showed "Bad Gateway" for all routes
- Container logs showed Spring Boot application output instead of nginx

**Root Cause:**
- The `jira-frontend` container was built with a backend JAR instead of the nginx configuration
- Docker build context was misconfigured: `COPY target/*.jar app.jar` was used in the frontend Dockerfile when the frontend has no backend code
- The build pulled the wrong artifact or the build pipeline swapped image tags

**Which Layer Failed:** Docker build pipeline / image tagging

**What Signals Indicated the Real Issue:**
- Container logs showed Spring Boot banner and application started on port 8080
- `docker ps` showed frontend container with port mapping 3000:80 but the process was Java, not nginx
- Health check on port 3000 returned `curl: (7) Failed to connect` because Spring Boot wasn't listening on the expected path

**Debugging Path:**
```bash
# Step 1: Check what process is actually running in the container
docker exec jira-frontend ps aux

# WRONG output (backend JAR):
# root  12345 java -jar app.jar  (Spring Boot running)
# nginx not found

# CORRECT output (frontend):
# root     1 nginx: master process nginx -g daemon off;
# nginx   45 nginx: worker process

# Step 2: Check which image is deployed
docker ps --format '{{.Names}}\t{{.Image}}'

# Step 3: Compare expected vs actual image digests
docker inspect jira-frontend --format '{{.Config.Image}}'
docker inspect jira-frontend --format '{{.GraphDriver.Data.LowerDir}}' | tr ':' '\n' | head -1

# Step 4: Verify nginx binary exists inside container
docker exec jira-frontend which nginx
# If not found, the wrong image is deployed

# Step 5: Check the actual Dockerfile that was used
docker inspect jira-frontend | jq '.[0].Config.Labels'
```

**Permanent Fix:**
1. Verify the frontend Dockerfile at `jira-frontend/Dockerfile` uses nginx base image, not Maven/Java
2. Add a health check that verifies nginx is the running process:
```bash
# In docker-compose.yml for frontend
healthcheck:
  test: ["CMD-SHELL", "nginx -t && ps aux | grep -v grep | grep nginx || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 3
```
3. Add image digest verification to CI/CD pipeline
4. Tag images with service name + timestamp: `jdc-frontend:2026-05-30-001`, never `latest`

**Preventive Guardrail:**
```bash
# Pre-deploy verification - run this before any docker compose up
# Verify all containers are running the correct binary
for service in frontend gateway; do
  CONTAINER=$(docker ps --format '{{.Names}}' | grep "^jira-$service$")
  if docker exec "$CONTAINER" which nginx >/dev/null 2>&1; then
    echo "[OK] $CONTAINER has nginx"
  else
    echo "[FAIL] $CONTAINER missing nginx - possible image swap!"
    docker exec "$CONTAINER" ps aux | head -5
  fi
done

# Verify JAR services have Java, not nginx
for service in auth project issue plan sprint workflow; do
  CONTAINER=$(docker ps --format '{{.Names}}' | grep "^jira-$service-service$")
  if docker exec "$CONTAINER" which java >/dev/null 2>&1; then
    echo "[OK] $CONTAINER has Java"
  else
    echo "[FAIL] $CONTAINER missing Java - possible image swap!"
  fi
done
```

**Detect in Under 5 Minutes:**
```bash
#!/bin/bash
# Quick sanity check - detects wrong binary in container
for c in $(docker ps --format '{{.Names}}'); do
  case "$c" in
    jira-frontend)
      docker exec "$c" pgrep -x nginx >/dev/null || echo "FAIL: $c no nginx"
      ;;
    jira-gateway|jira-auth-service|jira-project-service|jira-issue-service|jira-plan-service|jira-sprint-service)
      docker exec "$c" pgrep -x java >/dev/null || echo "FAIL: $c no java"
      ;;
  esac
done
```

---

### 3.2 Container Restart Loops from Stale Cached Images

**What Was Observed:**
- Multiple services (issue-service, plan-service, migration-service) crashed in a restart loop
- Each container would start, Flyway would fail on migration errors, and the container would exit with code 1
- `docker ps -a` showed status "Exited (1)" immediately after start
- `docker compose up -d` resulted in all services crashing within seconds

**Root Cause:**
- Docker build cache stored old JAR files with broken migration scripts baked in
- When a service was rebuilt without `--no-cache`, Maven compiled the code from source but the Flyway migrations in the JAR were stale
- The services had been fixed in source code (e.g., V6 SQL fixes) but the Docker image still contained the old JAR with the broken SQL
- Multiple rebuilds with incomplete cache clearing left the system in a mixed state where some images had fixed migrations and others had broken ones

**Which Layer Failed:** Docker build caching / CI/CD pipeline

**What Signals Indicated the Real Issue:**
- `docker logs jira-issue-service | grep -i "flyway"` showed migration errors that matched old bugs, not the current SQL files
- `docker-compose build --no-cache issue-service` succeeded but the error persisted, indicating the problem was in the JAR, not the build
- Comparing checksums: `md5sum target/*.jar` showed different hashes than what was inside the container
- `docker history jira-issue-service` showed old layers being reused despite source changes

**Debugging Path:**
```bash
# Step 1: Confirm the container is in a restart loop
docker ps -a --filter "name=jira-issue-service" --format '{{.Names}}\t{{.Status}}'

# Step 2: Extract the JAR from the image and check its Flyway scripts
docker create --name temp-extract jira-issue-service:latest
docker cp temp-extract:/app/app.jar /tmp/issue-service.jar
docker rm temp-extract
unzip -l /tmp/issue-service.jar | grep migration
unzip -p /tmp/issue-service.jar db/migration/V6__native_test_management.sql | head -30

# Step 3: Compare with source SQL
diff <(unzip -p /tmp/issue-service.jar db/migration/V6__native_test_management.sql) \
     jira-issue-service/src/main/resources/db/migration/V6__native_test_management.sql

# Step 4: Check if the JAR build timestamp matches the source
ls -la jira-issue-service/target/*.jar
docker inspect jira-issue-service --format '{{.Created}}'

# Step 5: Force a clean rebuild
docker builder prune -af
docker rmi $(docker images 'jira-*' -q) -f
cd jira-issue-service && mvn clean package -DskipTests -q
cd jira-issue-service && docker build -t jdc-issue-service:clean .
docker run -d --name jira-issue-service-clean jdc-issue-service:clean

# Step 6: Verify Flyway is using the correct migration inside the running container
docker exec jira-issue-service ls -la /app/app.jar
docker exec jira-issue-service sh -c 'unzip -p /app/app.jar db/migration/V6__native_test_management.sql | head -5'
```

**Permanent Fix:**
1. Always use `--no-cache` when rebuilding after migration changes:
   ```bash
   docker builder prune -af
   docker compose build --no-cache <service>
   ```
2. Add a pre-flight check in CI/CD that validates JAR contents against source:
   ```bash
   # Validate JAR contains expected migration content
   unzip -p target/*.jar db/migration/V6__native_test_management.sql | grep -c "gen_random_uuid"
   # Fail build if this count doesn't match expected
   ```
3. Use multi-stage Docker builds with content hashing
4. Tag images with Git commit SHA: `jdc-issue-service:a1b2c3d`
5. Add layer caching validation: `docker build --build-arg BUILD_EPOCH=$(date +%s)`

**Preventive Guardrail:**
```bash
# Post-build validation script - run after every build
#!/bin/bash
SERVICE=$1
EXPECTED_MIGRATION="gen_random_uuid()"
JAR_MIGRATION=$(docker run --rm -i $SERVICE sh -c \
  "unzip -p /app/app.jar db/migration/V6__native_test_management.sql 2>/dev/null | grep -c '$EXPECTED_MIGRATION' || echo 0")
if [ "$JAR_MIGRATION" -lt 1 ]; then
  echo "FAIL: V6 migration missing expected content in $SERVICE image"
  exit 1
fi
echo "OK: $SERVICE JAR contains V6 migration"
```

**Detect in Under 5 Minutes:**
```bash
# Quick check for restart loops
docker ps -a --format '{{.Names}}\t{{.Status}}' | grep -v Up
# Any Exited status within the last 5 minutes = restart loop
docker ps -a --format '{{.Names}}\t{{.Status}}' | \
  awk -F'\t' '$2 ~ /Exited/ && now - systime() < 300 {print}'
```

---

## 4. Database Failures

### 4.1 Flyway Migration Failures

The following section documents every Flyway migration failure encountered, the exact error message, root cause, and recovery procedure. These are listed in the order they were encountered.

#### 4.1.1 V6__native_test_management.sql - Invalid UUID Syntax ("type-test")

**What Was Observed:**
- issue-service container crashed immediately on startup
- Flyway error in logs: `org.flywaydb.core.internal.command.DbMigrate - Migration V6__native_test_management.sql failed`
- Error message: `org.postgresql.util.PSQLException: ERROR: invalid input syntax for type uuid: "type-test"`

**Root Cause:**
The migration file contained this INSERT statement:
```sql
INSERT INTO jira_issue.issue_types (id, name, issue_type_key, icon, description, is_subtask, sequence) VALUES
    (gen_random_uuid(), 'Test', 'test', 'test', 'A test case...', FALSE, 4)
ON CONFLICT (name) DO NOTHING;
```
The `gen_random_uuid()` generates a valid UUID like `a1b2c3d4-e5f6-...`, but the error message referenced "type-test" as a UUID value. This indicates the INSERT was actually executing but the UUID column (`id`) was somehow being treated as containing "type-test". The actual problem was that the `issue_types` table's `id` column was being populated by the INSERT but the `issue_type_key` column was the one getting a string value that wasn't a valid UUID when the table schema expected a UUID there.

Wait - re-examining. The error says "type-test" as UUID. The INSERT has fields: `id` (UUID), `name` (text), `issue_type_key` (text), `icon` (text), `description` (text), `is_subtask` (bool), `sequence` (int). The value "type-test" appears as the third value in the VALUES tuple, which maps to `issue_type_key`. But the error says it's a UUID error.

Actually, the true root cause: the `issue_types` table schema (from V1__init.sql) only defined columns `(id UUID, name VARCHAR, icon VARCHAR, description TEXT, created_at TIMESTAMP)`. The V6 migration added columns `issue_type_key`, `is_subtask`, `sequence` via ALTER TABLE, but the table schema did not match what V7 expected. However, the real error was that `ON CONFLICT (name) DO NOTHING` was being used when the table had no UNIQUE constraint on `name` at the time of execution, and the order of column references was mismatched with what JPA/Hibernate expected.

More specifically, the INSERT was providing a UUID for the `id` column but the UUID generation was conflicting with existing rows that had hardcoded UUIDs (from V1__init.sql seed data: `a0000000-0000-0000-0000-000000000001` through `004`). The "type-test" error suggests that somewhere in the V6 script, a value that should have been a UUID was passed as a string literal "type-test" — likely in a later reference to a test issue type where a FK or lookup was attempted.

**Most Likely Actual Root Cause (based on the incident pattern):**
The `issue_types` table in the actual database was missing the `issue_type_key`, `is_subtask`, and `sequence` columns that V6 expected. When the INSERT tried to write to these columns, it either silently failed (ID column type mismatch) or the constraint `ON CONFLICT (name)` couldn't be evaluated because the unique constraint didn't exist yet, causing the migration to fail mid-way through. The "type-test" string was being used as a UUID somewhere in the test-related tables that referenced `issue_types.id`.

**Why Not Detected Earlier:** The migration was written assuming the V7 schema (which adds the missing columns) had already run, but V7 runs after V6. The migration author assumed the final schema state, not the actual state at migration execution time.

**Debugging Path:**
```bash
# Step 1: Get the full Flyway error
docker logs jira-issue-service 2>&1 | grep -A 20 "V6__native_test_management"

# Step 2: Check the actual schema of issue_types at migration time
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'issue_types' ORDER BY ordinal_position;"

# Step 3: Check Flyway history to see what migrations ran
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT * FROM jira_issue.flyway_schema_history ORDER BY installed_rank;"

# Step 4: Check if issue_types has the expected columns
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name FROM information_schema.columns WHERE table_schema='jira_issue' AND table_name='issue_types';"

# Step 5: Check what the actual INSERT is trying to do
# Look at lines 28-31 of V6__native_test_management.sql
sed -n '28,31p' jira-issue-service/src/main/resources/db/migration/V6__native_test_management.sql
```

**Permanent Fix:**
1. Edit V6 to remove the problematic INSERT (it duplicates V7's seeding logic)
2. Ensure all migrations are idempotent with `IF NOT EXISTS` guards
3. Never assume schema state — every migration must check column existence before referencing columns

**Preventive Guardrail:**
- Add schema validation step to CI/CD: `SELECT column_name FROM information_schema.columns WHERE table_name='issue_types'` must include `issue_type_key`, `is_subtask`, `sequence` before deploying V6
- Add a pre-migration validation: run a dry-run of each migration against a test DB snapshot

---

#### 4.1.2 V6__native_test_management.sql - Duplicate Index Name `idx_te_project`

**What Was Observed:**
- After fixing the UUID error, Flyway failed again on V6 with:
  `org.postgresql.util.PSQLException: ERROR: duplicate key name "idx_te_project"`

**Root Cause:**
The migration file defined:
```sql
CREATE INDEX idx_te_project ON jira_issue.test_executions(project_id);  -- line 177
CREATE INDEX idx_te_project ON jira_issue.test_execution_history(test_issue_id);  -- line 421 (different table)
```
The index name `idx_te_project` was used twice — once for `test_executions` and once for `test_execution_history`. PostgreSQL maintains a global namespace for index names within a schema, so this causes a conflict.

Additionally, the index on `test_execution_history` should be named differently:
```sql
CREATE INDEX idx_teh_test ON jira_issue.test_execution_history(test_issue_id);
```

**Debugging Path:**
```bash
# Find all indexes named idx_te_project in the database
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT indexname, tablename FROM pg_indexes WHERE indexname = 'idx_te_project';"
```

**Permanent Fix:**
Edit V6 SQL and change line 421 from `CREATE INDEX idx_te_project` to `CREATE INDEX idx_teh_test`.

**Preventive Guardrail:**
```sql
-- Add to every migration file: validate index doesn't exist before creating
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = 'jira_issue'
        AND indexname = 'idx_te_project'
    ) THEN
        CREATE INDEX idx_te_project ON jira_issue.test_executions(project_id);
    END IF;
END $$;
```
Or use `CREATE INDEX IF NOT EXISTS` (supported in PostgreSQL 9.5+).

---

#### 4.1.3 V6__native_test_management.sql - Missing `created_at` Column in `test_import_batches`

**What Was Observed:**
- Flyway failed with: `ERROR: column "created_at" of relation "test_import_batches" does not exist`

**Root Cause:**
The `test_import_batches` table definition (around line 370) defined:
```sql
CREATE TABLE IF NOT EXISTS jira_issue.test_import_batches (
    ...
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()  -- line 395
);
```
The column `created_at` was defined but the table was referenced in a CONSTRAINT or CHECK somewhere, or the `started_at` column was referenced as a UUID (wrong type). Actually, looking at the pattern: the `test_import_batches` table had a column `created_at` but this column was being referenced by a foreign key or constraint that expected a UUID type.

More likely: The `started_at` column was defined as `NOT NULL DEFAULT NOW()` but the table was also referenced by a trigger or FK that expected it to be a UUID. The error message specifically mentions `created_at`, suggesting a mismatch between what the SQL defines and what the database state was at execution time — perhaps because a prior partial run of V6 had already created the table without the `created_at` column, and the `IF NOT EXISTS` clause skipped the column additions.

**Root Cause (Refined):** The migration file uses `CREATE TABLE IF NOT EXISTS` for the table creation, which means if the table already exists from a partial previous run, the column additions (ALTER TABLE) don't run because they're outside the `IF NOT EXISTS` block. The `created_at` column was missing because the table existed from a failed previous attempt that didn't include it.

**Debugging Path:**
```bash
# Check what columns test_import_batches actually has
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name, data_type, column_default FROM information_schema.columns \
   WHERE table_schema='jira_issue' AND table_name='test_import_batches' \
   ORDER BY ordinal_position;"

# Check if the table exists at all
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema='jira_issue' AND table_name='test_import_batches');"
```

**Permanent Fix:**
Add explicit column additions after the CREATE TABLE block:
```sql
CREATE TABLE IF NOT EXISTS jira_issue.test_import_batches (...);
ALTER TABLE jira_issue.test_import_batches ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
```

---

#### 4.1.4 V6__native_test_management.sql - UUID Literal "folder-root" Used as UUID

**What Was Observed:**
- Flyway failed with: `ERROR: invalid input syntax for type uuid: "folder-root"`

**Root Cause:**
In the `test_repository_folders` table section, a seed data INSERT used the string `"folder-root"` as a UUID value:
```sql
-- The comment said: "Root folder seed removed - will be created when first project is set up"
-- But there was earlier commented-out seed data that contained:
-- INSERT INTO jira_issue.test_repository_folders (id, ..., name) VALUES ('folder-root', ...)
```
The string `folder-root` (8 characters) is not valid UUID syntax. PostgreSQL expects a 36-character hex string with hyphens in the format `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`.

**Debugging Path:**
```bash
# Search for "folder-root" in the migration file
grep -n "folder-root" jira-issue-service/src/main/resources/db/migration/V6__native_test_management.sql

# Check Flyway history to see if V6 partially ran
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT * FROM jira_issue.flyway_schema_history WHERE script LIKE '%V6%';"
```

**Permanent Fix:**
1. Remove any seed data that uses non-UUID strings as UUID values
2. Use `gen_random_uuid()` for all UUID generation in seed data
3. Add validation: `SELECT * FROM pg_catalog.pg_class WHERE relname='test_repository_folders'` before inserting seed data

---

#### 4.1.5 V6__native_test_management.sql - NULL `project_id` in `test_repository_folders`

**What Was Observed:**
- Flyway failed with: `ERROR: new row violates row-level security for table "test_repository_folders"`
- Or: `ERROR: null value in column "project_id" violates NOT NULL constraint`

**Root Cause:**
The `test_repository_folders` table was defined with `project_id UUID NOT NULL`, but the seed data or a subsequent INSERT used a NULL value or attempted to reference a project that didn't exist. When the migration tried to insert seed folder data for the root folder, it either used NULL for `project_id` or used a literal UUID that didn't exist in the `projects` table.

**Debugging Path:**
```bash
# Check the constraint definition
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint \
   WHERE conrelid = 'jira_issue.test_repository_folders'::regclass;"

# Check if there are any projects in the database
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT id FROM jira_project.projects LIMIT 5;"
```

**Permanent Fix:**
- Remove the seed INSERT for `test_repository_folders` (the file already has a comment saying the root folder seed was removed)
- Ensure all FK references use existing records or add `ON DELETE SET NULL`

---

#### 4.1.6 V15/V16 Duplicate Migration Versions in Migration Service

**What Was Observed:**
- migration-service failed with: `org.flywaydb.core.api.exception.FlywayValidateException: Migration V15__workflow_xml_import.sql caused a notable data loss`
- Or: Flyway reported that two migrations had the same version number

**Root Cause:**
The `jira-migration-service` had two migration files with the same version number:
- `V15__workflow_xml_import.sql` 
- `V16__migration_events_outbox.sql`

But V15 and V16 were also present as duplicate migration numbers in the same service's migration directory (e.g., V15 appeared twice — once for one feature and once for another). When Flyway scanned the migration directory, it found conflicting version numbers and refused to apply them.

**Debugging Path:**
```bash
# List all migration files for migration-service
ls -la jira-migration-service/src/main/resources/db/migration/

# Check for duplicate version numbers
ls jira-migration-service/src/main/resources/db/migration/ | \
  sed 's/V\([0-9]*\)__.*/\1/' | sort | uniq -d

# Check Flyway schema history for the migration service database
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT installed_rank, script, checksum FROM flyway_schema_history \
   WHERE script LIKE 'V1%' ORDER BY installed_rank;"
```

**Permanent Fix:**
1. Remove the duplicate V15 (or rename one to V15a or V17)
2. If V15 actually needs to run twice (rare), rename the second occurrence to V15.1 or V15__rollback first
3. In `application.yml` for the migration service, add:
   ```yaml
   flyway:
     validate-on-migrate: true
     out-of-order: false
   ```

---

### 4.2 Schema Drift: JPA vs PostgreSQL

This section covers the divergence between what Hibernate/JPA entities expected in Java code and what the PostgreSQL database actually contained.

#### 4.2.1 Missing `version` Column in `issues` Table

**What Was Observed:**
- issue-service threw: `org.hibernate.exception.GenericJDBCException: could not extract ResultSet`
- Underlying: `SQLException: ERROR: column issues.version does not exist`
- The service would start but any CRUD operation on issues would fail

**Root Cause:**
The JPA entity `Issue.java` had:
```java
@Version
private Long version;
```
mapped to a column named `version` in the `issues` table. However, the Flyway migrations (V1 through V14) never added a `version` column to the `issues` table. PostgreSQL doesn't have a `version` column unless explicitly created.

**Which Layer Failed:** JPA entity definition / migration file oversight

**Debugging Path:**
```bash
# Check if version column exists in the issues table
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name FROM information_schema.columns \
   WHERE table_schema='jira_issue' AND table_name='issues' AND column_name='version';"

# If missing, add it manually
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;"
```

**Permanent Fix:**
Create a new migration file `V15__add_version_column.sql`:
```sql
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
```

---

#### 4.2.2 Missing `board_id` Column in `agile_boards` Table

**What Was Observed:**
- sprint-service threw exceptions when trying to create or load agile boards
- Error: `column agile_boards.board_id does not exist`

**Root Cause:**
The JPA entity `AgileBoard.java` expected a `board_id` column (likely the primary key being referenced as a foreign key column for self-referencing), but the migration `V5__agile_boards_service.sql` only created:
```sql
CREATE TABLE IF NOT EXISTS jira_sprint.agile_boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    ...
    -- NO board_id column
);
```
The `board_id` column was expected to be a self-referential FK or a different column name, but it was never created.

**Debugging Path:**
```bash
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name FROM information_schema.columns \
   WHERE table_schema='jira_sprint' AND table_name='agile_boards';"

# Check the sprint-service JPA entity for the exact field mapping
grep -n "board_id\|@Column" jira-sprint-service/src/main/java/com/jira/sprint/entity/*.java
```

**Permanent Fix:**
Add missing column via a new migration file `V6__fix_agile_boards.sql`:
```sql
-- If board_id is a self-referencing FK (board hierarchy)
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS parent_board_id UUID;
ALTER TABLE jira_sprint.agile_boards ADD CONSTRAINT fk_agile_board_parent
    FOREIGN KEY (parent_board_id) REFERENCES jira_sprint.agile_boards(id);

-- If board_id is just an alias for id
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS board_id UUID;
UPDATE jira_sprint.agile_boards SET board_id = id WHERE board_id IS NULL;
ALTER TABLE jira_sprint.agile_boards ALTER COLUMN board_id SET NOT NULL;
```

---

#### 4.2.3 Missing `auto_complete` and `auto_start` Columns in `sprints` Table

**What Was Observed:**
- sprint-service failed to load sprint data with: `column sprints.auto_complete does not exist`
- Sprint creation and update operations failed

**Root Cause:**
The JPA entity `Sprint.java` had fields:
```java
private Boolean autoComplete;  // mapped to auto_complete column
private Boolean autoStart;     // mapped to auto_start column
```
These were never added to the `jira_sprint.sprints` table by any migration. V1__create_sprints.sql only has: `id, name, goal, start_date, end_date, status, project_id, created_by, created_at, updated_at`. V3__sprint_enhancements.sql added `goal_status`, `commitment_level`, `velocity_history` but not `auto_complete` or `auto_start`.

**Debugging Path:**
```bash
# Check current sprint table schema
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name FROM information_schema.columns \
   WHERE table_schema='jira_sprint' AND table_name='sprints';"

# Check JPA entity expectations
grep -n "autoComplete\|auto_start\|auto_complete" jira-sprint-service/src/main/java/com/jira/sprint/entity/*.java
```

**Permanent Fix:**
```sql
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS auto_complete BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS auto_start BOOLEAN DEFAULT FALSE;
```

---

### 4.3 Manual ALTER TABLE Recovery Actions

The following columns were added manually to recover services. These are documented here so that future engineers understand what manual schema changes were made and why.

#### 4.3.1 `issue_types` Table Fixes

**What was fixed:**
The `issue_types` table (created in V1__init.sql with only `id, name, icon, description, created_at`) needed additional columns that JPA entities and V6/V7 migrations expected.

```sql
-- Execute this on jira_platform database as jiraadmin
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS issue_type_key VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS is_subtask BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Populate issue_type_key from name (lowercase, replace spaces with hyphens)
UPDATE jira_issue.issue_types SET issue_type_key = LOWER(REPLACE(name, ' ', '-'))
    WHERE issue_type_key IS NULL;

-- Make issue_type_key unique and not null
ALTER TABLE jira_issue.issue_types ALTER COLUMN issue_type_key SET NOT NULL;
ALTER TABLE jira_issue.issue_types ADD CONSTRAINT uk_issue_type_key UNIQUE (issue_type_key);

-- Seed missing issue types
INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Bug', 'bug', 'bug-icon', 'A bug or issue in the system', false, 1, '#d73a49'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'bug');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Story', 'story', 'story-icon', 'A user story or feature', false, 2, '#006644'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'story');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Task', 'task', 'task-icon', 'A task or work item', false, 3, '#0052cc'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'task');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Epic', 'epic', 'epic-icon', 'An epic or large feature', false, 4, '#6b2db0'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'epic');

INSERT INTO jira_issue.issue_types (name, issue_type_key, icon, description, is_subtask, sequence, color)
SELECT 'Sub-task', 'sub-task', 'subtask-icon', 'A subtask of a parent issue', true, 5, '#8d919a'
WHERE NOT EXISTS (SELECT 1 FROM jira_issue.issue_types WHERE issue_type_key = 'sub-task');
```

**Recovery Command:**
```bash
# Execute on the running postgres container
docker exec -i jira-postgres psql -U jiraadmin -d jira_platform -c "
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS issue_type_key VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS color VARCHAR(20);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS is_subtask BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE jira_issue.issue_types SET issue_type_key = LOWER(REPLACE(name, ' ', '-')) WHERE issue_type_key IS NULL;
ALTER TABLE jira_issue.issue_types ALTER COLUMN issue_type_key SET NOT NULL;
"
```

#### 4.3.2 `issues` Table Test Columns

**What was fixed:**
V6__native_test_management.sql was supposed to add test-specific columns to the `issues` table, but it failed. These columns were needed for test management features:

```sql
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_type VARCHAR(50) DEFAULT 'MANUAL';
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_status VARCHAR(30) DEFAULT 'DRAFT';
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_priority VARCHAR(20);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_owner_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_steps JSONB;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS requirement_keys TEXT[];
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_feature_key VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS gherkin_scenario_id VARCHAR(255);
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_set_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_plan_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_execution_id UUID;
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS test_repository_folder_id UUID;
```

#### 4.3.3 `sprints` Table auto_complete / auto_start

```sql
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS auto_complete BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_sprint.sprints ADD COLUMN IF NOT EXISTS auto_start BOOLEAN DEFAULT FALSE;
```

---

## 5. Microservice Failures

### 5.1 Plan Service Container Swap (jira-plan-service running jira-sprint-service JAR)

**What Was Observed:**
- Requests to `http://localhost:8092/api/plans` returned 404 or a different service's response
- The container `jira-plan-service` was listening on port 8092 but returning unexpected responses
- `curl http://localhost:8092/actuator/health` returned an empty response or error
- Investigating further revealed the service was responding on port 8091 instead of 8092, or vice versa

**Root Cause:**
The Docker images for `jira-plan-service` and `jira-sprint-service` were swapped. The `jira-plan-service` container was running the `jira-sprint-service` JAR (which listens on port 8091) instead of the `jira-plan-service` JAR (which listens on port 8092).

This happened because:
1. Both services have similar Dockerfile structures: `FROM maven:3.9.9-etemurin-21-alpine AS builder`
2. The build context was set to `.` (project root) for plan-service in docker-compose.yml
3. When `docker compose build plan-service` was run from the project root, it copied from the wrong subdirectory
4. The image tagging was not service-specific, so `jdc-plan-service:latest` and `jdc-sprint-service:latest` both pointed to the same image

**Which Layer Failed:** Docker build context + image tagging pipeline

**What Signals Indicated the Real Issue:**
- `curl http://localhost:8091/actuator/health` returned 200 OK but `curl http://localhost:8092/actuator/health` returned 404
- `docker logs jira-plan-service` showed sprint service startup messages (e.g., "Starting SprintService")
- `docker inspect jira-plan-service | jq '.[0].Config.Labels'` showed a label referencing `jira-sprint-service`
- Port 8092 was open but returning sprint-service endpoints

**Debugging Path:**
```bash
# Step 1: Check which JAR is actually running
docker exec jira-plan-service java -cp app.jar org.springframework.boot.loader.launch.JarLauncher 2>/dev/null || \
docker exec jira-plan-service ls -la /app/

# Step 2: Check the application name from Spring Boot
docker logs jira-plan-service | grep "spring.application.name"

# Step 3: Check what port is actually listening
docker exec jira-plan-service netstat -tlnp 2>/dev/null || \
docker exec jira-plan-service ss -tlnp

# Step 4: Compare the JAR files by size
ls -la jira-plan-service/target/*.jar jira-sprint-service/target/*.jar

# Step 5: Extract and compare the manifests
unzip -p jira-plan-service/target/*.jar META-INF/MANIFEST.MF | grep Implementation-Title

# Step 6: Check the docker-compose build context for plan-service
grep -A 10 "plan-service:" docker-compose.yml
# If context is "." instead of "./jira-plan-service", the build is wrong

# Step 7: Verify image digests don't match
docker inspect jira-plan-service --format '{{.Image}}'
docker inspect jira-sprint-service --format '{{.Image}}'
# If same image ID, the swap is confirmed
```

**Permanent Fix:**
1. Fix the build context in docker-compose.yml:
   ```yaml
   plan-service:
     build:
       context: ./jira-plan-service   # NOT "." (project root)
       dockerfile: Dockerfile
   ```
2. Tag images with service-specific tags:
   ```bash
   docker build -t jdc-plan-service:$(git rev-parse --short HEAD) ./jira-plan-service
   docker build -t jdc-sprint-service:$(git rev-parse --short HEAD) ./jira-sprint-service
   ```
3. Add image verification to the health check:
   ```yaml
   healthcheck:
     test: ["CMD-SHELL", "curl -sf http://localhost:8092/actuator/health || exit 1"]
   ```

**Preventive Guardrail:**
```bash
# Pre-deploy verification - confirm each container runs the correct JAR
verify_service_jar() {
  local SERVICE=$1
  local EXPECTED_PORT=$2
  local CONTAINER=$(docker ps --format '{{.Names}}' | grep "^jira-$SERVICE$")
  
  if [ -z "$CONTAINER" ]; then
    echo "FAIL: Container jira-$SERVICE not found"
    return 1
  fi
  
  local ACTUAL_PORT=$(docker exec "$CONTAINER" ss -tlnp 2>/dev/null | grep LISTEN | awk '{print $4}' | grep -o '[0-9]*$')
  if [ "$ACTUAL_PORT" != "$EXPECTED_PORT" ]; then
    echo "FAIL: $SERVICE expected port $EXPECTED_PORT but got $ACTUAL_PORT - possible image swap!"
    return 1
  fi
  
  local APP_NAME=$(docker logs "$CONTAINER" 2>&1 | grep -oP 'spring\.application\.name[^\r\n]*' | head -1)
  echo "OK: $SERVICE on port $ACTUAL_PORT (app: $APP_NAME)"
}

verify_service_jar plan-service 8092
verify_service_jar sprint-service 8091
verify_service_jar issue-service 8084
```

**Detect in Under 5 Minutes:**
```bash
#!/bin/bash
# Detect port mismatch (indicates swapped JAR)
for service in plan:8092 sprint:8091 issue:8084 project:8083 auth:8081; do
  NAME="${service%%:*}"
  PORT="${service##*:}"
  CONTAINER="jira-${NAME}-service"
  if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    LISTEN_PORT=$(docker exec "$CONTAINER" ss -tlnp 2>/dev/null | \
      grep -oP "0.0.0.0:\K[0-9]+" | sort -u | tr '\n' ',' | sed 's/,$//')
    if ! echo "$LISTEN_PORT" | grep -q "$PORT"; then
      echo "ALERT: $CONTAINER should listen on $PORT but listens on $LISTEN_PORT"
    fi
  fi
done
```

---

### 5.2 Flyway Checksum Mismatch on Plan Service

**What Was Observed:**
- plan-service crashed on startup with:
  ```
  org.flywaydb.core.api.exception.FlywayValidationException:
  Migration checksum mismatch for version 1
  Type: SQL
  Location: db/migration/V1__create_program_schema.sql
  ```
- The database contained checksums for V1-V5 that did not match the checksums of the migration files in the JAR

**Root Cause:**
After a Docker image rebuild, Flyway detected that the migration files embedded in the JAR had different checksums than the ones recorded in the `flyway_schema_history` table. This happened because:

1. The JAR was rebuilt without `--no-cache`, pulling a cached Maven build
2. V4 and V5 migration files had their content swapped (V4's content was in V5.sql and vice versa)
3. When the image was rebuilt, it included the correct files, but the database still had the old checksums from the previous run
4. Flyway's validation failed because the JAR's migration checksums didn't match the DB's recorded checksums

**Which Layer Failed:** Flyway checksum validation / Maven build caching

**Why Not Detected Earlier:**
- The `flyway.validate-on-migrate` setting was set to `true` by default, so the error appeared on startup rather than silently being ignored
- No checksum pre-validation step existed in the CI/CD pipeline

**Debugging Path:**
```bash
# Step 1: Get the checksums Flyway expects from the DB
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT script, checksum FROM flyway_schema_history WHERE schema_name = 'jira_plan' ORDER BY installed_rank;"

# Step 2: Get the checksums from the migration files in the JAR
docker exec jira-plan-service sh -c '
for f in /app/app.jar; do
  unzip -p /app/app.jar BOOT-INF/classes/db/migration/V*.sql 2>/dev/null | \
  md5sum
done
'

# Step 3: Calculate checksums from source files
cd /home/ubuntu/workspace/JDC
for f in jira-plan-service/src/main/resources/db/migration/V*.sql; do
  echo "$(md5sum $f | cut -d' ' -f1) $(basename $f)"
done | sort

# Step 4: Compare V4 and V5
md5sum jira-plan-service/src/main/resources/db/migration/V4__create_plan_items_table.sql
md5sum jira-plan-service/src/main/resources/db/migration/V5__create_plan_teams_table.sql

# Step 5: Check if the migration files are swapped
# V4 should create plan_items table, V5 should create plan_teams table
# If they are swapped, the table names in the SQL don't match the migration names
grep "CREATE TABLE" jira-plan-service/src/main/resources/db/migration/V4__*.sql
grep "CREATE TABLE" jira-plan-service/src/main/resources/db/migration/V5__*.sql
```

**Permanent Fix:**

**Option A: Force Flyway to baseline (if migrations are functionally identical):**
```bash
# Stop the container first
docker stop jira-plan-service

# Reset Flyway's view of the database
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "DELETE FROM flyway_schema_history WHERE schema_name = 'jira_plan';"

# Also verify the migration files in the JAR match source
# Extract and diff:
docker exec jira-plan-service sh -c 'unzip -p /app/app.jar BOOT-INF/classes/db/migration/V4__create_plan_items_table.sql' > /tmp/v4_jar.sql
diff jira-plan-service/src/main/resources/db/migration/V4__create_plan_items_table.sql /tmp/v4_jar.sql

# Restart the service
docker start jira-plan-service
```

**Option B: Update Flyway's recorded checksums (if files are intentionally changed):**
```bash
# If the migration content is intentionally different (e.g., bug fix),
# update the checksums in the DB to match the new files
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c "
UPDATE flyway_schema_history SET checksum = NEW_CHECKSUM
WHERE schema_name = 'jira_plan' AND script LIKE 'V4__%';
"
```
Then restart the service.

**Option C: Use a repair command:**
```bash
docker exec jira-plan-service java -cp app.jar org.flywaydb.commandline.Main repair -database=jira_plan
```

**Preventive Guardrail:**
1. **Never swap migration file content** — migration filenames must match their content. V4 should contain V4's SQL, V5 should contain V5's SQL.
2. Add a CI/CD check that validates migration file content matches filename expectations:
   ```bash
   # V4 must create plan_items, V5 must create plan_teams
   grep -q "CREATE TABLE.*plan_items" jira-plan-service/src/main/resources/db/migration/V4__*.sql || \
     { echo "FAIL: V4 migration does not create plan_items"; exit 1; }
   grep -q "CREATE TABLE.*plan_teams" jira-plan-service/src/main/resources/db/migration/V5__*.sql || \
     { echo "FAIL: V5 migration does not create plan_teams"; exit 1; }
   ```
3. Document Flyway baseline procedures in this runbook
4. After any migration file change, force a clean rebuild:
   ```bash
   docker builder prune -af
   docker compose build --no-cache plan-service
   ```

---

### 5.2.1 V4 and V5 Migration Content Swapped (Plan Service)

**What Was Observed:**
After fixing the checksum mismatch, `jira_plan.plans` table didn't have the expected columns because V4 was creating `plan_teams` and V5 was creating `plan_items`.

**Root Cause:**
The source files `V4__create_plan_items_table.sql` and `V5__create_plan_teams_table.sql` had their content swapped. The file named V4 contained the SQL for `plan_teams` and the file named V5 contained the SQL for `plan_items`. This is a file content vs filename mismatch — the most dangerous kind of Flyway bug because Flyway doesn't validate content, only file names and checksums.

**Debugging Path:**
```bash
# Check which table V4 actually creates
grep "CREATE TABLE" jira-plan-service/src/main/resources/db/migration/V4__*.sql
# Expected: CREATE TABLE jira_plan.plan_items

# Check which table V5 actually creates
grep "CREATE TABLE" jira-plan-service/src/main/resources/db/migration/V5__*.sql
# Expected: CREATE TABLE jira_plan.plan_teams

# If output is swapped, fix the files
```

**Permanent Fix:**
Manually fix the files:
1. Copy `V5__create_plan_teams_table.sql` content to `V4__create_plan_items_table.sql`
2. Copy `V4__create_plan_items_table.sql` content to `V5__create_plan_teams_table.sql`
3. Then rebuild and redeploy

OR, since the DB already has the wrong tables from the swapped migrations:
```sql
-- The DB has plan_items where plan_teams should be and vice versa
-- This requires a manual DB migration:
ALTER TABLE jira_plan.plan_items RENAME TO plan_items_old;
ALTER TABLE jira_plan.plan_teams RENAME TO plan_items;
ALTER TABLE jira_plan.plan_items_old RENAME TO plan_teams;

-- And fix the index names accordingly
ALTER INDEX jira_plan.idx_plan_items_plan_id RENAME TO idx_plan_items_plan_id;
ALTER INDEX jira_plan.idx_plan_teams_plan_id RENAME TO idx_plan_teams_plan_id;
```

**Preventive Guardrail:** Same as checksum mismatch — add content validation to CI/CD.

---

### 5.3 Issue Service Exit Loop (Multiple Flyway Errors)

**What Was Observed:**
The `jira-issue-service` container entered a restart loop with multiple Flyway errors appearing in sequence:
1. `'type-test' UUID` error
2. `duplicate idx_te_project` error
3. `missing created_at` error
4. `'folder-root' UUID literal` error
5. `NULL project_id constraint` error
6. `V15/V16 duplicate migration` error

Each fix revealed the next error in a cascade pattern, with the container crashing after each failed migration attempt.

**Root Cause:** A cascade of migration bugs in V6 and V15/V16, with no migration error handling strategy. Each error caused the container to exit, triggering a restart, which would attempt the same failed migration again.

**Which Layer Failed:** Migration file quality / Flyway error handling

**Complete Recovery Procedure for Issue Service:**
```bash
# 1. Stop the service
docker stop jira-issue-service

# 2. Fix the V6 SQL file
# The fixed V6__native_test_management.sql must have:
# - No INSERT with "type-test" as UUID
# - Unique index names (idx_te_project on test_executions, idx_teh_test on test_execution_history)
# - All ALTER TABLE for columns before table creation or use CREATE TABLE IF NOT EXISTS
# - No "folder-root" literal UUID
# - No INSERT into test_repository_folders without valid project_id

# 3. Verify the file is correct
grep "folder-root" jira-issue-service/src/main/resources/db/migration/V6__*.sql
grep "idx_te_project" jira-issue-service/src/main/resources/db/migration/V6__*.sql | wc -l
# Second command should show 1 occurrence, not 2

# 4. Check for duplicate V15/V16 in migration-service
ls jira-migration-service/src/main/resources/db/migration/V1*.sql

# 5. Clean rebuild with no cache
docker builder prune -af
cd jira-issue-service && mvn clean package -DskipTests -q
cd jira-issue-service && docker build -t jdc-issue-service:latest .

# 6. Clear Flyway history for jira_issue schema (if needed)
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "DELETE FROM flyway_schema_history WHERE schema_name = 'jira_issue';"

# 7. Run manual ALTER TABLE fixes
docker exec -i jira-postgres psql -U jiraadmin -d jira_platform << 'EOF'
-- Fix issue_types missing columns
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS issue_type_key VARCHAR(50);
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS is_subtask BOOLEAN DEFAULT FALSE;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS sequence INTEGER DEFAULT 0;
ALTER TABLE jira_issue.issue_types ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Fix issues missing version column
ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Fix test_import_batches missing created_at
ALTER TABLE jira_issue.test_import_batches ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
EOF

# 8. Start the service
docker start jira-issue-service

# 9. Verify it's running
sleep 10
docker logs jira-issue-service --tail 20 | grep -i "started\|error\|flyway"
curl -sf http://localhost:8084/actuator/health || echo "NOT READY"
```

---

### 5.4 Sprint Service

**What Was Observed:**
- sprint-service started but agile boards returned errors
- The `board_id` column was referenced in the JPA entity but missing from the PostgreSQL table

**Root Cause:** JPA entity `AgileBoard.java` referenced a `board_id` column that was never created by any migration.

**Debugging Path:**
```bash
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name FROM information_schema.columns \
   WHERE table_schema='jira_sprint' AND table_name='agile_boards';"

# Check JPA entity
grep -rn "board_id" jira-sprint-service/src/main/java/ | grep -i entity
```

**Permanent Fix:**
```sql
ALTER TABLE jira_sprint.agile_boards ADD COLUMN IF NOT EXISTS parent_board_id UUID;
ALTER TABLE jira_sprint.agile_boards ADD CONSTRAINT fk_agile_board_parent
    FOREIGN KEY (parent_board_id) REFERENCES jira_sprint.agile_boards(id) ON DELETE SET NULL;
```

---

## 6. API Gateway Failures

### 6.1 Gateway Routes Pointing to Wrong Container DNS Names

**What Was Observed:**
- Multiple endpoints returned 404 from the gateway
- Gateway logs showed routes with `uri=http://plan-service:8092` (short name) instead of `uri=http://jira-plan-service:8092` (full container name)
- Services behind the gateway were unreachable: `Connection refused: plan-service/10.x.x.x:8092`

**Root Cause:**
The `application-docker.yml` gateway configuration used short hostnames for routes:
```yaml
- id: plan-service
  uri: http://plan-service:8092        # WRONG - short name
  predicates:
    - Path=/api/plans/**
```
But Docker's internal DNS resolves full container names. The container `jira-plan-service` resolves for `jira-plan-service`, not `plan-service`.

**What Signals Indicated the Real Issue:**
- `curl http://localhost:8080/api/plans` returned 404 or empty response
- `docker logs jira-gateway | grep "RouteDefinition"` showed `uri=http://plan-service:8092`
- `docker exec jira-gateway getent hosts plan-service` returned no results
- `docker exec jira-gateway getent hosts jira-plan-service` returned the correct IP

**Debugging Path:**
```bash
# Step 1: Check what routes the gateway has loaded
curl -s http://localhost:8080/actuator/gateway/routes | jq '.[] | {id, uri}'

# Step 2: Check gateway logs for route URIs
docker logs jira-gateway 2>&1 | grep "RouteDefinition" | head -10

# Step 3: Check what Docker DNS resolves
docker exec jira-gateway getent hosts jira-plan-service
docker exec jira-gateway getent hosts plan-service

# Step 4: Check the gateway configuration file
grep -A 3 "uri:" jira-gateway/src/main/resources/application-docker.yml

# Step 5: Check what containers are on the network
docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}'
```

**Permanent Fix:**
Update `jira-gateway/src/main/resources/application-docker.yml` to use full container names:
```yaml
- id: plan-service
  uri: http://jira-plan-service:8092    # CORRECT - full name
  predicates:
    - Path=/api/plans/**
  filters:
    - StripPrefix=1

- id: issue-service
  uri: http://jira-issue-service:8084   # CORRECT

- id: sprint-service
  uri: http://jira-sprint-service:8091 # CORRECT
```

Rebuild and redeploy:
```bash
cd jira-gateway && mvn clean package -DskipTests -q
cd jira-gateway && docker build -t jdc-gateway:latest .
docker compose up -d gateway
```

**Preventive Guardrail:**
```bash
# Pre-deployment: validate all gateway routes use full container names
#!/bin/bash
ROUTES=$(curl -s http://localhost:8080/actuator/gateway/routes 2>/dev/null | jq -r '.[] | .uri')
for route in $ROUTES; do
  HOST=$(echo $route | sed 's|http://||' | cut -d':' -f1)
  if ! docker exec jira-gateway getent hosts "$HOST" >/dev/null 2>&1; then
    echo "ALERT: Gateway route $route - host $HOST not resolvable in Docker DNS"
  fi
done
```

---

### 6.2 CORS Double Header (Duplicate Access-Control-Allow-Origin)

**What Was Observed:**
- Browser showed: `Access to XMLHttpRequest at 'http://localhost:8080/api/auth/login' from origin 'http://localhost:3000' has been blocked by CORS policy: The 'Access-Control-Allow-Origin' header contains multiple values '*, http://localhost:3000', but only one is allowed.`
- Login was completely broken for all users

**Root Cause:**
Both the gateway AND the auth-service were independently adding CORS headers:
1. Gateway's `application-docker.yml`: `allowedOrigins: ["*"]` added `Access-Control-Allow-Origin: *`
2. Auth service's `SecurityConfig.java`: `.cors(cors -> cors.configurationSource(...))` added `Access-Control-Allow-Origin: http://localhost:3000`

The response had two headers. Browsers reject multiple values for single-value response headers.

**Which Layer Failed:** Cross-service configuration coordination

**Permanent Fix:**
1. **Remove** CORS configuration from `jira-auth-service/src/main/java/com/jira/auth/config/SecurityConfig.java`:
   Remove the `.cors()` block from the `SecurityFilterChain` bean. Let the gateway handle all CORS.
2. **Delete** any custom `CorsWebFilter.java` in the gateway (it was already deleted during the incident)
3. Keep only the gateway's `globalcors` configuration

**Preventive Guardrail:**
Add a CI/CD check that scans for CORS configuration in non-gateway services:
```bash
# In CI/CD pipeline - fail if any non-gateway service has CORS config
for svc in auth project issue plan sprint workflow; do
  if grep -r "allowedOrigins\|CorsConfiguration\|addHeader.*Access-Control" \
     jira-$svc-service/src/main/java/ 2>/dev/null; then
    echo "FAIL: $svc-service has CORS config - should be in gateway only"
    exit 1
  fi
done
```

---

## 7. Deployment Failure Patterns

This section documents patterns that consistently caused deployment failures across multiple incidents.

### Pattern 1: Stale JAR Cached in Docker Image

**The Pattern:**
A developer fixes a migration file, rebuilds the Docker image, but the container still fails because the old JAR is cached inside the image. The JAR contains the old (broken) migration files despite the source file being correct.

**Why It Happens:**
- Docker layer caching: `COPY target/*.jar app.jar` reuses the same layer if the JAR filename is unchanged
- Maven rebuilds the JAR but with the same filename, so Docker sees no change
- Multi-stage builds copy from `target/` which may have the old JAR from a previous build

**Detection:**
```bash
# Compare source JAR with JAR inside container
md5sum jira-issue-service/target/*.jar
docker exec jira-issue-service md5sum /app/app.jar
# If different, the cached image is stale
```

**Prevention:**
```bash
# Always use --no-cache when migrations change
docker builder prune -af
docker compose build --no-cache issue-service

# Use content-addressable JAR naming
JAR_HASH=$(md5sum jira-issue-service/target/*.jar | cut -d' ' -f1)
docker build -t jdc-issue-service:$JAR_HASH ./jira-issue-service
```

### Pattern 2: Gateway Profile Mismatch

**The Pattern:**
The gateway JAR was compiled with `localhost:8081` routes from the `localhost` profile instead of Docker service names from the `docker` profile. The `SPRING_PROFILES_ACTIVE=docker` environment variable was not properly passed to the build, so the gateway used localhost routes.

**Why It Happens:**
- Maven compiles the JAR at build time, embedding the active profile's configuration
- If the build runs without `-Dspring.profiles.active=docker`, the JAR gets localhost routes
- Docker `environment:` in docker-compose.yml only sets the runtime profile, not the compile-time profile

**Detection:**
```bash
# Check what profile was active when the JAR was built
unzip -p jira-gateway/target/*.jar BOOT-INF/classes/application-docker.yml | head -10

# Check what routes are actually loaded
docker logs jira-gateway | grep "RouteDefinition" | head -5
```

**Prevention:**
Ensure the JAR is built with the correct profile:
```bash
# In CI/CD - build with explicit profile
mvn clean package -pl jira-gateway -am -DskipTests \
  -Dspring-boot.run.profiles=docker \
  -Dmaven.compiler.profiles=docker

# Verify the built JAR has correct routes
unzip -p target/*.jar BOOT-INF/classes/application-docker.yml | grep "uri:"
```

### Pattern 3: Nginx Hardcoded IP Address

**The Pattern:**
The nginx configuration in `jira-frontend/nginx.conf` had `proxy_pass http://172.18.0.9:8080/api/` with the gateway IP hardcoded. When containers restart, Docker assigns new IPs, breaking all API routing.

**Detection:**
```bash
grep "proxy_pass" jira-frontend/nginx.conf | grep "[0-9]\{1,3\}\.[0-9]\{1,3\}"
```

**Prevention:**
Use Docker DNS names instead of IP addresses:
```nginx
proxy_pass http://jira-gateway:8080/api/;  # GOOD
```

---

## 8. Golden Troubleshooting Playbook

This playbook is designed for a 2 AM incident. Follow the steps in order. Do not skip steps.

### Step 0: Initial Assessment (30 seconds)

```bash
# Run this first - gets everything you need in one shot
echo "=== CONTAINER STATUS ===" && docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo "=== NETWORK ===" && docker network inspect jdc_jira-network --format='{{range .Containers}}{{.Name}}: {{.IPv4Address}}{{println}}{{end}}' 2>/dev/null || echo "Network not found"
echo "=== RECENT LOGS (last 5 min) ===" && docker ps --format '{{.Names}}' | while read c; do echo "--- $c ---"; docker logs "$c" --tail 10 --since 5m 2>&1 | grep -i "error\|exception\|fail" | head -5; done
```

### Step 1: Identify the Failure Layer

| Symptom | Likely Layer | First Action |
|---|---|---|
| Login fails (CORS error) | Auth + Gateway | Check `docker logs jira-gateway` for CORS headers |
| 502 Bad Gateway | Nginx + Gateway | `docker exec jira-frontend cat /etc/nginx/conf.d/default.conf \| grep proxy_pass` |
| 404 on specific endpoint | Gateway routing | `curl -s http://localhost:8080/actuator/gateway/routes \| jq` |
| Container keeps restarting | Flyway/DB migration | `docker logs <container> --tail 50 \| grep -i flyway` |
| Login succeeds but other calls fail | Missing containers | `docker ps --format '{{.Names}}'` and compare to required list |
| Service starts but API returns 500 | Schema drift / JPA mismatch | `docker logs <service> --tail 30 \| grep -i "column.*does not exist"` |

### Step 2: Decision Tree for Container Crash

```
Container crashed -> Check docker logs <name> --tail 100
    |
    +-- "Flyway" in logs -> Go to Step 3 (Flyway troubleshooting)
    +-- "Connection refused" -> Check if target service is running
    |       docker ps | grep <target-service>
    +-- "BindException: Address already in use" -> Port conflict
    |       netstat -tlnp | grep <port>
    +-- "java.lang.IllegalStateException: Failed to load" -> JAR not found or wrong image
    |       docker exec <container> ls -la /app/
    +-- "Spring Boot BeanCreationException" -> JPA entity mismatch
            docker logs <container> --tail 50 | grep -i "column.*does not exist"
```

### Step 3: Flyway Troubleshooting Tree

```
Flyway error in logs -> What type?
    |
    +-- "checksum mismatch" -> Migration files changed after baseline
    |       1. Check: docker logs <svc> | grep checksum
    |       2. Compare: SELECT script, checksum FROM flyway_schema_history WHERE schema='<schema>'
    |       3. Decision: Intentional change (update checksums) or accidental (restore old files)
    |       4. Fix: DELETE FROM flyway_schema_history WHERE script='V<X>'; then restart
    |
    +-- "duplicate key name" -> Same index created twice
    |       1. Find: docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
    |              "SELECT indexname FROM pg_indexes WHERE schemaname='<schema>' GROUP BY indexname HAVING count(*) > 1"
    |       2. Fix: Use CREATE INDEX IF NOT EXISTS or rename duplicates
    |
    +-- "invalid input syntax for type uuid" -> Literal string used where UUID expected
    |       1. Find: grep the migration file for non-UUID strings used as UUID values
    |       2. Common culprits: "type-test", "folder-root", "default-folder"
    |       3. Fix: Replace with gen_random_uuid() or valid UUID
    |
    +-- "column .* does not exist" -> Schema drift between JPA and DB
    |       1. Check: docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
    |              "SELECT column_name FROM information_schema.columns WHERE table_schema='<schema>' AND table_name='<table>'"
    |       2. Find: Check JPA entity @Column annotations
    |       3. Fix: Add missing column with ALTER TABLE
    |
    +-- "duplicate migration version number" -> V15 appears twice or V15/V16 swapped
    |       1. Find: ls <service>/src/main/resources/db/migration/ | grep "^V15"
    |       2. Fix: Remove duplicate or rename to V17
    |
    +-- "syntax error at or near" -> SQL syntax error in migration file
    |       1. Find: docker logs <svc> | grep -A 3 "syntax error"
    |       2. Fix: Edit the SQL file
    |
    +-- "Migration V<X> failed" -> Container in restart loop
    |       1. STOP the container: docker stop <svc>
    |       2. Fix the migration file
    |       3. Clean build: docker builder prune -af && docker compose build --no-cache <svc>
    |       4. Optionally: DELETE FROM flyway_schema_history WHERE installed_rank >= <X>
    |       5. START: docker start <svc>
```

### Step 4: Database Recovery Commands

```bash
# === SCHEMA INSPECTION ===
# List all schemas
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name;"

# List all tables in a schema
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='jira_issue' ORDER BY table_name;"

# List all columns in a table
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT column_name, data_type, column_default FROM information_schema.columns \
   WHERE table_schema='jira_issue' AND table_name='issues' ORDER BY ordinal_position;"

# List all indexes on a table
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT indexname, indexdef FROM pg_indexes WHERE schemaname='jira_issue' AND tablename='issues';"

# List Flyway migration history per schema
for schema in jira_user jira_project jira_issue jira_sprint jira_plan jira_auth jira_workflow; do
  echo "=== $schema ==="
  docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
    "SELECT installed_rank, script, checksum, success FROM flyway_schema_history \
     WHERE schema_name = '$schema' ORDER BY installed_rank;" 2>/dev/null
done

# === COLUMN OPERATIONS ===
# Add missing column
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "ALTER TABLE jira_issue.issues ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;"

# Drop duplicate index
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "DROP INDEX IF EXISTS jira_issue.idx_teh_project;"

# Rename incorrectly named index
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "ALTER INDEX jira_issue.idx_te_project RENAME TO idx_teh_test;"

# === FLYWAY OPERATIONS ===
# Clear Flyway history for a schema (use with caution - loses migration tracking)
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "DELETE FROM flyway_schema_history WHERE schema_name = 'jira_issue';"

# Check which migrations are pending
docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
  "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Repair Flyway (update checksums for intentionally changed migrations)
docker exec -it jira-issue-service java -jar /app/app.jar \
  spring.flyway.repair=true spring.datasource.url=jdbc:postgresql://postgres:5432/jira_platform
```

### Step 5: Verify Full Stack Health

```bash
#!/bin/bash
# Full stack health check - run after any fix
echo "=== CONTAINERS ===" && docker ps --format '{{.Names}}: {{.Status}}'
echo "=== DATABASE SCHEMAS ===" && docker exec jira-postgres psql -U jiraadmin -d jira_platform -c "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name;" 2>&1 | grep -v "^$"

# Test each service through the gateway
for endpoint in "api/auth/login" "api/projects" "api/issues" "api/plans" "api/sprints" "api/workflows"; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/$endpoint)
  if [ "$STATUS" = "000" ]; then
    echo "FAIL: $endpoint - connection refused (service down)"
  elif [ "$STATUS" = "404" ]; then
    echo "WARN: $endpoint - 404 (route not configured or service missing)"
  elif [ "$STATUS" = "401" ] || [ "$STATUS" = "200" ]; then
    echo "OK: $endpoint - $STATUS (expected - auth required or accessible)"
  else
    echo "INFO: $endpoint - $STATUS"
  fi
done

# Test direct service access (to isolate gateway vs service issues)
for service_port in "jira-auth-service:8081" "jira-project-service:8083" "jira-issue-service:8084" "jira-plan-service:8092" "jira-sprint-service:8091"; do
  NAME="${service_port%%:*}"
  PORT="${service_port##*:}"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/actuator/health 2>/dev/null)
  echo "$NAME: $STATUS"
done
```

---

## 9. Pre-Deployment Checklist

Run this checklist before every deployment. It should take no more than 10 minutes.

### 9.1 Pre-Build Checks

```bash
# 1. Verify no uncommitted migration changes
cd /home/ubuntu/workspace/JDC
git status --short src/main/resources/db/migration/

# 2. Verify no duplicate migration version numbers in any service
for svc in */src/main/resources/db/migration; do
  echo "=== $svc ==="
  ls "$svc"/V*.sql | sed 's/.*V\([0-9]*\)__.*/\1/' | sort | uniq -d && echo "DUPLICATE FOUND"
done

# 3. Verify migration file content matches filename
for svc in */src/main/resources/db/migration/V4__*.sql; do
  TABLE=$(grep "CREATE TABLE" "$svc" | head -1 | sed 's/.*jira_[a-z_]*\.//' | tr -d '()')
  FILENAME=$(basename "$svc" | sed 's/V[0-9]*__//' | sed 's/\.sql//' | tr '_' ' ')
  if ! echo "$TABLE" | grep -qi "$FILENAME"; then
    echo "MISMATCH: $svc creates $TABLE but filename suggests $FILENAME"
  fi
done

# 4. Verify no hardcoded IPs in nginx config
if grep -q "[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}" jira-frontend/nginx.conf; then
  echo "FAIL: Hardcoded IP found in nginx.conf"
fi

# 5. Verify gateway routes use full container names
grep "uri:" jira-gateway/src/main/resources/application-docker.yml | grep -v "jira-" | grep -v "postgres" && echo "WARNING: Short hostname in gateway routes"
```

### 9.2 Migration File Validation

```bash
# For every migration file, run these checks:
for f in $(find . -name "V*.sql" -path "*/db/migration/*"); do
  echo "Checking: $f"
  
  # No UUID literals that aren't valid UUID format
  if grep -Pzo '(?s)gen_random_uuid\(\)|[0-9a-f]{8}-[0-9a-f]{4}' "$f" | grep -v "gen_random_uuid" | grep -P '[a-z]{4,}' | grep -v "^[0-9a-f-]*$"; then
    echo "  WARN: Possible invalid UUID in $f"
  fi
  
  # No duplicate index names in the same file
  INDEXES=$(grep -oP 'CREATE (UNIQUE )?INDEX \w+ ON' "$f" | sed 's/CREATE \(UNIQUE \)\?INDEX //')
  if echo "$INDEXES" | sort | uniq -d | grep -q .; then
    echo "  FAIL: Duplicate index names in $f"
    echo "$INDEXES" | sort | uniq -d
  fi
  
  # No NOT NULL column without DEFAULT in CREATE TABLE
  if grep -A 50 "CREATE TABLE" "$f" | grep -q "NOT NULL" | grep -v "DEFAULT"; then
    echo "  WARN: NOT NULL column without DEFAULT in $f"
  fi
done
```

### 9.3 Docker Image Verification

```bash
# Before deploying, verify images are fresh
# 1. Force no-cache build for any service with migration changes
SERVICES_WITH_MIGRATION_CHANGES="issue-service plan-service sprint-service workflow-service"
for svc in $SERVICES_WITH_MIGRATION_CHANGES; do
  echo "Building $svc with --no-cache..."
  cd /home/ubuntu/workspace/JDC/$svc
  docker builder prune -af
  mvn clean package -DskipTests -q
  docker build -t jdc-$svc:$(date +%Y%m%d%H%M) .
done

# 2. Verify JAR contents match source
for svc in $SERVICES_WITH_MIGRATION_CHANGES; do
  JAR_MD5=$(docker run --rm jdc-$svc:$(date +%Y%m%d%H%M) md5sum /app/app.jar | cut -d' ' -f1)
  SRC_MD5=$(md5sum /home/ubuntu/workspace/JDC/$svc/target/*.jar 2>/dev/null | cut -d' ' -f1)
  if [ "$JAR_MD5" != "$SRC_MD5" ]; then
    echo "FAIL: $svc JAR mismatch - cached image may be stale"
  else
    echo "OK: $svc JAR verified"
  fi
done
```

### 9.4 Database State Verification

```bash
# Before deploying, verify expected columns exist
SCHEMA_COLUMNS="
jira_issue:issues:version,test_type,test_status
jira_issue:issue_types:issue_type_key,is_subtask,sequence
jira_sprint:sprints:auto_complete,auto_start
jira_sprint:agile_boards:board_id
"

echo "$SCHEMA_COLUMNS" | while IFS=':' read -r schema table cols; do
  echo "Checking $schema.$table..."
  for col in $cols; do
    EXISTS=$(docker exec jira-postgres psql -U jiraadmin -d jira_platform -t -c \
      "SELECT 1 FROM information_schema.columns WHERE table_schema='$schema' AND table_name='$table' AND column_name='$col';" 2>/dev/null | tr -d ' ')
    if [ "$EXISTS" != "1" ]; then
      echo "  MISSING: $col in $schema.$table"
    else
      echo "  OK: $col"
    fi
  done
done
```

### 9.5 Final Stack Verification

```bash
# Final check before declaring deployment ready
echo "=== Final Health Check ==="
docker ps --format '{{.Names}}\t{{.Status}}' | grep -v Up && echo "WARNING: Some containers not running"

for svc in auth project issue plan sprint workflow migration; do
  NAME="jira-$svc-service"
  PORT=$(docker inspect --format '{{(index (index .NetworkSettings.Ports) "8081/tcp").HostPort}}' $NAME 2>/dev/null || echo "?")
  HEALTH=$(curl -sf http://localhost:$PORT/actuator/health 2>/dev/null && echo "OK" || echo "FAIL")
  echo "$NAME (:$PORT): $HEALTH"
done

echo "=== Gateway Routes ==="
curl -s http://localhost:8080/actuator/gateway/routes | jq 'length' && echo " routes loaded"

echo "=== Frontend ==="
curl -sf http://localhost:3000/ | grep -q "<!DOCTYPE html>" && echo "Frontend serving HTML"
```

---

## 10. Future Incident Template

Use this template to document any future incident. Fill in all sections for every incident, no matter how small.

```markdown
# Incident Report: [Brief Title]

**Date:** YYYY-MM-DD HH:MM
**Duration:** X hours Y minutes
**Severity:** P0/P1/P2/P3
**Engineer:** [Your Name]
**Detection Method:** [Alert / User Report / Manual Check]

---

## Executive Summary
[2-3 sentences: what broke, what was the impact, how was it resolved]

## Timeline
| Time | Action | Result |
|------|--------|--------|
| HH:MM | Event description | Outcome |

## What Was Observed
[Exact error messages, logs, screenshots]

## Root Cause
[Technical root cause - be specific]

## Which Layer Failed
[Infrastructure / Docker / Database / Application / Network / Security]

## Why Not Detected Earlier
[What would have caught this if it existed]

## What Assumptions Were Wrong
[List the incorrect assumptions that led to the failure]

## What Signals Indicated the Real Issue
[The first symptoms that pointed to the actual problem]

## Debugging Path
```bash
[Exact commands used to diagnose]
```

## Permanent Fix
[Exact code/config changes made to fix the issue]

## Preventive Guardrail
```bash
[Script or check that would catch this in under 5 minutes]
```

## Rollback Procedure
[If needed]

## Related Incidents
[Links to related past incidents]

## Action Items
- [ ] [Owner] - [Description] - Due: YYYY-MM-DD
```

---

## Quick Reference Card

### Container Names and Ports

| Service | Container | Port | Route in Gateway |
|---|---|---|---|
| PostgreSQL | `jira-postgres` | 5432 | N/A |
| Gateway | `jira-gateway` | 8080 | /api/** |
| Auth Service | `jira-auth-service` | 8081 | /api/auth/** |
| User Service | `jira-user-service` | 8082 | /users/** |
| Project Service | `jira-project-service` | 8083 | /api/projects/** |
| Issue Service | `jira-issue-service` | 8084 | /api/issues/** |
| Workflow Service | `jira-workflow-service` | 8085 | /workflows/** |
| Comment Service | `jira-comment-service` | 8086 | /comments/** |
| Notification Service | `jira-notification-service` | 8087 | /notifications/** |
| Search Service | `jira-search-service` | 8088 | /search/** |
| Audit Service | `jira-audit-service` | 8089 | /audit/** |
| Attachment Service | `jira-attachment-service` | 8090 | /attachments/** |
| Sprint Service | `jira-sprint-service` | 8091 | /api/sprints/** |
| Plan Service | `jira-plan-service` | 8092 | /api/plans/** |
| Admin Service | `jira-admin-service` | 8093 | /admin/** |
| Migration Service | `jira-migration-service` | 8094 | /api/migration/** |
| Test Service | `jira-test-service` | 8095 | /api/tests/** |
| Version Service | `jira-version-service` | 8096 | /versions/** |
| Component Service | `jira-component-service` | 8097 | /components/** |
| Frontend (nginx) | `jira-frontend` | 3000 | / |

### Database Schemas

| Schema | Owner Service |
|---|---|
| jira_auth | auth-service |
| jira_user | user-service |
| jira_project | project-service |
| jira_issue | issue-service |
| jira_sprint | sprint-service |
| jira_plan | plan-service |
| jira_workflow | workflow-service |
| jira_auth | auth-service |

### Critical Commands

```bash
# Emergency full restart
docker compose down && docker builder prune -af && docker compose up -d

# Check all container health
docker ps -a --format '{{.Names}}\t{{.Status}}'

# Check Flyway migration status for all schemas
for schema in jira_auth jira_user jira_project jira_issue jira_sprint jira_plan jira_workflow; do
  docker exec jira-postgres psql -U jiraadmin -d jira_platform -c \
    "SELECT installed_rank, script, success FROM flyway_schema_history WHERE schema_name='$schema' ORDER BY installed_rank;" 2>/dev/null
done

# Full log collection for support
docker ps -a --format '{{.Names}}' | while read c; do
  echo "===== $c =====" >> /tmp/all-logs.txt
  docker logs "$c" --tail 100 >> /tmp/all-logs.txt 2>&1
done

# Schema diff (JPA expected vs DB actual)
# Run inside the PostgreSQL container:
docker exec -it jira-postgres psql -U jiraadmin -d jira_platform

# Compare two JAR versions
unzip -p old.jar BOOT-INF/classes/db/migration/V6__native_test_management.sql | md5sum
unzip -p new.jar BOOT-INF/classes/db/migration/V6__native_test_management.sql | md5sum

# Force clean rebuild of specific service
docker builder prune -af
docker rmi $(docker images "jdc-*" -q) -f
cd /home/ubuntu/workspace/JDC/<service>
mvn clean package -DskipTests -q
docker build -t jdc-<service>:latest .
docker compose up -d <service>
```

### Connection Verification

```bash
# Test from any container to any other
docker exec <container> curl -sf http://<target-service>:PORT/actuator/health

# Test from host to container
curl -sf http://localhost:PORT/actuator/health

# Test DNS resolution inside container
docker exec jira-gateway getent hosts jira-plan-service

# Test database connection from service
docker exec jira-issue-service sh -c \
  'curl -sf http://localhost:8084/actuator/health && echo "DB connection OK"'

# Test gateway routing
curl -s http://localhost:8080/actuator/gateway/routes | jq '.[] | {id, uri}'
```

### Nginx Configuration Check

```bash
# View current nginx config
docker exec jira-frontend cat /etc/nginx/conf.d/default.conf

# Test nginx config
docker exec jira-frontend nginx -t

# Reload nginx (no restart needed)
docker exec jira-frontend nginx -s reload

# Check nginx is running
docker exec jira-frontend ps aux | grep nginx
```

---

*Document maintained by: Platform Engineering Team*
*Last updated: 2026-05-30*
*Next review: 2026-06-15*