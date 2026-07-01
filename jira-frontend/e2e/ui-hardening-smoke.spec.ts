import { test, expect } from '@playwright/test';

/**
 * UI-Hardening smoke — discoverability paths (Xray plugin, DC shell, migration catalog).
 * Run: npx playwright test e2e/ui-hardening-smoke.spec.ts
 */
test.describe('UI hardening smoke', () => {
  test('Xray hub loads at /tests with project picker', async ({ page }) => {
    await page.goto('/tests');
    await expect(page.getByRole('heading', { name: /Xray Test Management/i })).toBeVisible();
    await expect(page.getByText(/Xray plugin/i).first()).toBeVisible();
    await expect(page.getByLabel('Project')).toBeVisible();
  });

  test('More menu exposes Xray and Migration', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('button', { name: /More/i }).click();
    await expect(page.getByRole('menuitem', { name: /Xray Test Management/i })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /Migration Center/i })).toBeVisible();
  });

  test('Traceability route registered', async ({ page }) => {
    await page.goto('/tests/traceability');
    await expect(page.locator('body')).toContainText(/traceability|Traceability/i);
  });

  test('Administration gear reachable', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByTitle('Administration').click();
    await expect(page).toHaveURL(/\/admin/);
  });
});
