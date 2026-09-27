//! Caso de uso: quitar de en medio una categoria del usuario.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::category::CategoryId;
use gastonomo_domain::value_object::UserId;

use super::parsing::domain_error;
use crate::error::AppError;
use crate::port::Clock;
use crate::port::category::CategoryRepository;

/// Que paso con la categoria.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CategoryRemoval {
    /// Tenia movimientos: se archiva y se queda donde esta.
    Archived,
    /// No la usaba ningun movimiento: se borra de verdad.
    Deleted,
}

/// Archiva una categoria del usuario, o la borra si no la usa ningun movimiento.
///
/// **Archivar no toca ni una fila de `movements`.** Antes, borrar ejecutaba
/// `UPDATE movements SET category_id = <Otros>`, y ese era el ultimo sitio por el
/// que se podia alterar como se lee el historico: los importes no cambiaban, pero
/// un gasto de hace dos anos pasaba a leerse con otra categoria. El historico es de
/// solo anadir a proposito, asi que ese camino ya no existe.
///
/// Una categoria **sin movimientos** si se borra del todo: no hay historico que
/// proteger, y asi no se acumula basura de categorias creadas por error.
#[derive(Clone)]
pub struct ArchiveCategory {
    categories: Arc<dyn CategoryRepository>,
    clock: Arc<dyn Clock>,
}

impl fmt::Debug for ArchiveCategory {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ArchiveCategory").finish_non_exhaustive()
    }
}

impl ArchiveCategory {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(categories: Arc<dyn CategoryRepository>, clock: Arc<dyn Clock>) -> Self {
        Self { categories, clock }
    }

    /// Archiva la categoria, o la borra si nadie la usa.
    ///
    /// Archivar una ya archivada no es un error: responde igual, porque desde fuera
    /// lo que se pidio ya esta hecho.
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
    ) -> Result<CategoryRemoval, AppError> {
        let mut found = self
            .categories
            .find(user_id, category_id)
            .await?
            .ok_or_else(|| AppError::not_found("category"))?;
        found
            .category
            .check_writable()
            .map_err(|error| domain_error(&error))?;

        if found.movements == 0 {
            self.categories.delete_unused(user_id, category_id).await?;
            return Ok(CategoryRemoval::Deleted);
        }

        found
            .category
            .archive(self.clock.now())
            .map_err(|error| domain_error(&error))?;
        self.categories.update(&found.category).await?;
        Ok(CategoryRemoval::Archived)
    }
}
