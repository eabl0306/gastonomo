import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { after, describe, it } from 'node:test';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const sandboxes = [];

// Los ejemplos se arman por concatenacion para que este fichero no viole las
// mismas reglas que comprueba.
const FOCUS = '.on' + 'ly(';
const MARKER = 'TO' + 'DO';

function sandbox(files) {
  const root = mkdtempSync(path.join(tmpdir(), 'gastonomo-checks-'));
  sandboxes.push(root);
  for (const [relativePath, content] of Object.entries(files)) {
    const absolute = path.join(root, relativePath);
    mkdirSync(path.dirname(absolute), { recursive: true });
    writeFileSync(absolute, content, 'utf8');
  }
  return root;
}

function runCheck(script, root) {
  const result = spawnSync(process.execPath, [path.join(here, script), '--root', root], {
    encoding: 'utf8',
  });
  return { code: result.status, output: `${result.stdout}${result.stderr}` };
}

after(() => {
  for (const root of sandboxes) rmSync(root, { recursive: true, force: true });
});

describe('file-size', () => {
  it('acepta un fichero dentro del limite', () => {
    const root = sandbox({ 'src/small.ts': 'const a = 1;\n'.repeat(10) });
    assert.equal(runCheck('file-size.mjs', root).code, 0);
  });

  it('rechaza un fichero de codigo de mas de 300 lineas', () => {
    const root = sandbox({ 'src/big.ts': 'const a = 1;\n'.repeat(301) });
    const { code, output } = runCheck('file-size.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /src\/big\.ts/u);
  });

  it('revisa tambien los ficheros sueltos de la raiz y los de .github', () => {
    const root = sandbox({
      'tools/checks/ok.mjs': 'const a = 1;\n',
      'big.mjs': 'const a = 1;\n'.repeat(301),
      '.github/scripts/big.mjs': 'const a = 1;\n'.repeat(301),
    });
    const { code, output } = runCheck('file-size.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /^ {2}big\.mjs/mu);
    assert.match(output, /\.github\/scripts\/big\.mjs/u);
  });

  it('deja a los tests llegar a 400 lineas', () => {
    const root = sandbox({
      'src/a.test.ts': 'const a = 1;\n'.repeat(399),
      'src/b.test.ts': 'const a = 1;\n'.repeat(401),
    });
    const { code, output } = runCheck('file-size.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /b\.test\.ts/u);
    assert.doesNotMatch(output, /a\.test\.ts/u);
  });

  it('ignora lo generado y lo que no es codigo fuente', () => {
    const root = sandbox({
      'src/schema.generated.ts': 'const a = 1;\n'.repeat(500),
      'docs/manual.md': 'texto\n'.repeat(500),
    });
    assert.equal(runCheck('file-size.mjs', root).code, 0);
  });
});

describe('no-focused-tests', () => {
  it('rechaza un test enfocado en JavaScript', () => {
    const root = sandbox({ 'src/a.test.ts': `describe${FOCUS}'x', () => {});\n` });
    const { code, output } = runCheck('no-focused-tests.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /a\.test\.ts:1/u);
  });

  it('rechaza un #[ignore] de Rust sin motivo y acepta el que lo lleva', () => {
    const root = sandbox({
      'crates/a/src/lib.rs': '#[ignore]\nfn a() {}\n',
      'crates/b/src/lib.rs': '#[ignore = "necesita Postgres"]\nfn b() {}\n',
    });
    const { code, output } = runCheck('no-focused-tests.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /crates\/a\/src\/lib\.rs/u);
    assert.doesNotMatch(output, /crates\/b\/src\/lib\.rs/u);
  });

  it('rechaza un @Ignore de Kotlin sin motivo', () => {
    const root = sandbox({ 'app/src/test/A.kt': '@Ignore\nfun a() {}\n' });
    assert.equal(runCheck('no-focused-tests.mjs', root).code, 1);
  });

  it('rechaza un @Ignore cualificado y un @Disabled sin motivo', () => {
    const root = sandbox({
      'app/src/test/A.kt': '@kotlin.test.Ignore\nfun a() {}\n',
      'app/src/test/B.kt': '@org.junit.jupiter.api.Disabled\nfun b() {}\n',
    });
    const { code, output } = runCheck('no-focused-tests.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /A\.kt:1/u);
    assert.match(output, /B\.kt:1/u);
  });

  it('acepta un @Ignore cualificado con motivo o con comentario', () => {
    const root = sandbox({
      'app/src/test/A.kt': '@kotlin.test.Ignore("necesita emulador")\nfun a() {}\n',
      'app/src/test/B.kt': '// necesita emulador\n@kotlin.test.Ignore\nfun b() {}\n',
      'app/src/test/C.kt': '@kotlin.test.Ignore // necesita emulador\nfun c() {}\n',
    });
    assert.equal(runCheck('no-focused-tests.mjs', root).code, 0);
  });

  it('acepta un salto justificado con un comentario', () => {
    const root = sandbox({
      'src/a.test.ts': "// se salta hasta que exista el endpoint\nit.skip('x', () => {});\n",
    });
    assert.equal(runCheck('no-focused-tests.mjs', root).code, 0);
  });
});

describe('todo-without-ticket', () => {
  it('rechaza un marcador sin ticket', () => {
    const root = sandbox({ 'src/a.ts': `// ${MARKER}: arreglar esto\n` });
    const { code, output } = runCheck('todo-without-ticket.mjs', root);
    assert.equal(code, 1);
    assert.match(output, /src\/a\.ts:1/u);
  });

  it('acepta un marcador con ticket', () => {
    const root = sandbox({ 'src/a.ts': `// ${MARKER}(GAS-12): arreglar esto\n` });
    assert.equal(runCheck('todo-without-ticket.mjs', root).code, 0);
  });

  it('no confunde la palabra castellana todo con un marcador', () => {
    const root = sandbox({ 'src/a.ts': '// esto lo hace todo de golpe\n' });
    assert.equal(runCheck('todo-without-ticket.mjs', root).code, 0);
  });
});
