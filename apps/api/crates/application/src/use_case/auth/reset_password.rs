//! Caso de uso: cambiar la contrasena con el token del correo.

use std::fmt;

use gastonomo_domain::Password;

use super::password_ports::PasswordPorts;
use crate::error::AppError;

/// Token del enlace mas la contrasena nueva.
#[derive(Debug, Clone)]
pub struct ResetPasswordCommand {
    /// Token que viajaba en el enlace.
    pub token: String,
    /// Contrasena nueva en claro.
    pub new_password: String,
}

/// Cambia la contrasena con un token de recuperacion.
#[derive(Clone)]
pub struct ResetPassword {
    ports: PasswordPorts,
}

impl fmt::Debug for ResetPassword {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ResetPassword").finish_non_exhaustive()
    }
}

impl ResetPassword {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: PasswordPorts) -> Self {
        Self { ports }
    }

    /// Cambia la contrasena.
    ///
    /// El token es de un solo uso y se marca gastado antes de responder. Al
    /// terminar se revocan todas las sesiones: quien pidio recuperar la contrasena
    /// puede haber perdido el telefono, y dejarle sesiones vivas seria dejarle la
    /// puerta abierta.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si la contrasena nueva no cumple el
    /// minimo, [`AppError::Unauthorized`] si el token no vale o ya se uso, y
    /// [`AppError::Dependency`] si falla una dependencia.
    pub async fn execute(&self, command: ResetPasswordCommand) -> Result<(), AppError> {
        let password = Password::parse(&command.new_password)
            .map_err(|error| AppError::validation("newPassword", error.to_string()))?;

        let digest = self.ports.opaque_tokens.digest(&command.token);
        let token = self
            .ports
            .reset_tokens
            .find_by_digest(&digest)
            .await?
            .ok_or_else(|| AppError::unauthorized("unknown password reset token"))?;

        let now = self.ports.clock.now();
        if !token.is_usable(now) {
            return Err(AppError::unauthorized(
                "the password reset link expired or was already used",
            ));
        }

        let hash = self.ports.password_hasher.hash(&password).await?;
        self.ports
            .users
            .update_password(token.user_id(), &hash, now)
            .await?;
        self.ports.reset_tokens.mark_used(token.id(), now).await?;
        self.ports
            .refresh_tokens
            .revoke_all_for_user(token.user_id(), None, now)
            .await
    }
}
