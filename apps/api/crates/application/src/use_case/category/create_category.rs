//! Caso de uso: crear una categoria del usuario.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::category::{Category, NewCategory};
use gastonomo_domain::value_object::UserId;

use super::parsing::{RawCategoryFields, domain_error, parse_fields};
use crate::error::{AppError, ValidationErrors};
use crate::port::Clock;
use crate::port::category::{CategoryRepository, CategoryUsage};

/// Datos que llegan del sheet de categoria.
#[derive(Debug, Clone)]
pub struct CreateCategoryCommand {
    /// Cuenta del token.
    pub user_id: UserId,
    /// Nombre, obligatorio y de 22 caracteres como maximo.
    pub name: String,
    /// Tipo: `in` o `out`.
    pub kind: String,
    /// Icono, uno de los 16 de la rejilla.
    pub icon: String,
    /// Color, uno de los 8 del sheet.
    pub color: String,
}

/// Crea una categoria del usuario.
#[derive(Clone)]
pub struct CreateCategory {
    categories: Arc<dyn CategoryRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for CreateCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("CreateCategory").finish_non_exhaustive()
    }
}

impl CreateCategory {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { categories, clock }
    }

    /// Guarda la categoria y la devuelve con su conteo de uso, que es cero.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Validation`] si un campo no vale, [`AppError::Conflict`]
    /// con codigo `category_name_taken` si el nombre ya existe en la cuenta y
    /// [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(&self, command: CreateCategoryCommand) -> Result<CategoryUsage, AppError> {
        let fields = parse_fields(&RawCategoryFields {
            name: Some(command.name),
            kind: Some(command.kind),
            icon: Some(command.icon),
            color: Some(command.color),
        })?;
        let (Some(name), Some(kind), Some(icon), Some(color)) =
            (fields.name, fields.kind, fields.icon, fields.color)
        else {
            return Err(missing_fields());
        };

        let category = Category::create_custom(NewCategory {
            user_id: command.user_id,
            name,
            kind,
            icon,
            color,
            created_at: self.clock.now(),
        })
        .map_err(|error| domain_error(&error))?;

        self.categories.insert(&category).await?;
        Ok(CategoryUsage {
            category,
            movements: 0,
        })
    }
}

/// Los cuatro campos son obligatorios al crear, asi que esta rama no se alcanza
/// con un cuerpo bien formado. Se cierra con un error de validacion en vez de un
/// panico.
fn missing_fields() -> AppError {
    let mut errors = ValidationErrors::new();
    errors.push("body", "Faltan campos obligatorios de la categoria.");
    AppError::Validation(errors)
}
