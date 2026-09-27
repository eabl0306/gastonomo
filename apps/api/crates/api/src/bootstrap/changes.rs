//! Cableado del feed de cambios.

use std::sync::Arc;

use gastonomo_application::use_case::changes::GetChanges;
use gastonomo_http::ChangesDependencies;
use gastonomo_infrastructure::persistence::PostgresChangeFeed;

use super::wiring::Wiring;

/// Monta el caso de uso de cambios con el adaptador de PostgreSQL.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> ChangesDependencies {
    let changes = Arc::new(PostgresChangeFeed::new(wiring.pool.clone()));
    ChangesDependencies {
        read: Arc::new(GetChanges::new(changes)),
    }
}
