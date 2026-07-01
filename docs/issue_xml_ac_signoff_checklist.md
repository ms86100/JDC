# Jira DC Issue XML — Enterprise AC Sign-Off Checklist

**Canonical tracker:** [issue_xml_enterprise_readiness_tracker.md](./issue_xml_enterprise_readiness_tracker.md)  
**Live evaluation API:** `GET /api/migration/jobs/{jobId}/dc-ac-signoff`  
**SLA proof API:** `GET /api/migration/jobs/{jobId}/dc-sla-proof`

This checklist is the **formal gate** before calling the importer production-ready. Status is computed honestly from job metadata — not manual overrides.

| ID | What it means (Jira DC context) | How we prove it | Sign-off ready when |
|----|----------------------------------|-----------------|---------------------|
| **AC-1** | Operators can upload a real **Jira Data Center backup ZIP** (entities.xml + attachment tree) through the migration UI and the engine extracts/parses it. | `backupZip` / `extractedBackupRoot` on job; `JiraDcProductionBackupFixtureTest`; UI backup option | Real DC 9.x customer ZIP soaked in staging (not synthetic only) |
| **AC-2** | **Live import SLA:** importing ~10k issues completes within tier budget (30 min for 10k, 5 min for 1k smoke) with **real** issue-service writes (`stubDownstream=false`). | `slaProof` on completed job; `JiraDcLiveImportSlaTest`; parse-only 10k gate | `slaProof.slaMet=true` on a non-stub job at target volume |
| **AC-3** | **Attachment integrity:** ≥99.9% of attachments with expected SHA-256 match after upload. | `attachmentChecksumMatchRate` on job metadata | Rate ≥99.9% on job with attachments |
| **AC-4** | **Changelog fidelity:** field-level history matches DC export (author, time, field, old/new). | `historyReplayed` + diff tests vs DC export | Automated diff-verified sample |
| **AC-5** | **History-only mode** does not apply spurious workflow transitions. | `historyOnlyImport` flag + workflow applier disabled on replay | E2E proof with history-only fixture |
| **AC-6** | **Plugin / unknown custom fields** mapped or registered (GreenHopper, AO, etc.). | PluginEntity merge + unknown CF registry UI | Registry + mapping for pilot plugins |
| **AC-7** | **Rollback** restores consistency (issues, attachments, links). | Rollback API + drill runbook | Documented rollback drill PASS |
| **AC-8** | **All operations in UI** (validate, import, progress, parity, conflicts, rollback). | UI panels + testids | UX audit complete |
| **AC-9** | **E2E suite green** in CI with live stack optional profile. | Playwright `MIGRATION_E2E_FULL=1` | CI job green |
| **AC-10** | **Security review** (XXE, zip slip, bomb limits, auth, audit). | Code + penetration checklist | Security sign-off attached |

**Formal sign-off:** all 10 rows **signoffReady** — currently **0/10** until evidence above is attached per row.
