//! Caso de uso: perfil del usuario autenticado.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::UserId;

use super::session::UserProfile;
use crate::error::AppError;
use crate::port::UserRepository;

/// Devuelve el nombre y el correo de la cuenta del token.
#[derive(Clone)]
pub struct GetProfile {
    users: Arc<dyn UserRepository>,
}

impl fmt::Debug for GetProfile {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("GetProfile").finish_non_exhaustive()
    }
}

impl GetProfile {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(users: Arc<dyn UserRepository>) -> Self {
        Self { users }
    }

    /// Lee el perfil.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la cuenta ya no existe y
    /// [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(&self, user_id: UserId) -> Result<UserProfile, AppError> {
        self.users
            .find_by_id(user_id)
            .await?
            .as_ref()
            .map(UserProfile::from)
            .ok_or_else(|| AppError::not_found("user"))
    }
}
