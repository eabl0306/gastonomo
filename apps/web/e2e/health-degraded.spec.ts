import { expect, test } from '@playwright/test';

import { USE_REAL_API, breakReadiness, degradedPayload, mockReadiness } from './health-api';

test.describe('pantalla de salud cuando la API falla', () => {
  test.skip(USE_REAL_API, 'E2E_USE_REAL_API=1: la API real decide el estado');

  test('muestra el servicio degradado ante un 503', async ({ page }) => {
    await mockReadiness(page, degradedPayload, 503);
    await page.goto('/');

    await expect(page.getByTestId('health-status')).toHaveText('Servicio degradado');
    await expect(page.getByTestId('health-check')).toHaveAttribute('data-status', 'error');
  });

  test('muestra API inalcanzable cuando no hay nadie al otro lado', async ({ page }) => {
    await breakReadiness(page);
    await page.goto('/');

    await expect(page.getByTestId('health-status')).toHaveText('API inalcanzable');
    await expect(page.getByTestId('health-reason')).toBeVisible();
  });
});
