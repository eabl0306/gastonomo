//! Caso de uso: tasas vigentes.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::currency::{ExchangeRateTable, RateStatus, rates_base};

use crate::error::AppError;
use crate::port::Clock;
use crate::port::currency::ExchangeRateRepository;

/// Tasas vigentes junto con su frescura.
///
/// La frescura depende del instante en que se pregunta, asi que se calcula aqui,
/// donde esta el reloj, y no en el adaptador HTTP.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ExchangeRatesView {
    /// Tabla guardada.
    pub table: ExchangeRateTable,
    /// `ok`, o `stale` si el ultimo refresco con exito ya tiene dos dias.
    pub status: RateStatus,
}

/// Devuelve las tasas con base USD que la API tiene guardadas.
#[derive(Clone)]
pub struct GetExchangeRates {
    rates: Arc<dyn ExchangeRateRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for GetExchangeRates {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("GetExchangeRates").finish_non_exhaustive()
    }
}

impl GetExchangeRates {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(rates: Arc<dyn ExchangeRateRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { rates, clock }
    }

    /// Lee las tasas.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos y
    /// [`AppError::Unavailable`] si no hay ninguna tabla guardada: eso no es un
    /// recurso que el cliente haya pedido mal ni un fallo, es que la tarea de
    /// refresco todavia no ha conseguido una pasada, asi que sale un 503 y el
    /// cliente sabe que tiene que reintentar.
    pub async fn execute(&self) -> Result<ExchangeRatesView, AppError> {
        let base = rates_base();
        let table = self.rates.latest(&base).await?.ok_or_else(|| {
            AppError::unavailable(format!(
                "there are no stored exchange rates for base {base}"
            ))
        })?;
        let status = table.status(self.clock.now());
        Ok(ExchangeRatesView { table, status })
    }
}
