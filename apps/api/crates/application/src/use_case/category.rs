//! Casos de uso del contexto de categorias.
//!
//! Los de la pantalla de Categorias: listar con el conteo de uso, crear, editar,
//! archivar y recuperar. Borrar reasignando los movimientos a "Otros" ya no existe:
//! era el ultimo camino por el que se podia alterar como se lee el historico.

mod archive_category;
mod create_category;
mod list_categories;
mod parsing;
mod restore_category;
mod update_category;

#[cfg(test)]
mod tests;

pub use archive_category::{ArchiveCategory, CategoryRemoval};
pub use create_category::{CreateCategory, CreateCategoryCommand};
pub use list_categories::ListCategories;
pub use parsing::{ParsedCategoryFields, RawCategoryFields, domain_error, parse_fields};
pub use restore_category::RestoreCategory;
pub use update_category::{UpdateCategory, UpdateCategoryCommand};
