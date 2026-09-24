//! Documento OpenAPI construido con los builders de utoipa.
//!
//! Se construye a mano en vez de anotar los handlers porque los handlers son
//! structs con dependencias inyectadas, no funciones libres. A cambio, el
//! documento vive junto al router y se testea igual que cualquier otro codigo.

use gastonomo_domain::ServiceVersion;
use utoipa::openapi::path::{HttpMethod, OperationBuilder, PathItem, PathsBuilder};
use utoipa::openapi::response::{ResponseBuilder, ResponseExt, ResponsesBuilder};
use utoipa::openapi::{Components, InfoBuilder, OpenApi, OpenApiBuilder};

use crate::dto::{ComponentCheckDto, HealthResponseDto};

/// Etiqueta comun de los endpoints de salud.
const TAG: &str = "health";

/// Construye el documento OpenAPI del servicio.
#[must_use]
pub fn document(version: ServiceVersion) -> OpenApi {
    OpenApiBuilder::new()
        .info(
            InfoBuilder::new()
                .title("Gastonomo API")
                .version(version.to_string())
                .description(Some(
                    "Endpoints de salud del servicio. Liveness no toca dependencias; \
                     readiness comprueba PostgreSQL a traves de un puerto.",
                ))
                .build(),
        )
        .paths(
            PathsBuilder::new()
                .path("/health/live", liveness_path())
                .path("/health/ready", readiness_path())
                .build(),
        )
        .components(Some(components()))
        .build()
}

/// Serializa el documento a JSON.
///
/// # Errors
///
/// Devuelve el error de `serde_json` si el documento no se puede serializar.
pub fn document_json(version: ServiceVersion) -> Result<String, serde_json::Error> {
    serde_json::to_string_pretty(&document(version))
}

fn components() -> Components {
    utoipa::openapi::ComponentsBuilder::new()
        .schema_from::<HealthResponseDto>()
        .schema_from::<ComponentCheckDto>()
        .build()
}

fn liveness_path() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response("200", healthy_response("El proceso esta vivo."))
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

fn readiness_path() -> PathItem {
    let responses = ResponsesBuilder::new()
        .response("200", healthy_response("El servicio acepta trafico."))
        .response("503", healthy_response("Algun componente esta degradado."))
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

fn healthy_response(description: &str) -> ResponseBuilder {
    ResponseBuilder::new()
        .description(description)
        .json_schema_ref("HealthResponseDto")
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_domain::ServiceVersion;

    use super::document_json;

    fn json() -> serde_json::Value {
        let raw = document_json(ServiceVersion::new(0, 1, 0)).expect("document should serialize");
        serde_json::from_str(&raw).expect("document should be valid json")
    }

    #[test]
    fn documents_both_health_endpoints() {
        let doc = json();
        assert!(doc["paths"]["/health/live"]["get"].is_object());
        assert!(doc["paths"]["/health/ready"]["get"].is_object());
    }

    #[test]
    fn readiness_documents_the_degraded_status_code() {
        let doc = json();
        assert!(doc["paths"]["/health/ready"]["get"]["responses"]["503"].is_object());
    }

    #[test]
    fn exposes_the_response_schemas() {
        let doc = json();
        assert!(doc["components"]["schemas"]["HealthResponseDto"].is_object());
        assert!(doc["components"]["schemas"]["ComponentCheckDto"].is_object());
    }

    #[test]
    fn carries_the_service_version() {
        assert_eq!(json()["info"]["version"], "0.1.0");
    }
}
