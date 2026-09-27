//! Handlers del contexto de categorias.
//!
//! El usuario autenticado se lee con [`crate::auth_context::current_user`] y los
//! errores se escriben con [`crate::problem::render_problem`].

mod create;
mod delete;
mod list;
mod restore;
mod support;
mod update;

pub use create::CreateCategoryHandler;
pub use delete::DeleteCategoryHandler;
pub use list::ListCategoriesHandler;
pub use restore::RestoreCategoryHandler;
pub use update::UpdateCategoryHandler;
