#!/usr/bin/env bash
# Ultima red antes del commit: ni secretos ni ficheros enormes en el historial.
# Recibe los ficheros del index como argumentos (lefthook pasa {staged_files}).
#
#   bash tools/scripts/scan-staged.sh src/a.ts .env
#
# Limite de tamano configurable con MAX_STAGED_BYTES (por defecto 512 KiB).
set -uo pipefail

max_bytes="${MAX_STAGED_BYTES:-524288}"
status=0

# Los patrones se arman por trozos para que este fichero no se delate a si mismo.
aws="AKIA"
gh="gh"
key="PRIVATE KEY"
patterns=(
  "${aws}[0-9A-Z]{16}"
  "${gh}[pousr]_[A-Za-z0-9]{30,}"
  "${gh}ithub_pat_[A-Za-z0-9_]{30,}"
  "-----BEGIN [A-Z ]*${key}-----"
  "xox[baprs]-[A-Za-z0-9-]{12,}"
  "(secret|password|passwd|token|api_key|apikey)[[:space:]]*[:=][[:space:]]*['\"][^'\"]{12,}['\"]"
)

for file in "$@"; do
  [ -f "${file}" ] || continue

  case "${file##*/}" in
    .env | .env.*)
      if [ "${file##*/}" != ".env.example" ]; then
        echo "  ${file}  los ficheros .env no se commitean (usa .env.example)" >&2
        status=1
      fi
      ;;
  esac

  size="$(wc -c <"${file}" | tr -d ' ')"
  if [ "${size}" -gt "${max_bytes}" ]; then
    echo "  ${file}  pesa ${size} bytes, el limite son ${max_bytes}" >&2
    status=1
  fi

  for pattern in "${patterns[@]}"; do
    # El -e es obligatorio: hay patrones que empiezan por guion y sin el grep
    # los toma por opciones, falla y deja pasar el secreto en silencio.
    if match="$(grep -IEn -m 1 -e "${pattern}" "${file}" 2>/dev/null)"; then
      echo "  ${file}:${match%%:*}  parece un secreto (patron: ${pattern})" >&2
      status=1
    fi
  done
done

if [ "${status}" -ne 0 ]; then
  echo "" >&2
  echo "scan-staged: saca eso del commit antes de seguir." >&2
fi
exit "${status}"
