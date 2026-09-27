//! Caso de uso: devolver a la lista una categoria archivada.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::category::CategoryId;
use gastonomo_domain::value_object::UserId;

use super::parsing::domain_error;
use crate::error::AppError;
use crate::port::Clock;
use crate::port::category::{CategoryRepository, CategoryUsage};

/// Devuelve a la lista una categoria archivada.
///
/// Hace falta porque el nombre sigue siendo unico incluyendo las archivadas: sin
/// esto, archivar por error no tendria vuelta atras, porque crear otra con el mismo
/// nombre chocaria con la unicidad.
#[derive(Clone)]
pub struct RestoreCategory {
    categories: Arc<dyn CategoryRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for RestoreCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("RestoreCategory").finish_non_exhaustive()
    }
}

impl RestoreCategory {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { categories, clock }
    }

    /// Desarchiva y devuelve la categoria ya activa.
    ///
    /// Desarchivar una que ya estaba activa responde igual: lo que se pidio ya esta.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la categoria no existe o es de otra
    /// cuenta, [`AppError::Conflict`] con codigo `category_is_default` si es una
    /// predeterminada y [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<CategoryUsage, AppError> {
        let mut found = self
            .categories
            .find(user_id, category_id)
            .await?
            .ok_or_else(|| AppError::not_found("category"))?;
        found
            .category
            .unarchive(self.clock.now())
            .map_err(|error| domain_error(&error))?;
        self.categories.update(&found.category).await?;
        Ok(found)
    }
}
