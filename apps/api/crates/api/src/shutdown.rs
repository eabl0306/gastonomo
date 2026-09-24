//! Apagado limpio del servidor.

use std::time::Duration;

use salvo::server::ServerHandle;

/// Margen que se da a las peticiones en vuelo antes de cortar.
pub const GRACE_PERIOD: Duration = Duration::from_secs(10);

/// Espera SIGINT o SIGTERM y pide al servidor que pare con gracia.
pub async fn wait_and_stop(handle: ServerHandle) {
    let reason = wait_for_signal().await;
    tracing::info!(signal = reason, "shutting down gracefully");
    handle.stop_graceful(GRACE_PERIOD);
}

#[cfg(unix)]
async fn wait_for_signal() -> &'static str {
    use tokio::signal::unix::{SignalKind, signal};

    let mut terminate = match signal(SignalKind::terminate()) {
        Ok(stream) => stream,
        Err(error) => {
            tracing::error!(%error, "could not listen for SIGTERM");
            return wait_for_ctrl_c().await;
        }
    };
    tokio::select! {
        () = wait_for_ctrl_c_unit() => "SIGINT",
        _ = terminate.recv() => "SIGTERM",
    }
}

#[cfg(not(unix))]
async fn wait_for_signal() -> &'static str {
    wait_for_ctrl_c().await
}

async fn wait_for_ctrl_c() -> &'static str {
    wait_for_ctrl_c_unit().await;
    "SIGINT"
}

async fn wait_for_ctrl_c_unit() {
    if let Err(error) = tokio::signal::ctrl_c().await {
        tracing::error!(%error, "could not listen for SIGINT");
        std::future::pending::<()>().await;
    }
}
