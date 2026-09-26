//! Caso de uso: pedir el enlace de recuperacion.

use std::fmt;

use chrono::Duration;
use gastonomo_domain::{
    Email, NewPasswordResetToken, PasswordResetToken, RESET_TOKEN_MINUTES, User,
};

use super::password_ports::PasswordPorts;
use crate::error::AppError;
use crate::port::MailMessage;

/// Asunto del correo de recuperacion.
const SUBJECT: &str = "Recupera tu contrasena de Gastonomo";

/// Correo para el que se pide el enlace.
#[derive(Debug, Clone)]
pub struct RequestPasswordResetCommand {
    /// Correo escrito en la pantalla de recuperar.
    pub email: String,
}

/// Manda el enlace de recuperacion, si la cuenta existe.
#[derive(Clone)]
pub struct RequestPasswordReset {
    ports: PasswordPorts,
}

impl fmt::Debug for RequestPasswordReset {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RequestPasswordReset")
            .finish_non_exhaustive()
    }
}

impl RequestPasswordReset {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: PasswordPorts) -> Self {
        Self { ports }
    }

    /// Pide el enlace.
    ///
    /// Termina bien aunque el correo no tenga cuenta, y aunque no sea un correo
    /// valido. El endpoint responde 202 siempre: si respondiera distinto seria un
    /// oraculo para saber quien esta registrado. La pantalla de "enviado" muestra
    /// el correo que escribio el usuario, no uno que confirme la API.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos o el correo.
    pub async fn execute(&self, command: RequestPasswordResetCommand) -> Result<(), AppError> {
        let Ok(email) = Email::parse(&command.email) else {
            return Ok(());
        };
        let Some(user) = self.ports.users.find_by_email(&email).await? else {
            return Ok(());
        };
        let link = self.store_token(&user).await?;
        self.send_mail(&user, &link).await
    }

    async fn store_token(&self, user: &User) -> Result<String, AppError> {
        let now = self.ports.clock.now();
        let opaque = self.ports.opaque_tokens.create();
        let token = PasswordResetToken::issue(NewPasswordResetToken {
            user_id: user.id(),
            digest: opaque.digest,
            created_at: now,
            expires_at: now + Duration::minutes(RESET_TOKEN_MINUTES),
        });
        self.ports.reset_tokens.insert(&token).await?;
        Ok(self.ports.reset_link(&opaque.value))
    }

    async fn send_mail(&self, user: &User, link: &str) -> Result<(), AppError> {
        let body = format!(
            "Hola, {name}.\n\n\
             Pediste crear una contrasena nueva para tu cuenta de Gastonomo.\n\
             Abre este enlace: {link}\n\n\
             Caduca en {RESET_TOKEN_MINUTES} minutos y solo sirve una vez.\n\
             Si no lo pediste, no hace falta que hagas nada.\n",
            name = user.name().as_str(),
        );
        self.ports
            .mailer
            .send(&MailMessage {
                to: user.email().clone(),
                subject: SUBJECT.to_owned(),
                body,
            })
            .await
    }
}
