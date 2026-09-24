//! Caso de uso: comprobar que el servicio puede aceptar trafico.

use std::sync::Arc;
use std::time::Instant;

use gastonomo_domain::{ComponentHealth, HealthReport, Latency, ServiceVersion};

use crate::port::HealthProbe;

/// Coleccion de probes inyectada en el caso de uso.
pub type Probes = Vec<Arc<dyn HealthProbe>>;

/// Ejecuta todos los probes registrados y compone el informe de salud.
///
/// El caso de uso no sabe que hay detras de cada probe: solo mide cuanto tarda
/// y deja que el dominio decida si el resultado degrada el servicio.
#[derive(Clone)]
pub struct CheckReadiness {
    version: ServiceVersion,
    probes: Probes,
}

impl CheckReadiness {
    /// Crea el caso de uso con los probes que debe consultar.
    #[must_use]
    pub const fn new(version: ServiceVersion, probes: Probes) -> Self {
        Self { version, probes }
    }

    /// Consulta los probes en orden y devuelve el informe resultante.
    pub async fn execute(&self) -> HealthReport {
        let mut components = Vec::with_capacity(self.probes.len());
        for probe in &self.probes {
            components.push(Self::run(probe.as_ref()).await);
        }
        HealthReport::new(self.version, components)
    }

    async fn run(probe: &dyn HealthProbe) -> ComponentHealth {
        let started = Instant::now();
        let outcome = probe.check().await;
        let latency = Latency::from_duration(started.elapsed());
        let name = probe.name().clone();
        match outcome {
            Ok(()) => ComponentHealth::healthy(name, latency),
            Err(error) => ComponentHealth::failed(name, latency, error.message()),
        }
    }
}

impl std::fmt::Debug for CheckReadiness {
    fn fmt(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        formatter
            .debug_struct("CheckReadiness")
            .field("version", &self.version)
            .field("probes", &self.probes.len())
            .finish()
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

    use std::sync::Arc;

    use gastonomo_domain::{ComponentStatus, HealthStatus, ServiceVersion};

    use super::CheckReadiness;
    use crate::testing::{FakeHealthProbe, block_on};

    fn version() -> ServiceVersion {
        ServiceVersion::new(0, 1, 0)
    }

    #[test]
    fn a_readiness_check_without_probes_is_healthy() {
        let report = block_on(CheckReadiness::new(version(), Vec::new()).execute());
        assert_eq!(report.status(), HealthStatus::Ok);
        assert!(report.components().is_empty());
    }

    #[test]
    fn every_probe_produces_one_component_reading() {
        let use_case = CheckReadiness::new(
            version(),
            vec![
                Arc::new(FakeHealthProbe::healthy("database")),
                Arc::new(FakeHealthProbe::healthy("cache")),
            ],
        );
        let report = block_on(use_case.execute());
        assert_eq!(report.status(), HealthStatus::Ok);
        assert_eq!(report.components().len(), 2);
        let names: Vec<&str> = report
            .components()
            .iter()
            .map(|component| component.name().as_str())
            .collect();
        assert_eq!(names, vec!["database", "cache"]);
    }

    #[test]
    fn a_failing_probe_degrades_the_report_and_keeps_the_reason() {
        let use_case = CheckReadiness::new(
            version(),
            vec![
                Arc::new(FakeHealthProbe::healthy("cache")),
                Arc::new(FakeHealthProbe::failing("database", "connection refused")),
            ],
        );
        let report = block_on(use_case.execute());
        assert_eq!(report.status(), HealthStatus::Degraded);
        assert!(report.contains_status(ComponentStatus::Error));
        let failed = report
            .components()
            .iter()
            .find(|component| component.status() == ComponentStatus::Error)
            .expect("one component should have failed");
        assert_eq!(failed.detail(), Some("connection refused"));
        assert_eq!(failed.name().as_str(), "database");
    }

    #[test]
    fn each_probe_is_called_exactly_once() {
        let probe = Arc::new(FakeHealthProbe::healthy("database"));
        let use_case = CheckReadiness::new(version(), vec![probe.clone()]);
        block_on(use_case.execute());
        block_on(use_case.execute());
        assert_eq!(probe.calls(), 2);
    }

    #[test]
    fn debug_hides_the_probe_internals() {
        let use_case = CheckReadiness::new(
            version(),
            vec![Arc::new(FakeHealthProbe::healthy("database"))],
        );
        assert!(format!("{use_case:?}").contains("probes: 1"));
    }
}
