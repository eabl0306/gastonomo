//! Adaptadores de persistencia sobre PostgreSQL.

mod pool;
mod postgres_health_probe;

/// Pool de conexiones de SQLx, reexportado para el composition root.
pub use sqlx::PgPool;

pub use pool::{PersistenceError, connect_pool, run_migrations};
pub use postgres_health_probe::PostgresHealthProbe;
