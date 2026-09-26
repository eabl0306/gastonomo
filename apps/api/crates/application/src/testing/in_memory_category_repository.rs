//! Repositorio de categorias en memoria.

use std::collections::HashMap;
use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::category::{
    Category, CategoryColor, CategoryIcon, CategoryId, CategoryName, DEFAULT_CATEGORIES,
    NewCategory,
};
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::category::{CategoryRepository, CategoryUsage};

/// Doble del puerto [`CategoryRepository`] sobre un vector y un contador.
#[derive(Debug, Default)]
pub struct InMemoryCategoryRepository {
    rows: Mutex<Vec<Category>>,
    usage: Mutex<HashMap<CategoryId, u32>>,
}

impl InMemoryCategoryRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub fn new() -> Self {
        Self {
            rows: Mutex::new(Vec::new()),
            usage: Mutex::new(HashMap::new()),
        }
    }

    /// Guarda una categoria sin comprobar nada, para preparar un escenario.
    pub fn seed(&self, category: Category) {
        self.with_rows(|rows| rows.push(category));
    }

    /// Siembra las 12 predeterminadas de una cuenta, como hace el registro.
    pub fn seed_defaults(&self, user_id: UserId, now: DateTime<Utc>) {
        for default in DEFAULT_CATEGORIES {
            let Ok(name) = CategoryName::parse(default.name) else {
                continue;
            };
            let Ok(icon) = CategoryIcon::parse(default.icon) else {
                continue;
            };
            let Ok(color) = CategoryColor::parse(default.color) else {
                continue;
            };
            self.seed(Category::rehydrate(
                CategoryId::generate(),
                NewCategory {
                    user_id,
                    name,
                    kind: default.kind,
                    icon,
                    color,
                    created_at: now,
                },
                true,
                now,
            ));
        }
    }

    /// Fija cuantos movimientos usan una categoria.
    pub fn set_usage(&self, category_id: CategoryId, movements: u32) {
        self.with_usage(|usage| {
            usage.insert(category_id, movements);
        });
    }

    /// Movimientos que usan una categoria.
    #[must_use]
    pub fn usage_of(&self, category_id: CategoryId) -> u32 {
        self.with_usage(|usage| usage.get(&category_id).copied().unwrap_or_default())
    }

    /// Categorias guardadas.
    #[must_use]
    pub fn count(&self) -> usize {
        self.with_rows(|rows| rows.len())
    }

    /// Categoria guardada con ese nombre, sin distinguir mayusculas.
    #[must_use]
    pub fn by_name(&self, name: &str) -> Option<Category> {
        self.with_rows(|rows| {
            rows.iter()
                .find(|category| category.name().same_as(name))
                .cloned()
        })
    }

    fn with_rows<T>(&self, operation: impl FnOnce(&mut Vec<Category>) -> T) -> T {
        match self.rows.lock() {
            Ok(mut rows) => operation(&mut rows),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }

    fn with_usage<T>(&self, operation: impl FnOnce(&mut HashMap<CategoryId, u32>) -> T) -> T {
        match self.usage.lock() {
            Ok(mut usage) => operation(&mut usage),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }

    fn name_taken(rows: &[Category], category: &Category) -> bool {
        rows.iter().any(|stored| {
            stored.user_id() == category.user_id()
                && stored.id() != category.id()
                && stored.name().comparison_key() == category.name().comparison_key()
        })
    }
}

#[async_trait]
impl CategoryRepository for InMemoryCategoryRepository {
    async fn list_with_usage(&self, user_id: UserId) -> Result<Vec<CategoryUsage>, AppError> {
        let mut found = self.with_rows(|rows| {
            rows.iter()
                .filter(|category| category.user_id() == user_id)
                .cloned()
                .collect::<Vec<_>>()
        });
        found.sort_by(|left, right| {
            right.is_default().cmp(&left.is_default()).then_with(|| {
                left.name()
                    .comparison_key()
                    .cmp(&right.name().comparison_key())
            })
        });
        Ok(found
            .into_iter()
            .map(|category| CategoryUsage {
                movements: self.usage_of(category.id()),
                category,
            })
            .collect())
    }

    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<CategoryUsage>, AppError> {
        Ok(self
            .with_rows(|rows| {
                rows.iter()
                    .find(|category| category.user_id() == user_id && category.id() == category_id)
                    .cloned()
            })
            .map(|category| CategoryUsage {
                movements: self.usage_of(category.id()),
                category,
            }))
    }

    async fn find_by_name(
        &self,
        user_id: UserId,
        name: &str,
    ) -> Result<Option<Category>, AppError> {
        Ok(self.with_rows(|rows| {
            rows.iter()
                .find(|category| category.user_id() == user_id && category.name().same_as(name))
                .cloned()
        }))
    }

    async fn insert(&self, category: &Category) -> Result<(), AppError> {
        self.with_rows(|rows| {
            if Self::name_taken(rows, category) {
                return Err(name_taken_error());
            }
            rows.push(category.clone());
            Ok(())
        })
    }

    async fn update(&self, category: &Category) -> Result<(), AppError> {
        self.with_rows(|rows| {
            if Self::name_taken(rows, category) {
                return Err(name_taken_error());
            }
            let Some(slot) = rows.iter_mut().find(|stored| stored.id() == category.id()) else {
                return Err(AppError::not_found("category"));
            };
            *slot = category.clone();
            Ok(())
        })
    }

    async fn delete_reassigning_movements(
        &self,
        user_id: UserId,
        category_id: CategoryId,
        fallback_id: CategoryId,
    ) -> Result<u32, AppError> {
        let removed = self.with_rows(|rows| {
            let Some(position) = rows
                .iter()
                .position(|category| category.user_id() == user_id && category.id() == category_id)
            else {
                return Err(AppError::not_found("category"));
            };
            Ok(rows.remove(position))
        })?;
        removed.check_writable().map_err(|_| {
            AppError::conflict(
                "category_is_default",
                "Las categorias predeterminadas no se pueden editar ni borrar.",
            )
        })?;
        let moved = self.usage_of(category_id);
        self.with_usage(|usage| {
            usage.remove(&category_id);
            *usage.entry(fallback_id).or_default() += moved;
        });
        Ok(moved)
    }
}

fn name_taken_error() -> AppError {
    AppError::conflict(
        "category_name_taken",
        "Ya existe una categoria con ese nombre.",
    )
}
