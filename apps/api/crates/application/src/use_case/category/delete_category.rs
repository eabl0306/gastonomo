//! Caso de uso: borrar una categoria del usuario.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::category::{CategoryId, FALLBACK_CATEGORY_NAME};
use gastonomo_domain::value_object::UserId;

use super::parsing::domain_error;
use crate::error::AppError;
use crate::port::category::CategoryRepository;

/// Borra una categoria del usuario y pasa sus movimientos a "Otros".
///
/// Las dos escrituras van en una transaccion dentro del adaptador: el diseno dice
/// "borrar pasa sus movimientos a Otros", y a medias dejaria movimientos apuntando
/// a una categoria que ya no existe.
#[derive(Clone)]
pub struct DeleteCategory {
    categories: Arc<dyn CategoryRepository>,
}

impl fmt::Debug for DeleteCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("DeleteCategory").finish_non_exhaustive()
    }
}

impl DeleteCategory {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>) -> Self {
        Self { categories }
    }

    /// Borra la categoria y devuelve cuantos movimientos se reasignaron.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::NotFound`] si la categoria no existe o es de otra
    /// cuenta, [`AppError::Conflict`] con codigo `category_is_default` si es una
    /// predeterminada y [`AppError::Dependency`] si falla la base de datos o si la
    /// cuenta no tiene la categoria "Otros".
    pub async fn execute(&self, user_id: UserId, category_id: CategoryId) -> Result<u32, AppError> {
        let found = self
            .categories
            .find(user_id, category_id)
            .await?
            .ok_or_else(|| AppError::not_found("category"))?;
        found
            .category
            .check_writable()
            .map_err(|error| domain_error(&error))?;

        let fallback = self
            .categories
            .find_by_name(user_id, FALLBACK_CATEGORY_NAME)
            .await?
            .ok_or_else(|| {
                AppError::dependency(format!(
                    "the account has no {FALLBACK_CATEGORY_NAME} category to reassign movements to"
                ))
            })?;

        self.categories
            .delete_reassigning_movements(user_id, category_id, fallback.id())
            .await
    }
}
