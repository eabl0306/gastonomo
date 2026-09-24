import { expect, test } from '@playwright/test';

import { USE_REAL_API } from './health-api';

/**
 * El unico test que atraviesa las tres piezas de verdad: navegador, web, API y
 * PostgreSQL, sin mockear nada. Necesita la pila de Docker levantada y entra por
 * el gateway, que sirve la web y la API bajo el mismo origen.
 *
 *   just e2e-stack
 *
 * En el modo normal (`just web-e2e`) se salta: los demas specs interceptan la red
 * y por eso son rapidos y deterministas.
 */
test.describe('la web contra la API real', () => {
  test.skip(!USE_REAL_API, 'necesita la pila levantada: just e2e-stack');

  test('pinta el informe que devuelve la API, no uno inventado', async ({ page }) => {
    await page.goto('/');

    const status = page.getByTestId('health-status');
    await expect(status).toHaveText('Servicio operativo');
    await expect(status).toHaveAttribute('data-tone', 'ok');
    await expect(page.getByTestId('health-version')).toHaveText(/^Version \d+\.\d+\.\d+$/u);
  });

  test('la comprobacion de la base de datos llega hasta PostgreSQL', async ({ page }) => {
    await page.goto('/');

    const database = page.getByTestId('health-check').filter({ hasText: 'database' });
    await expect(database).toHaveCount(1);
    await expect(database).toHaveAttribute('data-status', 'ok');
  });

  test('el boton de reintentar vuelve a preguntar a la API', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('health-status')).toHaveText('Servicio operativo');

    const readiness = page.waitForResponse(
      (response) => response.url().includes('/health/ready') && response.status() === 200,
    );
    await page.getByTestId('health-refresh').click();
    await readiness;

    await expect(page.getByTestId('health-status')).toHaveText('Servicio operativo');
  });
});
