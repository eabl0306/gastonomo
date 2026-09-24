import type { Page } from '@playwright/test';

/**
 * Los e2e no dependen de que la API este levantada: se intercepta la llamada.
 * Con E2E_USE_REAL_API=1 no se intercepta nada y se prueba contra la API real.
 */
export const USE_REAL_API = process.env['E2E_USE_REAL_API'] === '1';

const READINESS_PATTERN = '**/health/ready';

const CORS_HEADERS = {
  'content-type': 'application/json',
  'access-control-allow-origin': '*',
};

export const okPayload = {
  status: 'ok',
  checks: [{ name: 'database', status: 'ok', latencyMs: 4 }],
  version: '0.1.0',
};

export const degradedPayload = {
  status: 'degraded',
  checks: [{ name: 'database', status: 'error', latencyMs: 742 }],
  version: '0.1.0',
};

export async function mockReadiness(page: Page, body: unknown, status: number): Promise<void> {
  await page.route(READINESS_PATTERN, async (route) => {
    await route.fulfill({ status, headers: CORS_HEADERS, body: JSON.stringify(body) });
  });
}

export async function breakReadiness(page: Page): Promise<void> {
  await page.route(READINESS_PATTERN, async (route) => {
    await route.abort('connectionrefused');
  });
}
