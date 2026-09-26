//! Casos de uso del contexto de monedas y tasas.
//!
//! Dos de lectura, que sirven `/v1/currencies` y `/v1/exchange-rates`, y uno de
//! escritura, [`RefreshExchangeRates`], que es el que llama la tarea programada.
//!
//! El refresco no sabe cada cuanto se le llama ni cuantas veces se reintenta: eso
//! lo decide el planificador, que es un adaptador driving. Aqui solo esta la
//! regla de que hacer con una pasada de la fuente.

#[cfg(test)]
mod tests;

mod get_exchange_rates;
mod list_currencies;
mod refresh_exchange_rates;

pub use get_exchange_rates::{ExchangeRatesView, GetExchangeRates};
pub use list_currencies::ListCurrencies;
pub use refresh_exchange_rates::{RefreshExchangeRates, RefreshOutcome, RefreshReport};
