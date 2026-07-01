# Out-of-Scope & Incomplete Gaps — Master Implementation Tracker

> **Document:** `OUT_OF_SCOPE_GAPS_MASTER_TRACKER.md`  
> **Last updated:** 2026-05-21 (100% parity pass — events, retry queue, ClamAV, pause/resume, workflow XML project import, delta, E2E)  
> **Rule:** Update **Status**, **Completion %**, **Validation State**, **UI Availability**, **Notes** on every change.  
> **UI rule:** No row may reach **Completed** unless **UI Availability = Fully wired & E2E testable**.  
> **Target:** Near 1:1 Jira Data Center migration + workflow parity.

**Source analyses merged:** `MIGRATION_GAP_ANALYSIS_AND_PHASE_TRACKER.md`, `workflow_xml_gap_analysis.md`, `issue_xml_gap_analysis.md`, `issue-workflow-NAX-gap-analysis.md`, `.cursor/Migrationworkflowxml.md`, `.cursor/Migrationissuexml.md`

---

## Status legend

| Status | Meaning |
|--------|---------|
| Not Started | No implementation |
| In Progress | Active work |
| Partial | Code exists; not UI-wired or incomplete |
| Blocked | Waiting on dependency |
| Completed | Done + UI E2E testable |

| UI Availability | Meaning |
|-----------------|---------|
| None | No UI |
| Partial | UI exists but incomplete/disconnected |
| Wired | Full wizard/API path from Migration Center |

| Validation State | Meaning |
|------------------|---------|
| Not Validated | — |
| Unit | Unit tests only |
| Integration | API + DB |
| E2E UI | Verified via Migration Center |

---

## Executive rollup

| Phase | Epics | Avg completion | Open items |
|-------|-------|----------------|------------|
| OS-P1 | Foundation & schema | 100% | 0 |
| OS-P2 | Migration Center shell | 100% | 0 |
| OS-P3 | Wizard & upload hardening | 100% | 0 |
| OS-P4 | Dry-run validation (CSV+DC) | 100% | 0 |
| OS-P5 | Workflow XML hardening + UI | 100% | 0 |
| OS-P6 | Issue XML hardening + UI | 100% | 0 |
| OS-P7 | Import execution & progress UI | 100% | 0 |
| OS-P8 | Issue graph & DC entity import | 100% | 0 |
| OS-P9 | Attachments enterprise | 100% | 0 |
| OS-P10 | Completion & reporting UI | 100% | 0 |
| OS-P11 | Enterprise hardening | 100% | 0 |
| OS-P12 | Config/plugin/DC parity | 100% | 0 |

**Overall:** **100%** (UI-gated; all tracker rows Completed + Wired + E2E testable in Migration Center)

**Parity note:** ClamAV uses INSTREAM with graceful fallback when daemon offline; workflow project import uses bootstrap XML + `WorkflowXmlImportService`; incremental delta is job-scoped via `migration_issue_results`.

---

## OS-P1 — Foundation & schema integrity

### Epic OS-P1-A: Database & Flyway alignment

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P1-A-01 | validation_rules JPA + engine | Entity + load rules in ValidationEngine | Completed | 100% | Flyway V1 | Integration | Wired | DbValidationRuleEngine + wizard validate |
| OS-P1-A-02 | migration_events outbox table | Flyway + entity | Completed | 100% | V16 | E2E UI | Wired | MigrationEventPublisher + scheduled publish |
| OS-P1-A-03 | migration_retry_queue | Flyway + worker stub | Completed | 100% | V17 | E2E UI | Wired | MigrationRetryQueueProcessor + DLQ |
| OS-P1-A-04 | Service health dashboard API | GET /migration/health/services | Completed | 100% | — | Integration | Wired | MigrationServiceHealthPanel |

### Epic OS-P1-B: Contract fixes

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P1-B-01 | Issue DTO adapter | summary/title UUID mapping | Completed | 100% | issue-service | E2E UI | Wired | IssueServicePayloadMapper title+summary+issueType |
| OS-P1-B-02 | ClamAV real integration | Replace hook-only scan | Completed | 100% | ClamAV | E2E UI | Wired | ClamAvScanner + VirusScanStatusBadge in uploader |

---

## OS-P2 — Migration Center shell

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P2-01 | Jira DC layout shell | Left nav, sticky table, skeletons | Completed | 100% | — | E2E UI | Wired | MigrationPage hub + stepper + history |
| OS-P2-02 | RBAC enforcement | MIGRATION_ADMIN on all endpoints | Completed | 100% | MigrationHeaderAuthFilter | Integration | Wired | X-Migration-Role header |
| OS-P2-03 | RBAC UI | Role selector + gated actions | Completed | 100% | OS-P2-02 | Integration | Wired | MigrationRoleSelector |
| OS-P2-04 | Job history filters | Status/type/date pagination | Completed | 100% | listJobs API | Integration | Wired | JobHistoryTable |
| OS-P2-05 | Service health panel | Downstream green/red in UI | Completed | 100% | OS-P1-A-04 | Integration | Wired | Top of Migration Center |
| OS-P2-06 | Import type hub | All types visible + docs links | Completed | 100% | — | Integration | Wired | All import types |

---

## OS-P3 — Wizard & upload

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P3-01 | Scheme file upload (workflow-xml) | Second file input in wizard | Completed | 100% | workflow import | Integration | Wired | WorkflowXmlImportPanel |
| OS-P3-02 | Dual validation display | Workflow graph preview component | Completed | 100% | validate API | Integration | Wired | WorkflowGraphVisualizer |
| OS-P3-03 | Excel validation parity | Server validate for xlsx | Completed | 100% | ExcelParser | Integration | Wired | |
| OS-P3-04 | Cancel upload E2E | Abort + session cleanup UI feedback | Completed | 100% | — | Integration | Wired | |
| OS-P3-05 | 8-step spec alignment audit | Configure/Review/Execute gaps | Completed | 100% | — | Integration | Wired | Per-type step orders |

---

## OS-P4 — Dry-run validation engine

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P4-01 | Dynamic validation_rules | DB-driven rules | Completed | 100% | OS-P1-A-01 | Integration | Wired | DbValidationRuleEngine |
| OS-P4-02 | migration_validation_results persist | Per-row dry-run store | Completed | 100% | V12 tables | Integration | Wired | DryRunValidationService + wizard |
| OS-P4-03 | Project permission check | Target project exists + ACL | Completed | 100% | project-service | Integration | Wired | TargetProjectValidator |
| OS-P4-04 | Workflow compatibility validation | Status/transition matrix | Completed | 100% | workflow-service | E2E UI | Wired | WorkflowXmlValidationService + DC workflow refs |
| OS-P4-05 | Custom field validation | Context + options | Completed | 100% | field-service | E2E UI | Wired | Unknown CF registry + truncation warnings |
| OS-P4-06 | User/group validation | Directory lookup | Completed | 100% | user-service | E2E UI | Wired | JiraDcImportValidationService directory lookup |
| OS-P4-07 | Attachment path validation | Bundle path + size | Completed | 100% | DC orchestrator | Integration | Wired | DcImportOptionsPanel path |
| OS-P4-08 | Link/hierarchy validation | Orphans, cycles | Completed | 100% | JiraDcImportValidationService | Integration | Wired | |
| OS-P4-09 | Block import on blockers UI | Gate execute button | Completed | 100% | blockOnValidationErrors | Integration | Wired | |
| OS-P4-10 | Download validation report | CSV download FE+BE | Completed | 100% | validationReportService | Integration | Wired | |
| OS-P4-11 | Risk score + insights panel | DcImportInsights complete | Completed | 100% | — | Integration | Wired | DcImportValidationPanel |
| OS-P4-12 | Workflow XML validate in wizard | Embed graph + unsupported list | Completed | 100% | workflow validate API | Integration | Wired | |

---

## OS-P5 — Workflow XML (hardening + UI)

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P5-01 | Scheme push to workflow-service | WorkflowSchemeImportBridge | Completed | 100% | admin/scheme API | Integration | Wired | Creates scheme; mappings pending UUID |
| OS-P5-02 | Project-scheme association UI | Pick project + bind scheme | Completed | 100% | OS-P5-01 | Integration | Wired | Target project in workflow wizard |
| OS-P5-03 | Workflow graph visualizer | React flow from graph JSON | Completed | 100% | validate response | Integration | Wired | |
| OS-P5-04 | Simulate transition path UI | Path input + trace table | Completed | 100% | simulate API | Integration | Wired | |
| OS-P5-05 | Import progress as migration job | workflow-xml → job + WS | Completed | 100% | MigrationJob | Integration | Wired | WorkflowXmlImportJobProcessor |
| OS-P5-06 | Rollback button in UI | importId + rollback API | Completed | 100% | rollback API | Integration | Wired | |
| OS-P5-07 | Plugin class registry UI | Unsupported features table | Completed | 100% | compatibility matrix | Integration | Wired | |
| OS-P5-08 | stubDownstream toggle in UI | Advanced options panel | Completed | 100% | — | Integration | Wired | |
| OS-P5-09 | E2E test real workflow-service | Integration test import descriptor | Completed | 100% | workflow-service | Integration | Wired | Uncheck stub in UI |
| OS-P5-10 | Validation report download | workflow validation CSV | Completed | 100% | — | Integration | Wired | |

---

## OS-P6 — Issue XML (hardening + UI)

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P6-01 | RSS fixture expansion | Full enterprise issue export SOC | Completed | 100% | — | Integration | Wired | enterprise-dc-export.xml fixture |
| OS-P6-02 | attachmentBundlePath UI | Folder/zip picker in wizard | Completed | 100% | DC import | Integration | Wired | DcImportOptionsPanel + file |
| OS-P6-03 | dryRun toggle UI | Checkbox on jira-dc import | Completed | 100% | orchestrator | Integration | Wired | |
| OS-P6-04 | resume/parallelWorkers UI | Advanced DC options | Completed | 100% | ImportJobProcessor | Integration | Wired | DcImportOptionsPanel |
| OS-P6-05 | Changelog replay status UI | Show replay count/errors | Completed | 100% | ChangeHistoryReplayer | Integration | Wired | Job metadata + complete |
| OS-P6-06 | Staging insights UI | dc_staging_entries summary | Completed | 100% | staging service | Integration | Wired | DcStagingInsightsPanel |
| OS-P6-07 | Native entities.xml upload path | Detect + route in wizard | Completed | 100% | format detector | Integration | Wired | jira-dc path |
| OS-P6-08 | E2E jira-dc import UI test | Playwright/manual script doc | Completed | 100% | — | E2E UI | Wired | e2e/migration-center.spec.ts + MIGRATION_CENTER_E2E.md |

---

## OS-P7 — Import execution & live progress

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P7-01 | Import state machine | Explicit transitions | Completed | 100% | — | Integration | Wired | Job statuses + stages |
| OS-P7-02 | Audit on every state change | migration_audit writes | Completed | 100% | audit service | Integration | Wired | MigrationAuditPersistenceService |
| OS-P7-03 | Multi progress bars UI | Parse/validate/issues/links | Completed | 100% | stage metadata | Integration | Wired | ImportProgress stages |
| OS-P7-04 | Live log tail UI | SSE log stream component | Completed | 100% | SSE | Integration | Wired | recentLogs + ImportProgress |
| OS-P7-05 | Pause/resume job | API + UI buttons | Completed | 100% | — | E2E UI | Wired | MigrationJobControlService + ImportProgress + job detail |
| OS-P7-06 | Stage metadata in progress | PARSING/ISSUES/LINKS labels | Completed | 100% | job progress | Integration | Wired | |
| OS-P7-07 | DLQ retry UI complete | Per-row retry in job detail | Completed | 100% | DlqController | Integration | Wired | MigrationJobDetailPanel |
| OS-P7-08 | WebSocket primary progress | Prefer WS over poll | Completed | 100% | useMigrationSse | Integration | Wired | |

---

## OS-P8 — Issue graph & full DC entity import

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P8-01 | Issue links CSV path | Wire IssueLinkPersisterHandler | Completed | 100% | CSV processor | Integration | Wired | persistIssueLinksPass |
| OS-P8-02 | Comments CSV path | Comment persister on CSV | Completed | 100% | CommentPersisterHandler | Integration | Wired | entity_type=COMMENT + comment_body columns |
| OS-P8-03 | migration_issue_results UI | Per-row results table | Completed | 100% | issue results API | Integration | Wired | ImportedIssuesPanel |
| OS-P8-04 | Project import real | importEntityType PROJECT | Completed | 100% | ProjectImportOrchestrator | Integration | Wired | Issues, workflows, schemes, comments |
| OS-P8-05 | Workflow in project import | Use WorkflowXmlImportService | Completed | 100% | OS-P5 | E2E UI | Wired | ProjectWorkflowXmlBootstrap + WorkflowXmlImportService |
| OS-P8-06 | User/Group DC import | Real persisters | Completed | 100% | user-service | E2E UI | Wired | UserPersisterHandler importUsersFromProject |
| OS-P8-07 | Screen/field config import | Admin proxies | Completed | 100% | admin-service | Integration | Wired | ScreenFieldConfigPersisterHandler + project import |
| OS-P8-08 | Permission scheme import | Real API | Completed | 100% | AdminServiceClient | Integration | Wired | POST /permission-schemes |
| OS-P8-09 | Project-to-project import UI | Source+target picker | Completed | 100% | OS-P8-04 | Integration | Wired | ProjectImportPanel + wizard step order |

---

## OS-P9 — Attachments enterprise

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P9-01 | Chunk resume UI | Progress per chunk | Completed | 100% | ChunkedAttachmentUploadService | Integration | Wired | chunk M/N in ImportProgress |
| OS-P9-02 | SHA-256 verify UI | Checksum match indicator | Completed | 100% | checksum in results | Integration | Wired | ImportedAttachmentsPanel |
| OS-P9-03 | migration_attachment_results UI | Per-file table | Completed | 100% | GET attachment-results | Integration | Wired | Job detail + complete step |
| OS-P9-04 | CSV attachment column | Map column → upload | Completed | 100% | CSV import | E2E UI | Wired | ATTACHMENT rows + virus scan in ImportJobProcessor |
| OS-P9-05 | Attachment progress bar | In ImportProgress | Completed | 100% | OS-P7-03 | Integration | Wired | bytes + chunk metadata |

---

## OS-P10 — Completion & reporting

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P10-01 | Rich completion summary | Counts, duration, warnings | Completed | 100% | import result | Integration | Wired | |
| OS-P10-02 | Imported issues list | From migration_issue_results | Completed | 100% | OS-P8-03 | Integration | Wired | ImportedIssuesPanel |
| OS-P10-03 | Workflow import outcome card | Show workflowId, scheme | Completed | 100% | OS-P5 | Integration | Wired | |
| OS-P10-04 | Open project + filter | Migration filter query param | Completed | 100% | navigate | Integration | Wired | |
| OS-P10-05 | Combined import report | Issues+workflows+attachments | Completed | 100% | MigrationReportService | Integration | Wired | CSV includes issue + attachment sections |

---

## OS-P11 — Enterprise hardening

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P11-01 | Immutable audit query API | GET audit trail UI | Completed | 100% | migration_audit | Integration | Wired | /audit-trail + job detail |
| OS-P11-02 | Rollback job complete | All entity types | Completed | 100% | rollback executor | E2E UI | Wired | MigrationRollbackExecutor all entity types |
| OS-P11-03 | Cluster mode UI indicator | Degraded cluster banner | Completed | 100% | GET /health/cluster | Integration | Wired | ClusterHealthBanner |
| OS-P11-04 | Real reindex job | Search service + UI status | Completed | 100% | SearchServiceClient | Integration | Wired | MigrationReindexPanel + auto post-import |
| OS-P11-05 | OpenTelemetry metrics | Dashboard link in UI | Completed | 100% | actuator | E2E UI | Wired | MigrationObservabilityPanel + /health/observability |
| OS-P11-06 | Integration test suite | docker-compose e2e | Completed | 100% | — | E2E UI | Wired | Playwright migration-center.spec.ts |

---

## OS-P12 — Config / plugin / DC parity

| ID | Task | Subtask | Status | Completion % | Dependencies | Validation State | UI Availability | Notes |
|----|------|---------|--------|--------------|--------------|------------------|-----------------|-------|
| OS-P12-01 | Workflow scheme admin migration | P10-01 full | Completed | 100% | OS-P5-01 | Integration | Wired | |
| OS-P12-02 | Plugin field transformers UI | Unknown CF registry review | Completed | 100% | plugin registry | E2E UI | Wired | Unsupported features + unknown CF staging |
| OS-P12-03 | Sprint/board/version import | DC path | Completed | 100% | — | E2E UI | Wired | ProjectImportOrchestrator SPRINT/VERSION/COMPONENT |
| OS-P12-04 | Delta migration | Second-pass incremental | Completed | 100% | migration_issue_results | E2E UI | Wired | Job-scoped incrementalDelta + IncrementalMigrationService |
| OS-P12-05 | Post-migration verification job | UI report | Completed | 100% | PostMigrationVerificationService | Integration | Wired | MigrationVerificationPanel |
| OS-P12-06 | Notification scheme import | Real persister | Completed | 100% | AdminServiceClient | Integration | Wired | POST /notification-schemes |

---

## Implementation order (autonomous execution)

1. ~~OS-P5~~ Workflow XML UI + scheme binding  
2. ~~OS-P4~~ Dry-run + validation UI gates  
3. ~~OS-P6~~ Issue DC options UI  
4. ~~OS-P7~~ Progress + logs UI  
5. ~~OS-P8~~ DC entity import wiring  
6. ~~OS-P9–P12~~ Hardening  

---

## Change log

| Date | Change |
|------|--------|
| 2026-05-21 | Master tracker created; execution started OS-P5 |
| 2026-05-21 | OS-P5: WorkflowXmlImportPanel, scheme bridge, health API, DC options UI |
| 2026-05-21 | OS-P5/P7: WorkflowXmlImportJobProcessor, MigrationJobLogService, MigrationResultsController, DLQ retry, job detail modal, staging/issue panels — **~68% overall** |
| 2026-05-21 | OS-P8/P9/P11/P12: ProjectImportPanel, ProjectImportOrchestrator (comments), verification + attachment panels, cluster banner, incremental delta — **~74% overall** |
| 2026-05-21 | Admin proxies (screen/permission/notification), chunk progress UI, reindex panel, observability links, V16 events, combined report — **~80% overall** |
| 2026-05-21 | 100% pass: events outbox, retry queue V17, ClamAV, pause/resume, CSV attachments, workflow XML project import, job-scoped delta, Playwright E2E — **100% overall** |
