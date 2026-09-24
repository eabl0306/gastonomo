import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

const DEV_PORT = 5173;
const PREVIEW_PORT = 4173;
const CORE_THRESHOLD = { lines: 80, statements: 80, branches: 80, functions: 80 };

export default defineConfig({
  plugins: [react()],
  server: { host: true, port: DEV_PORT, strictPort: true },
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
