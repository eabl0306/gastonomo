//! Sub-router del feed de cambios.
//!
//! Cuelga del router protegido de `/v1`, asi que su handler ya recibe el usuario en
//! el `Depot`: se lee con [`crate::auth_context::current_user`].

use std::fmt;
use std::sync::Arc;

use gastonomo_application::use_case::changes::GetChanges;
use salvo::prelude::Router;

use crate::handler::changes::GetChangesHandler;

/// Casos de uso que necesita el router de cambios.
#[derive(Clone)]
pub struct ChangesDependencies {
    /// Lectura del siguiente lote de cambios.
    pub read: Arc<GetChanges>,
}

impl fmt::Debug for ChangesDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ChangesDependencies")
            .finish_non_exhaustive()
    }
}

/// Monta la ruta de cambios.
#[must_use]
pub fn router(dependencies: ChangesDependencies) -> Router {
    Router::new().push(Router::with_path("changes").get(GetChangesHandler::new(dependencies.read)))
}
