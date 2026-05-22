# JDC — Setup on a New Machine

This repository is self-contained. You do **not** need a sibling `Design System` folder.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 18+ |
| PostgreSQL | 16+ |
| npm | 9+ |

## 1. Clone

```bash
git clone https://github.com/ms86100/JDC.git
cd JDC
```

## 2. Frontend

```bash
cd jira-frontend
npm ci
npm run dev
```

Open http://localhost:3000/

Production build:

```bash
npm run build
```

Optional strict TypeScript check (may report existing type debt):

```bash
npm run typecheck
```

The UI uses local CSS under `jira-frontend/src/styles/` (no external Design System folder required).

## 3. Backend (microservices)

See [LOCAL-SETUP-GUIDE.md](LOCAL-SETUP-GUIDE.md) for database creation and per-service Maven builds.

Quick start (Windows, after DBs exist and JARs are built):

```powershell
.\start-platform.ps1
```

Runtime files (logs, PIDs, local Postgres data) go under `platform-runtime/` — created automatically and **not** committed to git.

## 4. What is intentionally not in git

- `jira-frontend/node_modules/` — run `npm ci`
- `jira-*/target/` — run `mvn package` per service
- `platform-runtime/`, `logs/` — generated at runtime
- `.env`, credentials, crash dumps (`hs_err_pid*.log`)
- `.claude/` — local agent tooling

## Migration E2E (optional live CI — MG-P1-7)

Local live API test (migration-service on **8094**, frontend dev server):

```bash
cd jira-frontend
MIGRATION_E2E_API=1 MIGRATION_E2E_LIVE=1 npm run test:e2e:migration-live
```

GitHub Actions: set repository variable **`MIGRATION_E2E_API=1`** to enable `.github/workflows/migration-e2e-live.yml`.

| Variable | Purpose |
|----------|---------|
| `MIGRATION_E2E_API=1` | Hit real migration validate/import APIs (not mocks) |
| `MIGRATION_E2E_FULL=1` | Full import + parity panel (needs full stack + `MIGRATION_E2E_PROJECT_ID`) |
| `MIGRATION_REQUIRE_JWT=true` | Migration service rejects requests without valid Bearer JWT |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `npm install` fails | Delete `jira-frontend/node_modules` and run `npm ci` from `jira-frontend` |
| Frontend build fails | Run `npm run build` (Vite). Use `npm run typecheck` to list TS issues separately |
| Services won't start | Check PostgreSQL, `config/services.json`, and Maven builds in each `jira-*-service` |
