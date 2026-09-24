//! Composicion del router HTTP.

use std::sync::Arc;

use gastonomo_application::{CheckLiveness, CheckReadiness};
use gastonomo_domain::ServiceVersion;
use salvo::prelude::Router;

use crate::handler::{LivenessHandler, OpenApiHandler, ReadinessHandler, SwaggerUiHandler};
use crate::middleware::RequestId;
use crate::openapi;

/// Casos de uso que el router necesita para responder.
///
/// El adaptador HTTP no construye casos de uso ni adaptadores concretos: los
/// recibe ya montados desde el composition root.
#[derive(Debug, Clone)]
pub struct HealthRouterDependencies {
    /// Version del servicio, que viaja en cada respuesta y en el OpenAPI.
    pub version: ServiceVersion,
    /// Caso de uso de liveness.
    pub liveness: CheckLiveness,
    /// Caso de uso de readiness.
    pub readiness: Arc<CheckReadiness>,
}

/// Construye el router completo del servicio.
///
/// # Errors
///
/// Devuelve el error de `serde_json` si el documento OpenAPI no se puede
/// serializar, lo que indicaria un fallo al describir los DTOs.
pub fn build_router(dependencies: HealthRouterDependencies) -> Result<Router, serde_json::Error> {
    let document = openapi::document_json(dependencies.version)?;
    Ok(Router::new()
        .hoop(RequestId::new())
        .push(health_router(dependencies))
        .push(Router::with_path("openapi.json").get(OpenApiHandler::new(document)))
        .push(Router::with_path("swagger-ui").get(SwaggerUiHandler::new())))
}

fn health_router(dependencies: HealthRouterDependencies) -> Router {
    Router::with_path("health")
        .push(Router::with_path("live").get(LivenessHandler::new(dependencies.liveness)))
        .push(Router::with_path("ready").get(ReadinessHandler::new(dependencies.readiness)))
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use std::sync::Arc;

    use gastonomo_application::{CheckLiveness, CheckReadiness};
    use gastonomo_domain::ServiceVersion;

    use super::{HealthRouterDependencies, build_router};

    #[test]
    fn builds_a_router_from_its_dependencies() {
        let version = ServiceVersion::new(0, 1, 0);
        let dependencies = HealthRouterDependencies {
            version,
            liveness: CheckLiveness::new(version),
            readiness: Arc::new(CheckReadiness::new(version, Vec::new())),
        };
        assert!(build_router(dependencies).is_ok());
    }
}
