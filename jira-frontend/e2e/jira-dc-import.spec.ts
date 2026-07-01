import { test, expect } from '@playwright/test';
import path from 'path';
import { seedE2EAuth } from './helpers/auth';
import { mockProjectsApi, E2E_PROJECT_ID } from './helpers/mockProjectsApi';
import {
  mockMigrationApis,
  enableDcConflictValidateMock,
  resetDcValidateMock,
} from './helpers/mockMigrationApi';
import { advanceJiraDcToConfigure } from './helpers/wizard';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const RSS_FIXTURE = path.resolve(
  __dirname,
  '../../jira-migration-service/src/test/resources/samples/jira_dc_issue_export.xml'
);

/**
 * Jira DC import wizard E2E.
 * Requires: jira-frontend (baseURL), jira-migration-service :8094
 * Set MIGRATION_E2E_SKIP_IMPORT=1 to run UI-only (no execute step).
 */
test.describe('Jira DC import wizard', () => {
  test.setTimeout(120_000);
  test.beforeEach(async ({ page }) => {
    resetDcValidateMock();
    await mockProjectsApi(page);
    await mockMigrationApis(page);
    await seedE2EAuth(page);
    await page.goto('/migration');
  });

  test('selects Systems and Avionics Backup import type', async ({ page }) => {
    await page.getByTestId('import-type-jira-dc').click();
    await expect(page.getByText(/Jira DC XML export|Systems and Avionics/i).first()).toBeVisible();
  });

  test('uploads RSS fixture and runs server validation', async ({ page }) => {
    test.skip(
      process.env.MIGRATION_E2E_LIVE === '1' && !process.env.MIGRATION_E2E_API,
      'Set MIGRATION_E2E_API=1 with migration-service for live API run'
    );

    await advanceJiraDcToConfigure(page, RSS_FIXTURE);

    await expect(page.getByTestId('dc-import-options-panel')).toBeVisible({ timeout: 5000 });
    await page.getByTestId('dc-validate-button').click();

    await expect(page.getByTestId('dc-import-validation-panel')).toContainText(/valid|Blockers|Warnings/i, {
      timeout: 30000,
    });
    if (process.env.MIGRATION_E2E_API === '1') {
      await expect(page.getByTestId('dc-import-ac-signoff-panel')).toBeVisible({ timeout: 10000 });
    }
  });

  test('full import completes with parity panel when live stack enabled', async ({ page }) => {
    test.skip(!process.env.MIGRATION_E2E_FULL, 'Set MIGRATION_E2E_FULL=1 with full stack running');
    const projectId = process.env.MIGRATION_E2E_PROJECT_ID;
    test.skip(!projectId, 'Set MIGRATION_E2E_PROJECT_ID to a valid target project UUID');

    await page.getByTestId('import-type-jira-dc').click();
    await page.locator('input[type="file"]').first().setInputFiles(RSS_FIXTURE);

    await page.getByTestId('step-continue-button').click();
    await page.getByTestId('step-continue-button').click();

    await expect(page.getByTestId('dc-import-options-panel')).toBeVisible({ timeout: 15000 });
    await page.getByTestId('dc-validate-button').click();
    await expect(page.getByTestId('dc-import-validation-panel')).toBeVisible({ timeout: 30000 });

    await page.getByTestId('step-continue-button').click();
    await page.locator('select').first().selectOption({ value: projectId! });
    await page.getByTestId('step-continue-button').click();
    await page.getByTestId('step-continue-button').click();

    await page.getByTestId('import-execute-button').click();
    await expect(page.getByText(/Import Completed|Import Failed/i).first()).toBeVisible({
      timeout: 180_000,
    });
    await expect(page.getByTestId('dc-import-parity-panel')).toBeVisible({ timeout: 30_000 });
  });

  test('configure step shows DC options and conflict panels', async ({ page }) => {
    test.skip(
      process.env.MIGRATION_E2E_LIVE === '1' && !process.env.MIGRATION_E2E_API,
      'Set MIGRATION_E2E_API=1 with migration-service for live API run'
    );

    await advanceJiraDcToConfigure(page, RSS_FIXTURE);

    await expect(page.getByTestId('dc-import-options-panel')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(/Block import when validation/i)).toBeVisible();
    await expect(page.getByText(/Incremental delta|history-only|Parallel workers/i).first()).toBeVisible();
    await expect(page.getByTestId('dc-history-replay-only')).toBeVisible();
    await expect(page.getByTestId('dc-history-replay-preset')).toBeVisible();
  });

  test.describe('with conflict validation payload', () => {
    test.beforeEach(async ({ page }) => {
      enableDcConflictValidateMock();
      await mockProjectsApi(page);
      await mockMigrationApis(page);
      await seedE2EAuth(page);
      await page.goto('/migration');
    });

    test('conflict panel supports per-row resolution when validation returns conflicts', async ({
      page,
    }) => {
      await page.getByTestId('import-type-jira-dc').click();
      await page.locator('input[type="file"]').first().setInputFiles(RSS_FIXTURE);
      await expect(page.getByText(/Processing file/i)).toBeHidden({ timeout: 90_000 });
      await page.getByTestId('step-continue-button').click();
      await page.getByTestId('migration-target-project-select').selectOption(E2E_PROJECT_ID);
      await page.getByTestId('step-continue-button').click();

      await expect(page.getByTestId('dc-import-conflict-panel').first()).toBeVisible({
        timeout: 30_000,
      });
      const actionSelect = page.getByTestId('dc-conflict-action-0').first();
      await actionSelect.selectOption('SKIP_ENTITY');
      await page.getByTestId('dc-conflicts-ack-checkbox').first().check();
    });
  });
});
