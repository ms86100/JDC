# Issue CRUD E2E — Results and Fixes (2026-05-22)

## Root cause: create/update/delete failing

| Issue | Symptom | Fix |
|-------|---------|-----|
| **Issue key SQL** | `POST /api/issues` → 500, `relation "jira_issue" does not exist` | Native query used `FROM jira_issue` instead of `FROM jira_issue.issues`; `generateIssueKey` now uses JPQL `findMaxIssueNumberByProjectKey` |
| **Permission checks** | 403 when project-service unreachable | `IssueController` uses `ProjectPermissionClient` with `jira.permissions.fail-open: true` in `application-local.yml` |
| **Frontend field names** | Estimates, versions not persisted | `issuePayload.ts` maps `originalEstimateSeconds` → `originalEstimate`, `fixVersionIds` → `fixVersions`, strips empty UUIDs |
| **Edit modal** | Sent `priority: "Medium"` strings instead of UUIDs | `EditIssueModal` uses `priorityId`, `statusId`, `storyPoints`, `dueDate` from issue + priority API |
| **Labels on create** | Labels in request ignored | `IssueService.createIssue` persists labels via `LabelService` after save |
| **Components on create** | `componentIds` not saved | Set on `Issue` entity in create builder |
| **Labels in GET** | Not returned in `IssueResponse` | `mapToIssueResponse` loads from `LabelRepository` |

## Verified E2E (issue-service :8084)

```text
CREATE OK TPX-2
UPDATE OK E2E-UPDATED sp=8
DELETE OK
```

Run locally:

```powershell
# Issue service only (no gateway auth)
.\scripts\e2e-issue-crud.ps1 -Gateway http://localhost:8084 -SkipAuth -ProjectId <your-project-uuid>

# Via gateway (requires auth + project + issue services)
.\scripts\e2e-issue-crud.ps1 -Gateway http://localhost:8080
```

## Start issue-service (local)

```powershell
cd jira-issue-service
mvn spring-boot:run "-Dspring-boot.run.profiles=local" "-Dmaven.test.skip=true"
```

Or: `python launcher.py --only issue` (builds JAR if missing).

## Frontend

After pulling changes, create/edit issue from UI should send correct JSON. Hard refresh the app if the browser cached old `issueApi` bundles.
