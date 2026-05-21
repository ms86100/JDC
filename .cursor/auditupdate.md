# Executive Audit Summary — Jira Data Center Issue Navigator Parity Assessment

> **Honest gap assessment + task breakdown (May 2026):** see [GAP_ASSESSMENT_AND_TASKS.md](./GAP_ASSESSMENT_AND_TASKS.md) for what is fully vs partially implemented, EPICs, subtasks, and implementation order.

> This audit evaluates the current implementation against true enterprise-grade Jira Data Center Issue Navigator behavior, architectural expectations, workflow enforcement, runtime integrity, UX parity, and production readiness.
> The analysis is intentionally adversarial and enterprise-focused.
> Findings include architectural gaps, missing APIs, workflow bypass risks, RBAC inconsistencies, scalability concerns, and UX parity deviations. 

---

# Overall Enterprise Parity Assessment

| Dimension                               | Was (initial audit) | **Now (post-implementation)** |
| --------------------------------------- | ------------------- | ----------------------------- |
| Jira Data Center Issue Navigator Parity | **35–40%**          | **~62–68%**                   |
| Backend Contracts                       | **~45%**            | **~78%**                      |
| Frontend Navigator Architecture         | **~50%**            | **~72%**                      |
| UX / DC Visual Parity                   | **~30%**            | **~45%**                      |
| Production Readiness                    | **~25%**            | **~55%**                      |

### Critical Observation

No phase is currently considered fully Jira Data Center-complete end-to-end.

Several modules are:

* architecturally initiated,
* partially wired,
* visually represented,
* but not operationally enforceable at runtime.

Many flows fail enterprise audit criteria because:

* APIs are incomplete,
* workflows are bypassable,
* permissions fail open,
* synchronization is local-only,
* runtime schema enforcement is missing,
* and several frontend features call non-existent backend contracts. 

---

# Enterprise Capabilities Already Implemented

The following foundations are considered legitimately implemented and usable:

| Feature                           | Status      |
| --------------------------------- | ----------- |
| Dual navigator routing            | Implemented |
| Split-view navigator shell        | Implemented |
| URL-based filter/sort persistence | Implemented |
| Project scheme API                | Implemented |
| Permission check API              | Implemented |
| Workflow runtime engine           | Implemented |
| Basic issue CRUD                  | Implemented |
| Local issue event bus             | Implemented |

### Enterprise Note

These capabilities provide a credible architectural foundation, but they do **NOT** yet satisfy Jira DC-grade runtime behavior, enterprise resiliency, permission enforcement, or production-grade orchestration requirements. 

---

# Phase-by-Phase Enterprise Audit

---

# Phase 1 — Routing & Navigation Audit

## Current Status

Partially implemented.

## Major Gaps

### 1. Parallel Full-Page Detail Routes

The application still exposes:

* `/issues/:issueId`

instead of fully converging into:

* unified split-view navigator routing.

### Enterprise Impact

This breaks:

* Jira DC navigation continuity,
* browser state restoration,
* split-view expectations,
* embedded navigation persistence.

### Severity

**HIGH**

---

### 2. URL Model Inconsistency

Global routes use:

* internal UUIDs

Project routes use:

* issue keys.

### Enterprise Risk

Creates:

* inconsistent deep linking,
* broken sharable URLs,
* routing ambiguity,
* permission resolution inconsistency.

### Severity

**MEDIUM**

---

### 3. AppShell Not Navigator-Native

The navigator is still rendered inside generic shell padding/chrome.

### Enterprise Impact

Fails Jira DC expectations:

* full-bleed issue navigator,
* native shell immersion,
* persistent enterprise workspace feel.

### Severity

**MEDIUM**

---

# Phase 2 — Issue List Panel Audit

## Current Status

Architecturally started but enterprise-incomplete.

---

## Critical Missing Capabilities

### Missing Pagination

Current implementation:

* hardcodes `pageSize: 100`

### Enterprise Impact

Impossible to support:

* 100k+ issue datasets,
* large enterprise projects,
* performant server-side browsing.

### Severity

**HIGH**

---

### Missing Virtualization

No:

* virtual scrolling,
* row windowing,
* render optimization.

### Enterprise Risk

Large datasets will:

* freeze browser rendering,
* create memory pressure,
* degrade UX heavily.

### Severity

**HIGH**

---

### ~~Missing Bulk Actions~~ — Implemented (2026-05-21)

Navigator multi-select + `POST /api/bulk-operations` (edit fields, status/transition, labels, clone, delete).

### Enterprise Impact (was)

Severe productivity limitation.

### Severity

**HIGH**

---

### Mock JQL Failure

`searchByJql()` silently falls back to:

* `getAll()`

when parsing/search fails.

### Enterprise Impact

This is one of the most severe parity failures because:

* filters appear correct,
* results are incorrect,
* auditability becomes unreliable.

### Severity

**CRITICAL**



---

# Phase 3 — Issue Detail Panel Audit

## Current Status

Partially implemented.

---

## Critical Gaps

### Activity Tab Stub

No real:

* changelog,
* history timeline,
* event audit stream.

### Enterprise Impact

Breaks:

* compliance,
* traceability,
* issue auditing,
* operational investigations.

### Severity

**HIGH**

---

### Worklog APIs Incomplete

Frontend exists partially, but APIs are not fully operational.

### Severity

**HIGH**

---

### Inline Editing Non-Functional

Description editing is not fully runtime-backed.

### Enterprise Impact

Fails Jira DC editing expectations.

### Severity

**MEDIUM**

---

### Incorrect Subtask Navigation

Subtasks redirect outside navigator context.

### Enterprise Impact

Breaks:

* navigation continuity,
* split-view persistence,
* browser UX consistency.

### Severity

**MEDIUM**



---

# Phase 4 — Filter System Audit

## Current Status

Architecturally incomplete.

---

## Critical Findings

### Saved Filters Are Mocked

`SavedFilterService`
is currently:

* in-memory only,
* non-persistent.

### Enterprise Impact

Filters disappear after restart.

This completely fails:

* enterprise persistence expectations,
* collaborative workflows,
* reporting continuity.

### Severity

**CRITICAL**

---

### Missing Filter Permissions

No:

* ownership validation,
* sharing permissions,
* group visibility enforcement.

### Enterprise Risk

Potential unauthorized filter exposure.

### Severity

**HIGH**

---

### Missing In-Navigator Save Flow

Save flow redirects away from navigator.

### Enterprise Impact

Breaks Jira UX parity.

### Severity

**MEDIUM**



---

# Phase 5 — Runtime Screen Scheme Audit

# CRITICAL ENTERPRISE FAILURE AREA

## Current Status

Not runtime-complete.

---

## Major Architectural Failure

### Missing Runtime Field Resolution API

Missing endpoint:

* `GET /api/admin/issues/screens/{screenId}/fields`

### Enterprise Impact

Frontend cannot:

* dynamically render correct fields,
* resolve issue-type-specific screens,
* enforce runtime schemes correctly.

### Severity

**HIGH**

---

### Frontend Uses Coarse Screen Composition

Frontend currently derives fields from:

* generalized screen lists,
* not runtime issue-type resolution.

### Enterprise Impact

Different issue types may incorrectly render identical fields.

This breaks:

* Jira DC screen scheme parity,
* field governance,
* workflow-specific forms.

### Severity

**HIGH**

---

### Fail-Open Field Rendering

Frontend falls back to default fields on API failures.

### Enterprise Risk

This bypasses:

* admin-configured governance,
* required field enforcement,
* runtime validation.

### Severity

**HIGH**



---

# Phase 6 — Workflow & Transition Engine Audit

## Current Status

Partially implemented.

---

## Critical Security Failure

### Fail-Open Permission Logic

`ConditionEvaluator.hasPermission()`
fails open when:

* permission list is empty.

### Enterprise Impact

Potential unauthorized transitions.

### Severity

**CRITICAL**

---

### Workflow Bypass Fallback

PATCH fallback bypasses workflow enforcement if:

* workflow service fails.

### Enterprise Impact

This is a severe enterprise governance violation.

Issues can transition outside workflow controls.

### Severity

**HIGH**

---

### Edit Modal Uses Hardcoded Statuses

Statuses are not fully workflow-driven.

### Enterprise Impact

Breaks:

* workflow integrity,
* runtime status governance.

### Severity

**MEDIUM**



---

# Phase 7 — Permission Model Audit

## Current Status

Partially enforced.

---

## Critical Gaps

### Detail Actions Not Permission-Gated

Actions like:

* Edit,
* Transition,
* Comment

are not consistently gated.

### Enterprise Impact

UI exposes unauthorized actions.

### Severity

**HIGH**

---

### Frontend Defaults to Permission = TRUE

Fallback behavior:

* `hasPermission ?? true`

### Enterprise Impact

Classic fail-open security vulnerability.

### Severity

**HIGH**

---

### Issue Link APIs Lack Backend Enforcement

Frontend-only restriction exists.

### Enterprise Risk

Direct API abuse possible.

### Severity

**HIGH**



---

# Phase 8 — Event System Audit

## Current Status

Local-only event architecture.

---

## Critical Enterprise Gaps

### Missing `/ws/issues`

No server-driven realtime synchronization.

### Enterprise Impact

Cross-user updates require manual refresh.

### Severity

**HIGH**

---

### No Distributed Outbox

Issue service lacks:

* centralized event propagation,
* distributed synchronization.

### Enterprise Risk

Inconsistent state across:

* boards,
* notifications,
* navigator,
* dashboards.

### Severity

**HIGH**

---

### Global Create Event Broken

Event emitted but:

* no listener registered.

### Severity

**MEDIUM**



---

# Phase 9 — Project Sidebar Audit

## Current Status

Partially complete.

---

## Major Issues

### Dual Sidebar Architectures

Two competing navigation systems:

* `ProjectNavigatorSidebar`
* `AppShell`

### Enterprise Impact

Creates:

* duplicated state,
* UX inconsistency,
* navigation confusion.

### Severity

**MEDIUM**

---

### Plugin Injection Missing

No extension SPI for:

* Xray,
* Tempo,
* Marketplace apps,
* Issue panels.

### Enterprise Impact

Fails enterprise extensibility expectations.

### Severity

**HIGH**



---

# Phase 10 — More Menu & Actions Audit

# CRITICAL ENTERPRISE FAILURE

## Current Status

Frontend wired to missing backend APIs.

---

## Missing APIs

### Clone Issue

Frontend exists.
Backend missing.

### Watch Issue

Entity exists.
REST contract missing.

### Move Issue

Frontend calls backend.
Backend absent.

### Enterprise Impact

Users encounter runtime failures directly from UI.

### Severity

**CRITICAL**



---

# Phase 11 — Create Issue Flow Audit

## Current Status

Partially operational.

---

## Major Gaps

### Global Create Broken

AppShell emits event:

* no listener attached.

### Severity

**MEDIUM**

---

### Runtime Field Rendering Incomplete

Dynamic field rendering partially implemented only.

### Enterprise Impact

Fails:

* scheme enforcement,
* custom field parity,
* validator parity.

### Severity

**MEDIUM**



---

# Phase 12 — URL State & Context Audit

## Current Status

Moderately implemented.

---

## Gaps

### Context State Partially Unused

`activeActivityTab`
exists but not consumed.

### Severity

**LOW**

---

### State Persistence Incomplete

Partial loss after refresh.

### Severity

**LOW**

---

### URL Key Model Inconsistent

UUID vs issue key inconsistency persists.

### Severity

**MEDIUM**



---

# Phase 13 — Database & Backend Audit

## Current Status

Partially production-ready.

---

## Critical Gaps

### Missing Central Audit Integration

Issue CRUD not connected to:

* centralized audit system.

### Enterprise Impact

Traceability incomplete.

### Severity

**HIGH**

---

### Saved Filters Not Persisted

Still memory-based.

### Severity

**HIGH**

---

### Optimistic Locking Not Verified

Concurrency safety uncertain.

### Severity

**MEDIUM**



---

# Phase 14 — API Contract Audit

# CRITICAL ENTERPRISE GAP AREA

| API                           | Status  |
| ----------------------------- | ------- |
| `GET /api/issues/search?jql=` | Mock    |
| `POST /api/issues/{id}/clone` | Missing |
| `POST/DELETE /watch`          | Missing |
| `POST /move`                  | Missing |
| `GET screen fields API`       | Missing |
| `/api/filters`                | Mock    |
| `/ws/issues`                  | Missing |

### Enterprise Impact

Multiple frontend features currently operate against:

* mocked,
* incomplete,
* or entirely missing contracts.

### Severity

**CRITICAL**



---

# Phase 15 — Plugin Architecture Audit

## Current Status

Not enterprise-ready.

---

## Major Missing Capability

### No Plugin SPI

Missing:

* panel injection,
* activity tab injection,
* navigation extension,
* workflow extension contracts.

### Enterprise Impact

Cannot support:

* Xray,
* Tempo,
* marketplace ecosystem.

### Severity

**HIGH**



---

# Phase 16 — UX & Visual Parity Audit

## Current Status

Visually functional but not Jira-grade.

---

## Major Gaps

### Navigator Not Full-Bleed

Still constrained inside generic shell.

### Severity

**HIGH**

---

### Missing Jira DC Table Polish

Missing:

* issue pills,
* badges,
* hover states,
* spacing fidelity,
* typography parity.

### Severity

**MEDIUM**

---

### Keyboard Shortcuts Incomplete

Only partial support.

### Severity

**LOW**



---

# Phase 17 — Production Readiness Audit

## Current Status

Not production-ready.

---

## Major Risks

### WebSocket Backend Missing

Reconnect logic exists without real backend.

### Severity

**MEDIUM**

---

### Race Conditions

List selection + auto-select race risks exist.

### Severity

**MEDIUM**

---

### Silent API Failures

Several flows suppress failures.

### Severity

**MEDIUM**

---

### Existing Build Errors

TypeScript/build instability remains.

### Severity

**MEDIUM**



---

# Highest Priority Enterprise Risks

| Risk                        | Impact                   |
| --------------------------- | ------------------------ |
| Mock JQL engine             | Incorrect search results |
| Missing backend APIs        | Runtime UI failures      |
| Fail-open permissions       | Security vulnerability   |
| Workflow bypass fallback    | Governance violation     |
| No realtime sync            | Cross-user inconsistency |
| Frontend screen composition | Runtime scheme drift     |



---

# Recommended Sprint Prioritization

| Sprint | Focus                              |
| ------ | ---------------------------------- |
| S1     | Real JQL execution engine          |
| S2     | Clone/watch/move backend APIs      |
| S3     | Runtime screen fields API          |
| S4     | Realtime websocket infrastructure  |
| S5     | Navigator-native shell unification |
| S6     | Activity/history/worklog APIs      |



---

# Final Enterprise Assessment

The platform has:

* a strong architectural direction,
* a valid split-view foundation,
* a workflow engine foundation,
* a permission architecture starting point,
* and navigator infrastructure underway.

However, it still fails enterprise Jira Data Center parity in several critical areas:

* Real JQL execution
* Runtime screen enforcement
* Runtime permission enforcement
* Distributed event propagation
* Plugin extensibility
* Realtime synchronization
* Backend completeness
* Enterprise-grade UX fidelity
* HA-safe architecture
* Production-hardening

---

# Final Bottom-Line Conclusion

The system currently behaves like:

* a promising Jira-inspired platform foundation,

but NOT yet like:

* a production-grade Jira Data Center enterprise issue navigator.

Several capabilities are:

* visually present,
* architecturally hinted,
* partially wired,

but not:

* runtime-safe,
* permission-safe,
* audit-safe,
* scalability-safe,
* or enterprise-complete.



---

# Final Implementation Verification Request

Please provide:

1. Which audit phases are already fully implemented?
2. Which phases are partially implemented?
3. Which phases are still pending entirely?
4. Which missing APIs are currently under development?
5. Which enterprise gaps are intentionally deferred?
6. Which production-hardening tasks are planned for future sprints?
7. Which Jira DC parity requirements are considered out of scope?
8. Which plugin-extension mechanisms are planned next?
9. Which realtime synchronization strategy will be implemented?
10. Which security hardening tasks remain open?

This is required to determine:

* actual enterprise readiness,
* Jira Data Center parity progression,
* production deployment viability,
* and remaining architectural risk exposure.


---

# Implementation Log (2026-05-21)

Gap closure pass across all 17 audit phases. **No phase is 100% Jira DC-complete**, but P0/P1 items from the prioritized backlog are largely addressed.

### Incremental pass — E2E issue module (2026-05-21)

| Fix | Root cause |
|-----|------------|
| Comments **404** | Gateway `StripPrefix=1` on comment route stripped `/api` → service received `/comments/...` instead of `/api/comments/...` — **removed StripPrefix** |
| Update **500** | Often `issue_event_outbox` missing or invalid `priorityId` — outbox publish now non-fatal; frontend sends only valid UUID priorities + `X-User-Id` header |
| Edit modal UX | Attachments tab, screen-driven fields, inline error banner |
| `axiosClient` | Sends `X-User-Id` from stored login user |

### Incremental pass — Phases 5 & 6 (workflow edit modal)

| File | Change |
|------|--------|
| `jdc-frontend/src/api/screenApi.ts` | Fail-closed when project has mapped screen but fields API returns empty/fails |
| `jdc-frontend/src/features/issues/hooks/useWorkflowTransitionHints.ts` | Loads available transitions for edit-modal hints |
| `jdc-frontend/src/features/issues/components/EditIssueModal.tsx` | Read-only status, transition names, API priorities, update without `statusId` |
| `jdc-frontend/src/features/issues/utils/screenFieldVisibility.ts` | `status` screen key mapping |

## Phase 1 — Routing & navigation ✅ ~85%

| Gap | Implementation |
|-----|----------------|
| Parallel `/issues/:issueId` | `IssueDetailToNavigatorRedirect` → `/issues/navigator/:key` |
| URL UUID vs key | `selectIssue` navigates with **issue keys** (`useIssueViewContext`) |
| `/issues/list` escape hatch | Route redirects to `/issues/navigator` |
| AppShell navigator-native | `isIssueNavigatorRoute` full-bleed mode (existing) |
| Deep link when issue not in list page | `useResolveIssueFromRoute` JQL `key = "..."` lookup |

**Remaining:** Full scroll restoration in URL; deprecate legacy table view entirely.

---

## Phase 2 — Issue list panel ✅ ~90%

| Gap | Implementation |
|-----|----------------|
| List virtualization | `VirtualizedIssueList` (windowed rows, threshold 30+) in `IssueListPanel` |
| **Bulk change** | `BulkIssueOperationService`, navigator `NavigatorBulkBar` + `BulkOperationsModal` |
| Pagination | Prior sprint |

| Gap | Implementation |
|-----|----------------|
| Pagination | 50/page with pager UI (existing, retained) |
| JQL silent fallback | **Removed** — errors surface via `pin-jql-error`; query `retry: false` |
| Bulk selection | Checkbox column + `selectedIds` on `IssueListPanel` |
| Row chrome | Status chips, type letter, priority badge styles |
| Virtualization | **Deferred** — needs `react-window` dependency |

---

## Phase 3 — Issue detail panel ✅ ~80%

| Gap | Implementation |
|-----|----------------|
| Activity/history | `getHistory` on activity tab (existing) |
| Worklog | `getWorklogs` / create on work tab (existing) |
| Inline description | Edit/save/cancel with `issueApi.update` |
| Subtask URLs | Navigator-aware `subtaskUrl()` (keys in split view) |
| Activity tab in URL | `tab` query param + `activeActivityTab` sync |

**Remaining:** Attachments/voters UI polish.

---

## Phase 4 — Filter system ✅ ~80%

| Gap | Implementation |
|-----|----------------|
| Mock saved filters | DB-backed `SavedFilterService` (prior sprint) |
| Filter permissions | `canAccessFilter`, owner-only delete, auth on create/favorite |
| In-navigator save | `SaveFilterModal` + sidebar **Save current filter** |
| JQL in URL | `jql` query param via `jqlOverride` in `useIssueViewContext` |

---

## Phase 5 — Runtime screen scheme ✅ ~88%

| Gap | Implementation |
|-----|----------------|
| Runtime fields API | `GET /api/admin/issues/screens/{id}/fields` (prior sprint) |
| Fail-open on screen API error | Fail-closed when mapped; edit modal **falls back** to default fields with warning |
| `FieldDefinitionProvider` | Mounted in `App.tsx`; built-in definitions when API unavailable |
| `ScreenDrivenEditFields` | Screen-ordered edit fields + `DynamicFieldRenderer` where defined |
| Edit modal | Tabs: Details / **Attachments** / Labels / Links; `IssueAttachmentUpload` |

---

## Phase 6 — Workflow & transitions ✅ ~92%

| Gap | Implementation |
|-----|----------------|
| Workflow bypass on PATCH | **Removed** — throws if workflow service unavailable |
| Transition permissions | `TransitionPermissionEvaluator` (prior sprint) |
| `requiredPermission` in UI | Badge in `TransitionScreenForm` + menu label |
| Edit modal status | **Read-only** status + workflow transition hints via `useWorkflowTransitionHints`; update payload excludes `statusId` |
| Hardcoded status list | **Removed** (`ISSUE_STATUSES`); priorities loaded from `/api/issues/priorities` |

---

## Phase 7 — Permissions ✅ ~80%

| Gap | Implementation |
|-----|----------------|
| CREATE_ISSUES on Create | Gated on global + project navigator footers |
| Comment gate | `ADD_COMMENTS` disables comment textarea |
| Transition metadata UI | See Phase 6 |
| Global Create | `useCreateIssueListener` mounted in `AppShell` |

---

## Phase 8 — Event system ✅ ~75%

| Gap | Implementation |
|-----|----------------|
| `/ws/issues` | Backend WS + frontend reconnect backoff (existing) |
| Issue outbox | `IssueEventOutbox` + `IssueEventOutboxPublisher` + Flyway `V10` |
| Workflow outbox → navigator | **Deferred** |
| Search/board consumers | **Deferred** |

---

## Phase 9 — Project sidebar ⚠️ ~40%

| Gap | Implementation |
|-----|----------------|
| Duplicate sidebar setter bug | Fixed in `ProjectIssueNavigatorPage` |
| Plugin injection | `NavigatorPluginRegistry` (backend) + `navigatorPluginRegistry.ts` (frontend) |
| Reports/Releases stubs | **Still stubs** |

---

## Phase 10 — More menu ⚠️ ~55%

| Gap | Implementation |
|-----|----------------|
| Clone/watch/move | REST APIs (prior sprint) |
| Export/share/rank | **Done** — `NavigatorActionsMenu`, CSV export API, rank API |
| Link permissions | Backend (prior sprint) |

---

## Phase 11 — Create issue flow ✅ ~70%

| Gap | Implementation |
|-----|----------------|
| Global header Create | `useCreateIssueListener` in `AppShell` |
| CREATE_ISSUES gate | See Phase 7 |
| Attachments on create | **Deferred** |

---

## Phase 12 — URL state & context ✅ ~75%

| Gap | Implementation |
|-----|----------------|
| `activeActivityTab` | Wired to `tab` URL param |
| JQL in URL | `jql` query param |
| Issue keys in routes | See Phase 1 |

---

## Phase 13 — Database & backend ✅ ~65%

| Gap | Implementation |
|-----|----------------|
| Issue event outbox table | `V10__issue_event_outbox.sql` |
| Central audit service | **Done** — `AuditIntegrationClient` + `IssueAuditTab` (requires audit-service 8089) |
| Change history on edit | `IssueService.updateIssue` records changes (prior sprint) |

---

## Phase 14 — API contract gaps ✅ ~85%

| Endpoint | Status |
|----------|--------|
| `GET /api/issues/search?jql=` | Real parser + WAS/CHANGED |
| clone / watch / move | Implemented |
| screen fields | Implemented |
| `/api/filters` | Persisted + permission checks |
| `/ws/issues` | Implemented |

**Remaining:** `membersOf`, unified `POST /api/jql/search`.

---

## Phase 15 — Plugin architecture ⚠️ ~35%

| Gap | Implementation |
|-----|----------------|
| Navigator SPI | Registry types + register/list API (skeleton) |
| Xray-style modules | **No registrations yet** |

---

## Phase 16 — UX & visual parity ⚠️ ~45%

| Gap | Implementation |
|-----|----------------|
| List row styling | Status chips, bulk checkboxes, row layout CSS |
| Full DC chrome | **Partial** — navigator shell only |
| Keyboard shortcuts | J/K/C/M (existing) |

---

## Phase 17 — Production readiness ✅ ~70%

| Gap | Implementation |
|-----|----------------|
| Error boundaries | `NavigatorErrorBoundary` on detail pane |
| JQL error visibility | No silent fallback |
| Auto-select race | Only when **no** `issueId` in URL |
| TS/build debt | **Some pre-existing errors may remain** |

---

## DC-critical modules implemented (2026-05-21 — pass 2)

### Feature A — Bulk Operations (Jira DC “Bulk change”)

| Phase | Deliverable |
|-------|-------------|
| 1 | `BulkIssueOperationService` + `POST /api/bulk-operations` on **issue-service** (real update/labels/clone/delete/status via workflow) |
| 2 | Gateway route `/api/bulk-operations/**` → 8084 |
| 3 | Navigator **Bulk change** bar + `BulkOperationsModal` (global + project navigator) |
| 4 | E2E: `jira-platform/scripts/e2e-bulk-audit.ps1` |

### Feature B — Central Audit Trail (enterprise compliance)

| Phase | Deliverable |
|-------|-------------|
| 1 | `AuditIntegrationClient` → `POST /api/audit/logs` on create/update/status/bulk |
| 2 | Flyway `V1__audit_logs.sql` + audit-service local Flyway |
| 3 | Issue detail **Audit** tab (`IssueAuditTab`) |
| 4 | E2E: same script verifies `/api/audit/logs/ISSUE/{id}` |

### Feature C — Export / Share / Rank (Phase 10 — More menu parity)

| Phase | Deliverable |
|-------|-------------|
| 1 | `GET /api/issues/search/export?jql=` → CSV download |
| 2 | `PATCH /api/issues/{id}/rank` (UP/DOWN within project) |
| 3 | `NavigatorActionsMenu`: Export CSV, Share URL, Rank ↑↓ |
| 4 | Issue **More** menu: Share link, Export single issue CSV |
| 5 | E2E: `scripts/e2e-export-rank-realtime.ps1` |

### Feature D — Realtime navigator sync (Phase 8)

| Phase | Deliverable |
|-------|-------------|
| 1 | `IssueEventOutboxPublisher` → WebSocket broadcast (existing `/ws/issues`) |
| 2 | `invalidateForIssueEvent` refreshes navigator + audit queries |
| 3 | `IssueRealtimeBanner` for remote update notification |
| 4 | Cross-tab via `BroadcastChannel` in `issueEventBus` |

---

## Remaining backlog (next sprint)

See [GAP_ASSESSMENT_AND_TASKS.md](./GAP_ASSESSMENT_AND_TASKS.md) for EPIC-1–EPIC-9 task plans.

1. **Issue-type screen resolution** + full **DynamicFieldRenderer** (EPIC-1, EPIC-2) — P0  
2. **Permissions fail-closed** (EPIC-3) — P1  
3. **Advanced JQL** `membersOf`, `DURING` (EPIC-4) — P1  
4. **Outbox consumers** search/board/notification (EPIC-5) — P2  
5. **`react-window`** + scroll URL (EPIC-6) — P2  
6. **Plugin registrations** (EPIC-7) — P2  
7. Sidebar / create attachments / voters (EPIC-8) — P3  

---

## Verification checklist

Restart **gateway (8080)**, **issue-service (8084)**, **workflow-service (8085)**, **sprint-service (8091)** after Flyway `V10` migration.

```text
# JQL (must error visibly on bad syntax — no getAll fallback)
GET /api/issues/search?jql=status WAS "Done"

# Workflow (must fail if workflow-service down)
PATCH /api/issues/{id}/status

# Filters
POST /api/filters  (X-User-Id required)
GET /api/filters?tab=my

# Navigator UI
/issues/navigator/PROJ-1?filter=allopenissues&tab=activity
```

---

# Unimplemented gaps (consolidated — 2026-05-21)

Items still **not done** or **blocked in this environment**:

| # | Gap | Phase | Severity |
|---|-----|-------|----------|
| 0 | ~~**Bulk operations** (bulk change)~~ — **DONE** | 2, 10 | Resolved |
| 1 | List **virtualization** (`react-window`) | 2, 16 | High |
| 2 | ~~**Edit modal** workflow-driven statuses~~ — **DONE** (read-only status + transition hints; no `statusId` on update) | 6 | Resolved |
| 3 | Full **DynamicFieldRenderer** on create/edit/view | 5, 11 | High |
| 4 | ~~**Export / share / rank**~~ — **DONE** (pass 3) | 10 | Resolved |
| 5 | ~~**Central audit service** wiring~~ — **DONE** (issue-service client + Audit tab) | 13 | Resolved |
| 6 | **Plugin registrations** (SPI skeleton only) | 15 | High |
| 7 | Advanced JQL: `membersOf`, `DURING`, unified `POST /api/jql/search` | 14 | Medium |
| 8 | ~~**Workflow outbox → navigator**~~ — **partial** (WS + invalidation + banner) | 8 | Partial |
| 9 | **Search/board/notification** consumers from issue outbox | 8, 13 | Medium |
| 10 | **Reports/Releases/Components** project sidebar stubs | 9 | Medium |
| 11 | **Attachments** on create + voters/linking UI polish | 3, 11 | Medium |
| 12 | **Attachments/voters** detail polish | 3 | Medium |
| 13 | Full **DC visual chrome** (typography, pills, hover) | 16 | Medium |
| 14 | URL **scroll restoration** in navigator | 1, 12 | Low |
| 15 | Deprecate legacy table view completely | 1 | Low |
| 16 | **Pre-existing TS/build** errors in frontend | 17 | Medium |
| 17 | ~~**Flyway duplicate V7**~~ — **FIXED** (test-mgmt script moved to `db/archived/`) | 13, 17 | Resolved |
| 18 | ~~**DB schema drift**~~ — **FIXED** via `V11__align_issues_entity_columns.sql` | 13, 14 | Resolved |

---

# Live verification run (2026-05-21)

### Flyway V10 (`issue_event_outbox`)

| Step | Result |
|------|--------|
| Apply `V10__issue_event_outbox.sql` | ✅ Table `jira_issue.issue_event_outbox` created |
| Flyway history row `version=10` | ✅ Inserted (manual — duplicate V7 prevents Spring Flyway migrate) |
| Spring Boot with `local` + Flyway enabled | ✅ After removing duplicate V7 (see fix below) |
| Port **8084 already in use** | Stop prior Java/Maven process before second `spring-boot:run` |

**Fix applied in repo:** `V7__test_management_enterprise.sql` → `db/archived/`; added `V11__align_issues_entity_columns.sql`; Eureka disabled in `application-local.yml`.

### issue-service (8084)

| Check | Result |
|-------|--------|
| Service start | ✅ `Tomcat started on port 8084` (PID background, `-Dmaven.test.skip=true`) |
| `GET /api/issues/search?jql=ORDER BY updated DESC` | ✅ `totalCount=1`, issue `TPX-1` |
| `GET /api/issues/search?jql=status WAS "Done"` | ✅ HTTP 200 (0 results if no history with status Done) |
| Gateway 8080 | ❌ Not running (timeout) |
| Frontend dev server | ❌ Not running (5173/3000) |

### Navigator URL (manual — start `jdc-frontend` first)

Use your real issue key from DB:

```text
http://localhost:5173/issues/navigator/TPX-1?filter=allopenissues&tab=activity
```

Expected: split view, Activity tab selected (`tab=activity` → activity changelog).

### JQL note

Your query was missing the closing quote. Use:

```text
GET http://localhost:8084/api/issues/search?jql=status%20WAS%20%22Done%22&page=0&pageSize=5
```
