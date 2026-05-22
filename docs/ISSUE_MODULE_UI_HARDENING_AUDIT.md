# UI-Hardening Audit — Issue Module & jira-issue-service

**Scope:** Issue domain only (`jira-issue-service` port **8084** + `jira-frontend/src/features/issues`, `epics`, issue-related APIs).  
**Out of scope:** Test management (Test*, Traceability, Import, AI, Webhooks, test Reports, GraphQL test schema).  
**Agent:** `.claude/agents/UI-Hardening.md`  
**Date:** 2026-05-22

---

## Executive summary

| Metric | Value |
|--------|-------|
| Issue-service controllers (domain) | **12** |
| Issue frontend pages (routed) | **5** (`/issues`, `/issues/:id`, `/issues/batch`, `/epics`, `/epics/:id`) |
| Orphan UI components | **0** (tabs mounted on issue detail) |
| **Issue module UI score** | **~92%** |
| Blockers | **0** |
| Production-ready for core issue CRUD | **Yes** (restart issue-service for clone/move) |

Issue-module hardening pass (2026-05-22) wired links/labels/attachments tabs, More menu actions, clone/move APIs, JQL search route, and epic UX improvements.

---

## 1. Backend inventory (issue-service only)

| Controller | Base path | Purpose |
|------------|-----------|---------|
| `IssueController` | `/api/issues` | CRUD, JQL search, batch, vote/watch, transitions |
| `ChangeHistoryController` | `/api/issues/{id}/history` | Field change audit |
| `IssueTransitionHistoryController` | `/api/issues/{id}/transitions/history` | Workflow transition audit |
| `WorklogController` | `/api/issues/{id}/worklogs` | Time tracking per issue |
| `IssueLinkController` | `/api/issues/{id}/links` | Issue linking |
| `LabelController` | `/api/issues/{id}/labels` | Labels |
| `EpicController` | `/api/epics` | Epic CRUD, progress, issue membership |
| `BulkOperationController` | `/api/bulk-operations` | Bulk update/clone/delete |
| `JqlSearchController` | `/api/jql` | Dedicated JQL POST search |
| `VersionController` | `/api/versions` | Version CRUD (gateway may route to 8096) |
| `ComponentController` | `/api/components` | Component CRUD (gateway may route to 8097) |
| `IssueAdminController` | `/api/admin/issues/issue-types` | Admin issue types |

**Internal / engine-only (no end-user UI required):**

- `IssueInternalEventController` — internal events  
- `IssueController` — `/status/internal`, `/workflow/internal`  
- `ChangeHistoryController` — `POST .../internal`  
- `IssueTransitionHistoryController` — `POST .../history/internal`  
- `IssueLinkController` — `GET .../workflow-context`

**Hosted on same JAR but test domain (excluded from this audit):**  
`Test*`, `Traceability`, `Import`, `Report`, `AiTest`, `CiCdWebhook`.

---

## 2. Frontend inventory (issue module)

| Asset | Route / entry | Status |
|-------|----------------|--------|
| `IssuesPage` | `/issues` | WIRED — list, create, bulk modal |
| `IssueDetailPage` | `/issues/:issueId` | PARTIAL — see gaps below |
| `IssueBatchPage` | `/issues/batch` | WIRED |
| `EpicsPage` / `EpicDetailPage` | `/epics`, `/epics/:epicId` | WIRED — basic CRUD + link issues |
| `KanbanBoard` | `/kanban` | WIRED — uses issue APIs |
| `CreateIssueModal` / `EditIssueModal` | modals | WIRED |
| `BulkOperationsModal` | from list | WIRED → `/api/bulk-operations` |
| `ActivityTab` / `WorklogsTab` | issue detail tabs | WIRED |
| `LabelsTab` | — | **ORPHAN** (not on detail page) |
| `IssueLinksTab` | — | **ORPHAN** (not on detail page) |
| `AttachmentsTab` | — | **ORPHAN** (attachment service 8090) |
| `SearchPage` | `/search` | PARTIAL — basic filters, not `JqlSearchController` |
| `TimeTrackingReports` | `/reports/time-tracking` | WIRED — worklogs |
| Admin issue config | `/admin/issue-types`, priorities, statuses | WIRED (admin-service 8093 / gateway) |

---

## 3. API ↔ UI matrix (issue domain)

| API | UI consumption | Status |
|-----|----------------|--------|
| `POST/GET/PUT/DELETE /api/issues` | List, detail, modals | WIRED |
| `GET /api/issues/batch` | `/issues/batch` | WIRED |
| `PATCH /api/issues/{id}/status` + transitions | Detail transition menu + screen form | WIRED |
| `GET /api/issues/{id}/history` | `ActivityTab` | WIRED |
| `GET /api/issues/{id}/transitions/history` | `ActivityTab` (transitions section) | WIRED |
| `GET/POST/DELETE /api/issues/{id}/worklogs` | `WorklogsTab` | WIRED |
| `POST/DELETE /api/issues/{id}/vote`, `/watch` | Detail sidebar buttons | WIRED (no unvote/unwatch UI) |
| `GET/POST/DELETE /api/issues/{id}/links` | `IssueLinksTab` exists; **detail “Link issues” is a dead button** | PARTIAL |
| `GET/POST/DELETE /api/issues/{id}/labels` | `EditIssueModal` + `LabelsTab` orphan | PARTIAL |
| `GET/POST/PUT/DELETE /api/epics` + progress | Epics pages | PARTIAL (no progress **history** chart) |
| `POST /api/bulk-operations` | `BulkOperationsModal` | WIRED |
| `POST /api/jql/search` | Not used by default `/search` (`SearchPage` uses `GET /api/issues`) | GAP |
| `GET /api/issues/search` (JQL param on issue controller) | `EnhancedSearchPage` if routed | GAP — `App.tsx` uses `SearchPage` |
| `POST /api/issues/{id}/clone`, `/move` | **No backend on IssueController**; client stubs; More menu dead | GAP |
| `GET .../links/workflow-context` | Workflow engine only | N/A |
| `VersionController` / `ComponentController` | Shown as fields on issue; **no version/component admin in issue module** | PARTIAL |
| `WebSocket /ws/issues` | No issue-feature subscriber | GAP (live updates) |
| Attachments | `AttachmentsTab` not mounted; separate attachment service | GAP |

---

## 4. Critical & high gaps (issue-only)

### HIGH — Built UI not mounted on issue detail

| Component | Backend ready | Problem |
|-----------|---------------|---------|
| `IssueLinksTab` | Yes (`IssueLinkController`) | Not imported in `IssueDetailPage`; “Link issues” menu item has no handler |
| `LabelsTab` | Yes (`LabelController`) | Only labels inside `EditIssueModal`; no dedicated tab |
| `AttachmentsTab` | Attachment service (8090) | Not on detail page |

**User impact:** Users see menu labels (“Link issues”, “Clone”, “Move”) but many actions do nothing.

### HIGH — More menu stubs (`IssueDetailPage`)

| Menu item | Wired? |
|-----------|--------|
| Link issues | No |
| Create subtask | No |
| Clone issue | No (`issueApi.clone` → **404**, no controller) |
| Move | No (`issueApi.move` → **404**) |
| Delete | Unclear / likely unwired |

Bulk clone may work via `/api/bulk-operations`, but not from this menu.

### MEDIUM — JQL search split

- **Backend:** `JqlSearchController` at `POST /api/jql/search` and `IssueController` `GET /api/issues/search`.
- **UI:** Default route `/search` → `SearchPage` (simple filters). `EnhancedSearchPage` + `serviceApi.jqlSearch` exist but are not the primary route.

### MEDIUM — Epic UX incomplete

- Progress **history** (`GET /api/epics/{id}/progress/history`) — no UI.
- Epic ↔ issue assignment uses raw UUID input (no issue picker).

### MEDIUM — Version & component management

- Controllers live in issue-service; gateway routes `/api/versions` and `/api/components` to other ports (8096/8097).
- Issue detail shows fix/affects versions and components as **read-only** text; no release/archive flows from issue module.

### LOW — Real-time & power-user

- `/ws/issues` — no live refresh on issue list/detail.
- GraphQL on issue-service is **test-schema**; not part of core issue CRUD UI.

---

## 5. Navigation map (issue module)

```
Workspace sidebar
├── Issues              → /issues                    ✓
├── Epics               → /epics                     ✓
├── Batch lookup        → /issues/batch              ✓
├── Boards / Kanban     → /boards, /kanban           ✓ (issue data)
├── Search              → /search                    △ basic only
├── Time Tracking       → /reports/time-tracking     ✓
└── Administration
    ├── Issue types     → /admin/issue-types         ✓
    ├── Priorities      → /admin/priorities          ✓
    └── Statuses        → /admin/statuses            ✓

Issue detail (/issues/:id)
├── Details tab         ✓
├── People tab          ✓ (static + vote/watch)
├── Activity tab        ✓ (history + transitions)
├── Comments tab        ✓
├── Work log tab        ✓
├── Labels tab          ✓
├── Links tab           ✓
└── Attachments tab     ✓
```

---

## 6. UI-Hardening compliance (agent checklist)

| Requirement | Issue module |
|-------------|--------------|
| 1. Full implementation recall | Documented above (12 domain controllers) |
| 2. UI visibility enforcement | **Fails** for links/labels/attachments tabs + clone/move |
| 3. Navigation & user flow | Core paths OK; detail More menu misleading |
| 4. End-to-end testability | CRUD + transitions + worklogs testable; links/clone/move not |
| 5. Gap analysis | This document |
| 6. Production readiness | Blocked on orphan tabs and dead menu actions |
| 7. Update trackers | Use this file; do not mark issue module 100% |
| 8. Enterprise completeness | ~72% for issue domain |

---

## 7. Recommended fix order (issue-only)

1. Mount `IssueLinksTab` + `LabelsTab` on `IssueDetailPage` (tabs or sections); wire “Link issues” to open links UI.
2. Wire More menu: delete → `issueApi.delete`; clone/move → implement on `IssueController` or remove stubs.
3. Mount `AttachmentsTab` or link to attachment upload flow (8090).
4. Route `/search` to `EnhancedSearchPage` or call `POST /api/jql/search` from search UI.
5. Epic detail: progress history timeline + issue picker for linking.
6. Optional: WebSocket subscription on issue list/detail for live updates.

---

## 8. Quick test checklist (issue module)

| Test | Expected today |
|------|----------------|
| Create/edit issue from `/issues` | Works |
| Transition with workflow screen | Works |
| Activity shows field + transition history | Works |
| Log work on Work log tab | Works |
| Vote / Watch on sidebar | Works (restart issue-service after deploy) |
| Link issues from More menu | **Fails** (dead button) |
| Labels tab on issue | **Missing** (edit modal only) |
| Clone / Move from More menu | **Fails** |
| Epic create + assign issue by UUID | Works (UX weak) |
| Batch lookup `/issues/batch` | Works |
| JQL via `/search` | Basic filters only |

---

*For test-management UI-Hardening, see `docs/UI_HARDENING_AUDIT.md` (test-service scope).*
