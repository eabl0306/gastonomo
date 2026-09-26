//! Caso de uso: inicio de sesion.

use std::fmt;

use gastonomo_domain::{Email, Password, User};

use super::session::{IssuedSession, SessionPorts, open_session};
use crate::error::AppError;

/// Datos del formulario de login.
#[derive(Debug, Clone)]
pub struct LoginUserCommand {
    /// Correo de la cuenta.
    pub email: String,
    /// Contrasena en claro.
    pub password: String,
    /// `User-Agent` de quien entra.
    pub user_agent: Option<String>,
}

/// Comprueba las credenciales y abre una sesion.
#[derive(Clone)]
pub struct LoginUser {
    ports: SessionPorts,
}

impl fmt::Debug for LoginUser {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("LoginUser").finish_non_exhaustive()
    }
}

impl LoginUser {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: SessionPorts) -> Self {
        Self { ports }
    }

    /// Inicia sesion.
    ///
    /// La respuesta es la misma con un correo desconocido y con una contrasena
    /// equivocada: [`AppError::InvalidCredentials`]. Distinguirlas convertiria el
    /// login en un oraculo para saber quien tiene cuenta.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::InvalidCredentials`] si no coinciden, y
    /// [`AppError::Dependency`] si falla una dependencia.
    pub async fn execute(&self, command: LoginUserCommand) -> Result<IssuedSession, AppError> {
        let user = self.find_user(&command.email).await?;
        let Ok(password) = Password::parse(&command.password) else {
            return Err(AppError::InvalidCredentials);
        };
        if !self
            .ports
            .password_hasher
            .verify(&password, user.password_hash())
            .await?
        {
            return Err(AppError::InvalidCredentials);
        }
        open_session(&self.ports, &user, command.user_agent).await
    }

    async fn find_user(&self, raw_email: &str) -> Result<User, AppError> {
        let Ok(email) = Email::parse(raw_email) else {
            return Err(AppError::InvalidCredentials);
        };
        self.ports
            .users
            .find_by_email(&email)
            .await?
            .ok_or(AppError::InvalidCredentials)
    }
}
