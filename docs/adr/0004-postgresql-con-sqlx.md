# ADR 0004: PostgreSQL con SQLx y migraciones en el repositorio

- Estado: aceptado
- Fecha: 2026-09-23

## Contexto

Hace falta persistencia relacional. El primer corte no tiene dominio de negocio, pero el
adaptador de base de datos tiene que existir desde el principio: es lo que demuestra que
los puertos funcionan y lo que comprueba el readiness del servicio.

Las opciones en Rust eran un ORM (SeaORM, Diesel) o un cliente SQL con verificacion en
compilacion (SQLx).

## Decision

PostgreSQL 18 con SQLx 0.9, sin ORM.

- El pool se crea de forma perezosa y las migraciones se aplican al arrancar con
  `sqlx::migrate!("../../migrations")`.
- Las migraciones son ficheros SQL numerados en `apps/api/migrations`. La primera deja
  la extension `pgcrypto` y una tabla `schema_metadata` de una sola fila. No hay tablas
  de negocio inventadas.
- El adaptador `PostgresHealthProbe` implementa el puerto `HealthProbe` con un
  `SELECT 1` y timeout de 2 segundos.
- Los tests de integracion viven en `crates/infrastructure/tests/postgres_integration.rs`
  y se saltan solos si no hay `GASTONOMO_TEST_DATABASE_URL`, para que `cargo test` pase
  en una maquina sin base de datos.
- No se usa `#[sqlx::test]`, que obligaria a tener `DATABASE_URL` en tiempo de
  compilacion de las macros.
- En desarrollo Postgres lo levanta `compose.yaml`. Ojo: en la imagen 18 el volumen va
  montado en `/var/lib/postgresql`, no en `/var/lib/postgresql/data`, o el contenedor se
  niega a arrancar.

## Consecuencias

A favor:

- SQL a la vista, sin capa magica que traducir mentalmente.
- Las migraciones viajan con el codigo y se aplican solas al desplegar.
- El readiness check es real: si la base no responde, `/health/ready` devuelve 503.

En contra:

- Si PostgreSQL no responde al arrancar, la migracion falla con un aviso y el proceso
  sigue vivo devolviendo 503. Es deliberado, para que el contenedor no entre en bucle de
  reinicios, pero significa que un fallo real de migracion no mata el proceso. Cambiarlo
  es una linea en `crates/api/src/bootstrap.rs`.
- Sin ORM hay que escribir el mapeo a mano. Con el dominio real eso sera mas codigo.
