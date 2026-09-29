//! Cableado del contexto de dictado.
//!
//! El interprete se construye aunque no haya clave configurada. Sin ella el caso de
//! uso responde 503 y el resto de la API sigue igual: el dictado es una capacidad
//! opcional y no puede impedir que el proceso arranque, que es la regla de todo el
//! bootstrap.

use std::sync::Arc;

use gastonomo_application::use_case::dictation::{ParseDictation, ParseDictationPorts};
use gastonomo_http::DictationDependencies;
use gastonomo_infrastructure::dictation::ClaudeDictationInterpreter;
use gastonomo_infrastructure::persistence::{
    PostgresCategoryRepository, PostgresDictationAccess, PostgresExchangeRateRepository,
    PostgresUserSettingsRepository,
};

use super::wiring::Wiring;
use crate::error::StartupError;

/// Monta el caso de uso del dictado.
///
/// # Errors
///
/// Devuelve [`StartupError::Dictation`] si el cliente HTTP del interprete no se
/// puede construir.
pub fn dependencies(wiring: &Wiring) -> Result<DictationDependencies, StartupError> {
    let interpreter = ClaudeDictationInterpreter::new(wiring.config.claude.clone())
        .map_err(|error| StartupError::Dictation(Box::new(error)))?;
    Ok(DictationDependencies {
        parse: Arc::new(ParseDictation::new(ParseDictationPorts {
            access: Arc::new(PostgresDictationAccess::new(wiring.pool.clone())),
            interpreter: Arc::new(interpreter),
            settings: Arc::new(PostgresUserSettingsRepository::new(wiring.pool.clone())),
            categories: Arc::new(PostgresCategoryRepository::new(wiring.pool.clone())),
            rates: Arc::new(PostgresExchangeRateRepository::new(wiring.pool.clone())),
            clock: wiring.clock.clone(),
        })),
    })
}
