//! Documento OpenAPI construido con los builders de utoipa.
//!
//! Se construye a mano en vez de anotar los handlers porque los handlers son
//! structs con dependencias inyectadas, no funciones libres. A cambio, el
//! documento vive junto al router y se testea igual que cualquier otro codigo.
//!
//! Cada contexto anade sus rutas en `openapi/<contexto>_paths.rs`, con dos
//! funciones: `paths(PathsBuilder) -> PathsBuilder` y
//! `schemas(ComponentsBuilder) -> ComponentsBuilder`. Asi nadie se pisa aqui.

mod auth_paths;
mod category_paths;
mod changes_paths;
mod currency_paths;
mod dictation_paths;
mod health_paths;
mod idempotency_docs;
mod loan_paths;
mod movement_paths;
mod settings_paths;

use gastonomo_domain::ServiceVersion;
use utoipa::openapi::path::PathsBuilder;
use utoipa::openapi::{Components, ComponentsBuilder, InfoBuilder, OpenApi, OpenApiBuilder, Paths};

use crate::dto::{ComponentCheckDto, HealthResponseDto};

/// Construye el documento OpenAPI del servicio.
#[must_use]
pub fn document(version: ServiceVersion) -> OpenApi {
    OpenApiBuilder::new()
        .info(
            InfoBuilder::new()
                .title("Gastonomo API")
                .version(version.to_string())
                .description(Some(
                    "Endpoints de salud y de autenticacion. Los importes viajan como \
                     string y las fechas de registro como YYYY-MM-DD. Los errores \
                     usan application/problem+json.",
                ))
                .build(),
        )
        .paths(paths())
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

fn paths() -> Paths {
    let builder = PathsBuilder::new();
    let builder = health_paths::paths(builder);
    let builder = auth_paths::paths(builder);
    let builder = movement_paths::paths(builder);
    let builder = loan_paths::paths(builder);
    let builder = category_paths::paths(builder);
    let builder = settings_paths::paths(builder);
    let builder = changes_paths::paths(builder);
    let builder = dictation_paths::paths(builder);
    currency_paths::paths(builder).build()
}

fn components() -> Components {
    let builder = ComponentsBuilder::new()
        .schema_from::<HealthResponseDto>()
        .schema_from::<ComponentCheckDto>();
    let builder = auth_paths::schemas(builder);
    let builder = movement_paths::schemas(builder);
    let builder = loan_paths::schemas(builder);
    let builder = category_paths::schemas(builder);
    let builder = settings_paths::schemas(builder);
    let builder = changes_paths::schemas(builder);
    let builder = dictation_paths::schemas(builder);
    currency_paths::schemas(builder).build()
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
