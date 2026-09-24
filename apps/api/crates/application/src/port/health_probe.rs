//! Puerto driven para comprobar la salud de una dependencia externa.

use async_trait::async_trait;
use gastonomo_domain::ComponentName;
use thiserror::Error;

/// Fallo devuelto por un probe cuando la dependencia no responde.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
#[error("{message}")]
pub struct ProbeError {
    message: String,
}

impl ProbeError {
    /// Crea un fallo con el motivo observado por el adaptador.
    #[must_use]
    pub fn new(message: impl Into<String>) -> Self {
        Self {
            message: message.into(),
        }
    }

    /// Motivo del fallo.
    #[must_use]
    pub fn message(&self) -> &str {
        &self.message
    }
}

/// Comprueba que una dependencia externa responde.
///
/// El adaptador solo decide si la dependencia contesta; la latencia la mide el
/// caso de uso y la regla de negocio la aplica el dominio.
#[async_trait]
pub trait HealthProbe: Send + Sync {
    /// Nombre del componente que este probe observa.
    fn name(&self) -> &ComponentName;

    /// Comprueba la dependencia.
    ///
    /// # Errors
    ///
    /// Devuelve [`ProbeError`] si la dependencia no responde o responde mal.
    async fn check(&self) -> Result<(), ProbeError>;
}

#[cfg(test)]
mod tests {
    use super::ProbeError;

    #[test]
    fn keeps_and_renders_the_failure_reason() {
        let error = ProbeError::new("connection refused");
        assert_eq!(error.message(), "connection refused");
        assert_eq!(error.to_string(), "connection refused");
    }
}
