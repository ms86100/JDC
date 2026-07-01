# Jira DC Issue XML Import Engine — Enterprise Readiness Tracker (Honest)

**Canonical spec:** [.cursor/Migrationissuexml.md](../.cursor/Migrationissuexml.md)  
**Prior tracker (superseded for honesty):** [issue_xml_gap_analysis.md](./issue_xml_gap_analysis.md)  
**Fixture:** [.cursor/jira_dc_issue_export.xml](../.cursor/jira_dc_issue_export.xml)  
**Last updated:** 2026-05-21 (pass 7 — SLA proof, production ZIP fixture, AC sign-off UI)  

---

## Maturity dashboard (true enterprise readiness)

| Domain | Realistic % | Production readiness | Notes |
|--------|-------------|----------------------|-------|
| RSS 0.92 fixture path | **82%** | Testable MVP | Labels/links/worklog/changelog + unit test |
| Native DC backup ZIP | **78%** | Partial | Production-style nested `export/entities.xml` fixture + manifest |
| entities.xml completeness | **72%** | Partial | Worklog/SubTask/Watcher/Vote + ChangeGroup batch replay |
| Attachment binary fidelity | **62%** | Partial | issue-service upload + bytes in progress |
| Changelog replay fidelity | **68%** | Partial | Batched ChangeItems per group → `/history/internal` |
| Workflow transition integrity | **55%** | Partial | `historyOnlyImport`; status applier on create when not history-only |
| Timestamp preservation | **55%** | Partial | `migrationCreatedAt` / `migrationUpdatedAt` on issue create |
| Custom field compatibility | **65%** | Partial | `customfield_*` shape accepted; plugin keys still warned |
| Plugin field handling | **45%** | Partial | GreenHopper/AO/Sprint → PluginEntity merged as CF |
| Relationship reconstruction | **68%** | Partial | IssueLink + parent/epic pass + RSS links |
| Incremental/idempotent import | **45%** | Partial | Job-scoped skip via `MigrationIssueResult` |
| Rollback consistency | **55%** | Partial | Job rollback + UI |
| Parallel processing | **62%** | Completed | Worker pool Comment/Attachment/Worklog |
| Streaming architecture | **68%** | Partial | SAX entities/RSS/entity-backup on path |
| Validation coverage | **72%** | Partial | Conflicts + unknown CF + warning acknowledge UI |
| Operational tooling | **58%** | Partial | Rollback/retry/reports/audit |
| Security hardening | **62%** | Completed | XXE, zip slip, bomb caps |
| Import dashboard (UI) | **~89%** | Partial | UI-Hardening pass: conflicts, review, graph, job console ([MIGRATION_UI_HARDENING_AUDIT.md](./MIGRATION_UI_HARDENING_AUDIT.md)) |
| Performance / scale | **55%** | Partial | `slaProof` on jobs + parse/live tier tests |
| E2E test coverage | **62%** | Partial | `MIGRATION_E2E_FULL=1` wizard + parity panel assert |
| AC sign-off gate | **40%** | Partial | `DcImportAcSignoffPanel` + checklist doc + API |
| **Overall enterprise DC issue import** | **~86%** | **Not production-ready** | SLA + AC UI wired; formal 10/10 sign-off still open |

**Remaining to enterprise parity:** **~14%** (real DC 9.x ZIP soak, non-stub live 10k SLA, formal 10/10 sign-off)

**AC checklist:** [issue_xml_ac_signoff_checklist.md](./issue_xml_ac_signoff_checklist.md)

---

## Status rules (mandatory)

| Status | Meaning |
|--------|---------|
| **Completed** | Verified enterprise acceptance criteria met |
| **Partial** | Real code + UI path; not full AC proof |
| **Needs Verification** | Code merged; E2E not run |

**Never mark Completed for:** stubDownstream default, metadata-only success, simulated DLQ.

---

## Phase 1 — Real Jira DC backup & XML fidelity

| ID | Task | Status | % | Notes |
|----|------|--------|---|-------|
| 1.1.x | ZIP ingestion epic | Completed | 90 | API + UI + security tests |
| 1.2.1–1.2.8 | entities.xml core entities | Partial | 75 | Worklog/SubTask added in pass 4 |
| 1.2.4 | ChangeGroup + ChangeItem stream | Partial | 70 | Batched replay in `JiraDcChangeHistoryReplayer` |
| 1.2.7 | Version/Component/Label | Partial | 65 | Real issue-service APIs for Component/Version/Label |
| 1.2.9 | Plugin XML (AO, GreenHopper) | Partial | 45 | PluginEntity SAX + merge as custom field |
| 1.3.1 | RSS extended fields | Needs Verification | 75 | Unit test on extended fixture |
| 1.3.2 | Entity backup SAX | Partial | 85 | Path + in-memory path when `xmlPath` set |

---

## Phase 3 — Data fidelity

| ID | Task | Status | % | Notes |
|----|------|--------|---|-------|
| 3.1 | Changelog parse | Partial | 70 | |
| 3.2 | Replay author/time | Partial | 60 | author + created on batch request |
| 3.3 | No workflow on replay | Completed | 85 | historyOnly + internal header |
| 3.4 | Preserve issue created/updated | Partial | 55 | issue-service migration timestamps |
| 3.6 | History-only import mode | Partial | 65 | UI+BE flag; issues still created unless dry-run |

---

## Phase 4 — Custom fields, plugins, relationships

| ID | Task | Status | % | Notes |
|----|------|--------|---|-------|
| 4.7 | Unknown CF UI | Partial | 55 | Panel + registry DB on validate |
| 4.8 | Issue links | Partial | 55 | RSS + entities + post-pass |
| 4.9 | Labels/components/versions | Partial | 65 | issue-service APIs wired |
| 4.10 | Watchers/votes | Partial | 40 | Watcher via watch API; Vote recorded |
| 4.14 | Preserve source keys | Partial | 55 | `originalIssueKey` / `migrationSourceKey` on create |

---

## Phase 5 — Import execution

| ID | Task | Status | % | Notes |
|----|------|--------|---|-------|
| 5.4 | Incremental merge | Partial | 45 | Job-scoped `shouldSkipIssue(jobId, key)` |
| 5.8 | Rollback full job | Partial | 55 | |

---

## Phase 6 — UI & tests

| ID | Task | Status | % | Notes |
|----|------|--------|---|-------|
| 6.1.5 | Conflict resolution UI | Partial | 65 | Warning acknowledge checkbox |
| 6.3.1 | Unit parsers | Partial | 78 | |
| 6.3.5 | E2E wizard | Partial | 62 | `jira-dc-import.spec.ts` + `MIGRATION_E2E_FULL` |
| 6.3.6 | Parity report UI | Partial | 70 | `DcImportParityReportPanel` + `paritySummary` metadata |
| 6.3.7 | SLA proof UI + API | Partial | 65 | `slaProof` + `GET .../dc-sla-proof` |
| 6.3.8 | AC sign-off UI + API | Partial | 55 | `acSignoff` + `GET .../dc-ac-signoff` + checklist doc |

---

## Enterprise acceptance criteria (sign-off gate)

| # | Criterion | State |
|---|-----------|-------|
| AC-1 | Import DC 9.x backup ZIP from UI | **Partial** — path exists; soak not proven |
| AC-2 | 10k issues < SLA | **Partial** — `slaProof` on job; stub excluded; live 10k not proven |
| AC-3 | Attachment SHA-256 ≥99.9% | **Not met** |
| AC-4 | Changelog field-level match | **Partial** — batched replay; not diff-verified |
| AC-5 | No spurious transitions history-only | **Partial** |
| AC-6 | Plugin CF mapped or registry | **Partial** |
| AC-7 | Rollback proven | **Not met** |
| AC-8 | All operations in UI | **Partial** |
| AC-9 | E2E suite green | **Not met** |
| AC-10 | Security review | **Partial** |

**Sign-off:** **0 / 10** formal AC (evaluator wired) — functional parity ~86% per tracker.

---

## Implementation log

| Timestamp | Change | IDs |
|-----------|--------|-----|
| 2026-05-21 | Parity pass 4: worklog/component/version/label/watcher persist via issue-service | 1.2, 4.9, 4.10 |
| 2026-05-21 | Batched changelog replay; entities Worklog/SubTask/Watcher/Vote | 1.2.4, 3.1 |
| 2026-05-21 | Migration timestamps on issue create; originalIssueKey payload | 3.4, 4.14 |
| 2026-05-21 | Reference catalog; no default skip for IssueType/Status/etc. | 5.x |
| 2026-05-21 | CF validation: accept `customfield_\d+` shape | 4.7 |
| 2026-05-21 | Conflict warning acknowledge UI | 6.1.5 |
| 2026-05-21 | `JiraDcChangeHistoryReplayerTest` | 6.3.1 |
| 2026-05-21 | `enterprise-dc-entities.xml` + `JiraDcEnterpriseFixtureTest` | 1.2, 2.6, 6.3 |
| 2026-05-21 | PluginEntity parser (GreenHopper/AO/Sprint) | 1.2.9, 4.6 |
| 2026-05-21 | Playwright DC wizard E2E + `data-testid` hooks | 6.3.5, 6.1 |
| 2026-05-21 | 2k-issue streaming soak unit gate | 2.6 |
| 2026-05-21 | Pass 6: `JiraDcParitySummaryBuilder` + `DcImportParityReportPanel` | 6.3.6 |
| 2026-05-21 | `JiraDcBackupZipIntegrationTest` (enterprise ZIP → prepareValidate) | 1.1, AC-1 |
| 2026-05-21 | 10k-issue SAX soak unit gate (&lt;120s) | 2.6, AC-2 |
| 2026-05-21 | E2E `MIGRATION_E2E_FULL` + parity panel + execute/continue testids | 6.3.5 |
| 2026-05-21 | Pass 7: SLA proof + AC sign-off evaluator/API/UI + production ZIP fixture | 6.3.7, 6.3.8, AC-1, AC-2 |
| 2026-05-21 | UI-Hardening: wire conflict/unknown/review/graph panels, job console, stub DC option | [MIGRATION_UI_HARDENING_AUDIT.md](./MIGRATION_UI_HARDENING_AUDIT.md) |
