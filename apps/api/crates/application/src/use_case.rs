//! Casos de uso de la aplicacion.

mod check_liveness;
mod check_readiness;

pub use check_liveness::CheckLiveness;
pub use check_readiness::CheckReadiness;
