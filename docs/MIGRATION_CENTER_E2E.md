# Migration Center — E2E test guide

Manual and Playwright-ready flows for UI-gated parity verification.

## Prerequisites

- `jira-migration-service` on port **8094**
- `jira-admin-service` on **8093** (screen/scheme import)
- `jira-frontend` dev server with proxy to gateway
- Migration role: `MIGRATION_ADMIN` via **Migration Center → role selector**

## Core flows

### 1. Workflow XML import

1. Open **Migration Center** → select **Workflow XML**
2. Upload workflow XML (+ optional scheme XML)
3. Validate step → graph + validation table visible
4. Review → uncheck **stub downstream** only if workflow-service is UP (health panel)
5. Execute → job appears in history with live progress + logs
6. Complete → workflow outcome + optional rollback

### 2. Jira DC import

1. Select **Jira DC** → upload entities.xml or RSS zip
2. Set target project, **DC options** (dry-run, resume, incremental delta, attachment bundle)
3. Validate → block on errors if enabled
4. Execute → multi-stage progress (ISSUES, COMMENTS, ATTACHMENTS)
5. Complete → verification panel, issues table, attachments table (SHA column), download report CSV

### 3. Project-to-project import

1. Select **Project import** (no file upload on source step)
2. Pick **source** and **target** projects
3. Review → start import
4. Job detail → entity types include SCREEN, FIELD_CONFIG, COMMENT
5. Post-complete → reindex panel shows status

### 4. CSV with comments

1. CSV with `entity_type=COMMENT` or `comment_body` column
2. Map fields → import
3. Job logs mention `CSV comments: N ok`

## API smoke checks

| Endpoint | Purpose |
|----------|---------|
| `GET /api/migration/health/services` | Downstream health |
| `GET /api/migration/health/cluster` | Cluster banner |
| `GET /api/migration/health/observability` | Metrics links |
| `GET /api/migration/jobs/{id}/verification` | Post-migration checks |
| `GET /api/migration/jobs/{id}/attachment-results` | Per-file attachments |
| `POST /api/migration/jobs/{id}/reindex` | Search reindex |
| `GET /api/migration/jobs/{id}/report` | Combined CSV report |

## Chunked attachment UI

During large attachment upload, progress shows **chunk M/N** and current filename when `attachmentChunked=true` in job metadata.

## Incremental delta

Enable **Incremental delta** on DC import; re-run same export — issues with prior `SUCCESS` in `migration_issue_results` are skipped.
