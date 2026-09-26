//! Piezas compartidas por todos los contextos del composition root.

use std::fmt;
use std::sync::Arc;

use gastonomo_application::port::{Clock, IdempotencyStore};
use gastonomo_domain::ServiceVersion;
use gastonomo_infrastructure::{AppConfig, PgPool, PostgresIdempotencyStore, SystemClock};

/// Lo que cualquier contexto necesita para montar sus adaptadores.
///
/// Se construye una vez en [`crate::bootstrap`] y se le pasa por referencia a la
/// funcion de cableado de cada contexto.
#[derive(Clone)]
pub struct Wiring {
    /// Configuracion completa del servicio.
    pub config: AppConfig,
    /// Version del servicio.
    pub version: ServiceVersion,
    /// Pool de conexiones a PostgreSQL.
    pub pool: PgPool,
    /// Reloj del sistema, compartido por todos los casos de uso.
    pub clock: Arc<dyn Clock>,
    /// Claves de idempotencia. Lo comparten los tres POST que crean dinero, para
    /// que la misma clave en dos operaciones distintas no se estorbe.
    pub idempotency: Arc<dyn IdempotencyStore>,
}

impl fmt::Debug for Wiring {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("Wiring")
            .field("version", &self.version)
            .finish_non_exhaustive()
    }
}

impl Wiring {
    /// Crea el contexto de cableado.
    #[must_use]
    pub fn new(config: AppConfig, version: ServiceVersion, pool: PgPool) -> Self {
        Self {
            config,
            version,
            idempotency: Arc::new(PostgresIdempotencyStore::new(pool.clone())),
            pool,
            clock: Arc::new(SystemClock::new()),
        }
    }
}
