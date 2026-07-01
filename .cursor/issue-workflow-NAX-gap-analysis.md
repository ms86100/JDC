# Issue Workflow NAX — Gap Analysis & Implementation Tracker

**Spec source:** [issueworkflow.md](./issueworkflow.md)  
**Primary UI:** `jdc-frontend`  
**Backend:** `jira-platform` (issue, workflow, project, admin, comment, gateway services)  
**Last updated:** 2026-05-21 (Wave 0–3 implemented)  

---

## Status legend

| Status | Meaning |
|--------|---------|
| **Done** | Implemented, tested, meets acceptance criteria |
| **WIP** | Actively being implemented in current sprint |
| **Partial** | Exists but incomplete, bypassed, or stubbed |
| **Not Implemented** | No working implementation |
| **Blocked** | Depends on another gap row |

**Rule:** Mark **Done** only when acceptance criteria pass. Then start the next row in the same wave (or next wave if the wave is complete).

---

## Summary dashboard

| Wave | Total gaps | Done | WIP | Partial | Not Implemented | Blocked |
|------|------------|------|-----|---------|-----------------|---------|
| Wave 0 — Stabilize runtime | 6 | 6 | 0 | 0 | 0 | 0 |
| Wave 1 — Runtime pipeline | 7 | 7 | 0 | 0 | 0 | 0 |
| Wave 2 — Schemes & UI linkage | 6 | 6 | 0 | 0 | 0 | 0 |
| Wave 3 — Enterprise | 6 | 6 | 0 | 0 | 0 | 0 |
| **Total** | **25** | **25** | **0** | **0** | **0** | **0** |

*Update this table when any row status changes.*

---

## Phase 0 — Foundation (spec principles)

| ID | Gap | Spec phase | Status | Key files / area | Acceptance criteria | Notes |
|----|-----|------------|--------|------------------|---------------------|-------|
| P0-01 | Issues do **not** store `workflow_id`; workflow resolved dynamically | Core principle | **Partial** | `WorkflowContextResolver.java`, `Issue` entity | Issue table has no `workflow_id`; resolver uses `project_id` + `issue_type_id` | Verify no code path writes `workflow_id` on issue |
| P0-02 | Single enterprise workflow execution engine (no fake/parallel runtimes) | Phase 2, 6, 14 | **Partial** | `WorkflowExecutionEngine.java`, `WorkflowController.java`, `WorkflowService.java` | All user transitions go through engine only | Legacy execute endpoint deprecated; `validateTransitionExecution` stubs remain |
| P0-03 | No hardcoded statuses/transitions on issue UI | Phase 5C, Final | **Partial** | `IssueDetailPage.tsx`, `IssueAvailableTransitionsService.java` | UI shows only API-returned transitions | Fallback off by default; enable with `jira.workflow.transition-fallback=true` for dev only |
| P0-04 | jdc-frontend is canonical issue UX (jira-frontend not DC-parity) | Phase 5 | **Partial** | `jdc-frontend/`, `jira-frontend/` | All new workflow UX in jdc only | jira-frontend still static edit/status |

---

## Wave 0 — Stabilize runtime (Sprint 1)

*Goal: Issue view, comments, transitions, and edit work without 500s or bypass paths.*

| ID | Gap | Status | Owner / sprint | Key files | Acceptance criteria | Depends on |
|----|-----|--------|----------------|---------|---------------------|------------|
| W0-01 | Comment service DB schema (`version`, `internal`) + Flyway | **Done** | S1 | `V2`, `V3` migrations, `application.yml` Flyway enabled | `GET /api/comments/issue/{id}` → 200 via gateway with JWT | — |
| W0-02 | Comment API resilient in FE (no console spam on 500) | **Done** | S1 | `jdc-frontend/src/api/commentApi.ts` | 404/500/503 → empty list; no throw loop | W0-01 |
| W0-03 | Deprecate legacy transition path; delegate to engine | **Done** | S1 | `WorkflowController.java` `@Deprecated`, `WorkflowService.executeTransition` → engine | User execute path uses `WorkflowExecutionEngine` only | — |
| W0-04 | Disable unsafe transition/status fallbacks (default off) | **Done** | S1 | `IssueService`, `IssueAvailableTransitionsService`, `WorkflowExecutionEngine`, `application-local.yml` | `transition-fallback: false` default; synthetic transitions disabled | W0-03 |
| W0-05 | Permission alignment: UI vs engine (`EDIT_ISSUES` / `RESOLVE_ISSUES` / per-transition) | **Done** | S1 | `IssueDetailPage.tsx`, `IssueController.java` PATCH status | Transitions visible with `canEdit \|\| canResolve`; API uses `EDIT_ISSUES` | — |
| W0-06 | Post-function comment URL → comment-service (not issue-service) | **Done** | S1 | `WorkflowIntegrationClient.java`, `jira.services.comment-url` | Transition comments POST to `:8086/api/comments` | W0-01 |

**Wave 0 exit gate:** Open issue → comments load → one transition with comment → edit issue → save. All 200 or 403; no repeated 500 on comments.

---

## Wave 1 — Complete runtime pipeline (Sprint 2–3)

*Goal: Issue behaves as workflow state machine instance (Phase 2 + 6).*

| ID | Gap | Spec phase | Status | Key files | Acceptance criteria | Depends on |
|----|-----|------------|--------|-------------|---------------------|------------|
| W1-01 | Full 17-step transition pipeline traced and gaps closed | Phase 2 | **Done** | `WorkflowExecutionEngine.java`, `docs/WORKFLOW_TRANSITION_PIPELINE.md` | 17-step checklist documented + structured engine methods | W0-03, W0-04 |
| W1-02 | `issue_transition_history` + `issue_status_history` on every transition | Phase 7 | **Done** | `V12` migration, `IssueTransitionHistoryService`, engine integration | `GET /api/issues/{id}/transitions/history` returns rows | W1-01 |
| W1-03 | Validator parity (P0): required field, comment, resolution, attachment, regex, parent status | Phase 6 | **Done** | `ValidatorExecutor.java` | P0 validators enforced; unsupported types skipped | W1-01 |
| W1-04 | Post-function parity (P0): assign, set resolution, add comment, update field, webhook, automation queue | Phase 6 | **Done** | `PostFunctionExecutor.java` | `TRIGGER_WEBHOOK`, `TRIGGER_AUTOMATION` post-functions | W0-06, W1-01 |
| W1-05 | Transition screen validation (`screenInput` vs transition screen fields) | Phase 5C, 6 | **Done** | `TransitionScreenService`, `TransitionScreenForm`, `InvalidTransitionException` | Field-level `validationErrors` in API + FE client check | W1-03 |
| W1-06 | Available transitions: engine-only (no global status catalog fallback) | Phase 5C, 8 | **Done** | `IssueAvailableTransitionsService.java`, `WorkflowExecutionEngine` | Empty/wrong workflow → empty list; fallback opt-in only | W0-04 |
| W1-07 | `updateIssue` cannot change status (status only via transition) | Phase 2, 5C | **Done** | `IssueService.updateIssue`, `UpdateIssueRequest` | PUT with `statusId` → 400 ValidationException | W1-01 |

**Wave 1 exit gate:** Execute transition with screen + validators; history and audit written; edit does not change status.

---

## Wave 2 — Project & UI linkage (Sprint 4)

*Goal: Schemes drive screens and workflows (Phase 3–5).*

| ID | Gap | Spec phase | Status | Key files | Acceptance criteria | Depends on |
|----|-----|------------|--------|-------------|---------------------|------------|
| W2-01 | Project scheme bundle API (workflow, permission, notification, screen, issue type) | Phase 3 | **Done** | `ProjectController`, `ProjectSchemesBundleResponse` | `GET /api/projects/{id}/schemes` returns all scheme IDs | — |
| W2-02 | Screen scheme: issue-type overrides for create/edit/view | Phase 3, 5C | **Done** | `V8` migration, `ScreenSchemeController`, `resolveScreens` | Different issue types resolve different CREATE/EDIT/VIEW screen IDs | W2-01 |
| W2-03 | Field configuration scheme (required/visible/hidden per field + issue type) | Phase 3 | **Done** | `FieldConfigurationService`, `IssueFieldConfigurationClient` | Server rejects create without required fields (summary, issuetype, etc.) | W2-02 |
| W2-04 | Workflow scheme UI: issue type → workflow matrix + project assignment | Phase 4, 5B | **Done** | `ProjectWorkflowSchemePanel.tsx`, `WorkflowManagementPage` | Matrix + assign scheme on project settings | W2-01 |
| W2-05 | Workflow designer: draft + publish workflow versions | Phase 4, 12 | **Done** | `WorkflowDesignerPage.tsx`, `workflow-schemes/drafts/.../publish` | Edit-as-draft + publish draft path; in-flight issues unchanged | W2-04 |
| W2-06 | Issue view fully dynamic: transitions, transition screen, view fields from scheme | Phase 5C | **Done** | `IssueDetailPage.tsx`, `useIssueScreenFields`, `useFieldConfiguration` | VIEW screen + workflow transitions only; create respects field config | W1-05, W1-06, W2-02 |

**Wave 2 exit gate:** New project configured with schemes; create/edit/view/transition all respect configuration.

---

## Wave 3 — Enterprise (Sprint 5+)

*Goal: Migration, bulk, events, edge cases (Phase 9–12).*

| ID | Gap | Spec phase | Status | Key files | Acceptance criteria | Depends on |
|----|-----|------------|--------|-------------|---------------------|------------|
| W3-01 | Migration: resolve workflow scheme + status mapping on issue import | Phase 9 | **Done** | `MigrationWorkflowStatusApplier`, `WorkflowRuntimeClient` | Post-import status via workflow transition or internal apply | W2-04, W1-02 |
| W3-02 | Bulk workflow transitions UI wired to `execute-bulk` | Phase 8 | **Done** | `BulkOperationsModal.tsx`, `workflowApi.executeBulkTransitions` | Navigator bulk uses workflow transitions (not raw status) | W1-01 |
| W3-03 | Event system: `ISSUE_TRANSITIONED`, `STATUS_CHANGED`, WS + cache invalidation | Phase 11 | **Done** | `IssueInternalEventController`, `WorkflowEventOutboxProcessor`, `issueEventBus` | WS + BroadcastChannel invalidate issue/transitions | W1-01 |
| W3-04 | Notifications on transition (notification scheme + post-function) | Phase 11 | **Done** | `ProjectNotificationSchemeClient`, outbox processor | Scheme recipients + assignee/reporter notified | W1-04 |
| W3-05 | Edge cases: optimistic lock on transition, idempotent execute, post-function failure policy | Phase 12 | **Done** | `TransitionIdempotencyService`, `expectedVersion`, 409 handler | Concurrent transition → 409; idempotent key cached | W1-01 |
| W3-06 | Retire or align jira-frontend issue workflow with jdc | Phase 5, 14 | **Done** | `jira-frontend/IssueDetailPage.tsx` redirect | Legacy issue detail redirects to jdc-frontend | W2-06 |

**Wave 3 exit gate:** Migration smoke test; bulk move; events and notifications documented and working.

---

## Phase-level checklist (from issueworkflow.md)

| Phase | Title | Overall status | Tracker rows |
|-------|--------|----------------|--------------|
| 1 | Core workflow architecture | **Partial** | P0-01, P0-02, W2-04, W2-05 |
| 2 | Issue runtime execution model | **Partial** | W1-01 … W1-07 |
| 3 | Project linkage | **Partial** | W2-01 … W2-03 |
| 4 | Workflow scheme engine | **Partial** | W2-04, W2-05 |
| 5 | UI/UX (designer, scheme, issue view) | **Partial** | W2-04 … W2-06, P0-03, P0-04 |
| 6 | Transition engine (conditions, validators, post-functions) | **Partial** | W1-03, W1-04, W1-05 |
| 7 | Database architecture | **Partial** | W1-02, W0-01 |
| 8 | Backend APIs + RBAC + audit | **Partial** | W0-05, W1-01, W1-06 |
| 9 | Migration linkage | **Partial** | W3-01 |
| 10 | Security & permissions | **Partial** | W0-05, W1-06 |
| 11 | Event & automation | **Partial** | W3-03, W3-04 |
| 12 | Enterprise edge cases | **Partial** | W3-05, W2-05 |
| 13 | Task breakdown | **Done** | This document |
| 14 | Enterprise audit | **WIP** | All rows above |

---

## Implementation order (close gaps in sequence)

```text
1. ~~W0-01 → … → W0-06~~ **[Wave 0 complete]**
2. ~~W1-01 → … → W1-07~~ **[Wave 1 complete]**
3. **W2-01** → W2-02 → W2-04 → W2-06 → W2-03 → W2-05   [Wave 2 — next]
3. W2-01 → W2-02 → W2-04 → W2-06 → W2-03 → W2-05   [Wave 2]
4. W3-01 → W3-02 → W3-03 → W3-04 → W3-05 → W3-06   [Wave 3]
```

Update row **Status** to **WIP** when starting work, **Done** when acceptance criteria pass, then proceed to the next ID in the list.

---

## Change log

| Date | ID | Old status | New status | Notes |
|------|-----|------------|------------|-------|
| 2026-05-21 | — | — | — | Initial NAX gap analysis created from issueworkflow.md audit |
| 2026-05-21 | W0-01 | Not Implemented | Done | Flyway enabled; V3 `internal` column |
| 2026-05-21 | W0-02 | Partial | Done | `commentApi` handles 500 |
| 2026-05-21 | W0-03 | Not Implemented | Done | Legacy route `@Deprecated`; delegates to engine |
| 2026-05-21 | W0-04 | Not Implemented | Done | `transition-fallback: false` in issue + workflow services |
| 2026-05-21 | W0-05 | Partial | Done | PATCH status `EDIT_ISSUES`; UI `canEdit \|\| canResolve` |
| 2026-05-21 | W0-06 | Not Implemented | Done | `comment-url` on `WorkflowIntegrationClient` |
| 2026-05-21 | W1-06 | Partial | Done | Issue-service catalog fallback removed when flag off |
| 2026-05-21 | W1-01 | Partial | Done | Pipeline doc + refactored engine |
| 2026-05-21 | W1-02 | Not Implemented | Done | V12 + issue transition/status history API |
| 2026-05-21 | W1-03 | Partial | Done | P0 validators + attachment count |
| 2026-05-21 | W1-04 | Partial | Done | Webhook + automation post-functions |
| 2026-05-21 | W1-05 | Partial | Done | Screen field validationErrors E2E |
| 2026-05-21 | W1-07 | Not Implemented | Done | statusId rejected on PUT issue |

---

## QA smoke script (run after each wave)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Login, open issue detail | 200 on issue, comments, transitions |
| 2 | Add comment | 201, comment visible |
| 3 | Open edit modal, change summary, save | 200, issue updated |
| 4 | Execute workflow transition (with screen if required) | 200, status changed, history entry |
| 5 | User without permission opens same issue | No transition button; edit blocked 403 |
| 6 | Admin: assign workflow scheme to project, new issue | Uses mapped workflow |

---

*This file is the single source of truth for gap closure. Do not mark **Done** without meeting acceptance criteria.*
