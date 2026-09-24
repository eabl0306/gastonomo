//! Adaptador driven: comprueba PostgreSQL a traves del puerto `HealthProbe`.

use std::time::Duration;

use async_trait::async_trait;
use gastonomo_application::port::{HealthProbe, ProbeError};
use gastonomo_domain::ComponentName;
use sqlx::PgPool;
use tokio::time::timeout;

/// Nombre publico del componente, fijado por el contrato compartido.
const COMPONENT: &str = "database";

/// Comprueba que PostgreSQL responde a una consulta trivial.
#[derive(Debug, Clone)]
pub struct PostgresHealthProbe {
    name: ComponentName,
    pool: PgPool,
    timeout: Duration,
}

impl PostgresHealthProbe {
    /// Crea el probe sobre un pool ya construido.
    ///
    /// # Panics
    ///
    /// No entra en panico: el nombre del componente es una constante valida.
    #[must_use]
    pub fn new(pool: PgPool, probe_timeout: Duration) -> Self {
        let name = match ComponentName::parse(COMPONENT) {
            Ok(name) => name,
            Err(error) => unreachable!("component name constant is invalid: {error}"),
        };
        Self {
            name,
            pool,
            timeout: probe_timeout,
        }
    }
}

#[async_trait]
impl HealthProbe for PostgresHealthProbe {
    fn name(&self) -> &ComponentName {
        &self.name
    }

    async fn check(&self) -> Result<(), ProbeError> {
        let query = sqlx::query("SELECT 1").execute(&self.pool);
        match timeout(self.timeout, query).await {
            Ok(Ok(_)) => Ok(()),
            Ok(Err(error)) => Err(ProbeError::new(error.to_string())),
            Err(_) => Err(ProbeError::new(format!(
                "postgres did not answer within {}ms",
                self.timeout.as_millis()
            ))),
        }
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use std::time::Duration;

    use gastonomo_application::port::HealthProbe;

    use super::PostgresHealthProbe;
    use crate::config::DatabaseConfig;
    use crate::persistence::connect_pool;

    fn probe(url: &str, millis: u64) -> PostgresHealthProbe {
        let config = DatabaseConfig {
            url: url.to_owned(),
            ..DatabaseConfig::default()
        };
        let pool = connect_pool(&config).expect("lazy pool should build");
        PostgresHealthProbe::new(pool, Duration::from_millis(millis))
    }

    #[tokio::test]
    async fn exposes_the_component_name_from_the_shared_contract() {
        let probe = probe("postgres://u:p@127.0.0.1:1/none", 50);
        assert_eq!(probe.name().as_str(), "database");
    }

    #[tokio::test]
    async fn reports_an_error_when_postgres_is_unreachable() {
        let probe = probe("postgres://u:p@127.0.0.1:1/none", 500);
        let error = probe.check().await.expect_err("port 1 should refuse");
        assert!(!error.message().is_empty());
    }
}
