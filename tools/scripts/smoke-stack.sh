#!/usr/bin/env bash
# Prueba de humo de la pila compuesta: comprueba que por el gateway se llega a
# lo que tiene que llegar. Existe por un fallo real: durante semanas el gateway
# solo enrutaba /health, /openapi.json y la web, asi que las 24 rutas de /v1
# acababan en el nginx de la SPA. Los tests de cada imagen por separado pasaban
# igual, porque ninguno miraba la composicion.
#
#   bash tools/scripts/smoke-stack.sh [base_url]
#
# La senal que mas importa: una respuesta text/html en una ruta /v1 significa
# que el gateway mando la peticion a la web en vez de a la API.
set -euo pipefail

base="${1:-http://127.0.0.1:${GATEWAY_PORT:-8090}}"
failures=0
probe_email="smoke-$(date +%s)-$$@ejemplo.local"

say() { printf '  %-52s %s\n' "$1" "$2"; }

fail() {
  say "$1" "FALLO: $2"
  failures=$((failures + 1))
}

# Devuelve "codigo|content-type" y deja el cuerpo en el fichero indicado.
request() {
  local out="$1" method="$2" path="$3"
  shift 3
  curl -sS -o "${out}" -w '%{http_code}|%{content_type}' \
    -X "${method}" "${base}${path}" --max-time 20 "$@" || echo '000|error'
}

expect() {
  local label="$1" want_code="$2" want_type="$3" got="$4"
  local code="${got%%|*}" ctype="${got#*|}"
  if [ "${code}" != "${want_code}" ]; then
    fail "${label}" "esperaba ${want_code}, llego ${code} (${ctype})"
    return 1
  fi
  case "${ctype}" in
    *"${want_type}"*) say "${label}" "ok (${code} ${want_type})" ;;
    *) fail "${label}" "esperaba ${want_type}, llego ${ctype}" ; return 1 ;;
  esac
}

body=$(mktemp) ; trap 'rm -f "${body}"' EXIT

echo "Prueba de humo de la pila en ${base}"

expect "GET /health/live" 200 json \
  "$(request "${body}" GET /health/live)" || true

expect "GET /health/ready" 200 json \
  "$(request "${body}" GET /health/ready)" || true

# La sonda clave. Sin token tiene que contestar la API con problem+json.
# Si contesta text/html, el gateway no enruta /v1 hacia la API.
expect "GET /v1/currencies sin token" 401 problem+json \
  "$(request "${body}" GET /v1/currencies)" || true

expect "GET /v1/exchange-rates sin token" 401 problem+json \
  "$(request "${body}" GET /v1/exchange-rates)" || true

# POST a /v1: la SPA responde 405 a los POST, asi que este caso distingue bien.
registro=$(request "${body}" POST /v1/auth/register \
  -H 'content-type: application/json' \
  -d "{\"name\":\"Smoke\",\"email\":\"${probe_email}\",\"password\":\"contrasena123\",\"acceptedTerms\":true}")
if expect "POST /v1/auth/register" 201 json "${registro}"; then
  token=$(node -e 'let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{process.stdout.write(JSON.parse(d).accessToken||"")}catch(e){}})' <"${body}")
  if [ -z "${token}" ]; then
    fail "el registro devuelve accessToken" "no venia en el cuerpo"
  else
    expect "GET /v1/me con token" 200 json \
      "$(request "${body}" GET /v1/me -H "authorization: Bearer ${token}")" || true
    expect "GET /v1/categories con token" 200 json \
      "$(request "${body}" GET /v1/categories -H "authorization: Bearer ${token}")" || true
    # Las 12 predeterminadas se siembran al registrarse.
    total=$(node -e 'let d="";process.stdin.on("data",c=>d+=c).on("end",()=>{try{const j=JSON.parse(d);const a=Array.isArray(j)?j:j.items;process.stdout.write(String((a||[]).length))}catch(e){process.stdout.write("0")}})' <"${body}")
    if [ "${total}" -ge 12 ]; then
      say "el registro siembra las categorias" "ok (${total})"
    else
      fail "el registro siembra las categorias" "esperaba 12 o mas, llegaron ${total}"
    fi
  fi
fi

expect "GET / (la web)" 200 text/html \
  "$(request "${body}" GET /)" || true

expect "GET /una/ruta/de/la/spa (fallback)" 200 text/html \
  "$(request "${body}" GET /una/ruta/de/la/spa)" || true

echo
if [ "${failures}" -ne 0 ]; then
  echo "prueba de humo: ${failures} comprobacion(es) fallaron." >&2
  exit 1
fi
echo "prueba de humo: la pila responde lo que debe."
