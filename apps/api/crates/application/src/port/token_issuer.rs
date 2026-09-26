//! Puerto de emision y validacion del access token.

use chrono::{DateTime, Utc};
use gastonomo_domain::UserId;

use crate::error::AppError;

/// Access token recien emitido.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AccessToken {
    /// Token codificado que viaja en la cabecera `Authorization`.
    pub value: String,
    /// Segundos de validez, que es lo que el cliente necesita para renovar antes.
    pub expires_in_seconds: i64,
}

/// Emite y valida access tokens.
pub trait TokenIssuer: Send + Sync {
    /// Emite un token para una cuenta.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si el token no se puede firmar.
    fn issue(&self, user_id: UserId, now: DateTime<Utc>) -> Result<AccessToken, AppError>;

    /// Valida un token y devuelve el usuario que representa.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Unauthorized`] si la firma no cuadra, si ha caducado o
    /// si el contenido no es el esperado.
    fn verify(&self, token: &str, now: DateTime<Utc>) -> Result<UserId, AppError>;
}
