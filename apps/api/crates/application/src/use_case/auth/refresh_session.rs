//! Caso de uso: rotacion del refresh token.

use std::fmt;

use gastonomo_domain::RefreshToken;

use super::session::{IssuedSession, SessionPorts, rotate_session};
use crate::error::AppError;

/// Refresh token presentado por el cliente.
#[derive(Debug, Clone)]
pub struct RefreshSessionCommand {
    /// Token en claro.
    pub refresh_token: String,
    /// `User-Agent` de quien refresca.
    pub user_agent: Option<String>,
}

/// Renueva la sesion rotando el refresh token.
#[derive(Clone)]
pub struct RefreshSession {
    ports: SessionPorts,
}

impl fmt::Debug for RefreshSession {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RefreshSession").finish_non_exhaustive()
    }
}

impl RefreshSession {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(ports: SessionPorts) -> Self {
        Self { ports }
    }

    /// Rota el token y devuelve un par nuevo.
    ///
    /// Si el token presentado ya se habia rotado, la sesion se considera robada y
    /// se revoca la familia entera: el ladron y el dueno legitimo se quedan fuera
    /// y el dueno vuelve a entrar con su contrasena. Es la respuesta estandar a
    /// esa senal, porque un refresh token solo se usa una vez.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Unauthorized`] si el token no existe, ha caducado, ya
    /// se revoco o se esta reutilizando.
    pub async fn execute(&self, command: RefreshSessionCommand) -> Result<IssuedSession, AppError> {
        let digest = self.ports.opaque_tokens.digest(&command.refresh_token);
        let stored = self
            .ports
            .refresh_tokens
            .find_by_digest(&digest)
            .await?
            .ok_or_else(|| AppError::unauthorized("unknown refresh token"))?;

        if stored.was_rotated() {
            return Err(self.revoke_family(&stored).await);
        }
        if !stored.is_usable(self.ports.clock.now()) {
            return Err(AppError::unauthorized(
                "the refresh token is no longer valid",
            ));
        }

        let user = self
            .ports
            .users
            .find_by_id(stored.user_id())
            .await?
            .ok_or_else(|| AppError::unauthorized("the account no longer exists"))?;

        rotate_session(&self.ports, &user, stored.id(), command.user_agent).await
    }

    async fn revoke_family(&self, stored: &RefreshToken) -> AppError {
        let now = self.ports.clock.now();
        if let Err(error) = self
            .ports
            .refresh_tokens
            .revoke_all_for_user(stored.user_id(), None, now)
            .await
        {
            return error;
        }
        AppError::unauthorized("refresh token reuse detected; the session family was revoked")
    }
}
