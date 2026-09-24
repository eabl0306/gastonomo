//! Montaje de la aplicacion: el unico sitio que conoce adaptadores concretos.

use std::net::SocketAddr;
use std::sync::Arc;
use std::time::Duration;

use gastonomo_application::{CheckLiveness, CheckReadiness, HealthProbe};
use gastonomo_domain::ServiceVersion;
use gastonomo_http::{HealthRouterDependencies, build_router};
use gastonomo_infrastructure::{
    AppConfig, PgPool, PostgresHealthProbe, connect_pool, run_migrations,
};
use salvo::conn::Listener;
use salvo::prelude::{Router, Server, Service, TcpListener};

use crate::error::StartupError;
use crate::shutdown;

/// Tiempo maximo que se le concede a PostgreSQL para contestar al probe.
const PROBE_TIMEOUT: Duration = Duration::from_secs(2);

/// Version del servicio, leida del manifiesto del paquete.
///
/// # Errors
///
/// Devuelve [`StartupError::Version`] si el manifiesto no lleva una version
/// `major.minor.patch`.
pub fn service_version() -> Result<ServiceVersion, StartupError> {
    ServiceVersion::parse(env!("CARGO_PKG_VERSION")).map_err(StartupError::from)
}

/// Construye el router con los adaptadores reales y arranca el servidor.
///
/// # Errors
///
/// Devuelve [`StartupError`] si la persistencia, el documento OpenAPI o el
/// puerto de escucha fallan.
pub async fn serve(config: AppConfig) -> Result<(), StartupError> {
    let version = service_version()?;
    let router = assemble(&config, version).await?;
    let address = config.socket_addr()?;
    let acceptor = bind(address).await?;

    let server = Server::new(acceptor);
    tokio::spawn(shutdown::wait_and_stop(server.handle()));

    log::listening(address, version);
    server.serve(Service::new(router)).await;
    log::stopped();
    Ok(())
}

async fn bind(address: SocketAddr) -> Result<salvo::conn::tcp::TcpAcceptor, StartupError> {
    TcpListener::new(address)
        .try_bind()
        .await
        .map_err(|source| StartupError::Bind {
            address: address.to_string(),
            source: Box::new(source),
        })
}

async fn assemble(config: &AppConfig, version: ServiceVersion) -> Result<Router, StartupError> {
    let pool = connect_pool(&config.database)?;
    apply_migrations(&pool).await;

    let probes: Vec<Arc<dyn HealthProbe>> =
        vec![Arc::new(PostgresHealthProbe::new(pool, PROBE_TIMEOUT))];

    build_router(HealthRouterDependencies {
        version,
        liveness: CheckLiveness::new(version),
        readiness: Arc::new(CheckReadiness::new(version, probes)),
    })
    .map_err(|error| StartupError::OpenApi(error.to_string()))
}

/// Aplica las migraciones sin abortar el arranque si la base no esta lista.
///
/// El proceso debe poder arrancar antes que PostgreSQL: si las migraciones
/// fallan, `/health/ready` respondera 503 y el orquestador esperara, en vez de
/// entrar en un bucle de reinicios.
async fn apply_migrations(pool: &PgPool) {
    match run_migrations(pool).await {
        Ok(()) => log::migrations_applied(),
        Err(error) => log::migrations_failed(&error),
    }
}

/// Trazas del arranque.
///
/// Cada funcion envuelve una sola macro de `tracing`. Las macros expanden a
/// bastante codigo y disparan el limite de complejidad cognitiva si se mezclan
/// con logica, asi que viven aisladas.
mod log {
    use std::net::SocketAddr;

    use gastonomo_domain::ServiceVersion;
    use gastonomo_infrastructure::PersistenceError;

    pub fn listening(address: SocketAddr, version: ServiceVersion) {
        tracing::info!(%address, %version, "gastonomo api is listening");
    }

    pub fn stopped() {
        tracing::info!("gastonomo api stopped");
    }

    pub fn migrations_applied() {
        tracing::info!("database migrations are up to date");
    }

    pub fn migrations_failed(error: &PersistenceError) {
        tracing::warn!(%error, "could not apply migrations at startup");
    }
}
