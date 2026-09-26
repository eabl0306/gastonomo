//! Sub-router del contexto de prestamos.
//!
//! Cuelga del router protegido de `/v1`, asi que sus handlers ya reciben el usuario
//! en el `Depot`: se lee con [`crate::auth_context::current_user`]. El middleware de
//! access token no se monta aqui.

use std::fmt;
use std::sync::Arc;

use gastonomo_application::port::IdempotencyStore;
use gastonomo_application::use_case::loan::{CreateLoan, GetLoan, ListLoans, RegisterLoanPayment};
use salvo::prelude::Router;

use crate::handler::loan::{
    CreateLoanHandler, ListLoansHandler, LoanDetailHandler, RegisterLoanPaymentHandler,
};
use crate::idempotency::Idempotent;

/// Casos de uso que necesita el router de prestamos.
#[derive(Clone)]
pub struct LoanDependencies {
    /// Lista con el filtro de direccion.
    pub list: Arc<ListLoans>,
    /// Detalle de un prestamo.
    pub detail: Arc<GetLoan>,
    /// Nuevo prestamo, que escribe tambien su movimiento.
    pub create: Arc<CreateLoan>,
    /// Nuevo abono, que escribe tambien su movimiento.
    pub register_payment: Arc<RegisterLoanPayment>,
    /// Claves de idempotencia de las dos altas.
    pub idempotency: Arc<dyn IdempotencyStore>,
}

impl fmt::Debug for LoanDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("LoanDependencies").finish_non_exhaustive()
    }
}

/// Monta las rutas de prestamos.
///
/// Las dos altas van envueltas en [`Idempotent`]: escriben dinero en una
/// transaccion de dos filas, y un reintento sin proteccion deja el prestamo o el
/// abono duplicado. Las lecturas no la necesitan.
#[must_use]
pub fn router(dependencies: LoanDependencies) -> Router {
    Router::with_path("loans")
        .get(ListLoansHandler::new(dependencies.list))
        .post(Idempotent::new(
            dependencies.idempotency.clone(),
            "POST /v1/loans",
            CreateLoanHandler::new(dependencies.create),
        ))
        .push(Router::with_path("{id}").get(LoanDetailHandler::new(dependencies.detail)))
        .push(Router::with_path("{id}/payments").post(Idempotent::new(
            dependencies.idempotency,
            "POST /v1/loans/{id}/payments",
            RegisterLoanPaymentHandler::new(dependencies.register_payment),
        )))
}
