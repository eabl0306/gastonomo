//! Sub-router del contexto de movimientos.
//!
//! Cuelga del router protegido de `/v1`, asi que sus handlers ya reciben el usuario
//! en el `Depot`: se lee con [`crate::auth_context::current_user`].

use std::fmt;
use std::sync::Arc;

use gastonomo_application::port::IdempotencyStore;
use gastonomo_application::use_case::movement::{CreateMovement, ListMovements};
use salvo::prelude::Router;

use crate::handler::movement::{CreateMovementHandler, ListMovementsHandler};
use crate::idempotency::Idempotent;

/// Casos de uso que necesita el router de movimientos.
#[derive(Clone)]
pub struct MovementDependencies {
    /// Lista del periodo, paginada por cursor.
    pub list: Arc<ListMovements>,
    /// Alta de un ingreso o un gasto.
    pub create: Arc<CreateMovement>,
    /// Claves de idempotencia del alta.
    pub idempotency: Arc<dyn IdempotencyStore>,
}

impl fmt::Debug for MovementDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("MovementDependencies")
            .finish_non_exhaustive()
    }
}

/// Monta las rutas de movimientos.
///
/// El alta va envuelta en [`Idempotent`]: con la cabecera `Idempotency-Key` un
/// reintento no puede dejar el mismo gasto dos veces. La lista no la necesita.
#[must_use]
pub fn router(dependencies: MovementDependencies) -> Router {
    Router::with_path("movements")
        .get(ListMovementsHandler::new(dependencies.list))
        .post(Idempotent::new(
            dependencies.idempotency,
            "POST /v1/movements",
            CreateMovementHandler::new(dependencies.create),
        ))
}
