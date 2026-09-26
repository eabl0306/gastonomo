//! Ajustes de usuario en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::settings::UserSettings;
use gastonomo_domain::value_object::{CurrencyCode, UserId};

use crate::error::AppError;
use crate::port::settings::{DisplayCurrencyCatalog, UserSettingsRepository};

/// Doble del puerto [`UserSettingsRepository`] sobre un vector.
#[derive(Debug, Default)]
pub struct InMemoryUserSettingsRepository {
    rows: Mutex<Vec<UserSettings>>,
}

impl InMemoryUserSettingsRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            rows: Mutex::new(Vec::new()),
        }
    }

    /// Guarda unos ajustes sin pasar por el caso de uso.
    pub fn seed(&self, settings: UserSettings) {
        self.with(|rows| rows.push(settings));
    }

    /// Filas guardadas.
    #[must_use]
    pub fn count(&self) -> usize {
        self.with(|rows| rows.len())
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<UserSettings>) -> T) -> T {
        match self.rows.lock() {
            Ok(mut rows) => operation(&mut rows),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl UserSettingsRepository for InMemoryUserSettingsRepository {
    async fn find(&self, user_id: UserId) -> Result<Option<UserSettings>, AppError> {
        Ok(self.with(|rows| {
            rows.iter()
                .find(|settings| settings.user_id() == user_id)
                .cloned()
        }))
    }

    async fn save(&self, settings: &UserSettings) -> Result<(), AppError> {
        self.with(|rows| {
            if let Some(slot) = rows
                .iter_mut()
                .find(|stored| stored.user_id() == settings.user_id())
            {
                *slot = settings.clone();
            } else {
                rows.push(settings.clone());
            }
        });
        Ok(())
    }
}

/// Catalogo de monedas de test: una lista cerrada de codigos aceptados.
#[derive(Debug)]
pub struct FakeDisplayCurrencyCatalog {
    offered: Vec<String>,
}

impl Default for FakeDisplayCurrencyCatalog {
    fn default() -> Self {
        Self::new()
    }
}

impl FakeDisplayCurrencyCatalog {
    /// Crea el catalogo con las monedas que usa el diseno en sus ejemplos.
    #[must_use]
    pub fn new() -> Self {
        Self::with(&["COP", "USD", "EUR", "MXN", "CLP", "JPY"])
    }

    /// Crea el catalogo con los codigos dados.
    #[must_use]
    pub fn with(codes: &[&str]) -> Self {
        Self {
            offered: codes
                .iter()
                .map(|code| (*code).to_ascii_uppercase())
                .collect(),
        }
    }
}

#[async_trait]
impl DisplayCurrencyCatalog for FakeDisplayCurrencyCatalog {
    async fn is_selectable(&self, currency: &CurrencyCode) -> Result<bool, AppError> {
        Ok(self.offered.iter().any(|code| code == currency.as_str()))
    }

    async fn is_ready(&self) -> Result<bool, AppError> {
        Ok(!self.offered.is_empty())
    }
}
