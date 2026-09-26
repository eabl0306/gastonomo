//! Casos de uso del contexto de categorias.
//!
//! Son los cuatro de la pantalla de Categorias del diseno: listar con el conteo de
//! uso, crear, editar y borrar reasignando los movimientos a "Otros".

mod create_category;
mod delete_category;
mod list_categories;
mod parsing;
mod update_category;

#[cfg(test)]
mod tests;

pub use create_category::{CreateCategory, CreateCategoryCommand};
pub use delete_category::DeleteCategory;
pub use list_categories::ListCategories;
pub use parsing::{ParsedCategoryFields, RawCategoryFields, domain_error, parse_fields};
pub use update_category::{UpdateCategory, UpdateCategoryCommand};
