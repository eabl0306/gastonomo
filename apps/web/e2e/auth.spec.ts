import { expect, test, type Page } from '@playwright/test';

import { USE_REAL_API } from './health-api';

const SESSION = {
  accessToken: 'token-de-prueba',
  expiresIn: 900,
  refreshToken: 'refresco-de-prueba',
  user: { id: 'u1', name: 'Camilo Restrepo', email: 'camilo@correo.com' },
};

const JSON_HEADERS = { 'content-type': 'application/json' };

async function mockLogin(page: Page, status: number, body: unknown): Promise<void> {
  await page.route('**/v1/auth/login', async (route) => {
    await route.fulfill({ status, headers: JSON_HEADERS, body: JSON.stringify(body) });
  });
}

test.describe('entrar y salir', () => {
  test.skip(USE_REAL_API, 'E2E_USE_REAL_API=1: no se inventan cuentas contra la API real');

  test('sin sesion, cualquier direccion de la aplicacion lleva a Entrar', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeVisible();
  });

  /**
   * Este camino es el que se rompio una vez: la sesion se leia del navegador en cada
   * repintado y React entraba en bucle, asi que al entrar la pagina se quedaba en blanco.
   * Un test de componente no lo veia porque su doble devolvia siempre el mismo objeto.
   */
  test('al entrar se llega a Inicio y la sesion aguanta una recarga', async ({ page }) => {
    await mockLogin(page, 200, SESSION);
    await page.goto('/login');

    await page.getByRole('textbox', { name: 'Correo' }).fill('camilo@correo.com');
    await page.getByRole('textbox', { name: 'Contraseña' }).fill('clave de prueba');
    await page.getByRole('button', { name: 'Entrar' }).click();

    await expect(page.getByRole('heading', { name: 'Inicio' })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Inicio' })).toBeVisible();
  });

  test('un correo o una clave que no valen lo dicen, sin decir cual fallo', async ({ page }) => {
    await mockLogin(page, 401, { title: 'Unauthorized', status: 401 });
    await page.goto('/login');

    await page.getByRole('textbox', { name: 'Correo' }).fill('camilo@correo.com');
    await page.getByRole('textbox', { name: 'Contraseña' }).fill('la que no es');
    await page.getByRole('button', { name: 'Entrar' }).click();

    // `toContainText` y no `toHaveText`: el icono de Material Symbols lleva su nombre como
    // texto, asi que el contenido del aviso empieza por "error".
    await expect(page.getByRole('alert')).toContainText('Correo o contraseña incorrectos.');
    await expect(page).toHaveURL(/\/login$/u);
  });

  test('cerrar sesion devuelve a Entrar y no se puede volver con la direccion', async ({
    page,
  }) => {
    await mockLogin(page, 200, SESSION);
    await page.route('**/v1/auth/logout', async (route) => {
      await route.fulfill({ status: 204, body: '' });
    });
    await page.goto('/login');
    await page.getByRole('textbox', { name: 'Correo' }).fill('camilo@correo.com');
    await page.getByRole('textbox', { name: 'Contraseña' }).fill('clave de prueba');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByRole('heading', { name: 'Inicio' })).toBeVisible();

    await page.getByRole('link', { name: 'Ajustes' }).first().click();
    await page.getByRole('button', { name: 'Cerrar sesión' }).click();
    await expect(page.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeVisible();

    await page.goto('/loans');
    await expect(page.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeVisible();
  });
});
