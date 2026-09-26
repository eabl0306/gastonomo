//! Cableado de los endpoints de salud.

use std::sync::Arc;
use std::time::Duration;

use gastonomo_application::{CheckLiveness, CheckReadiness, HealthProbe};
use gastonomo_http::HealthDependencies;
use gastonomo_infrastructure::PostgresHealthProbe;

use super::wiring::Wiring;

/// Tiempo maximo que se le concede a PostgreSQL para contestar al probe.
const PROBE_TIMEOUT: Duration = Duration::from_secs(2);

/// Monta los casos de uso de salud.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> HealthDependencies {
    let probes: Vec<Arc<dyn HealthProbe>> = vec![Arc::new(PostgresHealthProbe::new(
        wiring.pool.clone(),
        PROBE_TIMEOUT,
    ))];
    HealthDependencies {
        version: wiring.version,
        liveness: CheckLiveness::new(wiring.version),
        readiness: Arc::new(CheckReadiness::new(wiring.version, probes)),
    }
}
