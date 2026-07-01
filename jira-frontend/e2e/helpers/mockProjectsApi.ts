import type { Page } from '@playwright/test';

const E2E_PROJECT_ID = '00000000-0000-0000-0000-0000000000e2';

/** Ensures wizard target/export project dropdowns have at least one project. */
export async function mockProjectsApi(page: Page) {
  const payload = [
    {
      id: E2E_PROJECT_ID,
      name: 'E2E Migration Project',
      projectKey: 'E2EMIG',
      description: 'Playwright fixture project',
    },
  ];

  await page.route('**/api/projects**', async (route) => {
    if (route.request().method() !== 'GET') {
      return route.continue();
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(payload),
    });
  });
}

export { E2E_PROJECT_ID };
