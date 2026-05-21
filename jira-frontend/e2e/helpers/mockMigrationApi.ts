import type { Page } from '@playwright/test';

const E2E_SESSION_ID = 'e2e-wizard-session-0001';

const dcValidateOk = {
  valid: true,
  totalEntities: 12,
  blockerCount: 0,
  warningCount: 0,
  entitiesByType: { Issue: 10, Comment: 2 },
  relationshipEdges: [{ from: 'TST-1', to: 'TST-2', type: 'blocks' }],
  conflicts: [] as Array<{
    code: string;
    message: string;
    severity: string;
    entityKey?: string;
    field?: string;
  }>,
  errors: [],
};

const dcValidateWithConflicts = {
  ...dcValidateOk,
  valid: true,
  warningCount: 1,
  conflicts: [
    {
      code: 'DUPLICATE_KEY',
      message: 'Duplicate issue key in export',
      severity: 'WARNING',
      entityKey: 'TST-1',
      field: 'key',
    },
    {
      code: 'MISSING_FIELD',
      message: 'Required custom field missing',
      severity: 'BLOCKER',
      entityKey: 'TST-2',
      field: 'customfield_10001',
    },
  ],
};

let useConflictValidate = false;

/** Use conflict-rich DC validate response (for conflict panel E2E). */
export function enableDcConflictValidateMock() {
  useConflictValidate = true;
}

export function resetDcValidateMock() {
  useConflictValidate = false;
}

/** Mock migration-service when not running (default for Playwright). Set MIGRATION_E2E_LIVE=1 to hit real APIs. */
export async function mockMigrationApis(page: Page) {
  if (process.env.MIGRATION_E2E_LIVE === '1') {
    return;
  }

  await page.route('**/api/migration/health/cluster**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'HEALTHY',
        activeNodes: 2,
        totalNodes: 2,
        warnings: [],
      }),
    });
  });

  await page.route('**/api/migration/health/**', async (route) => {
    if (route.request().url().includes('/cluster')) {
      return route.continue();
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        status: 'UP',
        services: [
          { name: 'project-service', status: 'UP' },
          { name: 'issue-service', status: 'UP' },
          { name: 'migration-service', status: 'UP' },
        ],
      }),
    });
  });

  await page.route('**/api/migration/templates**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  await page.route('**/api/migration/wizard/sessions**', async (route) => {
    const method = route.request().method();
    if (method === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sessionId: E2E_SESSION_ID,
          step: 'SOURCE',
          importType: 'JIRA_DC',
          status: 'ACTIVE',
        }),
      });
      return;
    }
    await route.continue();
  });

  await page.route(/\/api\/migration\/wizard\/sessions\/[^/]+/, async (route) => {
    if (route.request().method() === 'GET' || route.request().method() === 'PATCH') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          sessionId: E2E_SESSION_ID,
          step: 'CONFIGURE',
          importType: 'JIRA_DC',
          status: 'ACTIVE',
          detectedEntityType: 'ISSUE',
          totalRows: 12,
        }),
      });
      return;
    }
    await route.continue();
  });

  await page.route(/\/api\/migration\/wizard\/sessions\/[^/]+\/upload/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        uploadId: 'e2e-upload-1',
        virusScanStatus: 'CLEAN',
        detectedHeaders: [],
      }),
    });
  });

  await page.route('**/api/migration/import/jira-dc/validate**', async (route) => {
    const body = useConflictValidate ? dcValidateWithConflicts : dcValidateOk;
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(body),
    });
  });

  await page.route('**/api/fields/**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  await page.route('**/api/migration/uploads/*/virus-scan**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ virusScanStatus: 'CLEAN' }),
    });
  });
}

/** @deprecated use mockMigrationApis */
export async function mockMigrationHealthApis(page: Page) {
  await mockMigrationApis(page);
}
