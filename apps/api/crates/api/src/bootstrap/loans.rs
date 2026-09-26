//! Cableado del contexto de prestamos.

use std::sync::Arc;

use gastonomo_application::use_case::loan::{
    CreateLoan, GetLoan, ListLoans, LoanPorts, RegisterLoanPayment,
};
use gastonomo_http::LoanDependencies;
use gastonomo_infrastructure::persistence::PostgresLoanRepository;

use super::wiring::Wiring;

/// Monta los casos de uso de prestamos sobre el repositorio de PostgreSQL.
///
/// Un solo repositorio para las cuatro operaciones: crear un prestamo o un abono
/// escribe tambien su movimiento, y las dos escrituras tienen que compartir
/// transaccion.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> LoanDependencies {
    let ports = LoanPorts {
        loans: Arc::new(PostgresLoanRepository::new(wiring.pool.clone())),
        clock: wiring.clock.clone(),
    };
    LoanDependencies {
        list: Arc::new(ListLoans::new(ports.clone())),
        detail: Arc::new(GetLoan::new(ports.clone())),
        create: Arc::new(CreateLoan::new(ports.clone())),
        register_payment: Arc::new(RegisterLoanPayment::new(ports)),
        idempotency: wiring.idempotency.clone(),
    }
}
