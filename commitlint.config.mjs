/**
 * Convencion de commits del monorepo. Los scopes son los ambitos reales del repo,
 * asi el historial dice de un vistazo que parte cambio.
 */
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'scope-enum': [2, 'always', ['api', 'web', 'android', 'tools', 'ci', 'docs', 'deps', 'repo']],
    'header-max-length': [2, 'always', 100],
    'body-max-line-length': [2, 'always', 100],
  },
};
