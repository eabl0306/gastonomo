//! Doble in-memory del puerto de salud.

use std::sync::atomic::{AtomicUsize, Ordering};

use async_trait::async_trait;
use gastonomo_domain::ComponentName;

use crate::port::{HealthProbe, ProbeError};

/// Doble in-memory del puerto [`HealthProbe`].
#[derive(Debug)]
pub struct FakeHealthProbe {
    name: ComponentName,
    failure: Option<String>,
    calls: AtomicUsize,
}

impl FakeHealthProbe {
    /// Crea un probe que siempre responde bien.
    ///
    /// # Panics
    ///
    /// Entra en panico si `name` no es un nombre de componente valido; en un
    /// doble de test eso es siempre un error del propio test.
    #[must_use]
    pub fn healthy(name: &str) -> Self {
        Self::build(name, None)
    }

    /// Crea un probe que siempre falla con el motivo indicado.
    ///
    /// # Panics
    ///
    /// Entra en panico si `name` no es un nombre de componente valido.
    #[must_use]
    pub fn failing(name: &str, reason: &str) -> Self {
        Self::build(name, Some(reason.to_owned()))
    }

    /// Numero de veces que se ha invocado [`HealthProbe::check`].
    #[must_use]
    pub fn calls(&self) -> usize {
        self.calls.load(Ordering::Relaxed)
    }

    fn build(name: &str, failure: Option<String>) -> Self {
        let name = match ComponentName::parse(name) {
            Ok(name) => name,
            Err(error) => unreachable!("invalid fixture component name {name:?}: {error}"),
        };
        Self {
            name,
            failure,
            calls: AtomicUsize::new(0),
        }
    }
}

#[async_trait]
impl HealthProbe for FakeHealthProbe {
    fn name(&self) -> &ComponentName {
        &self.name
    }

    async fn check(&self) -> Result<(), ProbeError> {
        self.calls.fetch_add(1, Ordering::Relaxed);
        self.failure
            .as_ref()
            .map_or(Ok(()), |reason| Err(ProbeError::new(reason.clone())))
    }
}

#[cfg(test)]
mod tests {
    use super::FakeHealthProbe;
    use crate::port::HealthProbe;
    use crate::testing::block_on;

    #[test]
    fn a_healthy_fake_reports_success_and_counts_calls() {
        let probe = FakeHealthProbe::healthy("database");
        assert!(block_on(probe.check()).is_ok());
        assert_eq!(probe.calls(), 1);
        assert_eq!(probe.name().as_str(), "database");
    }

    #[test]
    fn a_failing_fake_reports_the_configured_reason() {
        let probe = FakeHealthProbe::failing("database", "boom");
        let error = block_on(probe.check());
        assert_eq!(
            error.map_err(|e| e.message().to_owned()),
            Err("boom".into())
        );
    }
}
