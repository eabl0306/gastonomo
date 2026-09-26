//! Sub-router del contexto de ajustes.
//!
//! Cuelga del router protegido de `/v1`, asi que sus handlers ya reciben el usuario
//! en el `Depot`: se lee con [`crate::auth_context::current_user`].

use std::fmt;
use std::sync::Arc;

use gastonomo_application::use_case::settings::{GetUserSettings, UpdateUserSettings};
use salvo::prelude::Router;

use crate::handler::settings::{GetSettingsHandler, UpdateSettingsHandler};

/// Casos de uso que necesita el router de ajustes.
#[derive(Clone)]
pub struct SettingsDependencies {
    /// Lectura de la moneda principal y el idioma.
    pub read: Arc<GetUserSettings>,
    /// Cambio de la moneda principal o del idioma.
    pub write: Arc<UpdateUserSettings>,
}

impl fmt::Debug for SettingsDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("SettingsDependencies")
            .finish_non_exhaustive()
    }
}

/// Monta las rutas de ajustes.
#[must_use]
pub fn router(dependencies: SettingsDependencies) -> Router {
    Router::new().push(
        Router::with_path("settings")
            .get(GetSettingsHandler::new(dependencies.read))
            .patch(UpdateSettingsHandler::new(dependencies.write)),
    )
}
