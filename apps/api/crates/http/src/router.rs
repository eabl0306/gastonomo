//! Composicion del router HTTP.
//!
//! El arbol es este:
//!
//! ```text
//! /health/live            publico
//! /health/ready           publico
//! /openapi.json           publico
//! /v1/auth/...            publico (registro, login, refresh, logout, recuperacion)
//! /v1/...                 detras de RequireAccessToken
//! ```
//!
//! Para anadir un contexto no hay que tocar este fichero: cada sub-router vive en
//! `router/<contexto>.rs` y ya esta colgado. Lo unico que hace falta es rellenar su
//! `router(...)` y su struct de dependencias, y cablearlo en
//! `crates/api/src/bootstrap/<contexto>.rs`.

mod auth;
mod categories;
mod changes;
mod currencies;
mod dictation;
mod health;
mod loans;
mod movements;
mod settings;

use salvo::prelude::Router;

pub use auth::AuthDependencies;
pub use categories::CategoryDependencies;
pub use changes::ChangesDependencies;
pub use currencies::CurrencyDependencies;
pub use dictation::DictationDependencies;
pub use health::HealthDependencies;
pub use loans::LoanDependencies;
pub use movements::MovementDependencies;
pub use settings::SettingsDependencies;

use crate::auth_context::RequireAccessToken;
use crate::handler::{OpenApiHandler, SwaggerUiHandler};
use crate::middleware::RequestId;
use crate::openapi;

/// Todo lo que el adaptador HTTP necesita para responder.
///
/// El router no construye casos de uso ni adaptadores concretos: los recibe ya
/// montados desde el composition root.
#[derive(Debug, Clone)]
pub struct RouterDependencies {
    /// Endpoints de salud.
    pub health: HealthDependencies,
    /// Autenticacion.
    pub auth: AuthDependencies,
    /// Movimientos.
    pub movements: MovementDependencies,
    /// Prestamos.
    pub loans: LoanDependencies,
    /// Categorias.
    pub categories: CategoryDependencies,
    /// Ajustes.
    pub settings: SettingsDependencies,
    /// Monedas y tasas.
    pub currencies: CurrencyDependencies,
    /// Feed de cambios incrementales.
    pub changes: ChangesDependencies,
    /// Dictado de un registro.
    pub dictation: DictationDependencies,
}

/// Construye el router completo del servicio.
///
/// # Errors
///
/// Devuelve el error de `serde_json` si el documento OpenAPI no se puede
/// serializar, lo que indicaria un fallo al describir los DTOs.
pub fn build_router(dependencies: RouterDependencies) -> Result<Router, serde_json::Error> {
    let document = openapi::document_json(dependencies.health.version)?;
    Ok(Router::new()
        .hoop(RequestId::new())
        .push(health::router(dependencies.health.clone()))
        .push(Router::with_path("openapi.json").get(OpenApiHandler::new(document)))
        .push(Router::with_path("swagger-ui").get(SwaggerUiHandler::new()))
        .push(v1_router(dependencies)))
}

/// Monta `/v1`: lo publico de autenticacion y, detras del middleware, el resto.
fn v1_router(dependencies: RouterDependencies) -> Router {
    let guard = RequireAccessToken::new(
        dependencies.auth.access_tokens.clone(),
        dependencies.auth.clock.clone(),
    );
    Router::with_path("v1")
        .push(auth::public_router(&dependencies.auth))
        .push(
            Router::new()
                .hoop(guard)
                .push(auth::protected_router(&dependencies.auth))
                .push(movements::router(dependencies.movements))
                .push(loans::router(dependencies.loans))
                .push(categories::router(dependencies.categories))
                .push(settings::router(dependencies.settings))
                .push(currencies::router(dependencies.currencies))
                .push(changes::router(dependencies.changes))
                .push(dictation::router(dependencies.dictation)),
        )
}
