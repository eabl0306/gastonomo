//! Handlers del contexto de movimientos.
//!
//! El usuario autenticado se lee con [`crate::auth_context::current_user`] y los
//! errores se escriben con [`crate::problem::render_problem`].

mod create;
mod list;
mod support;

pub use create::CreateMovementHandler;
pub use list::ListMovementsHandler;
