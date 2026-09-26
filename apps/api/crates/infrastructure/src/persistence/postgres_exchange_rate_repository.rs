//! Adaptador driven: tasas de cambio en PostgreSQL.
//!
//! La escritura es `INSERT ... ON CONFLICT (rate_date, base, quote) DO UPDATE`, asi
//! que refrescar dos veces la misma fecha no duplica nada y nunca borra lo
//! anterior. El refresco entre varias replicas se coordina con un advisory lock.

mod read;
mod write;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::currency::{ExchangeRateRepository, SaveOutcome};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRateTable, RateBulletin};
use sqlx::PgPool;

use super::mapping::database_error;

/// Clave del advisory lock del refresco.
///
/// Un numero fijo y arbitrario, distinto del que usa el migrador de SQLx (que
/// deriva el suyo del nombre de la base de datos).
const REFRESH_LOCK_KEY: i64 = 8_314_072_026;

/// Repositorio de tasas de cambio y del catalogo de monedas.
#[derive(Debug, Clone)]
pub struct PostgresExchangeRateRepository {
    pool: PgPool,
}

impl PostgresExchangeRateRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl ExchangeRateRepository for PostgresExchangeRateRepository {
    async fn latest(&self, base: &CurrencyCode) -> Result<Option<ExchangeRateTable>, AppError> {
        read::latest_table(&self.pool, base).await
    }

    async fn offered_currencies(&self) -> Result<Vec<Currency>, AppError> {
        read::offered_currencies(&self.pool).await
    }

    /// Guarda la pasada entera en una transaccion.
    ///
    /// El advisory lock es de transaccion (`pg_try_advisory_xact_lock`) y no de
    /// sesion: con un pool de conexiones, una sesion vuelve al pool con sus
    /// cerrojos puestos y el siguiente que la coja se los encuentra. El de
    /// transaccion se suelta al terminar, pase lo que pase.
    async fn save(
        &self,
        bulletin: &RateBulletin,
        fetched_at: DateTime<Utc>,
    ) -> Result<SaveOutcome, AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;
        if !write::try_lock(&mut transaction, REFRESH_LOCK_KEY).await? {
            return Ok(SaveOutcome::LockedElsewhere);
        }
        write::upsert_rates(&mut transaction, bulletin, fetched_at).await?;
        write::upsert_currencies(&mut transaction, bulletin, fetched_at).await?;
        write::stop_offering_the_rest(&mut transaction, bulletin, fetched_at).await?;
        transaction
            .commit()
            .await
            .map(|()| SaveOutcome::Written)
            .map_err(|error| database_error("could not commit the exchange rates", &error))
    }
}
