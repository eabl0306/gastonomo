//! Handlers del contexto de prestamos.
//!
//! Los cuatro leen el usuario del token con [`crate::auth_context::current_user`] y
//! escriben cualquier error con [`crate::problem::render_problem`]. Ninguno vuelve a
//! validar el token: si se ejecutan, el middleware ya lo hizo.

mod create;
mod payment;
mod read;
mod support;

pub use create::CreateLoanHandler;
pub use payment::RegisterLoanPaymentHandler;
pub use read::{ListLoansHandler, LoanDetailHandler};
