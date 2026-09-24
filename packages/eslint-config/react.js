import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';

/**
 * Capa de React: reglas de hooks, del compilador y de accesibilidad, mas el
 * entorno de navegador. El estilo del JSX no se toca: de eso vive Prettier.
 *
 * Sobre accesibilidad: eslint-plugin-jsx-a11y 6.10.2 todavia no declara ESLint 10
 * en sus peers, pero sus reglas funcionan con el. La excepcion esta escrita en
 * pnpm-workspace.yaml (peerDependencyRules) en lugar de dejarse como aviso suelto.
 *
 * @param {{ files?: string[] }} [options]
 * @returns {import('eslint').Linter.Config[]}
 */
export function reactConfig({ files = ['**/*.{ts,tsx}'] } = {}) {
  return [
    {
      files,
      languageOptions: {
        globals: { ...globals.browser },
        parserOptions: { ecmaFeatures: { jsx: true } },
      },
    },
    { ...reactHooks.configs.flat['recommended-latest'], files },
    { ...jsxA11y.flatConfigs.recommended, files },
    {
      files,
      rules: {
        'react-hooks/exhaustive-deps': 'error',
        'react-hooks/rules-of-hooks': 'error',
      },
    },
  ];
}
