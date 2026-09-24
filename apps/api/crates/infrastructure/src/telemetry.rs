//! Inicializacion de trazas y logs.

use thiserror::Error;
use tracing_subscriber::EnvFilter;

use crate::config::TelemetryConfig;

/// Fallo al inicializar la telemetria.
#[derive(Debug, Error)]
pub enum TelemetryError {
    /// El filtro de nivel configurado no es valido.
    #[error("invalid log filter {filter:?}: {source}")]
    InvalidFilter {
        /// Filtro rechazado.
        filter: String,
        /// Error original del parser.
        #[source]
        source: tracing_subscriber::filter::ParseError,
    },
    /// Ya habia un subscriber global instalado.
    #[error("a global tracing subscriber was already installed")]
    AlreadyInitialized,
}

/// Instala el subscriber global de `tracing`.
///
/// # Errors
///
/// Devuelve [`TelemetryError`] si el filtro no es valido o si ya habia un
/// subscriber instalado.
pub fn init_tracing(config: &TelemetryConfig) -> Result<(), TelemetryError> {
    let filter = build_filter(&config.log_level)?;
    tracing_subscriber::fmt()
        .with_env_filter(filter)
        .with_target(true)
        .with_level(true)
        .try_init()
        .map_err(|_| TelemetryError::AlreadyInitialized)
}

fn build_filter(level: &str) -> Result<EnvFilter, TelemetryError> {
    EnvFilter::try_new(level).map_err(|source| TelemetryError::InvalidFilter {
        filter: level.to_owned(),
        source,
    })
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use super::build_filter;

    #[test]
    fn accepts_a_plain_level() {
        assert!(build_filter("info").is_ok());
    }

    #[test]
    fn accepts_a_per_target_directive() {
        assert!(build_filter("warn,gastonomo_http=debug").is_ok());
    }

    #[test]
    fn rejects_a_malformed_directive() {
        assert!(build_filter("gastonomo_http=not-a-level").is_err());
    }
}
