//! Piezas compartidas por los casos de uso de sesion.

use std::fmt;
use std::sync::Arc;

use chrono::{DateTime, Duration, Utc};
use gastonomo_domain::{NewRefreshToken, RefreshToken, RefreshTokenId, User, UserId};

use crate::error::AppError;
use crate::port::{
    AccessToken, Clock, OpaqueTokenFactory, PasswordHasher, RefreshTokenRepository, TokenIssuer,
    UserRepository,
};

/// Puertos que necesita cualquier caso de uso que abra o renueve una sesion.
///
/// Van juntos en un struct porque son seis y el limite de parametros es cinco, y
/// porque los tres casos de uso que abren sesion necesitan exactamente los mismos.
#[derive(Clone)]
pub struct SessionPorts {
    /// Cuentas.
    pub users: Arc<dyn UserRepository>,
    /// Refresh tokens.
    pub refresh_tokens: Arc<dyn RefreshTokenRepository>,
    /// Hash de contrasenas.
    pub password_hasher: Arc<dyn PasswordHasher>,
    /// Emision del access token.
    pub access_tokens: Arc<dyn TokenIssuer>,
    /// Creacion de tokens opacos.
    pub opaque_tokens: Arc<dyn OpaqueTokenFactory>,
    /// Reloj.
    pub clock: Arc<dyn Clock>,
    /// Dias de validez del refresh token.
    pub refresh_ttl_days: i64,
}

impl fmt::Debug for SessionPorts {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("SessionPorts")
            .field("refresh_ttl_days", &self.refresh_ttl_days)
            .finish_non_exhaustive()
    }
}

/// Perfil que la app usa para el saludo y el avatar.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UserProfile {
    /// Identificador de la cuenta.
    pub id: UserId,
    /// Nombre que se muestra.
    pub name: String,
    /// Correo de la cuenta.
    pub email: String,
    /// Instante de creacion.
    pub created_at: DateTime<Utc>,
}

impl From<&User> for UserProfile {
    fn from(user: &User) -> Self {
        Self {
            id: user.id(),
            name: user.name().as_str().to_owned(),
            email: user.email().as_str().to_owned(),
            created_at: user.created_at(),
        }
    }
}

/// Par de tokens recien emitido mas el perfil de quien entra.
#[derive(Debug, Clone)]
pub struct IssuedSession {
    /// Access token de vida corta.
    pub access: AccessToken,
    /// Refresh token en claro. Es la unica vez que existe fuera del cliente.
    pub refresh_token: String,
    /// Perfil del usuario.
    pub user: UserProfile,
}

/// Emite un par de tokens nuevo para una cuenta.
///
/// Solo lo llaman los casos de uso de sesion de este modulo.
pub async fn open_session(
    ports: &SessionPorts,
    user: &User,
    user_agent: Option<String>,
) -> Result<IssuedSession, AppError> {
    let now = ports.clock.now();
    let (refresh_token, token) = build_refresh_token(ports, user.id(), user_agent, now);
    ports.refresh_tokens.insert(&token).await?;
    finish_session(ports, user, refresh_token, now)
}

/// Emite un par nuevo revocando el refresh token presentado.
pub async fn rotate_session(
    ports: &SessionPorts,
    user: &User,
    current: RefreshTokenId,
    user_agent: Option<String>,
) -> Result<IssuedSession, AppError> {
    let now = ports.clock.now();
    let (refresh_token, token) = build_refresh_token(ports, user.id(), user_agent, now);
    ports.refresh_tokens.rotate(current, &token, now).await?;
    finish_session(ports, user, refresh_token, now)
}

fn build_refresh_token(
    ports: &SessionPorts,
    user_id: UserId,
    user_agent: Option<String>,
    now: DateTime<Utc>,
) -> (String, RefreshToken) {
    let opaque = ports.opaque_tokens.create();
    let token = RefreshToken::issue(NewRefreshToken {
        user_id,
        digest: opaque.digest,
        created_at: now,
        expires_at: now + Duration::days(ports.refresh_ttl_days),
        user_agent,
    });
    (opaque.value, token)
}

fn finish_session(
    ports: &SessionPorts,
    user: &User,
    refresh_token: String,
    now: DateTime<Utc>,
) -> Result<IssuedSession, AppError> {
    Ok(IssuedSession {
        access: ports.access_tokens.issue(user.id(), now)?,
        refresh_token,
        user: UserProfile::from(user),
    })
}
