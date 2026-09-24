#!/usr/bin/env bash
# Comprobaciones que clippy no cubre.
#
#   apps/api/scripts/guards.sh
#
# Falla si encuentra un TODO/FIXME sin ticket, un test desactivado sin motivo
# o un fichero de codigo mas largo de la cuenta.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAX_SOURCE_LINES=300
MAX_TEST_LINES=400
STATUS=0

sources() {
  find "$ROOT/crates" -name '*.rs' -not -path '*/target/*' | sort
}

fail() {
  printf 'guard: %s\n' "$1" >&2
  STATUS=1
}

# 1. TODO y FIXME solo con ticket: TODO(GAS-123): ...
while IFS= read -r hit; do
  [ -z "$hit" ] && continue
  fail "TODO/FIXME sin ticket -> $hit"
done < <(sources | xargs grep -nE '(TODO|FIXME)' 2>/dev/null \
  | grep -vE '(TODO|FIXME)\([A-Z]+-[0-9]+\)' || true)

# 2. Tests desactivados sin motivo escrito: #[ignore = "..."]
# El motivo tiene que decir algo: un #[ignore = ""] tampoco vale.
while IFS= read -r hit; do
  [ -z "$hit" ] && continue
  fail "#[ignore] sin motivo -> $hit"
done < <(sources | xargs grep -nE '#\[ignore([[:space:]]*=[[:space:]]*"[[:space:]]*")?\]' 2>/dev/null || true)

# 3. Tamano de fichero
while IFS= read -r file; do
  lines=$(wc -l < "$file")
  limit=$MAX_SOURCE_LINES
  case "$file" in
    */tests/*) limit=$MAX_TEST_LINES ;;
  esac
  if [ "$lines" -gt "$limit" ]; then
    fail "fichero de $lines lineas (limite $limit) -> ${file#"$ROOT"/}"
  fi
done < <(sources)

if [ "$STATUS" -eq 0 ]; then
  printf 'guards: ok\n'
fi
exit "$STATUS"
