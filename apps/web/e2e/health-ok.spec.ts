import { expect, test } from '@playwright/test';

import { USE_REAL_API, mockReadiness, okPayload } from './health-api';

test.describe('pantalla de salud con la API respondiendo bien', () => {
  test.skip(USE_REAL_API, 'E2E_USE_REAL_API=1: la API real decide el estado');

  test('muestra el servicio operativo y sus comprobaciones', async ({ page }) => {
    await mockReadiness(page, okPayload, 200);
    await page.goto('/estado');

    await expect(page.getByTestId('health-status')).toHaveText('Servicio operativo');
    await expect(page.getByTestId('health-status')).toHaveAttribute('data-tone', 'ok');
    await expect(page.getByTestId('health-version')).toHaveText('Version 0.1.0');
    await expect(page.getByTestId('health-check')).toHaveCount(1);
  });
});

test.describe('humo contra el entorno que toque', () => {
  test('la pantalla siempre termina de cargar', async ({ page }) => {
    if (!USE_REAL_API) await mockReadiness(page, okPayload, 200);
    await page.goto('/estado');

    await expect(page.getByRole('heading', { name: 'Estado del servicio' })).toBeVisible();
    await expect(page.getByTestId('health-status')).not.toHaveText('Consultando la API...');
  });
});
