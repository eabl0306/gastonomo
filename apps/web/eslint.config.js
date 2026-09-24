import { fileURLToPath } from 'node:url';

import {
  DEFAULT_IGNORES,
  baseConfig,
  boundariesConfig,
  qualityConfig,
  reactConfig,
  testsConfig,
} from '@gastonomo/eslint-config';
import globals from 'globals';

const tsconfigRootDir = fileURLToPath(new URL('.', import.meta.url));

/** Capas de la arquitectura hexagonal, una carpeta por capa. */
const elements = [
  { type: 'domain', pattern: 'src/domain', partialMatch: false },
  { type: 'application', pattern: 'src/application', partialMatch: false },
  { type: 'infrastructure', pattern: 'src/infrastructure', partialMatch: false },
  { type: 'ui', pattern: 'src/ui', partialMatch: false },
  { type: 'app', pattern: 'src/app', partialMatch: false },
  { type: 'test-support', pattern: 'src/test', partialMatch: false },
];

const LOCAL_TYPES = ['domain', 'application', 'infrastructure', 'ui', 'app', 'test-support'];

/**
 * Los tests son una categoria de fichero, no una capa: pueden montar cualquier
 * adaptador porque ese es justo su trabajo. El codigo de produccion no.
 */
const fileCategories = [
  { category: 'test', pattern: ['src/**/*.test.{ts,tsx}', 'src/test/**/*'], exclusive: true },
  { category: 'source', pattern: 'src/**/*' },
];

function layer(type, allowedTypes) {
  return {
    from: { element: { type }, file: { categories: 'source' } },
    allow: { to: { element: { types: allowedTypes } } },
  };
}

/** Direccion de dependencias. Lo que no esta permitido, esta prohibido. */
const policies = [
  layer('domain', ['domain']),
  layer('application', ['domain', 'application']),
  layer('infrastructure', ['domain', 'application', 'infrastructure']),
  layer('ui', ['domain', 'application', 'ui']),
  layer('app', ['domain', 'application', 'infrastructure', 'ui', 'app']),
  {
    from: { file: { categories: 'test' } },
    allow: { to: { element: { types: LOCAL_TYPES } } },
  },
  { allow: { to: { module: { origin: ['external', 'core'] } } } },
  {
    from: { element: { type: 'domain' }, file: { categories: 'source' } },
    disallow: { to: { module: { origin: ['external', 'core'] } } },
    message: 'El dominio no puede depender de paquetes externos.',
  },
  {
    from: { element: { type: 'application' }, file: { categories: 'source' } },
    disallow: { to: { module: { origin: 'external', source: ['react', 'react-dom', 'zod'] } } },
    message: 'Los casos de uso no conocen ni React ni la validacion de DTOs.',
  },
  {
    from: { element: { type: 'ui' }, file: { categories: 'source' } },
    disallow: { to: { module: { origin: 'external', source: 'zod' } } },
    message: 'La interfaz no valida DTOs: eso vive en infrastructure.',
  },
];

export default [
  { ignores: [...DEFAULT_IGNORES] },
  ...baseConfig({ tsconfigRootDir }),
  ...qualityConfig(),
  ...reactConfig(),
  ...boundariesConfig({
    files: ['src/**/*.{ts,tsx}'],
    elements,
    fileCategories,
    policies,
    ignore: ['src/**/*.d.ts'],
  }),
  ...testsConfig(),
  {
    files: ['*.js'],
    languageOptions: { globals: { ...globals.node } },
  },
  {
    files: ['vite.config.ts', 'playwright.config.ts'],
    languageOptions: { globals: { ...globals.node } },
  },
];
