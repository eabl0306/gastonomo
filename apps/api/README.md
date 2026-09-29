# Gastonomo API

API en Rust con Salvo. Arquitectura hexagonal: el nucleo no sabe que existe
HTTP ni PostgreSQL, y un test lo comprueba en cada build.

El dominio esta completo: salud, autenticacion, movimientos, prestamos con sus
abonos, categorias, ajustes, monedas, tasas de cambio y el dictado de un registro.
Son 25 endpoints, once migraciones y 33 binarios de test, diez de ellos contra un
PostgreSQL de verdad.

El contrato completo, con el porque de cada decision, esta en
[docs/api-surface.md](../../docs/api-surface.md). La referencia ejecutable es
`GET /openapi.json`.

## Contrato publico

| Endpoint        | Que hace                                         | Codigos   |
|-----------------|--------------------------------------------------|-----------|
| `GET /health/live`  | Dice si el proceso atiende. No toca nada externo. | 200       |
| `GET /health/ready` | Comprueba PostgreSQL a traves de un puerto.       | 200 / 503 |
| `GET /openapi.json` | Documento OpenAPI del servicio.                   | 200       |
| `GET /swagger-ui`   | Interfaz para leer el documento. Carga desde CDN. | 200       |

### Autenticacion

Prefijo `/v1`. Todo JSON, claves en camelCase.

| Endpoint | Que hace | Codigos |
|---|---|---|
| `POST /v1/auth/register` | Registro. Siembra las 12 categorias y los ajustes. | 201 / 409 / 422 / 429 |
| `POST /v1/auth/login` | Login. | 200 / 401 / 422 / 429 |
| `POST /v1/auth/refresh` | Rota el refresh token y devuelve un par nuevo. | 200 / 401 |
| `POST /v1/auth/logout` | Revoca el refresh token recibido. Idempotente. | 204 |
| `POST /v1/auth/password/forgot` | Manda el enlace de recuperacion. | 202 / 429 |
| `POST /v1/auth/password/reset` | Cambia la contrasena con el token del correo. | 204 / 401 / 422 |
| `POST /v1/auth/password/change` | Cambia la contrasena estando dentro. | 204 / 401 / 422 |
| `GET /v1/me` | Perfil: nombre y correo. | 200 / 401 |

Todo lo que cuelga de `/v1` y no es `auth` exige `Authorization: Bearer <access
token>`. El access token dura 15 minutos; el refresh, 30 dias, y se rota en cada
refresco: reutilizar uno ya rotado revoca la familia entera, que es la senal
clasica de robo de token.

Decisiones que no se negocian: las contrasenas se guardan con Argon2id, los
refresh tokens y los enlaces de recuperacion se guardan **hasheados** (SHA-256),
`password/forgot` responde 202 siempre (exista o no el correo) y el enlace caduca
en 30 minutos y sirve una vez.

Los errores van en `application/problem+json` (RFC 9457):

```json
{
  "type": "https://gastonomo.app/problems/validation",
  "title": "Datos invalidos",
  "status": 422,
  "detail": "Revisa los campos marcados.",
  "errors": [{ "field": "email", "message": "..." }]
}
```

Respuesta de los dos endpoints de salud:

```json
{
  "status": "ok",
  "checks": [{ "name": "database", "status": "ok", "latencyMs": 3 }],
  "version": "0.1.0"
}
```

`status` vale `ok` o `degraded`. `/health/ready` devuelve 503 en cuanto un
componente falla; `/health/live` siempre devuelve 200, para que un fallo de
base de datos no provoque reinicios en cadena.

Toda respuesta lleva una cabecera `x-request-id`. Si la peticion trae una, se
propaga tal cual.

### Movimientos, prestamos, categorias y ajustes

| Endpoint | Que hace | Codigos |
|---|---|---|
| `GET /v1/movements?from=&to=&kind=&cursor=&limit=` | Lista del periodo, paginada por cursor. | 200 / 401 / 422 |
| `POST /v1/movements` | Nuevo ingreso o gasto. | 201 / 401 / 409 / 422 |
| `GET /v1/loans?direction=` | Prestamos con sus abonos dentro. | 200 / 401 / 422 |
| `GET /v1/loans/{id}` | Detalle de un prestamo. | 200 / 401 / 404 |
| `POST /v1/loans` | Nuevo prestamo. Escribe tambien su movimiento. | 201 / 401 / 409 / 422 |
| `POST /v1/loans/{id}/payments` | Nuevo abono. Escribe tambien su movimiento. | 201 / 401 / 404 / 409 / 422 |
| `GET /v1/categories` | Todas, con `isDefault` y el conteo de uso. | 200 / 401 |
| `POST /v1/categories` | Crear. | 201 / 401 / 409 / 422 |
| `PATCH /v1/categories/{id}` | Editar nombre, icono, color y tipo. | 200 / 401 / 404 / 409 / 422 |
| `DELETE /v1/categories/{id}` | Borrar y reasignar sus movimientos a `Otros`. | 204 / 401 / 404 / 409 |
| `GET /v1/settings` | Moneda principal e idioma. | 200 / 401 |
| `PATCH /v1/settings` | Cambiar una o las dos. | 200 / 401 / 422 / 503 |

Los importes viajan **como string**, y cada registro lleva cuatro campos:
`amount` y `currency` (lo que tecleo el usuario) mas `base` y `baseCurrency` (su
equivalencia al registrar, con la tasa que el usuario pudo editar). La API los
guarda tal cual y no recalcula nada. `base` y `baseCurrency` se pueden omitir los
dos juntos, y entonces la equivalencia es el propio importe.

Crear un prestamo o un abono son **dos escrituras en una transaccion**: el
registro y su movimiento generado. Si falla la segunda no queda la primera.

Un recurso de otra cuenta responde **404 y nunca 403**, igual que un
identificador de camino que no sea un UUID.

### Idempotencia

Los tres POST que crean dinero (`/v1/movements`, `/v1/loans` y
`/v1/loans/{id}/payments`) aceptan la cabecera `Idempotency-Key`. Es opcional,
pero un cliente movil deberia mandarla siempre: sin ella, un reintento tras
perder la cobertura duplica el gasto, y no hay pantalla para borrarlo.

El reintento con la misma clave y el mismo cuerpo recibe la respuesta original
con la cabecera `Idempotent-Replay: true`, sin escribir nada. La misma clave con
otro cuerpo es 409 `idempotency_key_reused`. Un cuerpo rechazado con 422 no quema
la clave. Las claves valen 24 horas. El detalle esta en
[el ADR 0012](../../docs/adr/0012-idempotencia-con-cabecera.md).

### Monedas y tasas

| Endpoint | Que hace | Codigos |
|---|---|---|
| `GET /v1/currencies` | Catalogo de monedas que se pueden elegir. | 200 / 401 |
| `GET /v1/exchange-rates` | Tasas vigentes con base USD. | 200 / 401 / 503 |

Los clientes no llaman a Frankfurter: lo hace una tarea de la API dos veces al
dia (06:00 y 18:00 UTC) y guarda el resultado en PostgreSQL. Al arrancar refresca
si no hay tasas o si las que hay llevan mas de 24 horas. Si la fuente falla, la
tarea reintenta tres veces con espera creciente y despues deja lo que haya: nunca
borra lo anterior.

El catalogo llega ya filtrado, fuera los metales (XAU, XAG, XPT, XPD), los XDR y
las monedas sin tasa. Las tasas viajan como **string**, igual que los importes:

```json
{
  "base": "USD",
  "date": "2026-09-25",
  "fetchedAt": "2026-09-26T02:30:27Z",
  "status": "ok",
  "rates": { "USD": "1", "COP": "3316.44", "EUR": "0.87732" }
}
```

`date` es la fecha de publicacion de las tasas y es la que Ajustes muestra en
"Tasas de cambio: Frankfurter · {fecha}". `status` pasa a `stale` cuando el
ultimo refresco con exito ya tiene 48 horas: las tasas se sirven igualmente y el
cliente pinta "Sin conexion". Con varias replicas solo una escribe, coordinadas
con un advisory lock de PostgreSQL.

Sin ninguna pasada guardada, `GET /v1/exchange-rates` responde **503** y no 500:
nada ha fallado, falta un dato que llena una tarea de fondo. Por lo mismo,
`PATCH /v1/settings` con el catalogo vacio responde 503 en vez de culpar al
cliente con un 422, y la moneda que la cuenta ya tiene puesta se acepta siempre
sin mirar el catalogo.

### Dictado

| Endpoint | Que hace | Codigos |
|---|---|---|
| `POST /v1/dictation/parse` | Convierte una frase dictada en un borrador. | 200 / 401 / 404 / 422 / 503 |

**No crea nada.** Devuelve un borrador de movimiento o de prestamo para que el
usuario lo revise, y confirmarlo es una llamada aparte a `POST /v1/movements` o
`POST /v1/loans` con su `Idempotency-Key`. Por eso este endpoint no acepta esa
cabecera: repetirlo no puede duplicar dinero.

El borrador sale ya comprobado contra la cuenta y ninguna regla rechaza la
peticion: una categoria que no existe cae en `Otros`, una moneda sin tasa cae en
la principal, una fecha futura cae en hoy y un importe que no se entendio sale
`null`. Lo que el usuario tiene que completar se lista en `missing`, y el cliente
deja el boton de guardar apagado mientras no este vacio.

Solo lo pueden usar las cuentas que estan en la tabla `dictation_allowlist`. Se
concede y se quita a mano, porque cada peticion sale a un modelo que se paga por
palabra:

```sql
INSERT INTO dictation_allowlist (user_id) VALUES ('<uuid>');
```

Quien no esta recibe **404 y no 403**, por la misma razon que un recurso de otra
cuenta: un 403 confirmaria que la capacidad existe. Sin
`GASTONOMO__CLAUDE__API_KEY` el endpoint responde **503** y el resto de la API
funciona igual. El porque esta en
[el ADR 0024](../../docs/adr/0024-dictar-un-registro.md).

## Configuracion

Variables de entorno con prefijo `GASTONOMO__` y doble guion bajo como
separador de nivel.

| Variable | Por defecto | Que es |
|---|---|---|
| `GASTONOMO__SERVER__HOST` | `0.0.0.0` | IP de escucha |
| `GASTONOMO__SERVER__PORT` | `8080` | Puerto de escucha |
| `GASTONOMO__DATABASE__URL` | `postgres://gastonomo:gastonomo@localhost:5432/gastonomo` | Conexion a PostgreSQL |
| `GASTONOMO__DATABASE__MAX_CONNECTIONS` | `5` | Tamano del pool |
| `GASTONOMO__DATABASE__ACQUIRE_TIMEOUT_MS` | `3000` | Espera maxima por conexion |
| `GASTONOMO__TELEMETRY__LOG_LEVEL` | `info` | Filtro de logs, sintaxis de `RUST_LOG` |
| `GASTONOMO__AUTH__ACCESS_TOKEN_SECRET` | secreto de desarrollo | Clave HMAC del access token. **Cambiala en produccion**: el arranque avisa si sigue la de desarrollo |
| `GASTONOMO__AUTH__ACCESS_TOKEN_MINUTES` | `15` | Validez del access token |
| `GASTONOMO__AUTH__REFRESH_TOKEN_DAYS` | `30` | Validez del refresh token |
| `GASTONOMO__AUTH__RESET_LINK_TEMPLATE` | `http://localhost:5173/reset-password?token={token}` | Enlace del correo. `{token}` se sustituye |
| `GASTONOMO__AUTH__RATE_LIMIT_MAX_REQUESTS` | `10` | Peticiones por ventana en login, registro y recuperacion |
| `GASTONOMO__AUTH__RATE_LIMIT_WINDOW_SECONDS` | `60` | Tamano de la ventana |
| `GASTONOMO__MAIL__DRIVER` | `log` | `log` escribe el correo en el log, `smtp` lo envia |
| `GASTONOMO__MAIL__HOST` | `localhost` | Host SMTP |
| `GASTONOMO__MAIL__PORT` | `1025` | Puerto SMTP (1025 es el de Mailpit) |
| `GASTONOMO__MAIL__USERNAME` | vacio | Usuario SMTP. Vacio significa sin autenticacion |
| `GASTONOMO__MAIL__PASSWORD` | vacio | Contrasena SMTP |
| `GASTONOMO__MAIL__FROM` | `Gastonomo <no-reply@gastonomo.local>` | Remitente |
| `GASTONOMO__MAIL__STARTTLS` | `false` | Exige STARTTLS. Apagado para hablar con Mailpit |
| `GASTONOMO__EXCHANGE_RATES__ENABLED` | `true` | Arranca la tarea de refresco de tasas. Apagala si el proceso no debe salir a internet |
| `GASTONOMO__EXCHANGE_RATES__BASE_URL` | `https://api.frankfurter.dev/v2` | Raiz de la fuente de tasas |
| `GASTONOMO__EXCHANGE_RATES__REFRESH_HOURS_UTC` | `6,18` | Horas UTC del refresco, separadas por comas |
| `GASTONOMO__EXCHANGE_RATES__REQUEST_TIMEOUT_MS` | `10000` | Espera maxima por peticion a la fuente |
| `GASTONOMO__EXCHANGE_RATES__RETRY_ATTEMPTS` | `3` | Intentos por pasada, contando el primero |
| `GASTONOMO__EXCHANGE_RATES__RETRY_BACKOFF_MS` | `2000` | Espera antes del segundo intento. Se duplica en cada intento |
| `GASTONOMO__CLAUDE__API_KEY` | vacio | Clave de la API de Anthropic para el dictado. Vacia lo apaga y el endpoint responde 503 |
| `GASTONOMO__CLAUDE__BASE_URL` | `https://api.anthropic.com` | Raiz de la API del modelo |
| `GASTONOMO__CLAUDE__MODEL` | `claude-haiku-4-5-20251001` | Modelo que interpreta lo dictado |
| `GASTONOMO__CLAUDE__REQUEST_TIMEOUT_MS` | `12000` | Espera maxima por la llamada al modelo |
| `GASTONOMO__CLAUDE__MAX_TOKENS` | `512` | Tope de la respuesta del modelo |

## Estructura

```
apps/api
  crates/domain           nucleo puro, solo depende de thiserror
  crates/application      casos de uso y puertos, sin framework
  crates/infrastructure   adaptadores driven: config, telemetria, PostgreSQL
  crates/http             adaptador driving: Salvo, DTOs y OpenAPI
  crates/api              composition root y binario
  migrations              migraciones de SQLx
```

La direccion de dependencias va siempre hacia dentro: `api` conoce a todos,
`http` e `infrastructure` conocen a `application` y `domain`, y `domain` no
conoce a nadie.

### Como se anade un caso de uso

1. Modela las reglas en `domain`.
2. Declara el puerto que necesites en `application/src/port` y escribe el caso
   de uso en `application/src/use_case`, con su doble de test.
3. Implementa el puerto en `infrastructure`.
4. Expon el caso de uso en `http` con un DTO propio. Nunca serialices un tipo
   de dominio.
5. Conecta las piezas en `crates/api/src/bootstrap/<contexto>.rs`. Es el unico
   sitio donde se nombran adaptadores concretos, y hay un fichero por contexto
   para que dos cambios en paralelo no choquen.

## Desarrollo

Hay dos pilas de Docker y conviene no mezclarlas. La del monorepo es
`compose.yaml` de la raiz (Postgres en el 5432) y es la que usan todas las recetas
de `just`. La de esta carpeta es un atajo para trabajar sin la web y publica otros
puertos a proposito: Postgres en el **5433**, la API en el 8081 y Mailpit en el
8026 y el 1026.

```sh
docker compose -f apps/api/docker-compose.yml up -d postgres   # solo la base
cargo run --bin gastonomo-api                                  # la API en local
docker compose -f apps/api/docker-compose.yml up               # todo con recarga
```

### Correo en desarrollo

La pila de Docker trae **Mailpit**: la API le habla por SMTP y los correos se leen
en el navegador. Con la pila de la raiz es el 1025 y <http://localhost:8025>; con
la de esta carpeta, el 1026 y <http://localhost:8026>. No sale nada a internet.

```sh
docker compose -f apps/api/docker-compose.yml up -d mailpit
```

Fuera de Docker, con `GASTONOMO__MAIL__DRIVER=log` (el valor por defecto) el
enlace de recuperacion aparece en el log del proceso.

Con `just` instalado, desde `apps/api`:

```sh
just check        # formato, clippy, tests y cobertura, igual que CI
just test-db      # tests contra un PostgreSQL de verdad
just coverage-html
```

## Calidad

Nada de esto es una recomendacion: si falla, no compila o no pasa CI.

| Regla | Quien la fuerza |
|---|---|
| Formato | `cargo fmt --all --check` con `rustfmt.toml` |
| Complejidad cognitiva <= 15 | `clippy::cognitive_complexity` en deny, umbral en `clippy.toml` |
| Funciones <= 50 lineas | `clippy::too_many_lines` en deny, umbral en `clippy.toml` |
| Parametros <= 5 | `clippy::too_many_arguments` en deny |
| Anidamiento <= 4 niveles | `clippy::excessive_nesting` en deny, umbral en `clippy.toml` |
| Sin codigo muerto ni imports sin usar | lints de `rustc` en deny via `-D warnings` |
| Sin `unwrap`, `expect`, `panic!`, `todo!`, `dbg!` | lints de clippy en deny |
| Documentacion de todo lo publico | `missing_docs` |
| Dependencias sin usar | `unused_crate_dependencies` |
| Cobertura: 80% en el nucleo, 60% global | `cargo llvm-cov --fail-under-lines` |
| El nucleo no toca infraestructura | `crates/api/tests/architecture.rs` |
| Licencias y avisos de seguridad | `cargo deny check` con `deny.toml` |

El limite de 300 lineas por fichero (400 en tests) lo comprueba el script
compartido del monorepo, en `tools/checks`.

La complejidad **ciclomatica** no se fuerza aqui: clippy no tiene lint que la mida,
solo la cognitiva. Lo que la acota en la practica es la combinacion de
`too_many_lines`, `too_many_arguments`, `cognitive_complexity` y `excessive_nesting`;
un `match` plano gigante se cuela. El porque esta en
[el ADR 0005](../../docs/adr/0005-enforcement-automatizado.md).

Los tests pueden relajar `unwrap`/`expect` con un `#![allow]` local y su motivo
escrito. No hay `#[ignore]` sin justificacion ni tests enfocados.

### El test de arquitectura

`crates/api/tests/architecture.rs` lee `Cargo.lock`, calcula el cierre
transitivo de dependencias de `gastonomo-domain` y `gastonomo-application` y
falla si aparece Salvo, SQLx, Tokio, reqwest, hyper, utoipa, argon2, jsonwebtoken
o lettre. Tambien comprueba que las dependencias directas del nucleo estan en una
lista corta de librerias puras (`thiserror`, `chrono`, `rust_decimal`, `uuid`),
asi que no vale colar una dependencia "pequena".

`serde` no esta en la lista del cierre transitivo, y no es un olvido: `Cargo.lock`
apunta tambien las dependencias **opcionales** de cada paquete, y `chrono`, `uuid`
y `rust_decimal` tienen una feature `serde`. Que el nucleo no serialice nada lo
garantiza la comprobacion de dependencias directas.

## Tests

```sh
cargo test --workspace     # unitarios y de integracion, sin base de datos
```

Los tests que necesitan PostgreSQL son siete suites en
`crates/infrastructure/tests`: `postgres_integration` (esquema y probe),
`postgres_auth` (cuentas, sesiones, tokens de recuperacion y siembra del
registro), `postgres_movements` (cuatro importes, cursor y aislamiento entre
cuentas), `postgres_loans` (las dos escrituras en una transaccion),
`postgres_categories_and_settings`, `postgres_exchange_rates` y
`postgres_idempotency` (la reserva atomica de una clave). Se saltan solas si no
hay `GASTONOMO_TEST_DATABASE_URL`:

```sh
just api-test-db     # desde la raiz del monorepo: levanta la base y los corre
```

Saltarselos solo vale en una maquina sin base de datos. Con
`GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta de la URL pasa a ser un fallo: si
el servicio de postgres no arranca, los tests se ponen en rojo en vez de pasar
sin haber tocado la base. Es lo que hace `just test-db`.

La base **no se vacia entre tests**: cada uno crea su propia cuenta con un correo
unico. Por eso el Postgres de desarrollo acumula filas de prueba en cada pasada, y
conviene un `just db-reset` de vez en cuando.

## Docker

```sh
docker build -t gastonomo-api:local apps/api
docker run --rm -p 8080:8080 \
  -e GASTONOMO__DATABASE__URL=postgres://gastonomo:gastonomo@host.docker.internal:5432/gastonomo \
  gastonomo-api:local
```

La imagen final es `debian:trixie-slim`, corre como usuario `gastonomo`
(uid 10001), expone el 8080 y trae un `HEALTHCHECK` que consulta
`/health/live`. El build usa cargo-chef para cachear dependencias.

## Migraciones

Estan en `apps/api/migrations` y se aplican al arrancar. Si PostgreSQL todavia
no acepta conexiones, el arranque no aborta: deja un aviso y `/health/ready`
responde 503 hasta que la base este lista. Asi el contenedor no entra en bucle
de reinicios cuando arranca antes que la base.
