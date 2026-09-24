import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/** Directorios donde nunca vive codigo que revisar. */
export const IGNORED_DIRS = new Set([
  '.git',
  '.gradle',
  '.idea',
  '.pnpm-store',
  '.vite',
  '__snapshots__',
  'build',
  'coverage',
  'dist',
  'generated',
  'node_modules',
  'playwright-report',
  'target',
  'test-results',
]);

/**
 * Ambitos del monorepo que se revisan a fondo. Los ficheros sueltos de la raiz
 * se barren aparte, sin recursion, para no entrar en directorios de datos.
 */
export const SCAN_ROOTS = ['.github', 'apps/api', 'apps/web', 'apps/android', 'packages', 'tools'];

export const SOURCE_EXTENSIONS = new Set([
  '.cjs',
  '.java',
  '.js',
  '.jsx',
  '.kt',
  '.kts',
  '.mjs',
  '.rs',
  '.ts',
  '.tsx',
]);

const EXCLUDED_PATTERNS = [
  /\.min\.(js|css)$/u,
  /\.generated\./u,
  /\.gen\.(ts|js|kt|rs)$/u,
  /(^|\/)gradle(w)?(\.bat)?$/u,
  /(^|\/)migrations\//u,
  /(^|\/)res\//u,
];

const TEST_PATTERNS = [
  /\.(test|spec)\.[cm]?[jt]sx?$/u,
  /(^|\/)(e2e|tests?)\//u,
  /(^|\/)src\/(test|androidTest)\//u,
  /Test\.(kt|java)$/u,
];

export function repoRoot() {
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
}

export function toPosix(relativePath) {
  return relativePath.split(path.sep).join('/');
}

export function isSourceFile(relativePath) {
  return SOURCE_EXTENSIONS.has(path.extname(relativePath));
}

export function isExcluded(relativePath) {
  return EXCLUDED_PATTERNS.some((pattern) => pattern.test(relativePath));
}

export function isTestFile(relativePath) {
  return TEST_PATTERNS.some((pattern) => pattern.test(relativePath));
}

function walk(absoluteDir, root, found) {
  for (const entry of readdirSync(absoluteDir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (IGNORED_DIRS.has(entry.name)) continue;
      walk(path.join(absoluteDir, entry.name), root, found);
      continue;
    }
    if (!entry.isFile()) continue;
    found.push(toPosix(path.relative(root, path.join(absoluteDir, entry.name))));
  }
  return found;
}

function stagedFiles(root) {
  const output = execFileSync('git', ['diff', '--cached', '--name-only', '--diff-filter=ACMR'], {
    cwd: root,
    encoding: 'utf8',
  });
  return output
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line !== '')
    .filter((relativePath) => existsSync(path.join(root, relativePath)))
    .filter((relativePath) => !relativePath.split('/').some((part) => IGNORED_DIRS.has(part)));
}

function walkShallow(absoluteDir, root, found) {
  for (const entry of readdirSync(absoluteDir, { withFileTypes: true })) {
    if (!entry.isFile()) continue;
    found.push(toPosix(path.relative(root, path.join(absoluteDir, entry.name))));
  }
  return found;
}

function scanRoots(root) {
  const present = SCAN_ROOTS.filter((candidate) => existsSync(path.join(root, candidate)));
  return present.length > 0 ? present : ['.'];
}

/** Lista de ficheros a revisar, en rutas relativas a la raiz y con barras normales. */
export function collectFiles({ root, staged }) {
  if (staged) return stagedFiles(root);
  const found = [];
  const scopes = scanRoots(root);
  for (const scope of scopes) {
    const absolute = path.join(root, scope);
    if (statSync(absolute).isDirectory()) walk(absolute, root, found);
  }
  if (!scopes.includes('.')) walkShallow(root, root, found);
  return found.sort();
}
