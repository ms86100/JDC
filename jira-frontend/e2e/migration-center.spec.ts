import { test, expect } from '@playwright/test';

import { seedE2EAuth } from './helpers/auth';

import { mockProjectsApi, E2E_PROJECT_ID } from './helpers/mockProjectsApi';
import { mockMigrationApis } from './helpers/mockMigrationApi';

test.describe('Migration center', () => {
  test.beforeEach(async ({ page }) => {
    await mockProjectsApi(page);
    await mockMigrationApis(page);
    await seedE2EAuth(page);
    await page.goto('/migration');
  });



  test('shows wizard tabs and capability map', async ({ page }) => {

    await expect(page.getByTestId('migration-nav-wizard')).toBeVisible();

    await expect(page.getByTestId('migration-nav-history')).toBeVisible();

    await expect(page.getByTestId('migration-nav-health')).toBeVisible();

    await page.getByTestId('migration-nav-catalog').click();

    await expect(page.getByTestId('migration-feature-catalog')).toBeVisible({ timeout: 5000 });

  });

  test('global DLQ and mapping templates tabs', async ({ page }) => {
    await page.getByTestId('migration-nav-dlq').click();
    await expect(page.getByTestId('global-dlq-console')).toBeVisible({ timeout: 5000 });

    await page.getByTestId('migration-nav-templates').click();
    await expect(page.getByTestId('saved-mapping-templates')).toBeVisible({ timeout: 5000 });
  });



  test('project export wizard surfaces panel and review', async ({ page }) => {

    await page.getByTestId('import-type-project-export').click();

    await expect(page.getByTestId('project-export-panel')).toBeVisible();



    await page.getByTestId('project-export-project-select').selectOption(E2E_PROJECT_ID);

    await page.getByTestId('step-continue-button').click();



    await expect(page.getByTestId('project-export-panel')).toBeVisible();

    await page.getByTestId('project-export-project-select').selectOption(E2E_PROJECT_ID);

    await page.getByTestId('step-continue-button').click();



    await expect(page.getByTestId('project-export-review')).toBeVisible({ timeout: 15_000 });

    await expect(page.getByTestId('import-execute-button')).toHaveText(/Start Export/i);

  });

});

