# E2E fixtures

DC import tests reference fixtures from `jira-migration-service/src/test/resources/samples/`.

Run API-backed E2E:

```bash
# Terminal 1: migration-service on :8094, issue-service, frontend on :3000
cd jira-frontend
set MIGRATION_E2E_API=1
npx playwright test e2e/jira-dc-import.spec.ts
```
