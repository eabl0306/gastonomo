//! Caso de uso: editar una categoria del usuario.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::category::{CategoryChange, CategoryId};
use gastonomo_domain::value_object::UserId;

use super::parsing::{RawCategoryFields, domain_error, parse_fields};
use crate::error::AppError;
use crate::port::Clock;
use crate::port::category::{CategoryRepository, CategoryUsage};

/// Cambio parcial que llega en el `PATCH`.
///
/// Un campo ausente o nulo se queda como estaba.
#[derive(Debug, Clone)]
pub struct UpdateCategoryCommand {
    /// Cuenta del token.
    pub user_id: UserId,
    /// Categoria a cambiar.
    pub category_id: CategoryId,
    /// Nombre nuevo.
    pub name: Option<String>,
    /// Tipo nuevo.
    pub kind: Option<String>,
    /// Icono nuevo.
    pub icon: Option<String>,
    /// Color nuevo.
    pub color: Option<String>,
}

/// Edita una categoria del usuario.
#[derive(Clone)]
pub struct UpdateCategory {
    categories: Arc<dyn CategoryRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for UpdateCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("UpdateCategory").finish_non_exhaustive()
    }
}

impl UpdateCategory {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { categories, clock }
    }

    /// Aplica el cambio y devuelve la categoria ya editada.
    ///
    /// Un cuerpo que no pide cambiar nada devuelve la categoria tal cual, sin
    /// escribir en la base de datos.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la categoria no existe o es de otra
    /// cuenta, [`AppError::Conflict`] con codigo `category_is_default` si es una
    /// predeterminada o `category_name_taken` si el nombre ya existe,
    /// [`AppError::Validation`] si un campo no vale y [`AppError::Dependency`] si
    /// falla la base de datos.
    pub async fn execute(&self, command: UpdateCategoryCommand) -> Result<CategoryUsage, AppError> {
        let mut found = self
            .categories
            .find(command.user_id, command.category_id)
            .await?
            .ok_or_else(|| AppError::not_found("category"))?;
        found
            .category
            .check_writable()
            .map_err(|error| domain_error(&error))?;

        let change = change_from(command)?;
        if change.is_empty() {
            return Ok(found);
        }
        found
            .category
            .apply(change, self.clock.now())
            .map_err(|error| domain_error(&error))?;
        self.categories.update(&found.category).await?;
        Ok(found)
    }
}

fn change_from(command: UpdateCategoryCommand) -> Result<CategoryChange, AppError> {
    let fields = parse_fields(&RawCategoryFields {
        name: command.name,
        kind: command.kind,
        icon: command.icon,
        color: command.color,
    })?;
    Ok(CategoryChange {
        name: fields.name,
        kind: fields.kind,
        icon: fields.icon,
        color: fields.color,
    })
}
