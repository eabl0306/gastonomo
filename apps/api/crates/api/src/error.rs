//! Errores de arranque del servicio.

use gastonomo_domain::ServiceVersionError;
use gastonomo_infrastructure::{ConfigError, MailError, PersistenceError};
use thiserror::Error;

/// Motivos por los que el servicio no llega a escuchar.
#[derive(Debug, Error)]
pub enum StartupError {
    /// La configuracion del entorno no es valida.
    #[error(transparent)]
    Config(#[from] ConfigError),
    /// La version del paquete no es una version semantica valida.
    #[error("invalid package version: {0}")]
    Version(#[from] ServiceVersionError),
    /// No se pudo preparar la persistencia.
    #[error(transparent)]
    Persistence(#[from] PersistenceError),
    /// La configuracion de correo no es valida.
    #[error(transparent)]
    Mail(#[from] MailError),
    /// El documento OpenAPI no se pudo serializar.
    #[error("could not build the OpenAPI document: {0}")]
    OpenApi(String),
    /// No se pudo abrir el puerto de escucha.
    #[error("could not bind {address}: {source}")]
    Bind {
        /// Direccion que se intento abrir.
        address: String,
        /// Error original del listener, en `Box` para no inflar el `Result`.
        #[source]
        source: Box<salvo::Error>,
    },
    /// No se pudo crear el runtime asincrono.
    #[error("could not start the async runtime: {0}")]
    Runtime(#[source] std::io::Error),
}
