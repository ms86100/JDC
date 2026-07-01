# Jira DC vs My Platform — Custom Fields, CSV Import & Attachments

**Last updated:** 2026-05-22  
**Demo target:** Stakeholder migration walkthrough (CSV + DC parity)  
**Sample file:** `docs/Jira 2026-05-16T12_52_47+0530.csv` (~55 columns, 5 issues)

---

## Executive summary

| Area | Jira DC | Platform today | Status |
|------|---------|----------------|--------|
| Map 55/60 CSV columns | External System Import | Map step + client/server headers | **Done** |
| Auto-create custom fields on import | Yes (External Import) | Wizard provision panel + session APIs | **Done** |
| Attachments via CSV | Attachments column (URLs/FILE) | Bundle + ATTACHMENT rows + UX hint | **Partial** (G-01/G-02) |
| Attachment max size | Default 10 MB, max 2 GB | Configurable via env/yml | **Done** |
| Admin custom fields UI | Full admin | Real CRUD via `/api/fields/custom` + `/api/custom-fields` | **Done** |
| Field on issue screens | Field configuration schemes | `FieldScreenConfigurationService` + scheme APIs | **Done** |
| Custom field value persist | Values on issues | `FieldValueService` + CSV custom column split | **Done** |

---

## 1. How Jira Data Center does it (official)

### Custom fields + CSV import

| Topic | Jira DC behavior | Official reference |
|--------|------------------|-------------------|
| Map CSV → fields | Admin **External System Import** (not lightweight “Import issues from CSV” under Issues) | [Importing data from CSV](https://confluence.atlassian.com/adminjiraserver/importing-data-from-csv-938847533.html) |
| Create fields on import | Importer can **auto-create custom fields** if they don’t exist; map “any other fields” to custom fields | Same doc — Setup field mappings |
| Attachments in CSV | Only via **External System Import**: map **Attachments** column (HTTP/HTTPS, or `FILE:` under `JIRA_HOME/import/attachments`) | [KB: CSV + attachments](https://support.atlassian.com/jira/kb/how-to-migrate-issues-with-attachments-using-the-csv-import-in-jira-server-and-data-center/) |
| Lightweight CSV import | Cannot map attachment field; subtasks/attachments/multi-project need system admin import | [Creating issues using CSV importer](https://confluence.atlassian.com/display/JIRA/Creating+issues+using+the+CSV+importer) |

### Attachments in Data Center

| Topic | Jira DC | Official reference |
|--------|---------|-------------------|
| Storage | `JIRA_HOME/data/attachments` (filesystem, not DB) | [Configuring file attachments](https://confluence.atlassian.com/display/ADMINJIRASERVER/Configuring+file+attachments) |
| Max file size | Default **10 MB**, configurable up to **2 GB** | Same |
| XML/backup | Restore attachment directory separately | [Restoring from XML backup](https://confluence.atlassian.com/adminjiraserver106/restoring-data-from-an-xml-backup-1573489215.html) |

---

## 2. Platform architecture (current)

```
CSV / DC XML upload
    → ImportWizardSessionService (parse, detectedHeaders, previewRows)
    → Migration wizard: useTargetFields → GET /api/fields/definitions|custom
    → POST /api/migration/wizard/sessions/{id}/fields/discover|provision-missing
    → POST /api/fields/map (auto-map)
    → PATCH wizard field-mappings → job.options.fieldMappings
    → ImportJobProcessor → CsvFieldMappingService.buildIssueDataFromCsvRow (customFields map)
    → IssuePersisterHandler → issue-service + CustomFieldPersisterHandler → FieldValueService
    → Attachments → AttachmentPersisterHandler → attachment-service :8090
```

### Key APIs

| API | Purpose |
|-----|---------|
| `POST /api/migration/wizard/sessions/{id}/fields/discover` | Scan session CSV headers |
| `POST /api/migration/wizard/sessions/{id}/fields/provision-missing` | Auto-create missing fields + screen alignment |
| `GET/POST/PUT/DELETE /api/fields/custom` | Admin custom field CRUD |
| `GET/POST/PUT/DELETE /api/custom-fields` | Legacy compat (same registry, migration-service) |
| `POST /api/fields/schemes/projects/{id}/ensure-fields` | Jira DC field configuration scheme alignment |
| `GET /api/fields/schemes/projects/{id}` | Visible screen configuration for project |
| `GET /api/fields/issues/{issueId}/values` | Read/write issue field values |

---

## 3. Implementation tracker

| ID | Gap | Status | Notes |
|----|-----|--------|-------|
| CF-01 | CSV upload shows 0 source columns | **completed** | Session + client fallback |
| CF-02 | FieldMappingPanel sync when headers arrive | **completed** | `useEffect` on `sourceHeaders` |
| CF-03 | UTF-8 BOM on Jira CSV | **completed** | Client + parser |
| CF-04 | Jira column aliases | **completed** | Mapping service + client |
| CF-05 | Wizard discover unknown columns | **completed** | `MigrationFieldProvisionPanel` |
| CF-06 | Wizard provision missing fields | **completed** | Session provision API |
| CF-07 | Auto-map after provision | **completed** | Refetch + `autoMapFromHeaders` |
| CF-08 | Attachment max size configurable | **completed** | `migration.attachment.max-size-bytes` |
| CF-09 | CSV Attachment column hint | **completed** | Panel guidance |
| CF-10 | Admin CustomFieldsPage real CRUD | **completed** | `fieldApi` + `CustomFieldsPage.tsx` |
| CF-11 | Gateway `/api/custom-fields` → migration | **completed** | Gateway + vite proxy → :8094 |
| CF-12 | Field config schemes on screens | **completed** | `FieldScreenConfigurationService` + scheme APIs |
| CF-13 | CustomFieldPersisterHandler real persist | **completed** | `FieldProvisioningService` + `FieldValueService` |

---

## 4. Open gaps — DC behavior (G-01–G-10) — **completed 2026-05-22**

| ID | Jira DC behavior | Status | UI location |
|----|------------------|--------|-------------|
| **G-01** | CSV Attachments column (HTTP/HTTPS) | **completed** | Configure → External profile; `JiraDcCsvAttachmentResolver` + issue-column pass |
| **G-02** | `FILE:` under import attachments dir | **completed** | `migration.import.attachments-dir` + Configure panel |
| **G-03** | Lightweight vs External Import | **completed** | `CsvImportOptionsPanel` + job `csvImportProfile` |
| **G-04** | Multi-project / subtasks CSV | **completed** | Subtasks via `parent_key`; multi-project warning in logs + Configure copy |
| **G-05** | Attachment max size / storage | **completed** | `GET /api/migration/settings` + Import settings tab + Admin attachments |
| **G-06** | XML + attachment bundle | **completed** | DC validation bundle checklist panel |
| **G-07** | Legacy `/api/migration/fields` | **completed** | `@Deprecated` on controller; settings API documents replacement |
| **G-08** | Provision ADMIN / wizard path | **completed** | Settings doc + provision button gated by role/profile |
| **G-09** | Custom field options | **completed** | `CustomFieldPersisterHandler` options + Option mapping matrix on Map step |
| **G-10** | Issue UI field values | **completed** | Issue Details → Imported custom fields (`IssueMigratedFieldsPanel`) |

---

## 5. Demo script (stakeholder)

1. Start **jira-migration-service** (8094), **attachment-service** (8090), issue/project services.
2. Open `/migration?import=csv` → upload sample CSV → **~55 columns**.
3. **Provision missing fields** → auto-map → execute import.
4. **Admin → Custom fields** (`/admin/custom-fields`) — list/create/disable fields.
5. Confirm custom column values via `GET /api/fields/issues/{issueId}/values`.
6. Attachments: use **DC XML + bundle** or explain G-01/G-02 for CSV URLs.

---

## 6. Files touched (CF-10–CF-13)

| File | Change |
|------|--------|
| `CustomFieldsCompatController.java` | `/api/custom-fields` compat CRUD |
| `FieldScreenConfigurationService.java` | Screen + context alignment |
| `CustomFieldPersisterHandler.java` | Real provision + value persist |
| `IssuePersisterHandler.java` | Persist custom field values after issue create |
| `CsvFieldMappingService.java` | `buildIssueDataFromCsvRow` + customFields split |
| `FieldController.java` | Custom CRUD + scheme endpoints |
| `ImportWizardController.java` | Screen align after provision |
| `application-local.yml` (gateway) | Route `/api/custom-fields` → 8094 |
| `vite.config.ts` | Proxy `/api/custom-fields` |
| `fieldApi.ts` | Admin CRUD + scheme APIs |
| `CustomFieldsPage.tsx` | Live data (no mocks) |

---

## 7. Regression checklist

- [ ] `npm run build` (frontend)
- [ ] `mvn -q compile test -Dtest=ImportSpreadsheetParserTest`
- [ ] CSV wizard: upload → 55 columns → provision → execute
- [ ] Admin custom fields: create + list + disable
- [ ] `GET /api/custom-fields` returns migration registry (not test-service)
- [ ] Custom field values after CSV import (`/api/fields/issues/{id}/values`)

---

## 8. References

- https://confluence.atlassian.com/adminjiraserver/importing-data-from-csv-938847533.html
- https://support.atlassian.com/jira/kb/how-to-migrate-issues-with-attachments-using-the-csv-import-in-jira-server-and-data-center/
- https://confluence.atlassian.com/display/ADMINJIRASERVER/Configuring+file+attachments
