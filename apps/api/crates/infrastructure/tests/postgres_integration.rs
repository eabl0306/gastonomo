//! Tests de integracion contra un PostgreSQL de verdad.
//!
//! No se ejecutan en `cargo test --workspace`: hace falta exportar
//! `GASTONOMO_TEST_DATABASE_URL`. Sin esa variable cada test se salta a si
//! mismo, para que el workspace siga pasando en una maquina sin base de datos.
//!
//! Con `GASTONOMO_TEST_REQUIRE_DATABASE=1` el salto deja de estar permitido y
//! la falta de la URL es un fallo. Es lo que usan `just test-db` y el paso de
//! CI dedicado: si el servicio de postgres no arranca, estos tests se ponen en
//! rojo en vez de pasar sin haber ejecutado nada.
//!
//! Para lanzarlos:
//!
//! ```sh
//! docker compose -f apps/api/docker-compose.yml up -d postgres
//! GASTONOMO_TEST_DATABASE_URL=postgres://gastonomo:gastonomo@localhost:5432/gastonomo \
//!   cargo test -p gastonomo-infrastructure --test postgres_integration
//! ```

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use std::time::Duration;

use gastonomo_application::HealthProbe;
use gastonomo_infrastructure::{
    DatabaseConfig, PgPool, PostgresHealthProbe, connect_pool, run_migrations,
};

/// Variable que habilita esta bateria de tests.
const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

/// Si la base de datos es obligatoria, saltarse los tests deja de ser una
/// opcion.
///
/// La bandera es explicita a proposito: `cargo test --workspace` corre tambien
/// en sitios donde no hay postgres y ahi el salto es legitimo. Solo el comando
/// que promete probar la base la enciende.
fn database_is_mandatory() -> bool {
    std::env::var(REQUIRE_VAR)
        .is_ok_and(|value| !matches!(value.trim(), "" | "0" | "false" | "FALSE"))
}

fn database_url() -> Option<String> {
    let url = std::env::var(URL_VAR).ok().filter(|url| !url.is_empty());
    assert!(
        url.is_some() || !database_is_mandatory(),
        "{URL_VAR} is empty but {REQUIRE_VAR} is set: \
         these tests must not pass without a real postgres"
    );
    url
}

async fn migrated_pool() -> Option<PgPool> {
    let url = database_url()?;
    let config = DatabaseConfig {
        url,
        ..DatabaseConfig::default()
    };
    let pool = connect_pool(&config).expect("pool should build");
    run_migrations(&pool)
        .await
        .expect("migrations should apply");
    Some(pool)
}

#[tokio::test]
async fn migrations_create_the_schema_metadata_table() {
    let Some(pool) = migrated_pool().await else {
        return;
    };

    let version: String = sqlx::query_scalar("SELECT schema_version FROM schema_metadata")
        .fetch_one(&pool)
        .await
        .expect("the metadata row should exist");

    assert_eq!(version, "0.1.0");
}

#[tokio::test]
async fn migrations_are_idempotent() {
    let Some(pool) = migrated_pool().await else {
        return;
    };

    run_migrations(&pool)
        .await
        .expect("running migrations twice should be a no-op");

    let rows: i64 = sqlx::query_scalar("SELECT count(*) FROM schema_metadata")
        .fetch_one(&pool)
        .await
        .expect("count should succeed");

    assert_eq!(rows, 1);
}

#[tokio::test]
async fn the_probe_reports_a_reachable_database() {
    let Some(pool) = migrated_pool().await else {
        return;
    };

    let probe = PostgresHealthProbe::new(pool, Duration::from_secs(2));
    assert_eq!(probe.name().as_str(), "database");
    probe
        .check()
        .await
        .expect("a running postgres should answer SELECT 1");
}

#[tokio::test]
async fn the_probe_reports_an_unreachable_database() {
    if database_url().is_none() {
        return;
    }

    let config = DatabaseConfig {
        url: "postgres://gastonomo:gastonomo@127.0.0.1:1/gastonomo".to_owned(),
        ..DatabaseConfig::default()
    };
    let pool = connect_pool(&config).expect("pool should build");
    let probe = PostgresHealthProbe::new(pool, Duration::from_secs(1));

    let error = probe.check().await.expect_err("port 1 should refuse");
    assert!(!error.message().is_empty());
}
