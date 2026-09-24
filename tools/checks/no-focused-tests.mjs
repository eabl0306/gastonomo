#!/usr/bin/env node
import { report, run } from './lib/cli.mjs';
import { collectFiles, isExcluded, isSourceFile, isTestFile } from './lib/scan.mjs';
import { hasNearbyComment, readLines } from './lib/text.mjs';

const FOCUSED_JS = /(?:\bdescribe|\bit|\btest|\bsuite|\bbench)(?:\.\w+)*\.only\s*\(/u;
const FOCUSED_SHORTHAND = /\bf(?:describe|it)\s*\(/u;
const SKIPPED_JS = /(?:\bdescribe|\bit|\btest|\bsuite)(?:\.\w+)*\.(?:skip|todo)\s*\(/u;
const SKIPPED_JS_WITH_REASON = /\.(?:skip|todo)\s*\([^)]*,\s*['"`]/u;
const RUST_IGNORE = /#\[\s*ignore\s*\]/u;
// El prefijo cualificado opcional caza tambien @kotlin.test.Ignore y @org.junit.Ignore,
// igual que hace SourceGuardTask.kt en el lado de Gradle.
const KOTLIN_IGNORE = /@(?:[\w.]*\.)?(?:Ignore|Disabled)\b(?!\s*\()/u;

const JS_EXTENSIONS = /\.[cm]?[jt]sx?$/u;

function jsViolations(lines, file) {
  const found = [];
  lines.forEach((line, index) => {
    if (FOCUSED_JS.test(line) || FOCUSED_SHORTHAND.test(line)) {
      found.push({
        file,
        line: index + 1,
        message: 'test enfocado: quita el .only antes de commitear',
      });
      return;
    }
    if (!SKIPPED_JS.test(line)) return;
    if (SKIPPED_JS_WITH_REASON.test(line)) return;
    if (hasNearbyComment(lines, index, '.skip')) return;
    found.push({
      file,
      line: index + 1,
      message: 'test saltado sin motivo: pasa una razon o comentala',
    });
  });
  return found;
}

/** Una anotacion nombrada dentro de un comentario no apaga ningun test. */
function isInsideComment(line, at) {
  const before = line.slice(0, at);
  return before.includes('//') || /^\s*(\/\*|\*)/u.test(before);
}

// El marcador para buscar el comentario justificativo es el texto que de verdad
// casa, no un literal fijo: asi tambien vale para una anotacion cualificada.
function annotationViolations(lines, file, pattern, message) {
  const found = [];
  lines.forEach((line, index) => {
    const match = pattern.exec(line);
    if (match === null) return;
    if (isInsideComment(line, match.index)) return;
    if (hasNearbyComment(lines, index, match[0])) return;
    found.push({ file, line: index + 1, message });
  });
  return found;
}

function violationsFor(root, file) {
  const lines = readLines(root, file);
  if (JS_EXTENSIONS.test(file)) return jsViolations(lines, file);
  if (file.endsWith('.rs')) {
    return annotationViolations(
      lines,
      file,
      RUST_IGNORE,
      'test ignorado sin motivo: usa #[ignore = "..."] o comenta por que',
    );
  }
  if (file.endsWith('.kt') || file.endsWith('.java')) {
    return annotationViolations(
      lines,
      file,
      KOTLIN_IGNORE,
      'test ignorado sin motivo: usa @Ignore("...") o comenta por que',
    );
  }
  return [];
}

export function checkFocusedTests({ root, staged }) {
  const files = collectFiles({ root, staged })
    .filter((file) => isSourceFile(file) && !isExcluded(file))
    .filter((file) => isTestFile(file) || file.endsWith('.rs') || file.endsWith('.kt'));
  const violations = files.flatMap((file) => violationsFor(root, file));
  return { scanned: files.length, violations };
}

if (import.meta.main) {
  run((options) => report({ name: 'tests enfocados', ...checkFocusedTests(options) }));
}
