export const meta = {
  name: 'ip-rebranding-jira-to-avionics-systems',
  description: 'Rename all 23074 jira references to avionics_systems across entire codebase',
  phases: [
    { title: 'RebrandCommons', detail: 'Rebrand shared library first (all services depend on it)' },
    { title: 'RebrandServices', detail: 'Rebrand all 24 services in parallel (content changes only)' },
    { title: 'RebrandFrontend', detail: 'Rebrand frontend (React/TypeScript/CSS)' },
    { title: 'StructuralRenames', detail: 'Rename folders, update Docker/Maven/infra' },
    { title: 'Build', detail: 'Compile verification and fix-up' },
  ],
}

const PROJECT_ROOT = 'c:/Users/SSHABNSA/Desktop/test/JDC-main'

const NAMING_MAP = `
## Naming Convention Map — FOLLOW EXACTLY
| Context | jira → | Example |
|---------|--------|---------|
| Java package | com.avionics_systems | com.avionics_systems.issue.service |
| Class name prefix "Jira" | AvionicsSystem | AvionicsSystemIssueServiceApplication |
| Class name prefix "JiraDc" | LegacyDc | LegacyDcXmlParser |
| Maven groupId | com.avionics_systems | <groupId>com.avionics_systems</groupId> |
| Maven artifactId | avionics-systems-xxx | avionics-systems-issue-service |
| Spring app name | avionics-systems-xxx | name: avionics-systems-issue-service |
| Config namespace jira: | avionics-systems: | avionics-systems.workflow.xxx |
| @Value \${jira.xxx} | \${avionics-systems.xxx} | @Value("\\$\\{avionics-systems.permissions.fail-open}") |
| DB schema jira_xxx | avionics_systems_xxx | avionics_systems_issue |
| DB name avionics_systems_platform | avionics_systems_platform | jdbc:...avionics_systems_platform |
| DB user avionicsadmin | avionicsadmin | username: avionicsadmin |
| Kafka group-id | avionics-systems-xxx | avionics-systems-issue-service |
| Kafka trusted pkg | com.avionics_systems.xxx | com.avionics_systems.issue.events |
| Kafka topics jira.xxx | avionics-systems.xxx | avionics-systems.issue.events |
| Redis channels jira: | avionics-systems: | avionics-systems:events:xxx |
| Docker service/image | avionics-systems-xxx | avionics-systems-issue-service |
| Docker user/group jira | appuser | RUN adduser -S appuser |
| Docker network | avionics-systems-network | networks: avionics-systems-network |
| GHCR image | ghcr.io/ms86100/avionics-systems-xxx | ghcr.io/ms86100/avionics-systems-gateway |
| Env var AVIONICS_SYSTEMS_XXX | AVIONICS_SYSTEMS_XXX | AVIONICS_SYSTEMS_PERMISSIONS_FAILOPEN |
| Logging com.avionics_systems | com.avionics_systems | com.avionics_systems.issue: DEBUG |
| S3 bucket jira-xxx | avionics-systems-xxx | avionics-systems-attachments |
| Volume /var/jira | /var/avionics-systems | /var/avionics-systems/attachments |
| JWT secret jira-platform | avionics-systems-platform | avionics-systems-platform-super-secret-key |
| SAML entity jira-platform-sp | avionics-systems-platform-sp | entityId: avionics-systems-platform-sp |
| Email @jira.local | @avionics-systems.local | noreply@avionics-systems.local |
| Comment/Javadoc "Jira" | "Avionics Systems" | * Avionics Systems Issue Service |
`

const CONTENT_CHANGE_INSTRUCTIONS = `
## What to Change (Content Only — NO file/folder renames in this phase)

### 1. ALL Java files in src/main/java/ and src/test/java/
For EVERY .java file:
- package com.avionics_systems.{domain} → package com.avionics_systems.{domain}
- import com.avionics_systems.{anything} → import com.avionics_systems.{anything}
- "com.avionics_systems." (string literals) → "com.avionics_systems."
- "com\\\\.jira\\\\." (regex patterns) → "com\\\\.avionics_systems\\\\."
- basePackages = "com.avionics_systems.xxx" → basePackages = "com.avionics_systems.xxx"
- @Table(schema = "jira_xxx") → @Table(schema = "avionics_systems_xxx")
- @Value("\\$\\{jira.xxx}") → @Value("\\$\\{avionics-systems.xxx}")
- Class names starting with "Jira" → "AvionicsSystem" prefix (update class declaration AND file must be renamed)
- Class names starting with "JiraDc" → "LegacyDc" prefix
- Javadoc/comments mentioning "Jira" → "Avionics Systems" or "avionics-systems"
- Log messages mentioning "Jira" → "Avionics Systems"
- String literals containing "jira" → appropriate replacement per context

### 2. pom.xml
- <groupId>com.avionics_systems</groupId> → <groupId>com.avionics_systems</groupId>
- <artifactId>jira-xxx</artifactId> → <artifactId>avionics-systems-xxx</artifactId>
- <name>jira-xxx</name> → <name>avionics-systems-xxx</name>
- <description>Jira xxx</description> → <description>Avionics Systems xxx</description>
- All <dependency> groupId/artifactId references to jira → avionics_systems/avionics-systems
- mainClass references: com.avionics_systems → com.avionics_systems, JiraXxx → AvionicsSystemXxx

### 3. application.yml / application-*.yml / bootstrap.yml
- spring.application.name: jira-xxx → avionics-systems-xxx
- JDBC URL default: avionics_systems_platform → avionics_systems_platform
- hibernate.default_schema: jira_xxx → avionics_systems_xxx
- jira: (top-level config key) → avionics-systems:
- Kafka group-id: jira-xxx → avionics-systems-xxx
- Kafka trusted.packages: com.avionics_systems → com.avionics_systems
- Logging: com.avionics_systems → com.avionics_systems
- Any inter-service URL defaults: http://avionics-systems-xxx → http://avionics-systems-xxx

### 4. Dockerfile
- User/group "jira" → "appuser"
- Any COPY/ADD paths referencing jira → avionics-systems
- Any labels/env with jira → avionics-systems

### 5. META-INF / Spring autoconfiguration files
- Any class references: com.avionics_systems → com.avionics_systems

### 6. logback-spring.xml / logback.xml
- com.avionics_systems → com.avionics_systems

### 7. Create schema rename migration
Add a NEW Flyway migration (next version number) that renames the schema:
ALTER SCHEMA jira_{domain} RENAME TO avionics_systems_{domain};
DO NOT modify any existing migration files.

## CRITICAL RULES
- DO NOT modify existing Flyway migration .sql files — only ADD a new one for schema rename
- DO NOT rename files or folders yet — only change file CONTENTS
- For class renames (JiraXxx → AvionicsSystemXxx): change the class name INSIDE the file, but do NOT rename the .java file yet (that happens in Phase 4)
- Actually, DO rename .java files whose name starts with Jira (use bash mv command) since the filename must match the public class name
- When renaming class files, update the class name, constructor name, and any self-references
- Ensure all import statements are updated consistently
- Handle both src/main/java AND src/test/java
`

// =============================================
// PHASE 1: REBRAND CLUSTER-COMMONS (must be first)
// =============================================
phase('RebrandCommons')
log('Rebranding jira-cluster-commons (shared library)...')

await agent(`You are performing an IP rebranding. Rename ALL "jira" references in the jira-cluster-commons module.

PROJECT ROOT: ${PROJECT_ROOT}
SERVICE: jira-cluster-commons/
DOMAIN PACKAGES: cluster (with sub-packages: archival, async, auth, cache, config, constants, datasource, event, health, idempotency, and possibly more like util)

${NAMING_MAP}

${CONTENT_CHANGE_INSTRUCTIONS}

## SPECIFIC TO THIS MODULE
- This is a SHARED LIBRARY used by all services. Get it right.
- Package root: com.avionics_systems.cluster → com.avionics_systems.cluster
- Has Spring Boot auto-configuration in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports — update ALL class references
- KafkaTopics.java: rename topic constants from jira.xxx to avionics-systems.xxx
- RedisClusterEventBus.java: rename channel prefixes from jira: to avionics-systems:
- ClusterProperties.java: rename property prefixes and defaults
- HealthAutoConfiguration.java → AvionicsSystemsHealthAutoConfiguration.java (rename class AND file)
- Any class starting with "Jira" in this module → rename to AvionicsSystem prefix

## Steps
1. Read ALL Java files to understand the full scope
2. Move the package directory: use bash to move src/main/java/com/jira/cluster/ to src/main/java/com/avionics_systems/cluster/
3. Same for src/test/java/ if it exists
4. Update ALL file contents (package declarations, imports, class names, strings)
5. Update pom.xml (groupId, artifactId, name)
6. Update application.yml files
7. Update META-INF auto-configuration files
8. Update logback config if present
9. Rename any Java files with "Jira" in the filename

After you're done, run: mvn compile -pl jira-cluster-commons -f "${PROJECT_ROOT}/pom.xml" 2>&1 | tail -50
If compilation errors, fix them.`, {
  label: 'rebrand:cluster-commons',
  phase: 'RebrandCommons',
  effort: 'high'
})

log('Cluster-commons rebranded. Now rebranding all services in parallel...')

// =============================================
// PHASE 2: REBRAND ALL BACKEND SERVICES (parallel)
// =============================================
phase('RebrandServices')

const SERVICES = [
  { name: 'avionics-systems-issue-service', domain: 'issue', schema: 'avionics_systems_issue', nextMigration: 'V26', classPrefix: 'JiraIssueServiceApplication' },
  { name: 'avionics-systems-project-service', domain: 'project', schema: 'avionics_systems_project', nextMigration: 'V10', classPrefix: 'JiraProjectServiceApplication' },
  { name: 'avionics-systems-workflow-service', domain: 'workflow', schema: 'avionics_systems_workflow', nextMigration: 'V27', classPrefix: 'JiraWorkflowServiceApplication' },
  { name: 'avionics-systems-sprint-service', domain: 'sprint', schema: 'avionics_systems_sprint', nextMigration: 'V9', classPrefix: 'JiraSprintServiceApplication', extraPackages: 'board' },
  { name: 'avionics-systems-plan-service', domain: 'plan', schema: 'avionics_systems_plan', nextMigration: 'V31', classPrefix: 'JiraPlanServiceApplication', extraPackages: 'admin' },
  { name: 'avionics-systems-user-service', domain: 'user', schema: 'avionics_systems_admin', nextMigration: 'V5', classPrefix: 'JiraUserServiceApplication' },
  { name: 'avionics-systems-auth-service', domain: 'auth', schema: 'avionics_systems_auth', nextMigration: 'V3', classPrefix: 'JiraAuthServiceApplication' },
  { name: 'avionics-systems-notification-service', domain: 'notification', schema: 'avionics_systems_notification', nextMigration: 'V6', classPrefix: 'JiraNotificationServiceApplication' },
  { name: 'avionics-systems-search-service', domain: 'search', schema: 'avionics_systems_search', nextMigration: 'V3', classPrefix: 'JiraSearchServiceApplication' },
  { name: 'avionics-systems-report-service', domain: 'report', schema: 'jira_report', nextMigration: 'V2', classPrefix: 'JiraReportServiceApplication' },
  { name: 'avionics-systems-comment-service', domain: 'comment', schema: 'avionics_systems_comment', nextMigration: 'V4', classPrefix: 'JiraCommentServiceApplication' },
  { name: 'avionics-systems-component-service', domain: 'component', schema: 'avionics_systems_component', nextMigration: 'V3', classPrefix: 'JiraComponentServiceApplication' },
  { name: 'avionics-systems-version-service', domain: 'version', schema: 'avionics_systems_version', nextMigration: 'V3', classPrefix: 'JiraVersionServiceApplication' },
  { name: 'avionics-systems-attachment-service', domain: 'attachment', schema: 'avionics_systems_attachment', nextMigration: 'V2', classPrefix: 'JiraAttachmentServiceApplication' },
  { name: 'avionics-systems-audit-service', domain: 'audit', schema: 'avionics_systems_audit', nextMigration: 'V4', classPrefix: 'JiraAuditServiceApplication' },
  { name: 'avionics-systems-dashboard-service', domain: 'dashboard', schema: 'jira_dashboard', nextMigration: 'V3', classPrefix: 'JiraDashboardServiceApplication' },
  { name: 'avionics-systems-document-service', domain: 'document', schema: 'jira_document', nextMigration: 'V2', classPrefix: 'JiraDocumentServiceApplication' },
  { name: 'avionics-systems-admin-service', domain: 'admin', schema: 'avionics_systems_admin', nextMigration: 'V11', classPrefix: 'JiraAdminServiceApplication' },
  { name: 'avionics-systems-portal-service', domain: 'portal', schema: 'jira_portal', nextMigration: 'V2', classPrefix: 'JiraPortalServiceApplication' },
  { name: 'avionics-systems-test-service', domain: 'test', schema: 'jira_test', nextMigration: 'V15', classPrefix: 'JiraTestServiceApplication' },
  { name: 'avionics-systems-migration-service', domain: 'migration', schema: 'avionics_systems_migration', nextMigration: 'V26', classPrefix: 'JiraMigrationServiceApplication' },
  { name: 'avionics-systems-marketplace-plugin', domain: 'plugin', schema: null, nextMigration: null, classPrefix: 'JiraMarketplacePluginApplication' },
  { name: 'avionics-systems-gateway', domain: 'gateway', schema: null, nextMigration: null, classPrefix: 'JiraGatewayApplication' },
]

const serviceResults = await parallel(SERVICES.map(svc => () =>
  agent(`You are performing an IP rebranding. Rename ALL "jira" references in ${svc.name}.

PROJECT ROOT: ${PROJECT_ROOT}
SERVICE: ${svc.name}/
DOMAIN PACKAGE: ${svc.domain}${svc.extraPackages ? ' (also has sub-package: ' + svc.extraPackages + ' under com.avionics_systems)' : ''}
DB SCHEMA: ${svc.schema || 'none'}
APP CLASS: ${svc.classPrefix}

${NAMING_MAP}

${CONTENT_CHANGE_INSTRUCTIONS}

## SCHEMA RENAME MIGRATION
${svc.schema && svc.nextMigration ?
  'Create file: src/main/resources/db/migration/' + svc.nextMigration + '__rename_schema_to_avionics_systems.sql\nContent:\nALTER SCHEMA ' + svc.schema + ' RENAME TO avionics_systems_' + svc.schema.replace('jira_', '') + ';\n\nAlso update ALL @Table(schema="' + svc.schema + '") annotations to schema="avionics_systems_' + svc.schema.replace('jira_', '') + '"'
  : 'No database schema for this service — skip migration creation.'}

## SPECIFIC NOTES FOR ${svc.name}
- Application class: ${svc.classPrefix} → rename to AvionicsSystem${svc.classPrefix.replace('Jira', '').replace('ServiceApplication', 'ServiceApplication')}
- Main class reference in pom.xml must be updated to match
- Check for any test classes that reference the old class name
- Check application.yml for inter-service URLs like http://avionics-systems-xxx-service:port → http://avionics-systems-xxx-service:port
- The cluster-commons dependency has ALREADY been rebranded to com.avionics_systems / avionics-systems-cluster-commons — use the new coordinates
${svc.domain === 'user' ? '- NOTE: This service uses avionics_systems_admin schema (shared with admin-service). The schema rename migration should only be in ONE service (admin-service handles it). Just update @Table annotations here.' : ''}
${svc.domain === 'sprint' ? '- NOTE: Has TWO domain packages under com.avionics_systems: "sprint" AND "board". Move BOTH to com.avionics_systems.' : ''}
${svc.domain === 'plan' ? '- NOTE: Has TWO domain packages under com.avionics_systems: "plan" AND "admin". Move BOTH to com.avionics_systems.' : ''}

## Steps
1. Read the full list of Java files to understand scope
2. Move package directories: bash mv com/jira/${svc.domain}/ → com/avionics_systems/${svc.domain}/${svc.extraPackages ? '\n   Also mv com/jira/' + svc.extraPackages + '/ → com/avionics_systems/' + svc.extraPackages + '/' : ''}
3. Remove empty com/jira/ directory after move
4. Update ALL Java file contents (package, imports, class names, strings, annotations)
5. Rename Java files with "Jira" in filename (mv JiraXxx.java → AvionicsSystemXxx.java)
6. Update pom.xml coordinates and dependencies
7. Update ALL application*.yml files
8. Update Dockerfile if present
9. Update logback config if present
10. Update META-INF files if present
11. Create schema rename migration if applicable
12. Verify: grep -r "com\\.jira" in the service directory should return ZERO results (except in existing SQL migrations)`, {
    label: 'rebrand:' + svc.name,
    phase: 'RebrandServices',
    effort: 'high'
  })
))

const svcSuccess = serviceResults.filter(Boolean).length
log('Rebranded ' + svcSuccess + '/' + SERVICES.length + ' backend services')

// =============================================
// PHASE 3: REBRAND FRONTEND
// =============================================
phase('RebrandFrontend')
log('Rebranding frontend...')

await agent(`You are performing an IP rebranding. Rename ALL "jira" references in jira-frontend.

PROJECT ROOT: ${PROJECT_ROOT}
DIRECTORY: jira-frontend/

## Naming Convention for Frontend
| Context | jira → | Example |
|---------|--------|---------|
| package.json name | avionics-systems-frontend | "name": "avionics-systems-frontend" |
| Tailwind theme key | avisys | colors: { avisys: { blue: '#xxx' } } |
| CSS class prefix .jira- | .avisys- | .avisys-admin-root |
| CSS class .ab-jira- | .ab-avisys- | .ab-avisys-toolbar |
| Component name Jira* | AviSys* | AviSysGlobalLayout |
| React query key 'jira*' | 'avisys*' | queryKey: ['avisysUsers'] |
| API type JiraDc* | LegacyDc* | LegacyDcValidateResponse |
| Import type 'avionics-systems-dc' | 'legacy-dc' | type: 'legacy-dc' |
| localStorage jira-* | avisys-* | avisys-test-settings |
| Env var VITE_AVIONICS_SYSTEMS_* | VITE_AVISYS_* | VITE_AVISYS_API_URL |
| Display text "Jira" | "Avionics Systems" | <title>Avionics Systems</title> |
| API path /jira/ | /avisys/ or just remove | fetch('/api/avisys/...') |
| CSS file jira-*.css | avisys-*.css | avisys-classic.css |
| Font family jira* | avisys* | font-family: 'avisys-sans' |

## What to Change
1. package.json — name, any scripts referencing jira
2. ALL .tsx, .ts, .jsx, .js files in src/:
   - Component names: Jira* → AviSys*
   - Import paths referencing jira
   - String literals: "Jira" → "Avionics Systems" (user-facing), "jira" → "avisys" (technical)
   - API endpoint strings
   - React query keys
   - CSS class references
   - localStorage keys
3. ALL .css, .scss files:
   - Class names with jira → avisys
   - Font references
   - Color variable names
4. tailwind.config.js / tailwind.config.ts:
   - Theme key jira → avisys
5. vite.config.* / webpack.config.*:
   - Any jira references
6. index.html:
   - <title>, meta tags, link tags
7. public/ directory:
   - Any manifest.json or meta files
8. nginx.conf:
   - Upstream server names: jira-gateway → avionics-systems-gateway
   - jira-workflow-service → avionics-systems-workflow-service
   - jira-issue-service → avionics-systems-issue-service
   - jira-migration-service → avionics-systems-migration-service
   - All other service references
9. .env files if present
10. Rename component files with Jira in filename → AviSys

## Steps
1. Read key files to understand scope (package.json, tailwind config, nginx.conf, main app file)
2. Use Grep to find ALL "jira" references (case insensitive) across the frontend
3. Systematically edit each file
4. Rename files with "Jira" in the name
5. Verify: grep -ri "jira" in jira-frontend/src/ should return ZERO hits`, {
  label: 'rebrand:frontend',
  phase: 'RebrandFrontend',
  effort: 'high'
})

log('Frontend rebranded')

// =============================================
// PHASE 4: STRUCTURAL RENAMES (folders, Docker, Maven root)
// =============================================
phase('StructuralRenames')
log('Performing structural renames (folders, Docker, Maven, infra)...')

await agent(`You are performing the final structural renames for the IP rebranding from "jira" to "avionics-systems".

PROJECT ROOT: ${PROJECT_ROOT}

All file CONTENTS have already been rebranded in previous phases. Now you need to:

## 1. Rename ALL Top-Level Service Directories
Run these bash commands:
mv jira-cluster-commons avionics-systems-cluster-commons
mv jira-admin-service avionics-systems-admin-service
mv jira-attachment-service avionics-systems-attachment-service
mv jira-audit-service avionics-systems-audit-service
mv jira-auth-service avionics-systems-auth-service
mv jira-comment-service avionics-systems-comment-service
mv jira-component-service avionics-systems-component-service
mv jira-dashboard-service avionics-systems-dashboard-service
mv jira-document-service avionics-systems-document-service
mv jira-gateway avionics-systems-gateway
mv jira-issue-service avionics-systems-issue-service
mv jira-marketplace-plugin avionics-systems-marketplace-plugin
mv jira-migration-service avionics-systems-migration-service
mv jira-notification-service avionics-systems-notification-service
mv jira-plan-service avionics-systems-plan-service
mv jira-portal-service avionics-systems-portal-service
mv jira-project-service avionics-systems-project-service
mv jira-report-service avionics-systems-report-service
mv jira-search-service avionics-systems-search-service
mv jira-sprint-service avionics-systems-sprint-service
mv jira-test-service avionics-systems-test-service
mv jira-user-service avionics-systems-user-service
mv jira-version-service avionics-systems-version-service
mv jira-workflow-service avionics-systems-workflow-service
mv jira-frontend avionics-systems-frontend
mv jira-backend avionics-systems-backend (if exists and not already renamed)

## 2. Update Root pom.xml
Read pom.xml at project root. Update:
- <groupId>com.avionics_systems</groupId> → <groupId>com.avionics_systems</groupId>
- <artifactId>jira-platform</artifactId> → <artifactId>avionics-systems-platform</artifactId>
- <name> and <description>
- ALL <module> entries: jira-xxx → avionics-systems-xxx
- ALL dependency management entries referencing jira

## 3. Update ALL Docker Compose Files
Find and update ALL docker-compose*.yml files:
- docker-compose.yml
- docker-compose.core.yml
- docker-compose.cloud-shell.yml
- docker-compose.cloudshell.yml
- docker-compose.demo.yml
- docker-compose.migration-dev.yml
- enterprise-architecture/docker-compose.enterprise.yml (if exists)

In each file, update:
- Service names (YAML keys): jira-xxx → avionics-systems-xxx
- container_name: jira-xxx → avionics-systems-xxx
- image: jira-xxx → avionics-systems-xxx
- image: ghcr.io/ms86100/avionics-systems-xxx → ghcr.io/ms86100/avionics-systems-xxx
- build.context: ./avionics-systems-xxx → ./avionics-systems-xxx
- depends_on references
- environment variables: DB_NAME avionics_systems_platform → avionics_systems_platform, DB_USER avionicsadmin → avionicsadmin
- JWT_SECRET defaults containing jira → avionics-systems
- AVIONICS_SYSTEMS_xxx env vars → AVIONICS_SYSTEMS_xxx
- networks: avionics-systems-network → avionics-systems-network
- volumes with jira → avionics-systems
- Any S3/MinIO bucket names with jira → avionics-systems

## 4. Update Postgres Init Scripts
File: postgres/init/init-schemas.sql (or similar)
- All CREATE SCHEMA jira_xxx → avionics_systems_xxx
- Any default user/db references

## 5. Update Config Files
- config/services.json: service names, JAR names
- config/services-external.json: same
- Any other config files with jira references

## 6. Update Scripts
- scripts/*.sh, scripts/*.ps1, scripts/*.js, scripts/*.py
- Replace jira references with avionics-systems

## 7. Update .gitignore
- Any glob patterns with jira → avionics-systems

## 8. Verify
After all renames, run:
grep -r "jira" --include="*.yml" --include="*.yaml" --include="*.xml" --include="*.json" --include="*.sh" --include="*.properties" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/V" | head -50

This should return minimal results (only existing SQL migrations which we don't modify).`, {
  label: 'rebrand:structural',
  phase: 'StructuralRenames',
  effort: 'high'
})

log('Structural renames complete')

// =============================================
// PHASE 5: BUILD VERIFICATION
// =============================================
phase('Build')
log('Running build verification...')

const buildResult = await agent(`You are a build engineer. The entire codebase has been rebranded from "jira" to "avionics-systems". Verify compilation.

PROJECT ROOT: ${PROJECT_ROOT}

NOTE: All service directories have been renamed from jira-* to avionics-systems-*. The root pom.xml module names have been updated accordingly.

## Steps

1. First check if the directory renames happened correctly:
   ls -d avionics-systems-*/ | head -30

2. Run Maven compilation:
   mvn compile -f pom.xml -T 1C 2>&1 | tail -200

3. If compilation fails, analyze the errors:
   Common issues after rebranding:
   a. Missed package rename — file still has "com.avionics_systems" package declaration
   b. Missed import — import still references "com.avionics_systems"
   c. Class name mismatch — filename says AvionicsSystem but class declaration still says Jira
   d. Missed @Table schema — annotation still says jira_xxx instead of avionics_systems_xxx
   e. Missed @Value property — still references \${jira.xxx}
   f. Missed Spring auto-configuration — META-INF file references old class name
   g. Maven coordinates mismatch — dependency references old jira artifactId
   h. Main class reference in pom.xml points to old class name
   i. Resource files still referencing old packages

4. Fix each error:
   - Read the failing file
   - Identify the remaining "jira" reference
   - Fix it
   - Re-run compilation for that module: mvn compile -pl avionics-systems-xxx

5. Iterate until the FULL build passes

6. Final verification:
   - grep -r "com\\.jira\\." --include="*.java" . | grep -v node_modules | grep -v ".git/" | grep -v "db/migration/" | wc -l
   This should be ZERO.

Fix ALL compilation errors. The build MUST succeed.`, {
  label: 'build:verify',
  phase: 'Build',
  effort: 'high'
})

return { svcSuccess, buildResult }
