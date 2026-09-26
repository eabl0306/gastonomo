//! Caso de uso: refresco de las tasas de cambio.

use std::fmt;
use std::sync::Arc;

use chrono::NaiveDate;
use gastonomo_domain::currency::rates_base;

use crate::error::AppError;
use crate::port::Clock;
use crate::port::currency::{ExchangeRateProvider, ExchangeRateRepository, SaveOutcome};

/// En que quedo un refresco.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RefreshOutcome {
    /// Habia una fecha nueva y las tasas quedaron escritas.
    Updated,
    /// La fuente devolvio la misma fecha que ya teniamos.
    ///
    /// La escritura es idempotente, asi que las tasas no cambian; lo que si se
    /// mueve es el instante del ultimo refresco, y eso es lo que evita que unas
    /// tasas de viernes se marquen rancias el lunes por la manana.
    Unchanged,
    /// Otra replica tenia el refresco tomado y esta no escribio nada.
    SkippedLocked,
}

/// Resultado de un refresco, para la traza del planificador.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct RefreshReport {
    /// Fecha de las tasas que trajo la fuente.
    pub date: NaiveDate,
    /// Monedas que quedaron ofrecidas.
    pub currencies: usize,
    /// En que quedo la operacion.
    pub outcome: RefreshOutcome,
}

/// Trae una pasada de la fuente y la guarda.
///
/// No reintenta ni espera: los reintentos con espera creciente son del
/// planificador, que es quien tiene reloj y puede dormir. Aqui esta solo la regla,
/// y por eso este caso de uso se prueba entero con un proveedor falso.
#[derive(Clone)]
pub struct RefreshExchangeRates {
    provider: Arc<dyn ExchangeRateProvider>,
    rates: Arc<dyn ExchangeRateRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for RefreshExchangeRates {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RefreshExchangeRates")
            .finish_non_exhaustive()
    }
}

impl RefreshExchangeRates {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(
        provider: Arc<dyn ExchangeRateProvider>,
        rates: Arc<dyn ExchangeRateRepository>,
        clock: Arc<dyn Clock>,
    ) -> Self {
        Self {
            provider,
            rates,
            clock,
        }
    }

    /// Refresca las tasas.
    ///
    /// Si la fuente falla no se toca nada de lo guardado: unas tasas de anteayer
    /// convierten mejor que ninguna, y el cliente ya sabe verlas marcadas.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si la fuente no responde o si falla la
    /// base de datos.
    pub async fn execute(&self) -> Result<RefreshReport, AppError> {
        let bulletin = self.provider.fetch().await?;
        let previous = self
            .rates
            .latest(bulletin.base())
            .await?
            .map(|table| table.date());
        let outcome = match self.rates.save(&bulletin, self.clock.now()).await? {
            SaveOutcome::LockedElsewhere => RefreshOutcome::SkippedLocked,
            SaveOutcome::Written if previous == Some(bulletin.date()) => RefreshOutcome::Unchanged,
            SaveOutcome::Written => RefreshOutcome::Updated,
        };
        Ok(RefreshReport {
            date: bulletin.date(),
            currencies: bulletin.currencies().len(),
            outcome,
        })
    }

    /// Refresca solo si hace falta, que es lo que se hace al arrancar.
    ///
    /// Hace falta si no hay ninguna tabla guardada o si la que hay lleva mas de un
    /// dia sin refrescarse. Sin esto, un despliegue en limpio se queda hasta la
    /// siguiente pasada sin poder convertir nada.
    ///
    /// # Errors
    ///
    /// Los mismos que [`RefreshExchangeRates::execute`].
    pub async fn execute_if_due(&self) -> Result<Option<RefreshReport>, AppError> {
        let due = self
            .rates
            .latest(&rates_base())
            .await?
            .is_none_or(|table| table.is_due_for_refresh(self.clock.now()));
        if due {
            self.execute().await.map(Some)
        } else {
            Ok(None)
        }
    }
}
