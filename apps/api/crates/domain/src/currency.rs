//! Monedas y tasas de cambio.
//!
//! Cuatro piezas: el catalogo que se ofrece en el sheet de moneda
//! ([`Currency`]), una tasa exacta ([`ExchangeRate`]), la pasada completa de la
//! fuente ([`RateBulletin`]) y la tabla vigente que sirve
//! `GET /v1/exchange-rates` ([`ExchangeRateTable`]).
//!
//! Las tasas las publica Frankfurter con base USD y las guarda la tarea de
//! refresco de la API: los clientes nunca llaman a Frankfurter. Aqui no hay nada
//! de red ni de SQL, solo las reglas: que monedas se ofrecen, cuando una tabla
//! esta rancia y cuando toca refrescarla.
//!
//! Las piezas compartidas ya existen: [`crate::value_object::CurrencyCode`] y
//! [`crate::value_object::ZERO_DECIMAL_CURRENCIES`].

mod bulletin;
mod catalog;
mod exchange_rate;
mod rate_table;

pub use bulletin::{BulletinError, RateBulletin};
pub use catalog::{Currency, CurrencyError, EXCLUDED_CURRENCIES, is_excluded};
pub use exchange_rate::{ExchangeRate, ExchangeRateError, RATE_SCALE};
pub use rate_table::{
    ExchangeRateTable, RATES_BASE, REFRESH_AFTER_HOURS, RateStatus, STALE_AFTER_HOURS, rates_base,
};
