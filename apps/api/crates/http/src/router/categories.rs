//! Sub-router del contexto de categorias.
//!
//! Cuelga del router protegido de `/v1`, asi que sus handlers ya reciben el usuario
//! en el `Depot`: se lee con [`crate::auth_context::current_user`].

use std::fmt;
use std::sync::Arc;

use gastonomo_application::use_case::category::{
    CreateCategory, DeleteCategory, ListCategories, UpdateCategory,
};
use salvo::prelude::Router;

use crate::handler::category::{
    CreateCategoryHandler, DeleteCategoryHandler, ListCategoriesHandler, UpdateCategoryHandler,
};

/// Casos de uso que necesita el router de categorias.
#[derive(Clone)]
pub struct CategoryDependencies {
    /// Listado con el conteo de uso.
    pub list: Arc<ListCategories>,
    /// Creacion.
    pub create: Arc<CreateCategory>,
    /// Edicion.
    pub update: Arc<UpdateCategory>,
    /// Borrado con reasignacion a "Otros".
    pub delete: Arc<DeleteCategory>,
}

impl fmt::Debug for CategoryDependencies {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("CategoryDependencies")
            .finish_non_exhaustive()
    }
}

/// Monta las rutas de categorias.
#[must_use]
pub fn router(dependencies: CategoryDependencies) -> Router {
    Router::new().push(
        Router::with_path("categories")
            .get(ListCategoriesHandler::new(dependencies.list))
            .post(CreateCategoryHandler::new(dependencies.create))
            .push(
                Router::with_path("{id}")
                    .patch(UpdateCategoryHandler::new(dependencies.update))
                    .delete(DeleteCategoryHandler::new(dependencies.delete)),
            ),
    )
}
