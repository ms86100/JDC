# IP REBRANDING PLAN: "jira" to "avionics-systems"

---

## 1. SCALE OF CHANGE

### Total References by Category

| Category | Estimated Count | Description |
|----------|----------------|-------------|
| PACKAGE_NAME (Java) | ~1,200+ declarations | `package com.jira.*` in every `.java` file across 26 modules |
| IMPORT_STATEMENT (Java) | ~3,500+ lines | `import com.jira.*` cross-package references |
| CLASS_NAME / FILE_NAME | ~50 classes | `Jira*ServiceApplication.java`, `JiraDc*`, etc. |
| ANNOTATION_VALUE (`@Table`, `@Value`, `@ConfigurationProperties`) | ~250+ annotations | JPA schema refs, Spring config property bindings |
| STRING_LITERAL (Java) | ~300+ literals | Service names, OpenAPI metadata, CSS classes, URLs |
| CONFIG_VALUE (YAML/properties) | ~400+ values | `application.yml`, `application-docker.yml`, `application-local.yml`, `application-external.yml` |
| CONFIG_KEY (YAML) | ~60+ keys | `jira:` namespace, `com.jira.*` logging keys |
| SQL_REFERENCE | ~1,500+ schema-qualified refs | `jira_issue.`, `jira_auth.`, etc. across ~150 migration files |
| MAVEN coordinates | ~100+ refs | `groupId`, `artifactId`, dependency declarations across 26 `pom.xml` files |
| DOCKER references | ~300+ refs | Container names, image names, build contexts, user/group, volumes |
| FRONTEND (TSX/CSS/TS) | ~716 refs | Component names, CSS class prefixes, API calls, Tailwind theme keys |
| COMMENT / JAVADOC | ~200+ refs | Documentation strings referencing "Jira DC", "Jira Platform" |
| SEED_DATA (SQL) | ~50+ refs | Group names, config values, welcome text persisted in DB |

### Totals

- **Total references**: ~23,074 across 26 areas
- **Total services affected**: 22 backend microservices + 1 shared library + 1 gateway + 1 marketplace plugin + 1 frontend
- **Total Java source files affected**: ~1,500+ (package + import changes in every file)
- **Total SQL migration files affected**: ~150 files (schema-qualified table references)
- **Total configuration files affected**: ~130 YAML/properties files
- **Total Docker/compose files affected**: ~35 files (8 compose files + 27 Dockerfiles)

---

## 2. NAMING CONVENTION MAP

### Java / Maven

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Maven groupId | `com.jira` | `com.avionics_systems` | `<groupId>com.avionics_systems</groupId>` |
| Maven artifactId | `jira-{service}` | `avionics-systems-{service}` | `avionics-systems-issue-service` |
| Parent artifactId | `jira-platform` | `avionics-systems-platform` | `<artifactId>avionics-systems-platform</artifactId>` |
| Java package root | `com.jira.{domain}` | `com.avionics_systems.{domain}` | `com.avionics_systems.issue.service` |
| Cluster commons package | `com.jira.cluster` | `com.avionics_systems.cluster` | `com.avionics_systems.cluster.cache` |
| Application class | `Jira{X}ServiceApplication` | `AvionicsSystemsApplication` or domain-specific | `AvionicsSystemsIssueServiceApplication` |
| JiraDc-prefixed classes | `JiraDc{X}` | `LegacyDc{X}` | `LegacyDcXmlParser` |
| User mgmt class | `JiraUserManagementService` | `UserManagementService` | Drop prefix entirely |

### Spring Configuration

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Application name | `jira-{service}` | `avionics-systems-{service}` | `name: avionics-systems-issue-service` |
| Config namespace | `jira:` | `avionics-systems:` | `avionics-systems.workflow.transition-fallback` |
| `@Value` properties | `${jira.*}` | `${avionics-systems.*}` | `@Value("${avionics-systems.permissions.fail-open:false}")` |
| Logging keys | `com.jira.{x}: DEBUG` | `com.avionics_systems.{x}: DEBUG` | `com.avionics_systems.issue: DEBUG` |
| Kafka group-id | `jira-{service}` | `avionics-systems-{service}` | `group-id: avionics-systems-issue-service` |
| Trusted packages | `com.jira.{x}.events` | `com.avionics_systems.{x}.events` | `com.avionics_systems.issue.events` |

### Database / SQL

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Database name | `jira_platform` | `avionics_systems_platform` | `${DB_NAME:avionics_systems_platform}` |
| DB schemas | `jira_{service}` | `avionics_systems_{service}` | `avionics_systems_issue`, `avionics_systems_auth` |
| DB username | `jiraadmin` | `avionicsadmin` | `username: avionicsadmin` |
| Default field type | `'jira'` | `'avionics_systems'` | `field_type DEFAULT 'avionics_systems'` |
| Group type enum | `JIRA_INTERNAL` | `INTERNAL` | Drop prefix |

### Docker / Infrastructure

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Service folder | `jira-{service}` | `avionics-systems-{service}` | `avionics-systems-issue-service/` |
| Container name | `jira-{infra}` | `avionics-systems-{infra}` | `avionics-systems-postgres` |
| Docker image | `jira-{service}:latest` | `avionics-systems-{service}:latest` | `avionics-systems-gateway:latest` |
| GHCR image | `ghcr.io/ms86100/jira-*` | `ghcr.io/ms86100/avionics-systems-*` | `ghcr.io/ms86100/avionics-systems-gateway:latest` |
| Docker user/group | `jira` | `appuser` | Use generic name for all services |
| Docker network | `jira-network` | `avionics-systems-network` | network definition |
| Build context | `./jira-{service}` | `./avionics-systems-{service}` | `context: ./avionics-systems-gateway` |
| Volume paths | `/var/jira/attachments` | `/var/avionics-systems/attachments` | storage paths |
| S3 bucket | `jira-attachments` | `avionics-systems-attachments` | MinIO bucket name |
| Env vars | `JIRA_*` | `AVIONICS_SYSTEMS_*` | `AVIONICS_SYSTEMS_PERMISSIONS_FAILOPEN` |

### Kafka / Redis

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Kafka topics | `jira.{x}.events` | `avionics-systems.{x}.events` | `avionics-systems.issue.events` |
| Redis channels | `jira:events:*` | `avionics-systems:events:*` | pub/sub channel prefix |
| Redis cache channel | `jira:cache:invalidation` | `avionics-systems:cache:invalidation` | cache invalidation channel |

### Frontend

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| Package name | `jira-frontend` | `avionics-systems-frontend` | `package.json` name field |
| Tailwind theme key | `jira` (colors/fonts) | `avisys` | `text-avisys-blue`, `font-avisys` |
| CSS class prefix | `.jira-*`, `.ab-jira-*` | `.avisys-*`, `.ab-avisys-*` | `.avisys-admin-root` |
| Component names | `Jira{X}` | `AviSys{X}` | `AviSysGlobalLayout` |
| React Query keys | `'jira*'` | `'avisys*'` | `queryKey: ['avisysUsers']` |
| API type names | `JiraDc{X}` | `AviSysDc{X}` | `AviSysDcValidateResponse` |
| Import type literal | `'jira-dc'` | `'legacy-dc'` | migration import type enum |
| localStorage keys | `jira-test-settings-*` | `avisys-test-settings-*` | browser storage keys |

### JWT / Secrets

| Context | Current Pattern | New Pattern | Example |
|---------|----------------|-------------|---------|
| JWT secret default | `jira-platform-super-secret-key-...` | `avionics-systems-platform-super-secret-key-...` | All services must match |
| SAML entity ID | `jira-platform-sp` | `avionics-systems-platform-sp` | IdP config must update too |
| Email domain | `@jira.local` | `@avionics-systems.local` | notification sender |

---

## 3. EXECUTION ORDER

### Phase 0: Preparation (Before Any Code Changes)

1. **Create a fresh Git branch** from `dev`
2. **Snapshot the database** -- full pg_dump of `jira_platform`
3. **Document all external integrations** that reference current names (IdP SAML config, CI/CD pipelines, monitoring dashboards, Kafka topics in production)
4. **Decide Flyway strategy**: Since migrations have been applied, existing migration files CANNOT be renamed. The strategy is:
   - Leave existing `V1__` through `V24__` files untouched (checksums must match)
   - Create a NEW migration `V{next}__rename_schemas.sql` per service that does `ALTER SCHEMA jira_{x} RENAME TO avionics_systems_{x}`
   - Update the Hibernate `default_schema` and Flyway `schemas`/`default-schema` config to point to the new schema name

### Phase 1: Shared Library First (jira-cluster-commons)

**Why first**: Every other service depends on this module. Renaming it first and installing to local Maven repo prevents cascading build failures.

1. Rename folder: `jira-cluster-commons/` -> `avionics-systems-cluster-commons/`
2. Update `pom.xml`: groupId, artifactId, name, description
3. Rename Java package directory: `src/main/java/com/jira/cluster/` -> `src/main/java/com/avionics_systems/cluster/`
4. Update all 40 Java files: package declarations, imports
5. Update `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (14 lines)
6. Update string literals: Kafka topics in `KafkaTopics.java`, Redis channels in `RedisClusterEventBus.java`, storage paths in `ClusterProperties.java`
7. Update `logback-spring.xml` default value
8. `mvn clean install -pl avionics-systems-cluster-commons` to publish to local `.m2`

### Phase 2: Root POM

1. Update `pom.xml` at project root:
   - `<groupId>com.avionics_systems</groupId>`
   - `<artifactId>avionics-systems-platform</artifactId>`
   - All 23 `<module>` entries from `jira-*` to `avionics-systems-*`

### Phase 3: Backend Services (In Dependency Order)

Rename in this order to respect inter-service Maven dependencies:

| Order | Service | Reason |
|-------|---------|--------|
| 1 | `jira-auth-service` | No Maven dependencies on other custom services |
| 2 | `jira-user-service` | Depends only on cluster-commons |
| 3 | `jira-project-service` | Depends only on cluster-commons |
| 4 | `jira-admin-service` | Depends only on cluster-commons; referenced by issue-service |
| 5 | `jira-issue-service` | Depends on project-service, admin-service, cluster-commons |
| 6 | `jira-workflow-service` | Depends on cluster-commons; references issue/project/notification URLs |
| 7 | `jira-comment-service` | Standalone |
| 8 | `jira-notification-service` | Depends on cluster-commons |
| 9 | `jira-search-service` | Standalone |
| 10 | `jira-audit-service` | Standalone |
| 11 | `jira-attachment-service` | Depends on cluster-commons |
| 12 | `jira-sprint-service` | Depends on cluster-commons |
| 13 | `jira-report-service` | Standalone |
| 14 | `jira-plan-service` | Depends on cluster-commons |
| 15 | `jira-version-service` | Standalone |
| 16 | `jira-component-service` | Standalone |
| 17 | `jira-dashboard-service` | Standalone |
| 18 | `jira-document-service` | Standalone |
| 19 | `jira-portal-service` | Standalone |
| 20 | `jira-test-service` | Depends on cluster-commons |
| 21 | `jira-migration-service` | Depends on cluster-commons |
| 22 | `jira-marketplace-plugin` | Standalone (Atlassian SDK) |

**Per-service rename checklist** (repeat for each):

1. Rename folder: `jira-{x}-service/` -> `avionics-systems-{x}-service/`
2. Update `pom.xml`: groupId, artifactId, name, description, mainClass, all dependency coordinates
3. Rename Java directory tree: `src/main/java/com/jira/{x}/` -> `src/main/java/com/avionics_systems/{x}/`
4. Same for `src/test/java/com/jira/{x}/`
5. Rename application class file: `Jira{X}ServiceApplication.java` -> `AvionicsSystemsServiceApplication.java`
6. Bulk-update all Java files: package declarations, import statements, class names, `@Value` annotations, `@Table(schema=...)`, string literals
7. Update `application.yml`, `application-docker.yml`, `application-local.yml`, `application-external.yml`: spring.application.name, JDBC URL defaults, schema names, config namespace, logging keys
8. Update `Dockerfile`: user/group name, COPY paths (if multi-stage)
9. Add new Flyway migration: `V{next}__rename_schema.sql` with `ALTER SCHEMA jira_{x} RENAME TO avionics_systems_{x}`
10. Verify: `mvn clean compile -pl avionics-systems-{x}-service`

### Phase 4: API Gateway

1. Rename `jira-gateway/` -> `avionics-systems-gateway/`
2. Update all Java files, config, Dockerfile (same pattern as services)
3. Update `application-docker.yml` route definitions (Docker service name URIs remain as-is until Phase 6)

### Phase 5: Frontend

1. Update `package.json` name field
2. Rename Tailwind theme key `jira` -> `avisys` in `tailwind.config.js`
3. Bulk find-replace CSS class prefixes across all `.tsx`, `.css` files:
   - `jira-blue` -> `avisys-blue` (and navy, gray variants)
   - `.jira-` -> `.avisys-` in CSS files
   - `.ab-jira-` -> `.ab-avisys-` in CSS files
   - `font-jira` -> `font-avisys`
4. Rename component files and exports: `JiraGlobalLayout` -> `AviSysGlobalLayout`, etc.
5. Update API type names: `JiraDcValidateResponse`, function names, query keys
6. Rename CSS files: `jira-classic.css` -> `avisys-classic.css`, update `index.css` imports
7. Update `nginx.conf` upstream references (must match Docker service names from Phase 6)
8. Update `e2e/` test files and fixtures

### Phase 6: Docker Compose and Infrastructure

**This must be done atomically -- all compose files updated together.**

1. Rename ALL service entries in `docker-compose.yml` and all variant files:
   - Service names (the YAML keys): `jira-auth-service:` -> `avionics-systems-auth-service:`
   - `container_name:` values
   - `image:` values
   - `build.context:` paths
   - Environment variables: `DB_NAME`, `DB_USER`, `JWT_SECRET`, `JIRA_*` env vars
   - `depends_on:` references
   - Network name: `jira-network` -> `avionics-systems-network`
   - Volume names
2. Update `docker-compose.core.yml`, `docker-compose.cloud-shell.yml`, `docker-compose.cloudshell.yml`, `docker-compose.demo.yml`, `docker-compose.migration-dev.yml`, `enterprise-architecture/docker-compose.enterprise.yml`
3. Update `jira-frontend/nginx.conf`: all upstream DNS names
4. Update `postgres/init/init-schemas.sql`: all 16 `CREATE SCHEMA` statements
5. Update `config/services.json` and `config/services-external.json`: service names, JAR names
6. Update `.gitignore`: glob patterns

### Phase 7: Database Migration

1. Create per-service migration files (each service gets one):
```sql
-- V{next}__rebrand_schema.sql
ALTER SCHEMA jira_{service} RENAME TO avionics_systems_{service};
```
2. For cross-schema references (jira_auth referenced from jira_project, jira_issue referenced from jira_workflow, etc.), ensure all schema renames happen in a single coordinated migration or use a consolidated migration script
3. Rename the database itself: `ALTER DATABASE jira_platform RENAME TO avionics_systems_platform;`
4. Create the new DB user: `CREATE USER avionicsadmin WITH PASSWORD '...'; GRANT ALL ON DATABASE avionics_systems_platform TO avionicsadmin;`
5. Update existing data: `UPDATE jira_auth.user_groups SET group_type = 'INTERNAL' WHERE group_type = 'JIRA_INTERNAL';`
6. Update seed data group names: `UPDATE jira_admin.user_groups SET group_name = 'avionics-systems-administrators' WHERE group_name = 'jira-administrators';`

---

## 4. HIGH-RISK AREAS

### Critical Runtime Failure Risks

**1. Docker Service Discovery (HIGHEST RISK)**
- Every `@Value("${*.url:http://jira-{service}:{port}}")` annotation uses Docker DNS names
- Files: `WorkflowIntegrationClient.java` (8 service URLs, lines 33-67), `JdcScriptBindings.java` (6 URLs, lines 26-38), `NotificationDispatchService.java` (3 URLs, lines 30-36), `BulkIssueOperationService.java` (line 42), `CFDSnapshotScheduler.java` (line 32), `JQLSearchService.java` (lines 28, 251), `WorkflowService.java` (lines 37-38), `IncomingMailService.java` (lines 30, 33)
- **Risk**: If Docker Compose service names are updated but Java default URLs are not (or vice versa), inter-service HTTP calls will fail with `UnknownHostException`
- **Mitigation**: Update Java defaults AND Docker Compose service names atomically; use grep to verify zero mismatches

**2. Maven Dependency Resolution**
- `jira-issue-service/pom.xml` depends on `jira-project-service` and `jira-admin-service` (lines 91-98)
- `jira-cluster-commons` is depended on by 12+ services
- **Risk**: If cluster-commons groupId/artifactId is renamed but consumers still reference the old coordinates, `mvn compile` fails immediately
- **Mitigation**: Rename cluster-commons first, `mvn install` it, then update consumers in dependency order

**3. Spring Application Names / Eureka Registration**
- `spring.application.name` in `application.yml` determines Eureka service IDs and Kafka consumer group names
- If services register with new names but other services still look up old names, service discovery fails
- Files: Every service's `application.yml` and `application-docker.yml` (lines 16 and 6 respectively in most services)
- **Mitigation**: Update all services simultaneously; no rolling deployment for this change

**4. Database Schema Names**
- JPA `@Table(schema = "jira_{x}")` annotations exist in ~200 entity classes
- If the PostgreSQL schema is renamed but Java annotations still reference the old name (or vice versa), every query fails with `relation "jira_issue.issues" does not exist`
- **Risk files**: Every entity class in every service (e.g., `IssueEventOutbox.java` line 17, `Project.java` line 15, `Workflow.java` line 25)
- **Mitigation**: Schema rename SQL and Java annotation updates must be deployed together

**5. Flyway Migration Checksums**
- Existing migration files (V1 through V24+ per service) CANNOT be modified -- Flyway validates checksums
- Any change to applied migration content causes `FlywayValidateException` on startup
- **Risk files**: All 150+ SQL migration files under `*/src/main/resources/db/migration/`
- **Mitigation**: Never modify existing migrations; only add NEW migrations for schema renames

**6. Kafka Topic Names**
- `KafkaTopics.java` (lines 6-12) defines 7 topic name constants
- All producers and consumers must switch topic names simultaneously
- **Risk**: Messages published to old topic names will not be consumed if consumers listen on new names
- **Mitigation**: Either use Kafka topic aliases, or stop all services, rename topics, restart all services

**7. Redis Channel Names**
- `RedisClusterEventBus.java` (lines 37, 42) uses `jira:events:` prefix
- `ClusterProperties.java` (line 57) uses `jira:cache:invalidation`
- During rolling deployment, old and new nodes will use different channel names
- **Mitigation**: Deploy all nodes simultaneously or use a transitional period subscribing to both

**8. JWT Secret Synchronization**
- Default JWT secret `jira-platform-super-secret-key-...` appears in every service
- Files: `application-docker.yml` across all services, `application-local.yml`, gateway configs
- **Risk**: If some services have the old default and others have the new default, JWT validation fails across service boundaries
- **Mitigation**: Use environment variable `JWT_SECRET` rather than relying on defaults

**9. Frontend API Endpoint Paths**
- `serviceApi.ts` calls `/api/migration/import/jira-dc` (lines 240, 266)
- `MigrationController.java` defines `@PostMapping("/import/jira-dc")` (line 136)
- **Risk**: This is a PUBLIC API contract; changing it breaks existing client integrations
- **Mitigation**: Add new endpoint path AND keep old path as deprecated alias, or document as breaking change

**10. Nginx Upstream DNS**
- `jira-frontend/nginx.conf` references `jira-gateway:8080`, `jira-workflow-service:8085`, `jira-issue-service:8084`, `jira-migration-service:8094` (lines 11, 26, 39, 52, 66, 80, 95, 110, 123, 127, 144)
- These MUST match the Docker Compose service names exactly
- **Mitigation**: Update nginx.conf and docker-compose.yml in the same commit

---

## 5. TOOLS AND AUTOMATION

### Script 1: Java Package Directory Rename (per service)

```bash
#!/bin/bash
# rename-java-packages.sh <service-dir>
SERVICE_DIR="$1"
# Move source directories
for root in src/main/java src/test/java; do
  if [ -d "$SERVICE_DIR/$root/com/jira" ]; then
    mkdir -p "$SERVICE_DIR/$root/com/avionics_systems"
    cp -r "$SERVICE_DIR/$root/com/jira/"* "$SERVICE_DIR/$root/com/avionics_systems/"
    rm -rf "$SERVICE_DIR/$root/com/jira"
  fi
done
```

### Script 2: Bulk Text Replacement (per service)

```bash
#!/bin/bash
# bulk-rename.sh <service-dir>
SERVICE_DIR="$1"
# Java files: package and import
find "$SERVICE_DIR" -name "*.java" -exec sed -i \
  -e 's/package com\.jira\./package com.avionics_systems./g' \
  -e 's/import com\.jira\./import com.avionics_systems./g' \
  -e 's/"com\.jira\./"com.avionics_systems./g' \
  -e 's/"com\\\\\.jira\\\\\./"com\\\\.avionics_systems\\\\./g' \
  {} +
# YAML files
find "$SERVICE_DIR" -name "*.yml" -exec sed -i \
  -e 's/jira-'"$(basename $SERVICE_DIR | sed 's/jira-//')"'/avionics-systems-'"$(basename $SERVICE_DIR | sed 's/jira-//')"'/g' \
  -e 's/jira_platform/avionics_systems_platform/g' \
  -e 's/com\.jira/com.avionics_systems/g' \
  {} +
```

### Script 3: JPA Schema Annotation Update

```bash
# Update all @Table schema annotations
find . -name "*.java" -exec sed -i \
  's/schema = "jira_\([a-z_]*\)"/schema = "avionics_systems_\1"/g' {} +
```

### Script 4: Docker Compose Rename

```bash
# Process all docker-compose files
for f in docker-compose*.yml enterprise-architecture/docker-compose*.yml; do
  sed -i \
    -e 's/jira-network/avionics-systems-network/g' \
    -e 's/container_name: jira-/container_name: avionics-systems-/g' \
    -e 's/image: jira-/image: avionics-systems-/g' \
    -e 's/image: ghcr.io\/ms86100\/jira-/image: ghcr.io\/ms86100\/avionics-systems-/g' \
    -e 's/context: .\/jira-/context: .\/avionics-systems-/g' \
    -e 's/POSTGRES_USER: jiraadmin/POSTGRES_USER: avionicsadmin/g' \
    -e 's/POSTGRES_DB: jira_platform/POSTGRES_DB: avionics_systems_platform/g' \
    -e 's/DB_NAME:-jira_platform/DB_NAME:-avionics_systems_platform/g' \
    -e 's/jira-platform-super-secret-key/avionics-systems-platform-super-secret-key/g' \
    "$f"
done
```

### Script 5: Frontend CSS Class Rename

```bash
# Tailwind/CSS bulk rename
find jira-frontend/src -name "*.tsx" -o -name "*.ts" -o -name "*.css" | \
  xargs sed -i \
    -e 's/jira-blue/avisys-blue/g' \
    -e 's/jira-navy/avisys-navy/g' \
    -e 's/text-jira-/text-avisys-/g' \
    -e 's/bg-jira-/bg-avisys-/g' \
    -e 's/border-jira-/border-avisys-/g' \
    -e 's/font-jira/font-avisys/g' \
    -e 's/\.jira-/.avisys-/g' \
    -e 's/\.ab-jira-/.ab-avisys-/g'
```

### Script 6: Service Folder Mass Rename

```bash
#!/bin/bash
# rename-folders.sh -- run from project root
for dir in jira-*; do
  newname="avionics-systems-${dir#jira-}"
  git mv "$dir" "$newname"
done
```

### IDE Refactoring Recommendations

1. **IntelliJ IDEA**: Use "Refactor > Move Package" for `com.jira` -> `com.avionics_systems` per service. This handles package declarations, imports, and file moves atomically. Then use "Refactor > Rename" on each `Jira*Application` class.
2. **VS Code**: Use "Search and Replace in Files" with regex for frontend bulk renames. Install the "Rename Symbol" extension for TSX component renames.
3. **Maven**: After renaming, run `mvn clean install -DskipTests` from root to validate all coordinates resolve.

---

## 6. VERIFICATION CHECKLIST

### Build Verification

- [ ] `mvn clean compile` passes from root (all 23 modules)
- [ ] `mvn clean package -DskipTests` produces JARs with new names
- [ ] `mvn clean test` -- all unit tests pass (ArchUnit tests in particular verify package names)
- [ ] Zero occurrences of `com.jira` in any compiled `.class` file: `find . -name "*.class" -exec strings {} \; | grep "com\.jira\." | head`
- [ ] Zero occurrences of `jira` in any `pom.xml` (except comments about the migration): `grep -r "jira" --include="pom.xml" . | grep -v "<!-- kept for migration compatibility -->"`

### Grep Verification

```bash
# Zero hits expected (excluding migration SQL comments and Atlassian SDK refs):
grep -rn "com\.jira\." --include="*.java" --include="*.yml" --include="*.xml" --include="*.properties" . \
  | grep -v "com\.atlassian\.jira" \
  | grep -v "com\.pyxis\.greenhopper\.jira" \
  | grep -v "src/main/resources/db/migration/V[0-9]" \
  | wc -l
# Expected: 0
```

### Docker Compose Verification

- [ ] `docker-compose -f docker-compose.yml config` validates with no errors
- [ ] `docker-compose build` succeeds for all services
- [ ] `docker-compose up -d postgres redis kafka zookeeper minio` -- infrastructure starts
- [ ] `docker-compose up -d` -- all 22 services + gateway + frontend start
- [ ] `docker-compose ps` -- all containers healthy
- [ ] `docker-compose logs --tail=50 avionics-systems-auth-service` -- no `FlywayValidateException`
- [ ] `docker-compose logs --tail=50 avionics-systems-issue-service` -- no `UnknownHostException`

### Database Verification

```sql
-- Verify all schemas renamed:
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name LIKE 'jira_%';
-- Expected: 0 rows

SELECT schema_name FROM information_schema.schemata 
WHERE schema_name LIKE 'avionics_systems_%';
-- Expected: 16+ schemas

-- Verify Flyway history intact:
SELECT * FROM avionics_systems_auth.flyway_schema_history ORDER BY installed_rank;
-- Should show all applied migrations with valid checksums
```

### API Endpoint Testing

- [ ] Gateway health: `curl http://localhost:8080/actuator/health`
- [ ] Auth login: `POST http://localhost:8080/auth-service/api/auth/login`
- [ ] Issue CRUD: `GET http://localhost:8080/issue-service/api/issues`
- [ ] Project list: `GET http://localhost:8080/project-service/api/projects`
- [ ] Swagger UI: `http://localhost:8080/swagger-ui-landing.html` -- no "Jira" text visible
- [ ] Frontend: `http://localhost:3000` -- loads without console errors
- [ ] Migration import: `POST http://localhost:8080/migration-service/api/migration/import/legacy-dc` -- endpoint responds

### Inter-Service Communication

- [ ] Create an issue in project -- verifies issue-service -> project-service communication
- [ ] Transition an issue -- verifies issue-service -> workflow-service communication
- [ ] Add a comment -- verifies comment-service standalone
- [ ] Run a search -- verifies search-service -> issue-service cross-schema queries
- [ ] Check notification delivery -- verifies notification-service -> user-service lookup

### Frontend Verification

- [ ] `npm run build` in `avionics-systems-frontend/` -- no compilation errors
- [ ] `npm run lint` -- no import resolution failures
- [ ] Visual regression: no broken CSS (spot-check admin layout, board view, issue detail)
- [ ] Browser console: zero 404s for renamed endpoints
- [ ] Zero visible "Jira" or "jira" text in the entire UI (search page source)

### Kafka/Redis Verification

- [ ] Issue events flow through new topic `avionics-systems.issue.events`
- [ ] Cache invalidation works across nodes via new Redis channel
- [ ] No orphaned messages on old `jira.*` topics