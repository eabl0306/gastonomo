//! Cableado del contexto de ajustes.

use std::sync::Arc;

use gastonomo_application::use_case::settings::{GetUserSettings, UpdateUserSettings};
use gastonomo_http::SettingsDependencies;
use gastonomo_infrastructure::persistence::{
    PostgresDisplayCurrencyCatalog, PostgresUserSettingsRepository,
};

use super::wiring::Wiring;

/// Monta los casos de uso de ajustes con los adaptadores de PostgreSQL.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> SettingsDependencies {
    let settings = Arc::new(PostgresUserSettingsRepository::new(wiring.pool.clone()));
    let currencies = Arc::new(PostgresDisplayCurrencyCatalog::new(wiring.pool.clone()));
    SettingsDependencies {
        read: Arc::new(GetUserSettings::new(settings.clone(), wiring.clock.clone())),
        write: Arc::new(UpdateUserSettings::new(
            settings,
            currencies,
            wiring.clock.clone(),
        )),
    }
}
