//! Adaptadores de correo.
//!
//! Hay dos y se eligen por configuracion: SMTP de verdad y uno que escribe el
//! mensaje en el log. En desarrollo la pila de Docker trae Mailpit, asi que ahi se
//! usa SMTP contra `mailpit:1025` y los correos se ven en http://localhost:8025.

mod logging_mailer;
mod smtp_mailer;

use std::sync::Arc;

use gastonomo_application::port::Mailer;
use thiserror::Error;

pub use logging_mailer::LoggingMailer;
pub use smtp_mailer::SmtpMailer;

use crate::config::{MailConfig, MailDriver};

/// Fallo al montar el adaptador de correo.
#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum MailError {
    /// El remitente configurado no es una direccion valida.
    #[error("{0:?} is not a valid sender mailbox")]
    InvalidSender(String),
    /// El transporte SMTP no se pudo construir.
    #[error("could not build the smtp transport: {0}")]
    Transport(String),
}

/// Monta el adaptador que dice la configuracion.
///
/// # Errors
///
/// Devuelve [`MailError`] si la configuracion de SMTP no es valida.
pub fn build_mailer(config: &MailConfig) -> Result<Arc<dyn Mailer>, MailError> {
    match config.driver {
        MailDriver::Log => Ok(Arc::new(LoggingMailer::new())),
        MailDriver::Smtp => Ok(Arc::new(SmtpMailer::from_config(config)?)),
    }
}
