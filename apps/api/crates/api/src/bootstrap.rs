//! Montaje de la aplicacion: el unico sitio que conoce adaptadores concretos.
//!
//! Cada contexto tiene su propio modulo con una funcion `dependencies(&Wiring)`:
//! asi dos contextos que se cablean a la vez no se pisan en este fichero. Para
//! anadir uno nuevo solo hay que rellenar `bootstrap/<contexto>.rs` y su linea en
//! [`router_dependencies`].

mod auth;
mod categories;
mod changes;
mod currencies;
mod dictation;
mod health;
mod loans;
mod movements;
mod settings;
mod wiring;

use std::net::SocketAddr;

use gastonomo_domain::ServiceVersion;
use gastonomo_http::{RouterDependencies, build_service};
use gastonomo_infrastructure::{AppConfig, PgPool, connect_pool, run_migrations};
use salvo::conn::Listener;
use salvo::prelude::{Server, Service, TcpListener};

use crate::error::StartupError;
use crate::shutdown;
use wiring::Wiring;

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
/// Devuelve [`StartupError`] si la persistencia, el correo, el documento OpenAPI o
/// el puerto de escucha fallan.
pub async fn serve(config: AppConfig) -> Result<(), StartupError> {
    let version = service_version()?;
    let service = assemble(config.clone(), version).await?;
    let address = config.socket_addr()?;
    let acceptor = bind(address).await?;

    let server = Server::new(acceptor);
    tokio::spawn(shutdown::wait_and_stop(server.handle()));

    log::listening(address, version);
    server.serve(service).await;
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

async fn assemble(config: AppConfig, version: ServiceVersion) -> Result<Service, StartupError> {
    let pool = connect_pool(&config.database)?;
    apply_migrations(&pool).await;
    if config.auth.uses_development_secret() {
        log::development_secret();
    }

    let wiring = Wiring::new(config, version, pool);
    let dependencies = router_dependencies(&wiring)?;
    // Adaptador driving, igual que el router: un bucle de tokio que refresca las
    // tasas de cambio al arrancar y despues a sus horas.
    currencies::start_refresh_task(&wiring);
    build_service(dependencies).map_err(|error| StartupError::OpenApi(error.to_string()))
}

/// Reune las dependencias de todos los contextos.
///
/// # Errors
///
/// Devuelve [`StartupError`] si el cableado de un contexto falla.
fn router_dependencies(wiring: &Wiring) -> Result<RouterDependencies, StartupError> {
    Ok(RouterDependencies {
        health: health::dependencies(wiring),
        auth: auth::dependencies(wiring)?,
        movements: movements::dependencies(wiring),
        loans: loans::dependencies(wiring),
        categories: categories::dependencies(wiring),
        settings: settings::dependencies(wiring),
        currencies: currencies::dependencies(wiring),
        changes: changes::dependencies(wiring),
        dictation: dictation::dependencies(wiring)?,
    })
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

    pub fn development_secret() {
        tracing::warn!(
            "GASTONOMO__AUTH__ACCESS_TOKEN_SECRET is still the development one: \
             anyone who reads the repository can sign an access token"
        );
    }
}
