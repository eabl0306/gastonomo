//! Caso de uso: comprobar que el proceso esta vivo.

use gastonomo_domain::{HealthReport, ServiceVersion};

/// Responde si el proceso esta en marcha, sin tocar ninguna dependencia.
///
/// Liveness nunca consulta componentes externos: si un orquestador reinicia el
/// contenedor porque Postgres esta caido, el problema solo empeora.
#[derive(Debug, Clone, Copy)]
pub struct CheckLiveness {
    version: ServiceVersion,
}

impl CheckLiveness {
    /// Crea el caso de uso para una version del servicio.
    #[must_use]
    pub const fn new(version: ServiceVersion) -> Self {
        Self { version }
    }

    /// Devuelve un informe siempre sano.
    #[must_use]
    pub const fn execute(self) -> HealthReport {
        HealthReport::without_components(self.version)
    }
}

#[cfg(test)]
mod tests {
    use gastonomo_domain::{HealthStatus, ServiceVersion};

    use super::CheckLiveness;

    #[test]
    fn liveness_is_always_healthy_and_has_no_components() {
        let report = CheckLiveness::new(ServiceVersion::new(1, 2, 3)).execute();
        assert_eq!(report.status(), HealthStatus::Ok);
        assert!(report.components().is_empty());
        assert_eq!(report.version(), ServiceVersion::new(1, 2, 3));
    }
}
