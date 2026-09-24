//! DTO de respuesta de los endpoints de salud.

use gastonomo_domain::{ComponentHealth, HealthReport};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// Lectura de un componente tal y como la ve un cliente.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ComponentCheckDto {
    /// Nombre del componente comprobado.
    #[schema(example = "database")]
    pub name: String,
    /// Estado del componente: `ok` o `error`.
    #[schema(example = "ok")]
    pub status: String,
    /// Latencia de la comprobacion en milisegundos.
    #[schema(example = 3)]
    pub latency_ms: u64,
    /// Motivo del fallo, presente solo cuando el estado es `error`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
}

impl From<&ComponentHealth> for ComponentCheckDto {
    fn from(component: &ComponentHealth) -> Self {
        Self {
            name: component.name().as_str().to_owned(),
            status: component.status().as_str().to_owned(),
            latency_ms: component.latency().as_millis(),
            detail: component.detail().map(ToOwned::to_owned),
        }
    }
}

/// Respuesta de `/health/live` y `/health/ready`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct HealthResponseDto {
    /// Estado global: `ok` o `degraded`.
    #[schema(example = "ok")]
    pub status: String,
    /// Lecturas de cada componente comprobado.
    pub checks: Vec<ComponentCheckDto>,
    /// Version del servicio que responde.
    #[schema(example = "0.1.0")]
    pub version: String,
}

impl From<&HealthReport> for HealthResponseDto {
    fn from(report: &HealthReport) -> Self {
        Self {
            status: report.status().as_str().to_owned(),
            checks: report.components().iter().map(Into::into).collect(),
            version: report.version().to_string(),
        }
    }
}

#[cfg(test)]
mod tests {
    #![allow(
        clippy::unwrap_used,
        clippy::expect_used,
        clippy::indexing_slicing,
        reason = "tests fail loudly on purpose; a panic here is the assertion"
    )]

    use gastonomo_domain::{ComponentHealth, ComponentName, HealthReport, Latency, ServiceVersion};

    use super::HealthResponseDto;

    fn name(raw: &str) -> ComponentName {
        ComponentName::parse(raw).expect("fixture name should be valid")
    }

    #[test]
    fn maps_a_healthy_report_to_the_public_contract() {
        let report = HealthReport::new(
            ServiceVersion::new(0, 1, 0),
            vec![ComponentHealth::healthy(
                name("database"),
                Latency::from_millis(4),
            )],
        );
        let dto = HealthResponseDto::from(&report);
        let json = serde_json::to_value(&dto).expect("dto should serialize");
        assert_eq!(json["status"], "ok");
        assert_eq!(json["version"], "0.1.0");
        assert_eq!(json["checks"][0]["name"], "database");
        assert_eq!(json["checks"][0]["status"], "ok");
        assert_eq!(json["checks"][0]["latencyMs"], 4);
        assert!(json["checks"][0].get("detail").is_none());
    }

    #[test]
    fn maps_a_degraded_report_and_keeps_the_detail() {
        let report = HealthReport::new(
            ServiceVersion::new(1, 0, 0),
            vec![ComponentHealth::failed(
                name("database"),
                Latency::from_millis(120),
                "connection refused",
            )],
        );
        let json =
            serde_json::to_value(HealthResponseDto::from(&report)).expect("dto should serialize");
        assert_eq!(json["status"], "degraded");
        assert_eq!(json["checks"][0]["status"], "error");
        assert_eq!(json["checks"][0]["detail"], "connection refused");
    }

    #[test]
    fn a_report_without_components_serializes_an_empty_array() {
        let report = HealthReport::without_components(ServiceVersion::new(2, 3, 4));
        let json =
            serde_json::to_value(HealthResponseDto::from(&report)).expect("dto should serialize");
        assert_eq!(json["status"], "ok");
        assert_eq!(json["version"], "2.3.4");
        assert_eq!(json["checks"].as_array().map(Vec::len), Some(0));
    }
}
