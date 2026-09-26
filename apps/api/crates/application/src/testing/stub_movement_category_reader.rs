//! Lector de categorias de movimiento con respuestas puestas a mano.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::UserId;
use gastonomo_domain::movement::{CategoryId, MovementCategory};

use crate::error::AppError;
use crate::port::movement::MovementCategoryReader;

/// Doble del puerto [`MovementCategoryReader`].
///
/// Guarda pares de cuenta y categoria: una categoria que no se registro para esa
/// cuenta no aparece, que es justo lo que tiene que pasar con la de otro usuario.
#[derive(Debug, Default)]
pub struct StubMovementCategoryReader {
    entries: Mutex<Vec<(UserId, MovementCategory)>>,
}

impl StubMovementCategoryReader {
    /// Crea el lector sin categorias.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            entries: Mutex::new(Vec::new()),
        }
    }

    /// Anade una categoria a una cuenta.
    pub fn add(&self, user_id: UserId, category: MovementCategory) {
        self.with(|entries| entries.push((user_id, category)));
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<(UserId, MovementCategory)>) -> T) -> T {
        match self.entries.lock() {
            Ok(mut entries) => operation(&mut entries),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

#[async_trait]
impl MovementCategoryReader for StubMovementCategoryReader {
    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<MovementCategory>, AppError> {
        Ok(self.with(|entries| {
            entries
                .iter()
                .find(|(owner, category)| *owner == user_id && category.id() == category_id)
                .map(|(_, category)| category.clone())
        }))
    }
}
