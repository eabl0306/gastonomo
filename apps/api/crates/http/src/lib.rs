//! Adaptador driving HTTP de Gastonomo.
//!
//! Traduce peticiones HTTP a casos de uso y respuestas de dominio a DTOs. El
//! dominio nunca se serializa directamente: entre el agregado y el JSON siempre
//! hay un DTO explicito, para que el contrato publico no cambie sin querer.

pub mod dto;
pub mod handler;
pub mod middleware;
pub mod openapi;
pub mod router;

pub use router::{HealthRouterDependencies, build_router};

// Solo lo usan los tests de integracion; el lint de dependencias sin usar no
// mira los targets de test, asi que se declara aqui de forma explicita.
#[cfg(test)]
use tokio as _;
