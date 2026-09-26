//! Repositorio de movimientos en memoria.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::movement::{Movement, MovementCursor};

use crate::error::AppError;
use crate::port::movement::{MovementFilter, MovementRepository};

/// Doble del puerto [`MovementRepository`] sobre un vector.
///
/// Aplica el mismo filtro y el mismo orden que el adaptador de PostgreSQL,
/// `(occurred_on DESC, id DESC)`, para que un test de paginacion signifique algo.
#[derive(Debug, Default)]
pub struct InMemoryMovementRepository {
    movements: Mutex<Vec<Movement>>,
}

impl InMemoryMovementRepository {
    /// Crea el repositorio vacio.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            movements: Mutex::new(Vec::new()),
        }
    }

    /// Precarga movimientos sin pasar por el caso de uso.
    pub fn seed(&self, movements: Vec<Movement>) {
        self.with(|stored| stored.extend(movements));
    }

    /// Movimientos guardados.
    #[must_use]
    pub fn all(&self) -> Vec<Movement> {
        self.with(|stored| stored.clone())
    }

    fn with<T>(&self, operation: impl FnOnce(&mut Vec<Movement>) -> T) -> T {
        match self.movements.lock() {
            Ok(mut movements) => operation(&mut movements),
            Err(poisoned) => operation(&mut poisoned.into_inner()),
        }
    }
}

fn matches(movement: &Movement, filter: &MovementFilter) -> bool {
    movement.user_id() == filter.user_id
        && filter
            .from
            .is_none_or(|from| movement.occurred_on() >= from)
        && filter.to.is_none_or(|to| movement.occurred_on() <= to)
        && filter.kind.is_none_or(|kind| movement.kind() == kind)
        && filter.after.is_none_or(|after| is_after(movement, after))
}

/// Misma condicion que `(occurred_on, id) < (cursor.date, cursor.id)` en SQL.
fn is_after(movement: &Movement, cursor: MovementCursor) -> bool {
    (movement.occurred_on(), movement.id()) < (cursor.occurred_on(), cursor.id())
}

#[async_trait]
impl MovementRepository for InMemoryMovementRepository {
    async fn list(&self, filter: &MovementFilter) -> Result<Vec<Movement>, AppError> {
        Ok(self.with(|stored| {
            let mut page: Vec<Movement> = stored
                .iter()
                .filter(|movement| matches(movement, filter))
                .cloned()
                .collect();
            page.sort_by(|left, right| {
                (right.occurred_on(), right.id()).cmp(&(left.occurred_on(), left.id()))
            });
            page.truncate(filter.limit as usize);
            page
        }))
    }

    async fn insert(&self, movement: &Movement) -> Result<(), AppError> {
        self.with(|stored| stored.push(movement.clone()));
        Ok(())
    }
}
