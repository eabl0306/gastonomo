//! Puerto de persistencia de cuentas.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::{Email, PasswordHash, User, UserId};

use crate::error::AppError;

/// Lee y escribe cuentas.
#[async_trait]
pub trait UserRepository: Send + Sync {
    /// Guarda una cuenta nueva.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Conflict`] con codigo `email_taken` si el correo ya
    /// existe, y [`AppError::Dependency`] si falla la base de datos.
    async fn insert(&self, user: &User) -> Result<(), AppError>;

    /// Busca una cuenta por correo.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find_by_email(&self, email: &Email) -> Result<Option<User>, AppError>;

    /// Busca una cuenta por identificador.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    async fn find_by_id(&self, user_id: UserId) -> Result<Option<User>, AppError>;

    /// Cambia el hash de la contrasena.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la cuenta no existe y
    /// [`AppError::Dependency`] si falla la base de datos.
    async fn update_password(
        &self,
        user_id: UserId,
        password_hash: &PasswordHash,
        now: DateTime<Utc>,
    ) -> Result<(), AppError>;
}
