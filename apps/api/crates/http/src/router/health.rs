//! Sub-router de `/health`.

use std::sync::Arc;

use gastonomo_application::{CheckLiveness, CheckReadiness};
use gastonomo_domain::ServiceVersion;
use salvo::prelude::Router;

use crate::handler::{LivenessHandler, ReadinessHandler};

/// Casos de uso que necesita el router de salud.
#[derive(Debug, Clone)]
pub struct HealthDependencies {
    /// Version del servicio, que viaja en cada respuesta y en el OpenAPI.
    pub version: ServiceVersion,
    /// Caso de uso de liveness.
    pub liveness: CheckLiveness,
    /// Caso de uso de readiness.
    pub readiness: Arc<CheckReadiness>,
}

/// Monta `/health/live` y `/health/ready`.
#[must_use]
pub fn router(dependencies: HealthDependencies) -> Router {
    Router::with_path("health")
        .push(Router::with_path("live").get(LivenessHandler::new(dependencies.liveness)))
        .push(Router::with_path("ready").get(ReadinessHandler::new(dependencies.readiness)))
}
