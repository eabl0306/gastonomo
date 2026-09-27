//! Caso de uso: listar las categorias de la cuenta.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::category::{CategoryRepository, CategoryUsage};

/// Devuelve las categorias de la cuenta con su conteo de uso.
///
/// Por defecto solo las activas. Las archivadas se piden a proposito, para resolver
/// la categoria de un movimiento viejo o para recuperarla.
///
/// El orden que llega es estable (las predeterminadas primero y luego por nombre)
/// pero el cliente lo reordena: la rejilla del diseno ensena las predeterminadas en
/// el orden del handoff y separa "Mis categorias".
#[derive(Clone)]
pub struct ListCategories {
    categories: Arc<dyn CategoryRepository>,
}

impl fmt::Debug for ListCategories {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ListCategories").finish_non_exhaustive()
    }
}

impl ListCategories {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>) -> Self {
        Self { categories }
    }

    /// Lista las categorias.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        user_id: UserId,
        include_archived: bool,
    ) -> Result<Vec<CategoryUsage>, AppError> {
        self.categories
            .list_with_usage(user_id, include_archived)
            .await
    }
}
