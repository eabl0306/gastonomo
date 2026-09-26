//! Adaptador driven: correo que se queda en el log.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::{MailMessage, Mailer};

/// Escribe el mensaje en el log en vez de enviarlo.
///
/// Es el adaptador de desarrollo cuando no hay servidor de correo delante: el
/// enlace de recuperacion sale por la salida del proceso y se puede copiar a mano.
/// En la pila de Docker no hace falta, porque ahi esta Mailpit.
#[derive(Debug, Clone, Copy, Default)]
pub struct LoggingMailer;

impl LoggingMailer {
    /// Crea el adaptador.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

#[async_trait]
impl Mailer for LoggingMailer {
    async fn send(&self, message: &MailMessage) -> Result<(), AppError> {
        tracing::info!(
            to = %message.to,
            subject = %message.subject,
            body = %message.body,
            "mail not sent: the logging mailer is configured"
        );
        Ok(())
    }
}
