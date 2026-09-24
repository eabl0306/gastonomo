//! Binario del servicio Gastonomo.
//!
//! Aqui, y solo aqui, se eligen los adaptadores concretos: PostgreSQL para el
//! readiness check, Salvo para el transporte HTTP y figment para leer el
//! entorno. Los casos de uso no saben nada de esa eleccion.

mod bootstrap;
mod error;
mod shutdown;

use std::process::ExitCode;

use gastonomo_infrastructure::{AppConfig, TelemetryConfig, init_tracing};

use crate::error::StartupError;

fn main() -> ExitCode {
    let config = AppConfig::from_env();
    let telemetry = config.as_ref().map_or_else(
        |_| TelemetryConfig::default(),
        |loaded| loaded.telemetry.clone(),
    );
    if let Err(error) = init_tracing(&telemetry) {
        // Sin subscriber no hay logs, pero tampoco motivo para abortar.
        let _ = &error;
    }

    match config.map_err(StartupError::from).and_then(run) {
        Ok(()) => ExitCode::SUCCESS,
        Err(error) => {
            tracing::error!(%error, "gastonomo api failed to start");
            ExitCode::FAILURE
        }
    }
}

fn run(config: AppConfig) -> Result<(), StartupError> {
    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .map_err(StartupError::Runtime)?
        .block_on(bootstrap::serve(config))
}
