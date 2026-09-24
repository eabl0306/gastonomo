# Gastonomo API

API en Rust con Salvo. Arquitectura hexagonal: el nucleo no sabe que existe
HTTP ni PostgreSQL, y un test lo comprueba en cada build.

Este primer corte solo implementa el caso de uso "comprobar la salud del
servicio" de punta a punta. No hay entidades de negocio todavia: los huecos
estan preparados para cuando lleguen.

## Contrato publico

| Endpoint        | Que hace                                         | Codigos   |
|-----------------|--------------------------------------------------|-----------|
| `GET /health/live`  | Dice si el proceso atiende. No toca nada externo. | 200       |
| `GET /health/ready` | Comprueba PostgreSQL a traves de un puerto.       | 200 / 503 |
| `GET /openapi.json` | Documento OpenAPI del servicio.                   | 200       |
| `GET /swagger-ui`   | Interfaz para leer el documento. Carga desde CDN. | 200       |

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
5. Conecta las piezas en `crates/api/src/bootstrap.rs`. Es el unico sitio donde
   se nombran adaptadores concretos.

## Desarrollo

```sh
docker compose -f apps/api/docker-compose.yml up -d postgres   # solo la base
cargo run --bin gastonomo-api                                  # la API en local
docker compose -f apps/api/docker-compose.yml up               # todo con recarga
```

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
falla si aparece Salvo, SQLx, Tokio, reqwest, hyper, serde o utoipa. Tambien
comprueba que las dependencias directas del nucleo son exactamente las
esperadas, asi que no vale colar una dependencia "pequena".

## Tests

```sh
cargo test --workspace     # unitarios y de integracion, sin base de datos
```

Los tests que necesitan PostgreSQL viven en
`crates/infrastructure/tests/postgres_integration.rs` y se saltan solos si no
hay `GASTONOMO_TEST_DATABASE_URL`:

```sh
GASTONOMO_TEST_DATABASE_URL=postgres://gastonomo:gastonomo@localhost:5432/gastonomo \
  cargo test -p gastonomo-infrastructure --test postgres_integration
```

Saltarselos solo vale en una maquina sin base de datos. Con
`GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta de la URL pasa a ser un fallo: si
el servicio de postgres no arranca, los tests se ponen en rojo en vez de pasar
sin haber tocado la base. Es lo que hace `just test-db`.

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
