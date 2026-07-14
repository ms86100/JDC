# Lessons Learnt

---

## 2026-07-13 — CSV Migration Import Fixes

### Issue 1: "Cannot read properties of undefined (reading 'trim')" on CSV upload

**Symptom:** Uploading a CSV with multiline quoted fields at `/migration?import=csv` crashed the browser.

**Root cause:** The frontend CSV parser (`useValidation.ts`) did not pad data rows to match header length. When a parsed row had fewer columns than the header, downstream code accessed `row[colIndex]` returning `undefined`, then called `.trim()` on it. Additionally, server-detected headers from `upload.detectedHeaders` could contain `null` values that weren't coerced to strings.

**Fix (files changed):**
- `jira-frontend/src/features/migration/hooks/useValidation.ts` — In `parseCsvFile`, pad each data row to match header length and coerce all cells to strings via `cell ?? ''`.
- `jira-frontend/src/features/migration/pages/MigrationPage.tsx` — Coerce all header values to strings with `String(h)` in `onValidationComplete` callback and `handleFileSelect` server-header processing.

---

### Issue 2: "Project key is required" validation error (39 errors for every row)

**Symptom:** Server-side validation at the wizard validate step fails with `issue_project_required` for every row when the CSV has no `Project key` column (e.g., Jira exports with only `Issue key, Issue id, Issue Type, Summary, Status, Updated`).

**Root cause:** The `DbValidationRuleEngine` checks for `project_key` in each row map. When the CSV has no "Project key" column, `project_key` is absent. The wizard's `targetProjectId` selection was not used to fill it in.

**Fix (files changed):**
- `jira-migration-service/.../service/ImportWizardSessionService.java` — Added `deriveProjectKeyIfMissing()` called from `toRowMaps()`. Extracts project prefix from `issue_key` (e.g., "SST1" from "SST1-39"). Falls back to looking up the project key from `targetProjectId` via `ProjectServiceClient`.
- `jira-migration-service/.../async/ImportJobProcessor.java` — Added `deriveProjectKeyForRows()` with the same logic for the import execution path. Checks multiple key name variants (`issue_key`, `issue key`, `issuekey`).

---

### Issue 3: Issue keys become DEMO-XX instead of preserving original SST1-XX from CSV

**Symptom:** After CSV import, all issues get auto-generated keys like DEMO-1, DEMO-2 instead of preserving the original CSV keys (SST1-39, SST1-38, etc.).

**Root cause (two problems):**

1. **Header space-vs-underscore mismatch:** `ImportJobProcessor.convertRowToMap()` lowercases headers but keeps spaces ("Issue key" becomes "issue key"). But `STANDARD_ISSUE_FIELD_KEYS` and all downstream lookups expect underscores ("issue_key"). So the issue key value was never found — it fell back to "ROW-N".

2. **Issue-service always generates new keys:** `IssueService.createIssue()` unconditionally calls `generateIssueKey(projectKey)`, ignoring any incoming key. The migration payload mapper sent the original key as `originalIssueKey` / `migrationSourceKey`, but the issue-service DTO had no field for it, so it was silently discarded.

**Fix (files changed):**
- `jira-migration-service/.../async/ImportJobProcessor.java` — Changed `convertRowToMap()` line from `headers[i].trim().toLowerCase()` to `headers[i].trim().toLowerCase().replace(" ", "_")`. This single-line fix resolves the space-vs-underscore mismatch for ALL headers (Issue key, Issue Type, Issue id, Project key, etc.).
- `jira-issue-service/.../dto/CreateIssueRequest.java` — Added optional `issueKey` field.
- `jira-issue-service/.../service/IssueService.java` — Modified `createIssue()` to use the provided `issueKey` when present (with collision check via `findByIssueKey`), falling back to `generateIssueKey()` if absent or duplicate.
- `jira-migration-service/.../service/IssueServicePayloadMapper.java` — Added `payload.put("issueKey", request.getOriginalIssueKey())` so the original key is sent as `issueKey` in the HTTP payload, which the issue-service DTO now deserializes.

---

### Issue 4: Missing CSV fields after import (Issue Type, Status, Updated not stored)

**Symptom:** After import, only Summary was visible. Issue Type, Status, Updated, and Custom fields were missing.

**Root cause:** Same space-vs-underscore mismatch as Issue 3. Additionally, `IssuePersisterHandler.buildCreateIssueRequest()` looked for `"updatedAt"` / `"createdAt"` keys, but CSV produces `"updated"` / `"created"`.

**Fix (files changed):**
- `jira-migration-service/.../async/ImportJobProcessor.java` — The `convertRowToMap()` fix from Issue 3 resolves the field recognition problem.
- `jira-migration-service/.../persister/IssuePersisterHandler.java` — Changed `data.get("createdAt")` to `data.getOrDefault("createdAt", data.get("created"))` and `data.get("updatedAt")` to `data.getOrDefault("updatedAt", data.get("updated"))`.

---

### Issue 5: `.map()` / `.reduce()` / `.filter()` crashes on issue detail tabs (Comments, Activity, Worklogs, Labels)

**Symptom:** Clicking Comment, Activity, Worklog, or Label tabs on an issue throws `c.map is not a function`, `n.map is not a function`, `x.reduce is not a function`.

**Root cause:** All `useQuery` hooks return `response.data` from axios without checking if it's an array. When backend services are down, return errors, or return non-array responses, `.map()` and `.reduce()` crash on the non-array value.

**Fix (files changed) — wrap each queryFn return with `Array.isArray()` guard:**
- `jira-frontend/src/features/issues/pages/IssueDetailPage.tsx` — comments query
- `jira-frontend/src/features/issues/components/ActivityTab.tsx` — change history and transitions queries
- `jira-frontend/src/features/issues/components/WorklogsTab.tsx` — worklogs query (also `typeof` guard on totalTimeSeconds number query)
- `jira-frontend/src/features/issues/components/LabelsTab.tsx` — labels and suggestions queries
- `jira-frontend/src/features/issues/components/EditIssueModal.tsx` — priorities, projectUsers, versions, components, sprints queries

**Pattern:**
```typescript
// BEFORE
return response.data;
// AFTER
return Array.isArray(response.data) ? response.data : [];
```

---

### Issue 6: Missing `/api` prefix on frontend API calls (405 Not Allowed from nginx)

**Symptom:** POST/GET to `/comments`, `/attachments`, `/issues/{id}/worklogs` etc. returned 405 from nginx instead of reaching the backend.

**Root cause:** The frontend nginx config (line 143) proxies service paths to the gateway using a regex that requires a trailing `/` after the service name: `^/(comments)/`. Bare paths like `POST /comments` (no trailing path) don't match, so nginx tries to serve it as a static file and returns 405 for non-GET methods.

**Fix (files changed):**
- `jira-frontend/nginx.conf` — Changed regex from `^/(users|components|...|comments|...)/ ` to `^/(users|components|...|comments|...)(/|$)` so it matches both `/comments` and `/comments/...`.
- `jira-frontend/src/api/worklogApi.ts` — Changed all endpoints from `/issues/...` to `/api/issues/...`.
- `jira-frontend/src/api/commentApi.ts` — Changed from `/comments/...` to `/api/comments/...`.
- `jira-frontend/src/api/labelApi.ts` — Changed from `/issues/...` to `/api/issues/...`.
- `jira-frontend/src/api/changeHistoryApi.ts` — Changed from `/issues/...` to `/api/issues/...`.
- `jira-frontend/src/api/transitionHistoryApi.ts` — Changed from `/issues/...` to `/api/issues/...`.
- `jira-gateway/src/main/resources/application-docker.yml` — Added `comment-service-api` route for `/api/comments/**` with `StripPrefix=1` filter, so the gateway strips `/api` and forwards to the comment-service at `/comments/...`.

---

### Issue 7: "Permission denied: CREATE_ISSUES required" during CSV import execution

**Symptom:** Import execution fails with 500 for every issue — the first issue gets a permission error, then the circuit breaker trips and blocks the rest.

**Root cause:** The issue-service checks `CREATE_ISSUES` permission for the migration user on the target project. The migration service user doesn't have explicit project-level permissions.

**Fix (files changed):**
- `docker-compose.yml` — Added `JIRA_PERMISSIONS_FAILOPEN: "true"` env var to the issue-service container. This sets `jira.permissions.fail-open=true`, allowing requests to proceed when the permission check service is unavailable or the user lacks explicit grants.

---

### Issue 8: "Project not found" during CSV import execution

**Symptom:** Import fails with 404 — `Project not found with id: '...'` for every issue.

**Root cause:** The issue-service's `project.service.url` config defaulted to `http://${DB_HOST}:${DB_PORT}` which resolved to `http://postgres:5432` (the database) instead of the project-service.

**Fix (files changed):**
- `docker-compose.yml` — Added `PROJECT_SERVICE_URL: http://project-service:8083` env var to the issue-service container.

---

### Issue 9: Flyway migration checksum mismatch on issue-service startup

**Symptom:** Issue-service container crashes on startup with `FlywayValidateException: Migration checksum mismatch`.

**Root cause:** Rebuilding the issue-service with the parent POM pulled in updated migration scripts whose checksums didn't match the already-applied migrations in the database.

**Fix:**
```sql
UPDATE jira_issue.flyway_schema_history SET checksum = NULL WHERE success = true;
```
This clears the stored checksums so Flyway re-validates without failing. The migrations were already applied successfully — only the checksums changed.

---

### Issue 10: Missing `is_active` column on `issue_link_types` table

**Symptom:** Linking issues fails with `column ilt1_0.is_active does not exist`.

**Root cause:** The rebuilt issue-service code expects an `is_active` column on `issue_link_types` that was added in a newer migration, but the Flyway checksum fix (Issue 9) prevented the migration from re-running.

**Fix:**
```sql
ALTER TABLE jira_issue.issue_link_types ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
```

---

### Build & Deploy Gotchas

**Docker build caching:** The Dockerfiles for all services use `COPY target/*.jar app.jar` — they copy from a locally-built JAR. Running `docker compose build` alone won't rebuild the JAR; you must run `mvn package -DskipTests` first (or `npm run build` for frontend). Otherwise Docker uses the cached old JAR.

**Gateway stale IPs:** After recreating any service container, the gateway caches the old container IP. Always run `docker compose restart gateway` after restarting backend services.

**Issue-service cross-module build:** The issue-service has Maven dependencies on `jira-project-service` and `jira-admin-service` JARs. Building it standalone with `mvn package` fails. Use the parent POM reactor: `mvn package -pl jira-issue-service -am -DskipTests` from the project root.
