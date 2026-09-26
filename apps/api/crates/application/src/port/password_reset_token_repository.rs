//! Puerto de persistencia de tokens de recuperacion.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{PasswordResetToken, PasswordResetTokenId, TokenDigest};

use crate::error::AppError;

/// Lee y escribe tokens de recuperacion de contrasena.
#[async_trait]
pub trait PasswordResetTokenRepository: Send + Sync {
    /// Guarda un token recien emitido.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn insert(&self, token: &PasswordResetToken) -> Result<(), AppError>;

    /// Busca un token por su huella.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find_by_digest(
        &self,
        digest: &TokenDigest,
    ) -> Result<Option<PasswordResetToken>, AppError>;

    /// Marca el token como usado. Solo surte efecto la primera vez.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn mark_used(
        &self,
        token: PasswordResetTokenId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError>;
}
