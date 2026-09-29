//! Adaptador driving HTTP de Gastonomo.
//!
//! Traduce peticiones HTTP a casos de uso y respuestas de dominio a DTOs. El
//! dominio nunca se serializa directamente: entre el agregado y el JSON siempre
//! hay un DTO explicito, para que el contrato publico no cambie sin querer.
//!
//! Tres piezas se comparten entre contextos y conviene conocerlas antes de anadir
//! un endpoint:
//!
//! - [`auth_context::current_user`] devuelve el `UserId` del token.
//! - [`problem::render_problem`] escribe cualquier error en `problem+json`.
//! - [`serialization`] fija las convenciones del JSON.
//!
//! El punto de entrada es [`build_service`], no [`build_router`]: el servicio
//! lleva enganchado el catcher que mantiene en JSON tambien los 404 y 405 que
//! genera el transporte. Quien monte solo el router se queda sin eso.

pub mod auth_context;
pub mod catcher;
pub mod dto;
pub mod handler;
pub mod idempotency;
pub mod middleware;
pub mod openapi;
pub mod problem;
pub mod rate_limit;
pub mod router;
pub mod serialization;

pub use auth_context::{AuthenticatedUser, RequireAccessToken, current_user};
pub use catcher::ProblemCatcher;
pub use idempotency::{IDEMPOTENCY_HEADER, Idempotent, REPLAY_HEADER};
pub use problem::{
    PROBLEM_CONTENT_TYPE, ProblemDto, problem_for, problem_for_status, render_problem,
};
pub use rate_limit::RateLimit;
pub use router::{
    AuthDependencies, CategoryDependencies, ChangesDependencies, CurrencyDependencies,
    DictationDependencies, HealthDependencies, LoanDependencies, MovementDependencies,
    RouterDependencies, SettingsDependencies, build_router,
};

/// Monta el servicio HTTP completo: el router mas el catcher del contrato.
///
/// Es lo que arranca el binario y lo que montan los tests, para que los dos vean
/// exactamente las mismas respuestas de error.
///
/// # Errors
///
/// Devuelve el error de `serde_json` si el documento OpenAPI no se puede
/// serializar, lo que indicaria un fallo al describir los DTOs.
pub fn build_service(
    dependencies: RouterDependencies,
) -> Result<salvo::Service, serde_json::Error> {
    let router = build_router(dependencies)?;
    Ok(salvo::Service::new(router).catcher(ProblemCatcher::catcher()))
}

// Solo lo usan los tests de integracion; el lint de dependencias sin usar no
// mira los targets de test, asi que se declara aqui de forma explicita.
#[cfg(test)]
use tokio as _;
