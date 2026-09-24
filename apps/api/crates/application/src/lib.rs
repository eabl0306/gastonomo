//! Capa de aplicacion de Gastonomo.
//!
//! Declara los puertos que el nucleo necesita y los casos de uso que los
//! orquestan. No conoce Salvo, SQLx ni ningun runtime asincrono concreto: solo
//! `async-trait` para poder usar los puertos como objetos de trait.

pub mod port;
pub mod use_case;

#[cfg(any(test, feature = "testing"))]
pub mod testing;

pub use port::{HealthProbe, ProbeError};
pub use use_case::{CheckLiveness, CheckReadiness};
