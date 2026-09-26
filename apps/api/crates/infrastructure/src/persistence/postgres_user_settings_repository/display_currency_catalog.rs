//! Adaptador driven: monedas que se pueden elegir como moneda principal.
//!
//! Lee la tabla `currencies`, que llena la tarea de refresco de tasas. Una moneda
//! no se borra nunca, porque los registros que la usan siguen existiendo: se marca
//! con `offered = false` y deja de ofrecerse. Por eso la comprobacion mira las dos
//! cosas, que exista y que se siga ofreciendo.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::settings::DisplayCurrencyCatalog;
use gastonomo_domain::value_object::CurrencyCode;
use sqlx::PgPool;

use crate::persistence::mapping::database_error;

/// Catalogo de monedas sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresDisplayCurrencyCatalog {
    pool: PgPool,
}

impl PostgresDisplayCurrencyCatalog {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl DisplayCurrencyCatalog for PostgresDisplayCurrencyCatalog {
    async fn is_selectable(&self, currency: &CurrencyCode) -> Result<bool, AppError> {
        sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS (SELECT 1 FROM currencies WHERE code = $1 AND offered)",
        )
        .bind(currency.as_str())
        .fetch_one(&self.pool)
        .await
        .map_err(|error| database_error("could not read the currency catalog", &error))
    }

    async fn is_ready(&self) -> Result<bool, AppError> {
        sqlx::query_scalar::<_, bool>("SELECT EXISTS (SELECT 1 FROM currencies WHERE offered)")
            .fetch_one(&self.pool)
            .await
            .map_err(|error| database_error("could not read the currency catalog", &error))
    }
}
