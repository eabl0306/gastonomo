//! Creacion del pool de conexiones y ejecucion de migraciones.

use std::time::Duration;

use sqlx::PgPool;
use sqlx::postgres::PgPoolOptions;
use thiserror::Error;

use crate::config::DatabaseConfig;

/// Fallo de la capa de persistencia.
#[derive(Debug, Error)]
pub enum PersistenceError {
    /// No se pudo crear el pool o conectar con PostgreSQL.
    #[error("could not connect to postgres: {0}")]
    Connect(#[source] sqlx::Error),
    /// Las migraciones no se pudieron aplicar.
    #[error("could not apply migrations: {0}")]
    Migrate(#[source] sqlx::migrate::MigrateError),
}

/// Migraciones embebidas en el binario.
static MIGRATOR: sqlx::migrate::Migrator = sqlx::migrate!("../../migrations");

/// Crea el pool de conexiones a PostgreSQL.
///
/// El pool es perezoso: no abre conexiones hasta que alguien las pide, asi el
/// proceso arranca aunque la base de datos todavia no acepte conexiones y el
/// readiness check refleje esa situacion en lugar de matar el contenedor.
///
/// # Errors
///
/// Devuelve [`PersistenceError::Connect`] si la cadena de conexion no es valida.
pub fn connect_pool(config: &DatabaseConfig) -> Result<PgPool, PersistenceError> {
    tracing::debug!(
        max_connections = config.max_connections,
        acquire_timeout_ms = config.acquire_timeout_ms,
        "building the postgres connection pool"
    );
    PgPoolOptions::new()
        .max_connections(config.max_connections)
        .acquire_timeout(Duration::from_millis(config.acquire_timeout_ms))
        .connect_lazy(&config.url)
        .map_err(PersistenceError::Connect)
}

/// Aplica las migraciones pendientes.
///
/// # Errors
///
/// Devuelve [`PersistenceError::Migrate`] si alguna migracion falla.
pub async fn run_migrations(pool: &PgPool) -> Result<(), PersistenceError> {
    tracing::info!(
        migrations = MIGRATOR.migrations.len(),
        "applying database migrations"
    );
    MIGRATOR.run(pool).await.map_err(PersistenceError::Migrate)
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::{MIGRATOR, connect_pool};
    use crate::config::DatabaseConfig;

    #[tokio::test]
    async fn a_lazy_pool_is_built_without_touching_the_network() {
        let config = DatabaseConfig::default();
        assert!(connect_pool(&config).is_ok());
    }

    #[tokio::test]
    async fn an_invalid_connection_string_is_rejected_up_front() {
        let config = DatabaseConfig {
            url: "not-a-url".to_owned(),
            ..DatabaseConfig::default()
        };
        assert!(connect_pool(&config).is_err());
    }

    #[test]
    fn the_migration_set_is_embedded_and_not_empty() {
        assert!(!MIGRATOR.migrations.is_empty());
    }
}
