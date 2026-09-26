//! Caso de uso: cierre de sesion.

use std::fmt;
use std::sync::Arc;

use crate::error::AppError;
use crate::port::{Clock, OpaqueTokenFactory, RefreshTokenRepository};

/// Refresh token que se quiere revocar.
#[derive(Debug, Clone)]
pub struct LogoutSessionCommand {
    /// Token en claro.
    pub refresh_token: String,
}

/// Revoca el refresh token recibido.
#[derive(Clone)]
pub struct LogoutSession {
    refresh_tokens: Arc<dyn RefreshTokenRepository>,
    opaque_tokens: Arc<dyn OpaqueTokenFactory>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for LogoutSession {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("LogoutSession").finish_non_exhaustive()
    }
}

impl LogoutSession {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(
        refresh_tokens: Arc<dyn RefreshTokenRepository>,
        opaque_tokens: Arc<dyn OpaqueTokenFactory>,
        clock: Arc<dyn Clock>,
    ) -> Self {
        Self {
            refresh_tokens,
            opaque_tokens,
            clock,
        }
    }

    /// Cierra la sesion.
    ///
    /// Es idempotente y no distingue si el token existia: cerrar sesion dos veces,
    /// o con un token inventado, responde igual. Asi el cliente puede reintentar
    /// sin tratar errores y nadie puede usar este endpoint para adivinar tokens.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(&self, command: LogoutSessionCommand) -> Result<(), AppError> {
        let digest = self.opaque_tokens.digest(&command.refresh_token);
        let Some(stored) = self.refresh_tokens.find_by_digest(&digest).await? else {
            return Ok(());
        };
        self.refresh_tokens
            .revoke(stored.id(), self.clock.now())
            .await
    }
}
