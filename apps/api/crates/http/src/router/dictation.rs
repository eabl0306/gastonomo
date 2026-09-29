//! Rutas del dictado.

use std::fmt;
use std::sync::Arc;

use gastonomo_application::use_case::dictation::ParseDictation;
use salvo::prelude::Router;

use crate::handler::dictation::ParseDictationHandler;

/// Casos de uso que necesita el router del dictado.
#[derive(Clone)]
pub struct DictationDependencies {
    /// Interpretar lo dictado.
    pub parse: Arc<ParseDictation>,
}

impl fmt::Debug for DictationDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("DictationDependencies")
            .finish_non_exhaustive()
    }
}

/// Monta las rutas del dictado.
///
/// **Sin idempotencia, y es correcto**: esta ruta no crea nada. Devuelve un
/// borrador que el usuario revisa, y confirmarlo es una llamada aparte a
/// `POST /v1/movements` o `POST /v1/loans`, que si la llevan.
#[must_use]
pub fn router(dependencies: DictationDependencies) -> Router {
    Router::with_path("dictation")
        .push(Router::with_path("parse").post(ParseDictationHandler::new(dependencies.parse)))
}
