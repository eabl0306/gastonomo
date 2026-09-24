#!/usr/bin/env node
import { report, run } from './lib/cli.mjs';
import { collectFiles, isExcluded, isSourceFile } from './lib/scan.mjs';
import { readLines } from './lib/text.mjs';

/**
 * Las palabras se arman por concatenacion para que este fichero no se delate
 * a si mismo. La comparacion distingue mayusculas: en castellano "todo" es una
 * palabra corriente y no puede disparar la comprobacion.
 */
const WORDS = ['TO' + 'DO', 'FIX' + 'ME', 'HA' + 'CK'];
const MARKER = new RegExp(`\\b(?:${WORDS.join('|')})\\b`, 'u');
const TICKETED = new RegExp(`\\b(?:${WORDS.join('|')})\\([A-Z][A-Z0-9]*-\\d+\\)`, 'u');
const EXAMPLE = `${WORDS[0]}(ABC-123): ...`;

export function checkTodos({ root, staged }) {
  const files = collectFiles({ root, staged }).filter(
    (file) => isSourceFile(file) && !isExcluded(file),
  );
  const violations = [];
  for (const file of files) {
    readLines(root, file).forEach((line, index) => {
      if (!MARKER.test(line) || TICKETED.test(line)) return;
      violations.push({
        file,
        line: index + 1,
        message: `marcador sin ticket: usa ${EXAMPLE} o borra la nota`,
      });
    });
  }
  return { scanned: files.length, violations };
}

if (import.meta.main) {
  run((options) => report({ name: 'marcadores sin ticket', ...checkTodos(options) }));
}
