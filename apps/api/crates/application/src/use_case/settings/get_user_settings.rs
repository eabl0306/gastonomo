//! Caso de uso: leer los ajustes de la cuenta.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::settings::UserSettings;
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::Clock;
use crate::port::settings::UserSettingsRepository;

/// Devuelve la moneda principal y el idioma de la cuenta.
///
/// Si la fila no existe devuelve los valores por defecto (`COP` y espanol) en vez
/// de un 404: la cuenta si existe, y Ajustes tiene que poder pintarse igual.
#[derive(Clone)]
pub struct GetUserSettings {
    settings: Arc<dyn UserSettingsRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for GetUserSettings {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("GetUserSettings").finish_non_exhaustive()
    }
}

impl GetUserSettings {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(settings: Arc<dyn UserSettingsRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { settings, clock }
    }

    /// Lee los ajustes.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(&self, user_id: UserId) -> Result<UserSettings, AppError> {
        Ok(self
            .settings
            .find(user_id)
            .await?
            .unwrap_or_else(|| UserSettings::defaults_for(user_id, self.clock.now())))
    }
}
