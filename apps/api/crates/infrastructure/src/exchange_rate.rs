//! Tasas de cambio: fuente externa y tarea de refresco.
//!
//! Dos adaptadores que miran en direcciones opuestas:
//!
//! - [`FrankfurterExchangeRateProvider`] es driven: implementa el puerto
//!   `ExchangeRateProvider` llamando a la API publica de Frankfurter.
//! - [`ExchangeRateScheduler`] es driving, igual que el router HTTP: un bucle de
//!   tokio que despierta a sus horas y llama al caso de uso `RefreshExchangeRates`.
//!   Los reintentos con espera creciente viven aqui, no en el caso de uso.

mod frankfurter_payload;
mod frankfurter_provider;
mod rate_scheduler;
mod schedule;

pub use frankfurter_provider::{FrankfurterExchangeRateProvider, ProviderError};
pub use rate_scheduler::ExchangeRateScheduler;
pub use schedule::next_tick_after;
