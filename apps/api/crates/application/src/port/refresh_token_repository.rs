//! Puerto de persistencia de refresh tokens.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{RefreshToken, RefreshTokenId, TokenDigest, UserId};

use crate::error::AppError;

/// Lee y escribe refresh tokens, con rotacion y revocacion por familia.
#[async_trait]
pub trait RefreshTokenRepository: Send + Sync {
    /// Guarda un token recien emitido.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn insert(&self, token: &RefreshToken) -> Result<(), AppError>;

    /// Busca un token por su huella.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find_by_digest(&self, digest: &TokenDigest) -> Result<Option<RefreshToken>, AppError>;

    /// Rota un token: revoca el viejo, lo apunta al nuevo y guarda el nuevo.
    ///
    /// Las tres escrituras van en una transaccion. Si se partieran, un fallo en
    /// medio dejaria al cliente con un token revocado y sin el nuevo, es decir,
    /// fuera de la sesion sin haber hecho nada mal.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn rotate(
        &self,
        current: RefreshTokenId,
        replacement: &RefreshToken,
        now: DateTime<Utc>,
    ) -> Result<(), AppError>;

    /// Revoca un token concreto. Es idempotente.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn revoke(&self, token: RefreshTokenId, now: DateTime<Utc>) -> Result<(), AppError>;

    /// Revoca todos los tokens del usuario, con la opcion de dejar uno vivo.
    ///
    /// Se usa al cambiar la contrasena, que deja viva la sesion actual, y al
    /// detectar la reutilizacion de un token ya rotado, que no deja ninguna.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn revoke_all_for_user(
        &self,
        user_id: UserId,
        except: Option<RefreshTokenId>,
        now: DateTime<Utc>,
    ) -> Result<(), AppError>;
}
