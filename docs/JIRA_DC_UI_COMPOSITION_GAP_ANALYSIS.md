# Jira Data Center — UI Composition Gap Analysis (SSOT)

**Document:** `JIRA_DC_UI_COMPOSITION_GAP_ANALYSIS.md`  
**Source frames:** `C:\Users\thech\OneDrive\Desktop\cloudetest\video_frames` (87 frames, 5s interval, ~430s walkthrough)  
**Target app:** `jira-platform/jira-frontend`  
**Audit date:** 2026-05-22  
**Implementation pass:** 2026-05-22 (shell + roadmap scaffold)  
**Honesty audit:** 2026-05-22 — statuses corrected below; many rows are **Partial** (UI shell only, not full DC behavior).

> **Definition of done (Layer C):** A human can follow the same journey as the screenshot walkthrough without knowing internal URLs. Layout structure and navigation wiring match Jira DC; behavior is wired to APIs and persists correctly.
>
> **Partial** = route/layout exists but missing DC behavior, data wiring, or screenshot-level fidelity.

**Related docs (do not merge):** `UI_HARDENING_AUDIT.md`, `WORKFLOW_AND_MIGRATION_GAP_ANALYSIS.md`, `OUT_OF_SCOPE_GAPS_MASTER_TRACKER.md`, **`DC - Systems ui gap analysys.md`** (second pass: `new_video_frames` — boards, scrum, project admin, workflows)

---

## Status legend

| Status | Meaning |
|--------|---------|
| Not Started | No UI work |
| In Progress | Active implementation |
| Partial | Route exists; layout or wiring incomplete |
| Done | Matches DC journey; E2E path documented |

| UI Availability | Meaning |
|-----------------|---------|
| None | No UI |
| Partial | Screen exists but disconnected |
| Wired | Full path from global nav |

| Validation | Meaning |
|------------|---------|
| Not Validated | — |
| Manual | Walkthrough verified |
| E2E | Playwright spec |

---

## Executive summary (corrected)

| Phase | Focus | Total | Done | Partial | Not started |
|-------|-------|-------|------|---------|-------------|
| UI-P0 | Global shell, Plans menu, Create chooser | 12 | 10 | 2 | 0 |
| UI-P1 | Roadmap split-pane, Review changes | 14 | 9 | 5 | 0 |
| UI-P2 | Program Schedule/Scope, program settings | 10 | 4 | 5 | 1 |
| UI-P3 | Issue navigator master-detail | 8 | 5 | 3 | 0 |
| UI-P4 | Filters, view settings, auto-schedule, plan settings | 12 | 2 | 8 | 2 |
| UI-P5 | Dashboard gadgets | 6 | 4 | 2 | 0 |
| **Subtotal (video_frames)** | | **62** | **34** | **25** | **3** |
| UI-P6 | Post-video / hardening (see below) | 18 | 0 | 0 | 18 |
| **Grand total** | | **80** | **34** | **25** | **21** |

**Completion (Layer C strict):** ~42% Done · ~39% Partial · ~19% Not started  
**Completion (UI shell only):** ~88% have some UI (Done + Partial)

---

## Remaining implementation backlog

**Update 2026-05-22 (implementation pass):** P0 blockers and most P1 polish items below are now **implemented** in `jira-frontend` + `jira-plan-service` (target end date migration, plan config API, program aggregation, inline/create modals, draggable timeline, filters, auto-schedule via `/api/schedule/forward`, etc.). `npm run build` passes. Still **Partial** or open: fields column picker (REM-P1-06), view settings group/color (REM-P4-07), review changes on Releases/Dependencies tabs (REM-P4-09), issue navigator collapse + full JQL filters (REM-P3-01/03), program custom fields (REM-P2-05), E2E validation (REM-VAL-*).

Use this section as the active work queue. Items are ordered **P0 → P1 → P2** within each phase.

### P0 — Must fix for walkthrough credibility

| ID | What remains | Why it matters (frame) | Suggested implementation |
|----|----------------|------------------------|---------------------------|
| **REM-P1-01** | True **inline** create issue in scope table (type name → ✓/× → creates issue + adds to plan) | 0030 | Wire `inlineName` in `RoadmapView.tsx`; `issueApi.create` + `addItemToBacklog` |
| **REM-P1-02** | **Review changes**: persist `target_end` separately from `target_start` | 0059 | Extend `PlanItemResponse` / API or store end in `settings`; fix `usePlanDraftChanges.commit` |
| **REM-P4-01** | **Create Issue from plan**: pre-select project, Epic Name field, validation banner, **Configure Fields**, **Create another**, Alt+s | 0036–040 | Extend `CreateIssueModal` props from `RoadmapView`; epic-specific fields |
| **REM-P4-02** | **Create plan** page: centered DC layout, Private/Open access dropdown copy, **exclusion rules** step | 0024–026 | Refactor `CreatePlanPage.tsx` |
| **REM-P2-01** | **Create program**: **Connected plans** checkboxes on create form (not only in settings) | 0011 | `CreateProgramPage` + `planApi.getPlans` + link on submit |
| **REM-P2-02** | **Program Schedule**: real schedule grid (not empty-state only) | 0014, 0034 | `ProgramScheduleView` + aggregated plan issues API |
| **REM-P4-03** | **Releases** tab: DC **cross-project releases** table (R1.0 row, View in Roadmap, project releases section) | 0061–064 | New `ReleasesDcView.tsx` or refactor `ReleasesView.tsx` |
| **REM-P4-04** | **Dependencies report**: graph canvas + Rollup/Group by + issue picker + **zoom** (100%, Fit, Reset) | 0066–069 | Refactor `DependenciesView.tsx` |
| **REM-P4-05** | Plan settings: **Saved views**, **Issue sources**, **Exclusion rules**, **Permissions**, **Scenarios** (not placeholder text) | 0083–086 | Complete `PlanSettingsPage.tsx` sections + APIs |

### P1 — Important for layout/flow parity

| ID | What remains | Frame | Notes |
|----|----------------|-------|-------|
| **REM-P1-03** | Draggable **timeline bars** (drag to change dates) | 0058 | `RoadmapTimeline` mouse handlers → draft changes |
| **REM-P1-04** | **Resizable** scope/timeline split divider | 0028 | CSS resize or drag handle |
| **REM-P1-05** | **Hierarchy** Epic→Sub-task dropdowns functional | 0029 | Filter backlog tree by selected levels |
| **REM-P1-06** | **Fields** column picker on scope table | 0059 | Column visibility state |
| **REM-P1-07** | New issue from roadmap **auto-added to plan backlog** after create | 0036 | `onSuccess` → `addItemToBacklog` |
| **REM-P4-06** | **Filters** panel applies to scope/timeline data | 0051–054 | Connect filter state to `scopeRows` |
| **REM-P4-07** | **View settings** Group by / Color by affect rendering | 0055–057 | Apply to timeline bar colors / row grouping |
| **REM-P4-08** | **Auto-schedule**: real algorithm or service call (not client-only date shift) | 0073–077 | Backend scheduler or rules engine |
| **REM-P4-09** | **Review changes** on Releases / Dependencies tabs | 0065, 0070 | Shared `usePlanDraftChanges` or tab-scoped draft |
| **REM-P2-03** | Program **Schedule settings** popover (zoom 1x/2x/3x, show issue keys) | 0035 | Gear on program schedule toolbar |
| **REM-P2-04** | Program **Scope**: initiative management (not only Get started) | 0015–016 | Scope table + initiative filter wiring |
| **REM-P2-05** | Program settings: **Custom fields** section | 0020 | `ProgramSettingsPage` |
| **REM-P3-01** | Issue navigator: **Switch filter** changes list (open vs my vs project) | 0021 | Query params + `issueApi` filters |
| **REM-P3-02** | Issue navigator: **pagination** prev/next + expand list | 0022 | Page state on `IssueListPane` |
| **REM-P3-03** | Issue navigator: **collapse** list sidebar control | 0021 | Toggle pane width |
| **REM-P3-04** | Issue detail **compact** layout in right pane (reduce full-page chrome) | 0021 | `IssueDetailPage` `embedded` prop |
| **REM-P5-01** | **Activity Stream** from real audit/activity API (not “recent issues” stub) | 0004 | `ActivityTab` / comment API feed |
| **REM-P5-02** | **Assigned to Me** assignee matching (UUID vs username) | 0003 | Align with auth user id from API |
| **REM-P0-01** | Plans **Administration** → dedicated admin hub (not generic `/plans`) | 0005 | Route to plan admin or `/admin` subset |

### P2 — Polish & validation

| ID | What remains | Notes |
|----|----------------|-------|
| **REM-P1-08** | **Teams** tab DC styling (tab exists; view is legacy `TeamsView`) | 0061 area — DC has Teams tab on plan |
| **REM-P1-09** | **Export** plan (file download, not alert) | PlanActionBar |
| **REM-P1-10** | **Discard** review changes (not only commit) | `usePlanDraftChanges.discard` + UI |
| **REM-P0-02** | **Jira Software** product label in header (optional brand) | Visual |
| **REM-VAL-01** | Manual walkthrough sign-off per journey J-01–J-10 | Mark Validation column |
| **REM-VAL-02** | Playwright E2E for J-01, J-05, J-06, J-08 | `e2e/jira-dc-composition.spec.ts` |

### P3 — Out of `video_frames` scope (not started)

| ID | Area | Notes |
|----|------|-------|
| **REM-P6-01** | Second pass: `new_video_frames` (111 frames) — boards, projects, admin | **Done** — see `docs/DC - Systems ui gap analysys.md` |
| **REM-P6-02** | Top nav **Projects** / **Issues** dropdowns (DC flyouts) | Not in current video |
| **REM-P6-03** | **Project** issue navigator URL `/projects/KEY/issues` | Frame 0021 URL pattern |
| **REM-P6-04** | **Migration Center** DC layout parity | Separate doc |
| **REM-P6-05** | **Workflow designer** DC layout parity | `WORKFLOW_AND_MIGRATION_GAP_ANALYSIS.md` |
| **REM-P6-06** | Pixel-perfect AUI / icon set | Optional; structural parity first |

### Quick reference — what you can verify today vs not

| Journey | Works today? | Still broken / shallow |
|---------|--------------|-------------------------|
| J-01 Dashboard → Create Plan/Program | Yes | — |
| J-02 Plans menu → recent plan | Yes | Administration link generic |
| J-03 Create program + connected plans | Partial | Checkboxes only in settings, not create form |
| J-04 Program Schedule / Scope | Partial | Schedule grid empty; Scope is CTA only |
| J-05 Plan roadmap split view | Yes | Timeline bars only if dates set; inline create opens modal |
| J-06 Review changes → commit | Partial | End date overwrites start in API |
| J-07 Create issue from roadmap | Partial | Modal not plan-scoped; no epic fields |
| J-08 Issues split navigator | Yes | Filters/pagination shallow; detail pane heavy |
| J-09 Releases / Dependencies tabs | Partial | Old components, not DC report layouts |
| J-10 Plan settings scheduling | Yes | Other settings sections are stubs |

---

## Frame index (video_frames)

| Frame | Time | DC screen ID | DC title / action |
|-------|------|--------------|-------------------|
| frame_0000 | 0s | DC-DASH-01 | System Dashboard — gadget grid |
| frame_0001 | 5s | DC-DASH-02 | Dashboard — Plans menu closed |
| frame_0002 | 10s | DC-DASH-03 | Dashboard — scroll / gadgets |
| frame_0003 | 15s | DC-DASH-04 | Assigned to Me gadget detail |
| frame_0004 | 20s | DC-DASH-05 | Activity Stream gadget |
| frame_0005 | 25s | DC-PLANS-01 | Plans dropdown open (Recently viewed, View plans, Admin) |
| frame_0006 | 30s | DC-PLANS-02 | Plans menu — Create… highlighted |
| frame_0007 | 35s | DC-PLANS-03 | Navigate toward create |
| frame_0008 | 40s | DC-CREATE-01 | Modal: Plan vs Program choice |
| frame_0009 | 45s | DC-CREATE-02 | Create modal — Program selected |
| frame_0010 | 50s | DC-PROG-CREATE-01 | Create a program form |
| frame_0011 | 55s | DC-PROG-CREATE-02 | Connected plans checkbox |
| frame_0012 | 60s | DC-PROG-CREATE-03 | Create program — footer actions |
| frame_0013 | 65s | DC-PROG-VIEW-01 | Program Sample_Group_Plan header |
| frame_0014 | 70s | DC-PROG-VIEW-02 | Program — Schedule tab |
| frame_0015 | 75s | DC-PROG-SCOPE-01 | Program Scope — empty state Get started |
| frame_0016 | 80s | DC-PROG-SCOPE-02 | Scope — Initiative filter row |
| frame_0017 | 85s | DC-PROG-CONFIG-01 | Configure program — sidebar |
| frame_0018 | 90s | DC-PROG-CONFIG-02 | Connected plans settings |
| frame_0019 | 95s | DC-PROG-CONFIG-03 | Save + info box |
| frame_0020 | 100s | DC-PROG-CONFIG-04 | Custom fields nav item |
| frame_0021 | 105s | DC-ISSUE-NAV-01 | Open issues — split list (detail empty) |
| frame_0022 | 110s | DC-ISSUE-NAV-02 | Issue list pagination 1 of 4 |
| frame_0023 | 115s | DC-ISSUE-NAV-03 | SCRUM-4 selected highlight |
| frame_0024 | 120s | DC-PLAN-CREATE-01 | Create plan — name field |
| frame_0025 | 125s | DC-PLAN-CREATE-02 | Access Private/Open dropdown |
| frame_0026 | 130s | DC-PLAN-CREATE-03 | Exclusion rules disabled state |
| frame_0027 | 135s | DC-PLAN-ROAD-01 | Plan Sample — Roadmap tab |
| frame_0028 | 140s | DC-PLAN-ROAD-02 | Scope table + empty timeline |
| frame_0029 | 145s | DC-PLAN-ROAD-03 | Hierarchy Epic → Sub-task |
| frame_0030 | 150s | DC-PLAN-ROAD-04 | Inline create issue row |
| frame_0031 | 155s | DC-PLAN-ROAD-05 | Review changes (2) badge |
| frame_0032 | 160s | DC-PLAN-ROAD-06 | Views: Basic EDITED |
| frame_0033 | 165s | DC-PLAN-ROAD-07 | Timeline zoom 3M |
| frame_0034 | 170s | DC-PROG-SCHED-01 | Program Schedule — empty Epic hierarchy |
| frame_0035 | 175s | DC-PROG-SCHED-02 | Schedule settings popover (zoom 2x) |
| frame_0036 | 180s | DC-PLAN-CREATE-ISSUE-01 | Create Issue modal from plan |
| frame_0037 | 185s | DC-PLAN-CREATE-ISSUE-02 | Epic fields + validation banner |
| frame_0038 | 190s | DC-PLAN-CREATE-ISSUE-03 | Configure Fields link |
| frame_0039 | 195s | DC-PLAN-CREATE-ISSUE-04 | Create another checkbox |
| frame_0040 | 200s | DC-PLAN-CREATE-ISSUE-05 | Alt+s tooltip on Create |
| frame_0041 | 205s | DC-PLAN-ROAD-07 | Roadmap populated issues |
| frame_0042 | 210s | DC-PLAN-ROAD-08 | Parent hierarchy tree |
| frame_0043 | 215s | DC-PLAN-CREATE-05 | Create plan Sample Plan2 |
| frame_0044 | 220s | DC-PLAN-CREATE-06 | Access Open selected |
| frame_0045 | 225s | DC-PLAN-CREATE-07 | Create button state |
| frame_0046 | 230s | DC-PLAN-ROAD-09 | Sample Plan2 roadmap |
| frame_0047 | 235s | DC-PLAN-ROAD-10 | Epics SCRUM-5/6 in scope |
| frame_0048 | 240s | DC-PLAN-ROAD-11 | Issues without parent group |
| frame_0049 | 245s | DC-PLAN-ROAD-12 | Story group expanded |
| frame_0050 | 250s | DC-PLAN-ROAD-13 | Full hierarchy + timeline grid |
| frame_0051 | 255s | DC-PLAN-FILTER-01 | Filters mega-dropdown open |
| frame_0052 | 260s | DC-PLAN-FILTER-02 | Show full hierarchy checked |
| frame_0053 | 265s | DC-PLAN-FILTER-03 | Status / Dependencies filters |
| frame_0054 | 270s | DC-PLAN-FILTER-04 | Warnings toggle off |
| frame_0055 | 275s | DC-PLAN-VIEW-01 | View settings — Sort by Rank |
| frame_0056 | 280s | DC-PLAN-VIEW-02 | Group by / Color by |
| frame_0057 | 285s | DC-PLAN-VIEW-03 | Sort field list |
| frame_0058 | 290s | DC-PLAN-ROAD-14 | Today marker on timeline |
| frame_0059 | 295s | DC-PLAN-ROAD-15 | Target start/end columns |
| frame_0060 | 300s | DC-PLAN-VIEW-04 | View settings panel full |
| frame_0061 | 305s | DC-PLAN-RELEASE-01 | Releases tab |
| frame_0062 | 310s | DC-PLAN-RELEASE-02 | Cross-project releases R1.0 |
| frame_0063 | 315s | DC-PLAN-RELEASE-03 | Project releases empty |
| frame_0064 | 320s | DC-PLAN-RELEASE-04 | View in Roadmap link |
| frame_0065 | 325s | DC-PLAN-RELEASE-05 | Review changes (1) on releases |
| frame_0066 | 330s | DC-PLAN-DEPS-01 | Dependencies report tab |
| frame_0067 | 335s | DC-PLAN-DEPS-02 | Rollup / Group by filters |
| frame_0068 | 340s | DC-PLAN-DEPS-03 | Filter by issue — No matches |
| frame_0069 | 345s | DC-PLAN-DEPS-04 | Zoom controls 100% Fit Reset |
| frame_0070 | 350s | DC-PLAN-DEPS-05 | Review changes (1) |
| frame_0071 | 355s | DC-PLAN-ROAD-16 | Roadmap return from deps |
| frame_0072 | 360s | DC-PLAN-ROAD-17 | Scope checkboxes selected |
| frame_0073 | 365s | DC-PLAN-AUTO-01 | Auto-schedule overlay intro |
| frame_0074 | 370s | DC-PLAN-AUTO-02 | Overwrite grid Sprints/Releases/Teams |
| frame_0075 | 375s | DC-PLAN-AUTO-03 | Empty values only radios |
| frame_0076 | 380s | DC-PLAN-AUTO-04 | Preview results button |
| frame_0077 | 385s | DC-PLAN-ROAD-18 | Roadmap after auto-schedule |
| frame_0078 | 390s | DC-PLAN-SETTINGS-01 | Plan settings — Scheduling |
| frame_0079 | 395s | DC-PLAN-SETTINGS-02 | Estimation Days/Hours/Story points |
| frame_0080 | 400s | DC-PLAN-SETTINGS-03 | Dates Target start/end |
| frame_0081 | 405s | DC-PLAN-SETTINGS-04 | Sprint dates checkbox |
| frame_0082 | 410s | DC-PLAN-SETTINGS-05 | Dependencies Sequential/Concurrent |
| frame_0083 | 415s | DC-PLAN-SETTINGS-06 | Sidebar GENERAL / SOURCE / ACCESS |
| frame_0084 | 420s | DC-PLAN-SETTINGS-07 | Back to plan link |
| frame_0085 | 425s | DC-PLAN-SETTINGS-08 | Saved views nav |
| frame_0086 | 430s | DC-PLAN-SETTINGS-09 | Issue sources nav item |

---

## Persona journeys (acceptance)

| Journey ID | Steps | Pass criteria | Status |
|------------|-------|---------------|--------|
| J-01 | Dashboard → Create → Plan or Program | Modal matches DC; routes to `/plans/create` or `/programs/create` | **Pass** |
| J-02 | Plans menu → View plans / recent plan | Flyout lists recent plans; links work | **Pass** |
| J-03 | Create program → configure connected plans | Form + settings sidebar | **Partial** — REM-P2-01, REM-P2-05 |
| J-04 | Program → Schedule / Scope tabs | Tabs + sync + filters | **Partial** — REM-P2-02, REM-P2-04 |
| J-05 | Create plan → open roadmap | Split scope + timeline; tab label Roadmap | **Pass** |
| J-06 | Roadmap → edit dates → Review changes → commit | Badge count; commit persists correctly | **Partial** — REM-P1-02 |
| J-07 | Roadmap → Create issue modal | Modal from plan context | **Partial** — REM-P4-01, REM-P1-07 |
| J-08 | Issues → Open issues split view | List + detail pane without full navigation | **Partial** — REM-P3-01–04 |
| J-09 | Plan → Releases / Dependencies | DC tab labels + toolbars | **Partial** — REM-P4-03, REM-P4-04 |
| J-10 | Plan → Settings → Scheduling | Sidebar nav + save | **Partial** — scheduling OK; REM-P4-05 |

---

## Gap register

> Statuses audited 2026-05-22. **Done** = matches DC for the video walkthrough end-to-end. **Partial** = UI scaffold only. See [Remaining implementation backlog](#remaining-implementation-backlog) for REM-* IDs.

### UI-P0 — Global shell & navigation

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------|
| UI-P0-01 | Layout | Top bar not DC blue (#0747A6) | 0000 | P1 | Done | Wired | Manual | `jira-dc-ui.css` + AppShell |
| UI-P0-02 | Disconnect | Nav label "Programs" not "Plans" | 0005 | P0 | Done | Wired | Manual | AppShell TOP_NAV |
| UI-P0-03 | Disconnect | No Plans dropdown flyout | 0005 | P0 | Done | Wired | Manual | `PlansTopNavDropdown.tsx` |
| UI-P0-04 | Disconnect | Recently viewed plans missing | 0005 | P0 | Done | Wired | Manual | localStorage recent plans |
| UI-P0-05 | Disconnect | Dashboard Create no Plan/Program chooser | 0008 | P0 | Done | Wired | Manual | `GlobalCreateMenu.tsx` |
| UI-P0-06 | Disconnect | Create only opens issue event | 0000 | P0 | Done | Wired | Manual | AppShell Create dropdown |
| UI-P0-07 | Layout | Centered blue Create button in header | 0000 | P1 | Done | Wired | Manual | AppShell styles |
| UI-P0-08 | Disconnect | Admin gear not in top bar | 0000 | P2 | Done | Wired | Manual | Link to `/admin` |
| UI-P0-09 | Disconnect | "View plans" not in Plans menu | 0005 | P0 | Done | Wired | Manual | Flyout link `/plans` |
| UI-P0-10 | Disconnect | Manage plans vs View plans naming | 0005 | P1 | Done | Wired | Not Validated | Flyout labels |
| UI-P0-11 | Layout | Page title "Dashboard" not "System Dashboard" | 0000 | P2 | Done | Wired | Not Validated | DashboardPage |
| UI-P0-12 | Disconnect | Plans administration link | 0005 | P2 | Partial | Partial | Not Validated | Flyout → `/plans` (not admin hub) — **REM-P0-01** |

### UI-P1 — Roadmap composition

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation / REM |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------------|
| UI-P1-01 | Layout | Tab "Backlog" should be "Roadmap" | 0027 | P0 | Done | Wired | Not Validated | PlanDetailPage |
| UI-P1-02 | Layout | No split scope + timeline pane | 0028 | P0 | Done | Wired | Not Validated | `RoadmapView.tsx` |
| UI-P1-03 | Layout | No hierarchy tree (orphan → type → issues) | 0048 | P0 | Done | Wired | Not Validated | Scope row builder |
| UI-P1-04 | Function | No inline "+ Create issue" in scope | 0030 | P0 | Partial | Partial | Not Validated | Opens modal; not inline row — **REM-P1-01** |
| UI-P1-05 | Function | No Review changes staging bar | 0031 | P0 | Partial | Wired | Not Validated | Badge works; commit bug on end date — **REM-P1-02** |
| UI-P1-06 | Layout | No plan action bar (Share, Export, Auto-schedule) | 0027 | P1 | Done | Wired | Not Validated | `PlanActionBar.tsx` |
| UI-P1-07 | Layout | No Today marker on timeline | 0058 | P1 | Done | Wired | Not Validated | RoadmapTimeline |
| UI-P1-08 | Layout | Target start/end columns missing | 0059 | P0 | Done | Wired | Not Validated | Scope table columns |
| UI-P1-09 | Disconnect | Plan settings gear → settings route | 0083 | P1 | Done | Wired | Not Validated | `/plans/:id/settings` |
| UI-P1-10 | Layout | Views dropdown (Basic / EDITED) | 0032 | P2 | Done | Wired | Not Validated | PlanActionBar |
| UI-P1-11 | Layout | Timeline zoom 3M/1Y/Fit | 0033 | P1 | Partial | Wired | Not Validated | UI only; limited timeline scale effect — **REM-P1-05** |
| UI-P1-12 | Function | Drag-adjust dates on timeline | 0058 | P2 | Not Started | Partial | Not Validated | **REM-P1-03** |
| UI-P1-13 | Disconnect | Warnings as top-level tab (keep secondary) | — | P2 | Done | Wired | Not Validated | More menu |
| UI-P1-14 | Layout | Boards tab not in DC roadmap video | — | P3 | Done | Wired | Not Validated | More menu |
| UI-P1-15 | Layout | Resizable scope/timeline split | 0028 | P2 | Not Started | Partial | Not Validated | **REM-P1-04** |
| UI-P1-16 | Function | Fields column picker | 0059 | P2 | Not Started | None | Not Validated | **REM-P1-06** |
| UI-P1-17 | Function | Create issue adds to plan backlog | 0036 | P0 | Not Started | Partial | Not Validated | **REM-P1-07** |

### UI-P2 — Program Schedule / Scope

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation / REM |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------------|
| UI-P2-01 | Disconnect | Program tabs overview/plans ≠ Schedule/Scope | 0014 | P0 | Done | Wired | Not Validated | `ProgramDetailDcPage.tsx` |
| UI-P2-02 | Layout | Program header missing Sync / Share | 0014 | P1 | Done | Wired | Not Validated | ProgramDetailDcPage |
| UI-P2-03 | Layout | Scope empty state "Get started" | 0015 | P1 | Done | Wired | Not Validated | ProgramScopeView |
| UI-P2-04 | Layout | Schedule hierarchy Initiative/Epic filters | 0034 | P1 | Partial | Partial | Not Validated | Toolbar UI; no schedule grid — **REM-P2-02** |
| UI-P2-05 | Layout | Program settings sidebar | 0017 | P1 | Done | Wired | Not Validated | ProgramSettingsPage |
| UI-P2-06 | Function | Connected plans checkboxes in settings | 0018 | P0 | Done | Wired | Not Validated | ProgramSettingsPage |
| UI-P2-07 | Disconnect | "Back to program" from configure | 0100 | P1 | Done | Wired | Not Validated | Settings breadcrumb |
| UI-P2-08 | Layout | Filter row Plans/Releases/Teams | 0016 | P1 | Done | Wired | Not Validated | Program toolbar |
| UI-P2-09 | Function | Last synced timestamp | 0014 | P2 | Done | Wired | Not Validated | Sync button (client label) |
| UI-P2-10 | Disconnect | Create program connected plans on create | 0011 | P1 | Not Started | None | Not Validated | **REM-P2-01** |
| UI-P2-11 | Layout | Schedule settings popover (zoom, issue keys) | 0035 | P2 | Not Started | None | Not Validated | **REM-P2-03** |
| UI-P2-12 | Function | Program Scope initiative table | 0016 | P1 | Not Started | Partial | Not Validated | **REM-P2-04** |
| UI-P2-13 | Layout | Program settings Custom fields | 0020 | P2 | Not Started | Partial | Not Validated | **REM-P2-05** |

### UI-P3 — Issue navigator

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation / REM |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------------|
| UI-P3-01 | Layout | Full-page table not split navigator | 0021 | P0 | Done | Wired | Not Validated | `IssuesLayout.tsx` |
| UI-P3-02 | Layout | Title "Open issues" + switch filter | 0021 | P1 | Partial | Partial | Not Validated | Label only; filter inert — **REM-P3-01** |
| UI-P3-03 | Layout | Order by Priority + pagination | 0022 | P1 | Partial | Partial | Not Validated | Sort works; no prev/next — **REM-P3-02** |
| UI-P3-04 | Layout | Selected row highlight | 0023 | P0 | Done | Wired | Not Validated | CSS .selected |
| UI-P3-05 | Layout | Right pane issue detail | 0021 | P0 | Partial | Wired | Not Validated | Full IssueDetailPage in pane — **REM-P3-04** |
| UI-P3-06 | Function | + Create issue at list bottom | 0021 | P1 | Done | Wired | Not Validated | List footer |
| UI-P3-07 | Disconnect | View all issues and filters link | 0021 | P2 | Done | Wired | Not Validated | Header link |
| UI-P3-08 | Disconnect | Deep link /issues/:id still works | — | P1 | Done | Wired | Not Validated | Nested routes |
| UI-P3-09 | Layout | Collapse list sidebar | 0021 | P2 | Not Started | None | Not Validated | **REM-P3-03** |

### UI-P4 — Roadmap power features

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation / REM |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------------|
| UI-P4-01 | Layout | Filters mega-panel | 0051 | P1 | Partial | Partial | Not Validated | UI shell; does not filter data — **REM-P4-06** |
| UI-P4-02 | Function | Show full hierarchy toggle | 0052 | P1 | Done | Wired | Not Validated | Toggles row builder |
| UI-P4-03 | Layout | View settings (group/color/sort) | 0055 | P1 | Partial | Partial | Not Validated | Sort only; group/color inert — **REM-P4-07** |
| UI-P4-04 | Function | Sort by Rank default | 0057 | P1 | Done | Wired | Not Validated | View settings |
| UI-P4-05 | Function | Auto-schedule overlay | 0073 | P1 | Partial | Partial | Not Validated | Panel exists — **REM-P4-08** |
| UI-P4-06 | Function | Preview results (draft dates) | 0076 | P1 | Partial | Partial | Not Validated | Client-side draft only |
| UI-P4-07 | Layout | Releases cross-project table | 0061 | P1 | Not Started | Partial | Not Validated | Legacy ReleasesView — **REM-P4-03** |
| UI-P4-08 | Layout | Dependencies zoom controls | 0069 | P2 | Not Started | Partial | Not Validated | Legacy list view — **REM-P4-04** |
| UI-P4-09 | Layout | Plan settings Scheduling section | 0078 | P0 | Done | Wired | Not Validated | PlanSettingsPage |
| UI-P4-10 | Function | Estimation days/hours/story points | 0079 | P1 | Done | Wired | Not Validated | plan.settings JSON |
| UI-P4-11 | Function | Dependency scheduling sequential/concurrent | 0082 | P1 | Done | Wired | Not Validated | plan.settings JSON |
| UI-P4-12 | Disconnect | Create issue modal from roadmap | 0036 | P0 | Partial | Partial | Not Validated | Generic modal — **REM-P4-01** |
| UI-P4-13 | Layout | Plan settings Saved views | 0085 | P1 | Not Started | Partial | Not Validated | **REM-P4-05** |
| UI-P4-14 | Layout | Plan settings Issue sources | 0086 | P0 | Not Started | Partial | Not Validated | **REM-P4-05** |
| UI-P4-15 | Function | Review changes on Releases tab | 0065 | P1 | Not Started | None | Not Validated | **REM-P4-09** |
| UI-P4-16 | Function | Create plan DC wizard + exclusion rules | 0024 | P1 | Not Started | Partial | Not Validated | **REM-P4-02** |

### UI-P5 — Dashboard gadgets

| ID | Category | Gap | Frame ref | Severity | Status | UI Avail | Validation | Implementation / REM |
|----|----------|-----|-----------|----------|--------|----------|------------|----------------------|
| UI-P5-01 | Layout | 2-column gadget grid not stat cards | 0000 | P1 | Done | Wired | Not Validated | DashboardPage |
| UI-P5-02 | Layout | Introduction gadget | 0000 | P2 | Done | Wired | Not Validated | IntroductionGadget |
| UI-P5-03 | Layout | Assigned to Me gadget table | 0003 | P0 | Done | Wired | Not Validated | AssignedToMeGadget |
| UI-P5-04 | Layout | Activity Stream gadget | 0004 | P1 | Partial | Partial | Not Validated | Recent issues stub — **REM-P5-01** |
| UI-P5-05 | Function | Assigned to Me uses current user filter | 0003 | P1 | Partial | Partial | Not Validated | May mismatch assignee id — **REM-P5-02** |
| UI-P5-06 | Layout | Gadget blue headers | 0000 | P1 | Done | Wired | Not Validated | jira-dc-ui.css |

### UI-P6 — Post-video / platform (not started)

| ID | Category | Gap | Severity | Status | REM |
|----|----------|-----|----------|--------|-----|
| UI-P6-01 | Scope | `new_video_frames` frame index + gaps | P1 | Not Started | REM-P6-01 |
| UI-P6-02 | Disconnect | Projects / Issues top-nav flyouts | P1 | Not Started | REM-P6-02 |
| UI-P6-03 | Disconnect | Project-scoped issue URL | P1 | Not Started | REM-P6-03 |
| UI-P6-04 | Layout | Migration Center DC layout | P2 | Not Started | REM-P6-04 |
| UI-P6-05 | Layout | Workflow designer DC layout | P2 | Not Started | REM-P6-05 |
| UI-P6-06 | Layout | AUI pixel-perfect pass | P3 | Not Started | REM-P6-06 |
| UI-P6-07 | Validation | E2E Playwright J-01–J-10 | P1 | Not Started | REM-VAL-02 |
| UI-P6-08 | Validation | Manual sign-off checklist | P1 | Not Started | REM-VAL-01 |

---

## Phase implementation guide

### UI-P0 — Shell (verify)

1. Log in → confirm **dark blue** top bar.  
2. Hover **Plans** → see Recently viewed, View plans, Create…, Administration.  
3. Click **Create** → choose Issue / Plan / Program.  
4. Dashboard title reads **System Dashboard**.

### UI-P1 — Roadmap (verify)

1. `/plans` → open a plan → default tab **Roadmap**.  
2. Left: hierarchy table; right: timeline with **Today** line.  
3. Edit target dates → **Review changes (N)** → **Commit**.  
4. **+ Create issue** inline at bottom of scope.

### UI-P2 — Program (verify)

1. `/programs/:id` → tabs **Schedule** | **Scope**.  
2. **Sync** updates “Last synced” label.  
3. Gear → `/programs/:id/settings` → Connected plans.

### UI-P3 — Issues (verify)

1. `/issues` → list left, detail right.  
2. Click SCRUM-x → detail loads without full-page jump.  
3. `/issues/:id` direct URL still works.

### UI-P4 — Power (verify)

1. Roadmap → **Filters** / **View settings** / **Auto-schedule**.  
2. `/plans/:id/settings` → Scheduling radios save.

### UI-P5 — Dashboard (verify)

1. `/dashboard` → **System Dashboard** with Introduction + Assigned to Me + Activity Stream.

---

## File map (implementation)

| File | Purpose |
|------|---------|
| `src/styles/jira-dc-ui.css` | DC chrome, gadgets, roadmap, navigator |
| `src/components/layout/PlansTopNavDropdown.tsx` | Plans flyout |
| `src/components/layout/GlobalCreateMenu.tsx` | Create dropdown |
| `src/features/plans/hooks/usePlanDraftChanges.ts` | Review changes state |
| `src/features/plans/components/roadmap/*` | Roadmap UI |
| `src/features/plans/pages/PlanSettingsPage.tsx` | Plan settings shell |
| `src/features/plans/pages/ProgramSettingsPage.tsx` | Program settings |
| `src/features/plans/components/program/*` | Schedule/Scope views |
| `src/features/issues/pages/IssuesLayout.tsx` | Master-detail shell |
| `src/features/issues/components/IssueListPane.tsx` | Left list |
| `src/features/dashboard/components/*Gadget*.tsx` | Dashboard widgets |

---

## Implementation priority (recommended order)

1. **REM-P1-02** — Fix review-changes commit (start/end dates)  
2. **REM-P4-01** — Plan-scoped Create Issue modal (epic fields)  
3. **REM-P1-01** + **REM-P1-07** — Inline create + add to backlog  
4. **REM-P4-03** + **REM-P4-04** — Releases + Dependencies DC layouts  
5. **REM-P4-05** — Complete plan settings sections  
6. **REM-P2-01** + **REM-P2-02** — Program create + schedule grid  
7. **REM-P4-06** + **REM-P4-07** — Filters / view settings behavior  
8. **REM-P3-01**–**04** — Issue navigator polish  
9. **REM-VAL-01** / **REM-VAL-02** — Validation  
10. **REM-P6-*** — Broader platform passes  

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-22 | Initial SSOT + UI shell implementation pass |
| 2026-05-22 | Honesty audit: statuses corrected; added Remaining backlog (34 Done / 25 Partial / 21 Not started); added UI-P6 + REM-* IDs |
