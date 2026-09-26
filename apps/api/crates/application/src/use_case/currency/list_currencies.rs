//! Caso de uso: catalogo de monedas que se ofrecen.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::currency::Currency;

use crate::error::AppError;
use crate::port::currency::ExchangeRateRepository;

/// Devuelve las monedas del sheet de seleccion.
///
/// El filtro (fuera metales y XDR, fuera monedas sin tasa) ya esta aplicado en lo
/// que se guarda, asi que aqui no se repite: el cliente pinta lo que llega.
#[derive(Clone)]
pub struct ListCurrencies {
    rates: Arc<dyn ExchangeRateRepository>,
}

impl fmt::Debug for ListCurrencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ListCurrencies").finish_non_exhaustive()
    }
}

impl ListCurrencies {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(rates: Arc<dyn ExchangeRateRepository>) -> Self {
        Self { rates }
    }

    /// Lee el catalogo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(&self) -> Result<Vec<Currency>, AppError> {
        self.rates.offered_currencies().await
    }
}
