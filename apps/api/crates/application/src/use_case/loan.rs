//! Casos de uso del contexto de prestamos.
//!
//! Son cuatro: listar, ver uno, crear y registrar un abono. Los dos ultimos
//! escriben tambien el movimiento que pide el diseno, y de la atomicidad se
//! encarga el adaptador detras de [`crate::port::loan::LoanRepository`].
//!
//! Lo que estos casos de uso no hacen: no calculan el saldo ni el estado del
//! prestamo. Dependen de la moneda principal del momento, asi que son del cliente.

mod create_loan;
mod get_loan;
mod list_loans;
mod ports;
mod register_payment;
mod validation;

#[cfg(any(test, feature = "testing"))]
pub mod testing;

#[cfg(test)]
mod tests;

pub use create_loan::{CreateLoan, CreateLoanCommand};
pub use get_loan::GetLoan;
pub use list_loans::{ListLoans, ListLoansQuery};
pub use ports::LoanPorts;
pub use register_payment::{RegisterLoanPayment, RegisterLoanPaymentCommand};
pub use validation::MoneyFields;
