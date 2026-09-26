//! Sub-router del contexto de monedas y tasas.
//!
//! Cuelga del router protegido de `/v1`. Los dos endpoints sirven datos globales,
//! asi que no hay nada que acotar por usuario: la tabla de tasas y el catalogo son
//! los mismos para todas las cuentas.

use std::sync::Arc;

use gastonomo_application::use_case::currency::{GetExchangeRates, ListCurrencies};
use salvo::prelude::Router;

use crate::handler::currency::{CurrencyListHandler, ExchangeRatesHandler};

/// Casos de uso que necesita el router de monedas y tasas.
#[derive(Debug, Clone)]
pub struct CurrencyDependencies {
    /// Catalogo de monedas.
    pub list_currencies: Arc<ListCurrencies>,
    /// Tasas vigentes.
    pub exchange_rates: Arc<GetExchangeRates>,
}

/// Monta las rutas de monedas y tasas.
#[must_use]
pub fn router(dependencies: CurrencyDependencies) -> Router {
    Router::new()
        .push(
            Router::with_path("currencies")
                .get(CurrencyListHandler::new(dependencies.list_currencies)),
        )
        .push(
            Router::with_path("exchange-rates")
                .get(ExchangeRatesHandler::new(dependencies.exchange_rates)),
        )
}
