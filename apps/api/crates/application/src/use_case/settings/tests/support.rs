//! Montaje comun de los tests de ajustes.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::value_object::UserId;

use crate::testing::{FakeDisplayCurrencyCatalog, FixedClock, InMemoryUserSettingsRepository};
use crate::use_case::settings::{GetUserSettings, UpdateUserSettings, UpdateUserSettingsCommand};

/// Todo lo que un test de ajustes necesita, ya cableado.
pub struct Harness {
    pub store: Arc<InMemoryUserSettingsRepository>,
    pub read: GetUserSettings,
    pub write: UpdateUserSettings,
    pub user: UserId,
}

impl Harness {
    /// Cablea los dobles con el catalogo de monedas del diseno.
    #[must_use]
    pub fn new() -> Self {
        Self::with_currencies(&["COP", "USD", "EUR"])
    }

    /// Cablea los dobles con las monedas dadas en el catalogo.
    #[must_use]
    pub fn with_currencies(codes: &[&str]) -> Self {
        let store = Arc::new(InMemoryUserSettingsRepository::new());
        let clock = Arc::new(FixedClock::at_fixture());
        let currencies = Arc::new(FakeDisplayCurrencyCatalog::with(codes));
        Self {
            read: GetUserSettings::new(store.clone(), clock.clone()),
            write: UpdateUserSettings::new(store.clone(), currencies, clock),
            store,
            user: UserId::generate(),
        }
    }

    /// Cuerpo de `PATCH` con la moneda y el idioma dados.
    #[must_use]
    pub fn change(
        &self,
        display_currency: Option<&str>,
        language: Option<&str>,
    ) -> UpdateUserSettingsCommand {
        UpdateUserSettingsCommand {
            user_id: self.user,
            display_currency: display_currency.map(str::to_owned),
            language: language.map(str::to_owned),
        }
    }
}
