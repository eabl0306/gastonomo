//! Casos de uso de la aplicacion.
//!
//! Cada contexto tiene ya su modulo declarado para que nadie tenga que tocar este
//! fichero: `movement`, `loan`, `category`, `settings` y `currency`.
//!
//! `validation_message` es la excepcion: guarda los mensajes en castellano de las
//! invariantes de dominio que comparten varios contextos.

mod check_liveness;
mod check_readiness;
pub(crate) mod validation_message;

pub mod auth;
pub mod category;
pub mod changes;
pub mod currency;
pub mod dictation;
pub mod loan;
pub mod movement;
pub mod settings;

pub use check_liveness::CheckLiveness;
pub use check_readiness::CheckReadiness;
