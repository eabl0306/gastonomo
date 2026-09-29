import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

const DEV_PORT = 5173;
const PREVIEW_PORT = 4173;
const CORE_THRESHOLD = { lines: 80, statements: 80, branches: 80, functions: 80 };

/**
 * A donde manda el servidor de desarrollo lo que va a la API.
 *
 * Hace falta un proxy porque la web y la API son origenes distintos y **la API
 * no manda cabeceras CORS a proposito**: en produccion las sirve Caddy bajo el
 * mismo dominio, y en la pila de Docker un nginx. Sin esto, en `pnpm dev` el
 * navegador bloquea toda llamada a la API y la web solo se puede probar con
 * dobles. Con el proxy, el cliente pide a su propio origen y aqui se reenvia,
 * que es lo mismo que pasa desplegado.
 */
const API_TARGET = process.env['VITE_DEV_API_TARGET'] ?? 'http://127.0.0.1:8080';
const API_PREFIXES = ['/v1', '/health', '/openapi.json'];

const devProxy = Object.fromEntries(
  API_PREFIXES.map((prefix) => [prefix, { target: API_TARGET, changeOrigin: true }]),
);

export default defineConfig({
  plugins: [react()],
  server: { host: true, port: DEV_PORT, strictPort: true, proxy: devProxy },
  preview: { host: true, port: PREVIEW_PORT, strictPort: true },
  build: { outDir: 'dist', sourcemap: true },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    clearMocks: true,
    restoreMocks: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/app/main.tsx',
        'src/test/**',
        'src/**/*.test.{ts,tsx}',
        'src/**/*.d.ts',
        'src/infrastructure/memory/**',
      ],
      thresholds: {
        lines: 60,
        statements: 60,
        branches: 60,
        functions: 60,
        'src/domain/**': CORE_THRESHOLD,
        'src/application/**': CORE_THRESHOLD,
      },
    },
  },
});
