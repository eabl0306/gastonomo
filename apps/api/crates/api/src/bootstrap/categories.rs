//! Cableado del contexto de categorias.

use std::sync::Arc;

use gastonomo_application::use_case::category::{
    CreateCategory, DeleteCategory, ListCategories, UpdateCategory,
};
use gastonomo_http::CategoryDependencies;
use gastonomo_infrastructure::persistence::PostgresCategoryRepository;

use super::wiring::Wiring;

/// Monta los casos de uso de categorias con el adaptador de PostgreSQL.
#[must_use]
pub fn dependencies(wiring: &Wiring) -> CategoryDependencies {
    let categories = Arc::new(PostgresCategoryRepository::new(wiring.pool.clone()));
    CategoryDependencies {
        list: Arc::new(ListCategories::new(categories.clone())),
        create: Arc::new(CreateCategory::new(
            categories.clone(),
            wiring.clock.clone(),
        )),
        update: Arc::new(UpdateCategory::new(
            categories.clone(),
            wiring.clock.clone(),
        )),
        delete: Arc::new(DeleteCategory::new(categories)),
    }
}
