//! Agregado que decide si el servicio esta sano.

use crate::health::component_health::{ComponentHealth, ComponentStatus};
use crate::value_object::{ComponentName, ServiceVersion};

/// Estado global del servicio tal y como lo expone el contrato publico.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum HealthStatus {
    /// Todos los componentes comprobados responden.
    Ok,
    /// Al menos un componente comprobado esta caido.
    Degraded,
}

impl HealthStatus {
    /// Indica si el servicio puede aceptar trafico.
    #[must_use]
    pub const fn is_ok(self) -> bool {
        matches!(self, Self::Ok)
    }

    /// Representacion estable para el contrato publico.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Ok => "ok",
            Self::Degraded => "degraded",
        }
    }
}

impl std::fmt::Display for HealthStatus {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter.write_str(self.as_str())
    }
}

/// Informe de salud del servicio en un instante dado.
///
/// La regla de negocio vive aqui: el informe esta degradado en cuanto un solo
/// componente falla. Un informe sin componentes (liveness) siempre esta sano,
/// porque no depende de nada externo.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct HealthReport {
    version: ServiceVersion,
    components: Vec<ComponentHealth>,
}

impl HealthReport {
    /// Construye un informe a partir de las lecturas de sus componentes.
    #[must_use]
    pub const fn new(version: ServiceVersion, components: Vec<ComponentHealth>) -> Self {
        Self {
            version,
            components,
        }
    }

    /// Construye un informe sin dependencias externas.
    #[must_use]
    pub const fn without_components(version: ServiceVersion) -> Self {
        Self {
            version,
            components: Vec::new(),
        }
    }

    /// Version del servicio que emitio el informe.
    #[must_use]
    pub const fn version(&self) -> ServiceVersion {
        self.version
    }

    /// Lecturas de los componentes comprobados.
    #[must_use]
    pub fn components(&self) -> &[ComponentHealth] {
        &self.components
    }

    /// Estado global derivado de las lecturas.
    #[must_use]
    pub fn status(&self) -> HealthStatus {
        if self
            .components
            .iter()
            .all(|component| component.status().is_ok())
        {
            HealthStatus::Ok
        } else {
            HealthStatus::Degraded
        }
    }

    /// Atajo para saber si el servicio esta sano.
    #[must_use]
    pub fn is_healthy(&self) -> bool {
        self.status().is_ok()
    }

    /// Nombres de los componentes que estan fallando.
    #[must_use]
    pub fn failing_components(&self) -> Vec<&ComponentName> {
        self.components
            .iter()
            .filter(|component| !component.status().is_ok())
            .map(ComponentHealth::name)
            .collect()
    }

    /// Indica si el informe contiene una lectura con ese estado.
    #[must_use]
    pub fn contains_status(&self, status: ComponentStatus) -> bool {
        self.components
            .iter()
            .any(|component| component.status() == status)
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

    use super::{HealthReport, HealthStatus};
    use crate::health::{ComponentHealth, ComponentStatus};
    use crate::value_object::{ComponentName, Latency, ServiceVersion};

    fn name(raw: &str) -> ComponentName {
        ComponentName::parse(raw).expect("fixture name should be valid")
    }

    fn version() -> ServiceVersion {
        ServiceVersion::new(0, 1, 0)
    }

    #[test]
    fn a_report_without_components_is_healthy() {
        let report = HealthReport::without_components(version());
        assert_eq!(report.status(), HealthStatus::Ok);
        assert!(report.is_healthy());
        assert!(report.components().is_empty());
        assert_eq!(report.version(), version());
    }

    #[test]
    fn all_components_ok_keeps_the_report_healthy() {
        let report = HealthReport::new(
            version(),
            vec![
                ComponentHealth::healthy(name("database"), Latency::from_millis(3)),
                ComponentHealth::healthy(name("cache"), Latency::from_millis(1)),
            ],
        );
        assert_eq!(report.status(), HealthStatus::Ok);
        assert!(report.failing_components().is_empty());
    }

    #[test]
    fn a_single_failing_component_degrades_the_whole_report() {
        let report = HealthReport::new(
            version(),
            vec![
                ComponentHealth::healthy(name("cache"), Latency::from_millis(1)),
                ComponentHealth::failed(name("database"), Latency::from_millis(50), "timeout"),
            ],
        );
        assert_eq!(report.status(), HealthStatus::Degraded);
        assert!(!report.is_healthy());
        assert!(report.contains_status(ComponentStatus::Error));
        let failing = report.failing_components();
        assert_eq!(failing.len(), 1);
        assert_eq!(failing.first().map(|c| c.as_str()), Some("database"));
    }

    #[test]
    fn status_renders_the_public_contract_values() {
        assert_eq!(HealthStatus::Ok.to_string(), "ok");
        assert_eq!(HealthStatus::Degraded.to_string(), "degraded");
        assert!(!HealthStatus::Degraded.is_ok());
    }
}
