import { expect, type Page } from '@playwright/test';
import { E2E_PROJECT_ID } from './mockProjectsApi';

async function selectFirstProject(page: Page) {
  const exportSelect = page.getByTestId('project-export-project-select');
  const targetSelect = page.getByTestId('migration-target-project-select');
  const genericSelect = page.locator('select').first();

  if (await exportSelect.isVisible()) {
    await exportSelect.selectOption(E2E_PROJECT_ID);
    return;
  }
  if (await targetSelect.isVisible()) {
    await targetSelect.selectOption(E2E_PROJECT_ID);
    return;
  }
  if (await genericSelect.isVisible()) {
    const count = await genericSelect.locator('option').count();
    if (count > 1) {
      await genericSelect.selectOption({ index: 1 });
    }
  }
}

async function resolveDcGates(page: Page) {
  const ack = page.getByTestId('dc-conflicts-ack-checkbox').first();
  if (await ack.isVisible()) {
    await ack.check();
  }

  const action0 = page.getByTestId('dc-conflict-action-0').first();
  if (await action0.isVisible()) {
    await action0.selectOption('SKIP_ENTITY');
  }
}

/** Advance Jira DC wizard until configure step (DcImportOptionsPanel). */
export async function advanceJiraDcToConfigure(page: Page, fixturePath: string) {
  await page.getByTestId('import-type-jira-dc').click();
  const fileInput = page.locator('input[type="file"]').first();
  await expect(fileInput).toBeAttached({ timeout: 10_000 });
  await fileInput.setInputFiles(fixturePath);

  await expect(page.getByText(/Processing file/i)).toBeHidden({ timeout: 90_000 });
  await expect(page.getByTestId('step-continue-button')).toBeEnabled({ timeout: 90_000 });

  await expect
    .poll(
      async () => {
        if (await page.getByTestId('dc-import-options-panel').isVisible()) {
          return true;
        }

        if (await page.getByTestId('dc-import-validation-panel').isVisible()) {
          const continueBtn = page.getByTestId('step-continue-button');
          if ((await continueBtn.isVisible()) && (await continueBtn.isEnabled())) {
            await continueBtn.click();
          }
        }

        await selectFirstProject(page);
        await resolveDcGates(page);

        const continueBtn = page.getByTestId('step-continue-button');
        if ((await continueBtn.isVisible()) && (await continueBtn.isEnabled())) {
          await continueBtn.click();
        }

        return page.getByTestId('dc-import-options-panel').isVisible();
      },
      { timeout: 120_000, intervals: [500, 1000, 2000] }
    )
    .toBe(true);
}
