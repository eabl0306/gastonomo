//! Cableado del contexto de monedas y tasas.
//!
//! Aqui se instancian el repositorio de PostgreSQL, el proveedor de Frankfurter y
//! el planificador. El planificador es un adaptador driving, como el router: se
//! arranca desde [`crate::bootstrap`] con [`start_refresh_task`], no desde el caso
//! de uso.

use std::sync::Arc;

use gastonomo_application::use_case::currency::{
    GetExchangeRates, ListCurrencies, RefreshExchangeRates,
};
use gastonomo_http::CurrencyDependencies;
use gastonomo_infrastructure::{
    ExchangeRateScheduler, FrankfurterExchangeRateProvider, PostgresExchangeRateRepository,
};

use super::wiring::Wiring;

/// Monta los casos de uso de monedas y tasas.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> CurrencyDependencies {
    let rates = Arc::new(PostgresExchangeRateRepository::new(wiring.pool.clone()));
    CurrencyDependencies {
        list_currencies: Arc::new(ListCurrencies::new(rates.clone())),
        exchange_rates: Arc::new(GetExchangeRates::new(rates, wiring.clock.clone())),
    }
}

/// Arranca la tarea de refresco de tasas.
///
/// Si el proveedor no se puede construir, la API arranca igual y sirve lo que haya
/// guardado: quedarse sin refresco es peor que quedarse sin API, pero no tanto como
/// no arrancar.
pub fn start_refresh_task(wiring: &Wiring) {
    let config = wiring.config.exchange_rates.clone();
    let provider = match FrankfurterExchangeRateProvider::new(config.clone()) {
        Ok(provider) => Arc::new(provider),
        Err(error) => {
            tracing::error!(%error, "could not build the exchange rate source client");
            return;
        }
    };
    let refresh = Arc::new(RefreshExchangeRates::new(
        provider,
        Arc::new(PostgresExchangeRateRepository::new(wiring.pool.clone())),
        wiring.clock.clone(),
    ));
    // El handle se suelta a proposito: la tarea vive lo que viva el proceso y no
    // hay nada que esperar de ella.
    drop(ExchangeRateScheduler::new(refresh, config).spawn());
}
