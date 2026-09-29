//! Puerto de persistencia de tokens de recuperacion.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{PasswordResetToken, PasswordResetTokenId, TokenDigest, UserId};

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

    /// El token vivo mas reciente de la cuenta, si queda alguno.
    ///
    /// Lo usa la espera entre emisiones: si el ultimo se mando hace nada, no se
    /// emite otro y no se borra el que la cuenta ya tiene.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn latest_live_for_user(
        &self,
        user: UserId,
        now: DateTime<Utc>,
    ) -> Result<Option<PasswordResetToken>, AppError>;

    /// Borra todos los tokens del usuario, usados o no.
    ///
    /// Se llama antes de emitir uno nuevo, asi que cada cuenta tiene como mucho un
    /// enlace vivo: pedir el enlace otra vez mata el anterior. Sin esto, tocar
    /// "reenviar" tres veces dejaba tres enlaces abriendo la misma cuenta durante
    /// media hora, y no hay ninguna pantalla donde verlos ni revocarlos.
    ///
    /// Borra en vez de marcar usados porque asi la tabla tampoco crece: un usuario
    /// deja una fila, no una por peticion. No hace falta ninguna tarea de limpieza.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn discard_all_for_user(&self, user: UserId) -> Result<(), AppError>;

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
