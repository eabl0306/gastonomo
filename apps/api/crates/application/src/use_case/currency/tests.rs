//! Tests de los casos de uso de monedas y tasas.
//!
//! Todos usan los dobles de [`crate::testing`]: sin red y sin base de datos. El
//! proveedor de tasas es un stub, asi que el refresco se prueba entero sin llamar
//! a Frankfurter ni esperar doce horas.

mod read_rates;
mod refresh_rates;
mod support;
