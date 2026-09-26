//! Montaje comun de los tests de categorias.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::category::{
    Category, CategoryColor, CategoryIcon, CategoryId, CategoryKind, CategoryName, NewCategory,
};
use gastonomo_domain::value_object::UserId;

use crate::port::Clock as _;
use crate::port::category::CategoryRepository as _;
use crate::testing::{FixedClock, InMemoryCategoryRepository, block_on};
use crate::use_case::category::{
    CreateCategory, CreateCategoryCommand, DeleteCategory, ListCategories, UpdateCategory,
    UpdateCategoryCommand,
};

/// Todo lo que un test de categorias necesita, ya cableado.
pub struct Harness {
    pub categories: Arc<InMemoryCategoryRepository>,
    pub clock: Arc<FixedClock>,
    pub list: ListCategories,
    pub create: CreateCategory,
    pub update: UpdateCategory,
    pub delete: DeleteCategory,
    pub user: UserId,
    pub other_user: UserId,
}

impl Harness {
    /// Cablea los dobles con las 12 predeterminadas ya sembradas.
    #[must_use]
    pub fn new() -> Self {
        let categories = Arc::new(InMemoryCategoryRepository::new());
        let clock = Arc::new(FixedClock::at_fixture());
        let user = UserId::generate();
        let other_user = UserId::generate();
        categories.seed_defaults(user, clock.now());
        categories.seed_defaults(other_user, clock.now());
        Self {
            list: ListCategories::new(categories.clone()),
            create: CreateCategory::new(categories.clone(), clock.clone()),
            update: UpdateCategory::new(categories.clone(), clock.clone()),
            delete: DeleteCategory::new(categories.clone()),
            categories,
            clock,
            user,
            other_user,
        }
    }

    /// Identificador de una predeterminada de la cuenta, por nombre.
    pub fn default_id(&self, name: &str) -> CategoryId {
        block_on(self.categories.find_by_name(self.user, name))
            .expect("should query")
            .expect("the default catalog is seeded")
            .id()
    }

    /// Guarda una categoria del usuario sin pasar por el caso de uso.
    pub fn seed_custom(&self, user_id: UserId, name: &str) -> CategoryId {
        let category = Category::create_custom(NewCategory {
            user_id,
            name: CategoryName::parse(name).expect("valid name"),
            kind: CategoryKind::Out,
            icon: CategoryIcon::parse("pets").expect("offered icon"),
            color: CategoryColor::parse("#2f7fb5").expect("offered color"),
            created_at: self.clock.now(),
        })
        .expect("the fixture respects the design rules");
        let id = category.id();
        self.categories.seed(category);
        id
    }
}

/// Cuerpo valido del sheet de categoria.
#[must_use]
pub fn creation(user_id: UserId, name: &str) -> CreateCategoryCommand {
    CreateCategoryCommand {
        user_id,
        name: name.to_owned(),
        kind: "out".to_owned(),
        icon: "pets".to_owned(),
        color: "#2f7fb5".to_owned(),
    }
}

/// Cuerpo de `PATCH` que no pide cambiar nada.
#[must_use]
pub fn empty_change(user_id: UserId, category_id: CategoryId) -> UpdateCategoryCommand {
    UpdateCategoryCommand {
        user_id,
        category_id,
        name: None,
        kind: None,
        icon: None,
        color: None,
    }
}
