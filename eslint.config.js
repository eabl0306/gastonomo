// ESLint de la raiz: cubre la configuracion compartida (packages/) y los
// scripts del monorepo (tools/), que son JavaScript puro y hasta ahora no los
// lintaba nadie. La web tiene su propio eslint.config.js con las reglas
// type-aware, de React y de arquitectura; aqui no hay TypeScript, asi que se
// aplican solo la capa recomendada, la de calidad y la de tests.
import js from '@eslint/js';
import { DEFAULT_IGNORES, qualityConfig, testsConfig } from '@gastonomo/eslint-config';
import globals from 'globals';

/** Lo que linta esta configuracion. apps/ queda fuera: cada app trae la suya. */
const FILES = [
  '*.{js,mjs,cjs}',
  'packages/**/*.{js,mjs,cjs}',
  'tools/**/*.{js,mjs,cjs}',
  '.github/**/*.{js,mjs,cjs}',
];

export default [
  { ignores: [...DEFAULT_IGNORES, 'apps/**'] },
  { files: FILES, ...js.configs.recommended },
  {
    files: FILES,
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: { ...globals.node },
    },
    rules: {
      'no-unused-vars': [
        'error',
        {
          args: 'all',
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          caughtErrorsIgnorePattern: '^_',
          ignoreRestSiblings: true,
        },
      ],
    },
  },
  ...qualityConfig({ files: FILES }),
  ...testsConfig({ files: ['tools/**/*.test.{js,mjs}'] }),
];
