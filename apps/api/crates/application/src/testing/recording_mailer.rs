//! Buzon de test: guarda los mensajes en memoria.

use std::sync::Mutex;

use async_trait::async_trait;

use crate::error::AppError;
use crate::port::{MailMessage, Mailer};

/// Doble del puerto [`Mailer`] que apunta lo que se habria enviado.
#[derive(Debug, Default)]
pub struct RecordingMailer {
    sent: Mutex<Vec<MailMessage>>,
}

impl RecordingMailer {
    /// Crea el buzon vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            sent: Mutex::new(Vec::new()),
        }
    }

    /// Mensajes enviados, en orden.
    #[must_use]
    pub fn sent(&self) -> Vec<MailMessage> {
        self.sent.lock().map_or_else(
            |poisoned| poisoned.into_inner().clone(),
            |sent| sent.clone(),
        )
    }
}

#[async_trait]
impl Mailer for RecordingMailer {
    async fn send(&self, message: &MailMessage) -> Result<(), AppError> {
        if let Ok(mut sent) = self.sent.lock() {
            sent.push(message.clone());
        }
        Ok(())
    }
}
