//! Caso de uso: cambiar la contrasena estando dentro.

use std::fmt;

use gastonomo_domain::{Password, UserId};

use super::password_ports::PasswordPorts;
use crate::error::AppError;

/// Contrasena actual, nueva y sesion que se quiere conservar.
#[derive(Debug, Clone)]
pub struct ChangePasswordCommand {
    /// Cuenta del token de acceso.
    pub user_id: UserId,
    /// Contrasena actual en claro.
    pub current_password: String,
    /// Contrasena nueva en claro.
    pub new_password: String,
    /// Refresh token de la sesion que hace el cambio, si el cliente lo manda.
    ///
    /// Es lo unico que deja distinguir "la sesion actual" de las demas: el access
    /// token no identifica una sesion, solo a un usuario.
    pub current_refresh_token: Option<String>,
}

/// Cambia la contrasena de una cuenta con sesion abierta.
#[derive(Clone)]
pub struct ChangePassword {
    ports: PasswordPorts,
}

impl fmt::Debug for ChangePassword {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ChangePassword").finish_non_exhaustive()
    }
}

impl ChangePassword {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: PasswordPorts) -> Self {
        Self { ports }
    }

    /// Cambia la contrasena.
    ///
    /// Revoca todas las sesiones del usuario menos la que hace el cambio, que es lo
    /// que pide el diseno: si alguien te habia robado la sesion, este es el momento
    /// de echarlo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si la nueva no cumple el minimo,
    /// [`AppError::InvalidCredentials`] si la actual no coincide,
    /// [`AppError::NotFound`] si la cuenta no existe y [`AppError::Dependency`] si
    /// falla una dependencia.
    pub async fn execute(&self, command: ChangePasswordCommand) -> Result<(), AppError> {
        let new_password = Password::parse(&command.new_password)
            .map_err(|error| AppError::validation("newPassword", error.to_string()))?;
        let user = self
            .ports
            .users
            .find_by_id(command.user_id)
            .await?
            .ok_or_else(|| AppError::not_found("user"))?;

        let Ok(current) = Password::parse(&command.current_password) else {
            return Err(AppError::InvalidCredentials);
        };
        if !self
            .ports
            .password_hasher
            .verify(&current, user.password_hash())
            .await?
        {
            return Err(AppError::InvalidCredentials);
        }

        let now = self.ports.clock.now();
        let hash = self.ports.password_hasher.hash(&new_password).await?;
        self.ports
            .users
            .update_password(user.id(), &hash, now)
            .await?;

        let keep = self
            .session_to_keep(command.current_refresh_token.as_deref())
            .await?;
        self.ports
            .refresh_tokens
            .revoke_all_for_user(user.id(), keep, now)
            .await
    }

    async fn session_to_keep(
        &self,
        refresh_token: Option<&str>,
    ) -> Result<Option<gastonomo_domain::RefreshTokenId>, AppError> {
        let Some(value) = refresh_token else {
            return Ok(None);
        };
        let digest = self.ports.opaque_tokens.digest(value);
        Ok(self
            .ports
            .refresh_tokens
            .find_by_digest(&digest)
            .await?
            .map(|token| token.id()))
    }
}
