import type { Page } from '@playwright/test';

/** Seed localStorage so ProtectedRoute allows /migration without a live auth service. */
export async function seedE2EAuth(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('accessToken', 'e2e-playwright-token');
    localStorage.setItem(
      'user',
      JSON.stringify({
        accessToken: 'e2e-playwright-token',
        refreshToken: 'e2e-refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
        userId: '00000000-0000-0000-0000-000000000001',
        username: 'e2e-admin',
        email: 'e2e-admin@test.local',
        roles: ['ADMIN'],
      })
    );
  });
}
