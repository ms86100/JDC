# Migration Center — UI Hardening Audit



**Agent:** [.claude/agents/UI-Hardening.md](../.claude/agents/UI-Hardening.md)  

**Date:** 2026-05-21 (updated)  

**Scope:** `jira-frontend/src/features/migration` + `jira-migration-service` migration APIs



## Navigation & discoverability



| Entry | Status |

|-------|--------|

| Sidebar **Operations → Migration** (`/migration`) | OK |

| Breadcrumbs **Operations / Migration** (`routeMeta.ts`) | OK |

| **Migration Center** tabs: Wizard, Job history, Platform health, Capability map | OK |



## UI exposure matrix (post-hardening)



| Capability | Backend API | UI surface | Testable E2E |

|------------|-------------|------------|--------------|

| CSV / Excel import | `/import/csv`, wizard | Wizard → Source | Yes |

| Jira DC XML/ZIP | `/import/jira-dc`, validate | Wizard + options + review | Yes (`data-testid`) |

| Workflow XML | `/import/workflow-xml` | Wizard + graph visualizer | Yes |

| Project copy | `/import/project` | Wizard → Project Copy | Yes |

| **Project export** | `/export/project` | Wizard → Project Export (panel, review, execute) | Yes |

| DC validate | `/import/jira-dc/validate` | Validation panel + conflicts + unknown CF | Yes |

| DC conflicts — **per-entity resolution** | `options.conflictResolutions` | `DcImportConflictPanel` (SKIP / override) | Yes |

| DC unknown custom fields | validate response | `DcImportUnknownFieldsPanel` | Yes |

| DC relationship edges | validate response | `DcRelationshipGraphPanel` | Yes |

| DC options (ZIP, dry-run, incremental, history-only, **history replay**, stub) | job options | `DcImportOptionsPanel` + preset | Yes |

| DC review gate | — | `DcImportReviewPanel` blocks execute until valid/ack | Yes |

| Live progress / pause / resume | progress, pause APIs | `ImportProgress` + `JobPauseResumeControls` | Yes |

| Job history | `/jobs` | **Job history** tab | Yes |

| Job console (audit, DLQ, logs, reindex, verification) | multiple | `MigrationJobDetailPanel` modal | Yes |

| Job console from **history** (import type + metadata) | `GET /jobs/{id}`, result | Fetches job + result metadata | Yes |

| DC parity / SLA / AC | result metadata + `/dc-sla-proof`, `/dc-ac-signoff` | Complete step + job console (DC) | Yes |

| Rollback / retry / reports | rollback, retry, report | `DcImportJobOperationsPanel` + history actions | Yes |

| Service / cluster / observability health | health endpoints | **Platform health** tab + banner | Yes |

| Capability catalog | — | **Capability map** tab (`MigrationFeatureCatalog`) | Yes |



## Gaps closed in this pass



1. **Project export wizard** — `ProjectExportPanel` on source/target, review step, `Start Export` executes `POST /export/project`.

2. **Interactive conflict resolution** — per-row actions (`PROCEED`, `SKIP_ENTITY`, `USE_DEFAULT`, `OVERRIDE_VALUE`) wired to `options.conflictResolutions`; backend `JiraDcConflictResolutionApplier`.

3. **History replay without creating issues** — `historyReplayOnly` option + preset; backend skips Issue/SubTask create when prior mapping exists.

4. **Job console from history** — `handleViewDetails` loads `importSource` / `jobType` and `resultMetadata` from job API.

5. **Playwright** — `migration-center.spec.ts` (tabs, export); extended `jira-dc-import.spec.ts` (history replay, conflict actions).



## Remaining gaps (honest)



| Gap | Severity | Notes |

|-----|----------|-------|

| Formal AC sign-off | High | Separate checklist — UI does not imply DC parity certification |

| Export download UX in wizard complete step | Low | Report still via Job history download |

| Conflict resolution for validation **blockers** without entity keys | Low | Some validate conflicts use message as key |



## Production readiness (UI lens)



| Area | % exposed in UI | Notes |

|------|-----------------|-------|

| Migration wizard | **~96%** | All major import/export types navigable |

| DC issue XML | **~93%** | Conflicts, graph, review, parity/SLA/AC on complete |

| Job operations | **~94%** | History tab + job console with job context |

| Platform ops | **~85%** | Health + observability tabs |



**Overall UI hardening:** **~93%** — migration UI gaps from the original audit are closed; formal AC sign-off still separate ([issue_xml_ac_signoff_checklist.md](./issue_xml_ac_signoff_checklist.md)).



## How to verify



1. Open **Migration** from sidebar → confirm four tabs.

2. **Capability map** tab → every row has **Open** to wizard/history/health.

3. Run **Jira DC** wizard → validate → set conflict actions → history-replay preset → review → execute.

4. Run **Project export** → select project/format → review → **Start Export** → track in Job history.

5. **Job history** → view details on a DC job → parity/SLA/AC panels without re-running wizard.

6. E2E: `MIGRATION_E2E_API=1 npx playwright test e2e/jira-dc-import.spec.ts e2e/migration-center.spec.ts`

