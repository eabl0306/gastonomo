//! Adaptadores driven de Gastonomo.
//!
//! Aqui viven las piezas que hablan con el mundo exterior: configuracion por
//! entorno, telemetria y PostgreSQL. El nucleo nunca importa este crate; es el
//! composition root quien conecta estos adaptadores con los casos de uso.

pub mod config;
pub mod persistence;
pub mod telemetry;

pub use config::{AppConfig, ConfigError, DatabaseConfig, ServerConfig, TelemetryConfig};
pub use persistence::{
    PersistenceError, PgPool, PostgresHealthProbe, connect_pool, run_migrations,
};
pub use telemetry::{TelemetryError, init_tracing};
