import { defineConfig, devices } from '@playwright/test';

const PREVIEW_PORT = 4173;
const isCi = process.env['CI'] === 'true' || process.env['CI'] === '1';
const baseURL = process.env['E2E_BASE_URL'] ?? `http://127.0.0.1:${String(PREVIEW_PORT)}`;

export default defineConfig({
  testDir: './e2e',
  ...(isCi ? { workers: 1 } : {}),
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 2 : 0,
  reporter: isCi ? [['list'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm run build && pnpm run preview',
    url: baseURL,
    reuseExistingServer: !isCi,
    timeout: 180_000,
  },
});
