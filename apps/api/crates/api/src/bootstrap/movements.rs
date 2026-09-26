//! Cableado del contexto de movimientos.
//!
//! El repositorio de PostgreSQL implementa los dos puertos del contexto, el de
//! movimientos y el de lectura de categorias, asi que una sola instancia sirve para
//! los dos casos de uso.

use std::sync::Arc;

use gastonomo_application::use_case::movement::{CreateMovement, ListMovements};
use gastonomo_http::MovementDependencies;
use gastonomo_infrastructure::persistence::PostgresMovementRepository;

use super::wiring::Wiring;

/// Monta los casos de uso de movimientos.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> MovementDependencies {
    let repository = Arc::new(PostgresMovementRepository::new(wiring.pool.clone()));
    MovementDependencies {
        list: Arc::new(ListMovements::new(repository.clone())),
        create: Arc::new(CreateMovement::new(
            repository.clone(),
            repository,
            wiring.clock.clone(),
        )),
        idempotency: wiring.idempotency.clone(),
    }
}
