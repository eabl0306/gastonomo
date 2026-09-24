# Atajos del monorepo. `just` los lista con `just` a secas.
#
# Las recetas son finas: delegan en el toolchain de cada app (cargo, pnpm,
# gradle) en lugar de reimplementar nada. Si una receta y la CI no hacen lo
# mismo, manda la CI: mirala en .github/workflows.

set shell := ["bash", "-euo", "pipefail", "-c"]
set dotenv-load := true

gradle := "bash tools/scripts/gradle.sh"
compose := "docker compose"
compose_prod := "docker compose -f compose.prod.yaml"
pg_user := env_var_or_default("POSTGRES_USER", "gastonomo")
pg_db := env_var_or_default("POSTGRES_DB", "gastonomo")

# Lista todas las recetas.
default:
    @just --list --unsorted

# ---------------------------------------------------------------- monorepo ---

# Instala dependencias, engancha los hooks de git y comprueba los toolchains.
[group('monorepo')]
setup:
    pnpm install --frozen-lockfile
    pnpm exec lefthook install
    @just doctor

# Comprueba que estan las herramientas que hacen falta.
[group('monorepo')]
doctor:
    @echo "node    $(node --version)"
    @echo "pnpm    $(pnpm --version)"
    @echo "cargo   $(cargo --version)"
    @echo "just    $(just --version)"
    @echo "docker  $(docker --version)"
    @bash -c 'source tools/scripts/jdk-env.sh && echo "java    $(java -version 2>&1 | head -1)"'

# Todo el enforcement del monorepo, igual que la CI pero en local.
[group('monorepo')]
check: guard root-lint fmt-check tools-test api-check web-check android-check

# Lo mismo que check mas lo que la CI corre aparte: e2e, auditoria y builds.
[group('monorepo')]
ci: check api-deny e2e build

# Formatea los tres lenguajes.
[group('monorepo')]
fmt:
    pnpm run format
    cd apps/api && cargo fmt --all
    @echo "Kotlin: el formato lo aplica detekt-formatting, usa 'just android-detekt' para verlo"

# Falla si algo no esta formateado.
[group('monorepo')]
fmt-check:
    pnpm run format:check
    cd apps/api && cargo fmt --all --check

# Lint de los tres.
[group('monorepo')]
lint: root-lint api-lint web-lint android-lint

# Tests unitarios de los tres.
[group('monorepo')]
test: api-test web-test android-test

# Cobertura de los tres, con sus umbrales.
[group('monorepo')]
cov: api-cov web-cov android-cov

# E2E: Playwright en la web y los tests de integracion de la API contra Postgres.
[group('monorepo')]
e2e: web-e2e api-test-db

# E2E contra la pila entera: navegador, web, API y PostgreSQL, sin mockear nada.
#
# Los e2e de `web-e2e` interceptan la red a proposito, para ser rapidos y
# deterministas. Este entra por el gateway, que sirve la web y la API bajo el
# mismo origen, y es el unico que prueba el contrato de verdad entre las dos.
# Al terminar la pila se queda levantada: `just down` para tumbarla.
#
# El texto de `just --list` va en [doc]: just toma como descripcion la ultima
# linea del comentario, y aqui seria un fragmento sin sentido.
[group('monorepo')]
[doc('E2E contra la pila de Docker entera, sin mockear la red.')]
e2e-stack:
    {{compose}} up -d --build --wait
    E2E_BASE_URL="http://localhost:${GATEWAY_PORT:-8090}" E2E_USE_REAL_API=1 \
      pnpm --filter @gastonomo/web e2e

# Build de produccion de los tres.
[group('monorepo')]
build: api-build web-build android-build

# Reglas comunes a los tres lenguajes: tamano de fichero, tests apagados, TODO sin ticket.
[group('monorepo')]
guard:
    pnpm run check:quality

# ESLint sobre la configuracion compartida (packages/) y los scripts (tools/).
[group('monorepo')]
root-lint:
    pnpm run lint

# Tests de los propios scripts de tools/checks.
[group('monorepo')]
tools-test:
    pnpm run check:tools-test

# Borra artefactos de build de los tres apps.
[group('monorepo')]
clean:
    cd apps/api && cargo clean
    rm -rf apps/web/dist apps/web/coverage apps/web/playwright-report apps/web/test-results
    {{gradle}} clean

# --------------------------------------------------------------------- api ---

# Todo el enforcement de la API (fmt, clippy, guards, tests y cobertura).
[group('api')]
api-check:
    cd apps/api && just check

# Formatea la API.
[group('api')]
api-fmt:
    cd apps/api && just fmt

# Clippy en modo estricto.
[group('api')]
api-lint:
    cd apps/api && just lint

# Tests que no necesitan base de datos.
[group('api')]
api-test:
    cd apps/api && just test

# Tests de integracion contra el Postgres de docker compose.
[group('api')]
api-test-db: db-up
    cd apps/api && GASTONOMO_TEST_DATABASE_URL="postgres://{{pg_user}}:${POSTGRES_PASSWORD:-gastonomo}@127.0.0.1:${POSTGRES_PORT:-5432}/{{pg_db}}" just test-db

# Cobertura de la API: 80% en el nucleo y 60% global.
[group('api')]
api-cov:
    cd apps/api && just coverage-core coverage

# Auditoria de dependencias (avisos de seguridad, licencias y fuentes).
[group('api')]
api-deny:
    cd apps/api && just deny

# Compila la API en modo release.
[group('api')]
api-build:
    cd apps/api && cargo build --release

# Arranca la API en local contra el Postgres de docker compose.
[group('api')]
api-run: db-up
    cd apps/api && GASTONOMO__DATABASE__URL="postgres://{{pg_user}}:${POSTGRES_PASSWORD:-gastonomo}@127.0.0.1:${POSTGRES_PORT:-5432}/{{pg_db}}" cargo run --bin gastonomo-api

# Construye la imagen de produccion de la API.
[group('api')]
api-docker:
    docker build -t gastonomo-api:local apps/api

# --------------------------------------------------------------------- web ---

# Todo el enforcement de la web (lint, tipos, cobertura, knip y build).
[group('web')]
web-check: web-lint web-typecheck web-cov web-knip web-build

# Servidor de desarrollo con HMR en el 5173.
[group('web')]
web-dev:
    pnpm --filter @gastonomo/web dev

# ESLint (capas hexagonales, complejidad, hooks, accesibilidad).
[group('web')]
web-lint:
    pnpm --filter @gastonomo/web lint

# TypeScript en modo estricto.
[group('web')]
web-typecheck:
    pnpm --filter @gastonomo/web typecheck

# Tests unitarios (vitest).
[group('web')]
web-test:
    pnpm --filter @gastonomo/web test --run

# Cobertura de la web: 80% en dominio y aplicacion, 60% global.
[group('web')]
web-cov:
    pnpm --filter @gastonomo/web test:coverage

# Codigo muerto: exports, ficheros y dependencias que nadie usa.
[group('web')]
web-knip:
    pnpm --filter @gastonomo/web knip

# Build de produccion.
[group('web')]
web-build:
    pnpm --filter @gastonomo/web build

# E2E de Playwright (levanta el preview el solo).
[group('web')]
web-e2e:
    pnpm --filter @gastonomo/web e2e

# Descarga el navegador que usa Playwright.
[group('web')]
web-e2e-install:
    pnpm --filter @gastonomo/web e2e:install

# Construye la imagen de produccion de la web (contexto en la raiz).
[group('web')]
web-docker:
    docker build -f apps/web/Dockerfile -t gastonomo-web:local .

# ----------------------------------------------------------------- android ---

# Todo el enforcement de Android (detekt, sourceGuard, lint, tests y kover).
#
# assembleDebugAndroidTest va aparte porque `check` no compila las fuentes de
# androidTest: sin el, un test instrumentado que no compila pasa desapercibido.
[group('android')]
android-check:
    {{gradle}} check assembleDebugAndroidTest

# detekt, que incluye las reglas de formato de ktlint.
[group('android')]
android-detekt:
    {{gradle}} detekt

# Android Lint, con los avisos tratados como errores.
[group('android')]
android-lint:
    {{gradle}} lint

# Tests unitarios de los seis modulos.
[group('android')]
android-test:
    {{gradle}} test testDebugUnitTest

# Cobertura con Kover y sus umbrales por modulo.
[group('android')]
android-cov:
    {{gradle}} koverVerify koverXmlReport

# APK de debug.
[group('android')]
android-build:
    {{gradle}} assembleDebug

# Tests instrumentados. Necesita un emulador o un movil conectado.
[group('android')]
android-connected:
    {{gradle}} connectedDebugAndroidTest

# Pasa cualquier tarea al Gradle de la app: just android-gradle "tasks --all"
[group('android')]
android-gradle +args:
    {{gradle}} {{args}}

# ------------------------------------------------------------------ docker ---

# Levanta la pila de desarrollo (db, api con recarga en caliente y web con HMR).
[group('docker')]
up:
    {{compose}} up -d --build

# Para la pila y borra los volumenes.
[group('docker')]
down:
    {{compose}} down -v

# Logs de la pila. just logs api
[group('docker')]
logs service="":
    {{compose}} logs -f --tail 100 {{service}}

# Estado de los servicios.
[group('docker')]
ps:
    {{compose}} ps

# Reconstruye las imagenes de desarrollo desde cero.
[group('docker')]
rebuild:
    {{compose}} build --no-cache

# Levanta la pila de produccion.
[group('docker')]
prod-up:
    {{compose_prod}} up -d --build

# Para la pila de produccion.
[group('docker')]
prod-down:
    {{compose_prod}} down

# Logs de la pila de produccion.
[group('docker')]
prod-logs service="":
    {{compose_prod}} logs -f --tail 100 {{service}}

# ---------------------------------------------------------------- database ---

# Levanta solo Postgres y espera a que responda.
[group('database')]
db-up:
    {{compose}} up -d --wait db

# Para Postgres.
[group('database')]
db-down:
    {{compose}} stop db

# Aplica las migraciones a mano. La API tambien las aplica sola al arrancar.
[group('database')]
migrate: db-up
    #!/usr/bin/env bash
    set -euo pipefail
    for file in apps/api/migrations/*.sql; do
      echo "aplicando ${file}"
      {{compose}} exec -T db psql -v ON_ERROR_STOP=1 -U "{{pg_user}}" -d "{{pg_db}}" < "${file}"
    done

# Abre una psql contra la base de datos de desarrollo.
[group('database')]
psql: db-up
    {{compose}} exec db psql -U "{{pg_user}}" -d "{{pg_db}}"

# Se borra la BASE DE DATOS, no el volumen: el volumen es del proyecto de
# compose y su nombre cambia con COMPOSE_PROJECT_NAME, asi que borrarlo a mano
# es fragil. DROP DATABASE ... WITH (FORCE) echa a quien este conectado, la API
# incluida, y vuelve a crearla vacia sin tumbar el contenedor.
#
# El texto de `just --list` va en [doc]: just toma como descripcion la ultima
# linea del comentario, y aqui seria un fragmento sin sentido.
[group('database')]
[doc('Borra la base de datos y la vuelve a crear vacia.')]
db-reset: db-up
    {{compose}} exec -T db psql -v ON_ERROR_STOP=1 -U "{{pg_user}}" -d postgres \
      -c 'DROP DATABASE IF EXISTS "{{pg_db}}" WITH (FORCE)' \
      -c 'CREATE DATABASE "{{pg_db}}" OWNER "{{pg_user}}"'
    @just migrate
