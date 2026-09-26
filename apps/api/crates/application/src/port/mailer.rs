//! Puerto de envio de correo.

use async_trait::async_trait;
use gastonomo_domain::Email;

use crate::error::AppError;

/// Mensaje a enviar.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MailMessage {
    /// Destinatario.
    pub to: Email,
    /// Asunto.
    pub subject: String,
    /// Cuerpo en texto plano.
    pub body: String,
}

/// Envia correo.
///
/// Tiene dos adaptadores: SMTP de verdad y uno que escribe el mensaje en el log,
/// que es el que se usa en desarrollo cuando no hay servidor de correo.
#[async_trait]
pub trait Mailer: Send + Sync {
    /// Envia un mensaje.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si el envio falla.
    async fn send(&self, message: &MailMessage) -> Result<(), AppError>;
}
