//! Documentacion OpenAPI de los endpoints de salud.

use utoipa::openapi::path::{HttpMethod, OperationBuilder, PathItem, PathsBuilder};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt as _, ResponsesBuilder};

/// Etiqueta comun de los endpoints de salud.
const TAG: &str = "health";

/// Anade `/health/live` y `/health/ready`.
#[must_use]
pub fn paths(builder: PathsBuilder) -> PathsBuilder {
    builder
        .path("/health/live", liveness())
        .path("/health/ready", readiness())
}

fn liveness() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response("200", health_response("El proceso esta vivo."))
        .build();
    PathItem::new(
        HttpMethod::Get,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("checkLiveness"))
            .summary(Some("Liveness del proceso"))
            .description(Some(
                "No consulta dependencias externas. Siempre responde 200 mientras el \
                 proceso atienda peticiones.",
            ))
            .responses(responses)
            .build(),
    )
}

fn readiness() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response("200", health_response("El servicio acepta trafico."))
        .response("503", health_response("Algun componente esta degradado."))
        .build();
    PathItem::new(
        HttpMethod::Get,
        OperationBuilder::new()
            .tag(TAG)
            .operation_id(Some("checkReadiness"))
            .summary(Some("Readiness del servicio"))
            .description(Some(
                "Comprueba cada componente registrado. Devuelve 503 en cuanto uno falla.",
            ))
            .responses(responses)
            .build(),
    )
}

fn health_response(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("HealthResponseDto")
}
