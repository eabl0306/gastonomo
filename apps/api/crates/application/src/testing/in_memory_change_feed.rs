//! Doble del feed de cambios.
//!
//! No reproduce la numeracion de PostgreSQL: el reparto de numeros es una propiedad
//! de como se guardan las filas y se prueba contra la base de verdad, en
//! `postgres_changes.rs`. Aqui solo hace falta un lote programable para que los
//! tests del endpoint puedan comprobar la forma de la respuesta y el cursor.

use std::sync::Mutex;

use async_trait::async_trait;
use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::changes::{ChangeBatch, ChangeFeedReader, ChangeNumber};

/// Feed de cambios en memoria.
#[derive(Debug, Default)]
pub struct InMemoryChangeFeed {
    batch: Mutex<Option<ChangeBatch>>,
    calls: Mutex<Vec<(Option<ChangeNumber>, u32)>>,
}

impl InMemoryChangeFeed {
    /// Crea el doble sin nada que devolver.
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// Programa el lote que devolvera la proxima lectura.
    pub fn set_batch(&self, batch: ChangeBatch) {
        if let Ok(mut slot) = self.batch.lock() {
            *slot = Some(batch);
        }
    }

    /// Lo que se pidio, para comprobar que el handler acota el tamano de lote.
    #[must_use]
    pub fn calls(&self) -> Vec<(Option<ChangeNumber>, u32)> {
        self.calls
            .lock()
            .map(|calls| calls.clone())
            .unwrap_or_default()
    }
}

#[async_trait]
impl ChangeFeedReader for InMemoryChangeFeed {
    async fn changes_since(
        &self,
        _user_id: UserId,
        since: Option<ChangeNumber>,
        limit: u32,
    ) -> Result<ChangeBatch, AppError> {
        if let Ok(mut calls) = self.calls.lock() {
            calls.push((since, limit));
        }
        Ok(self
            .batch
            .lock()
            .ok()
            .and_then(|slot| slot.clone())
            .unwrap_or_else(|| ChangeBatch::empty(since.unwrap_or_default())))
    }
}
