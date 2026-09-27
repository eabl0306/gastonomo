//! Caso de uso: que ha cambiado en la cuenta desde la ultima vez.

use std::fmt;
use std::sync::Arc;

use gastonomo_domain::value_object::UserId;

use crate::error::AppError;
use crate::port::changes::{ChangeBatch, ChangeFeedReader, ChangeNumber};

/// Cambios por lote si el cliente no pide otra cosa.
///
/// Un lote es de cinco colecciones a la vez, asi que 200 filas ya son varias
/// pantallas de cada cosa y sigue cabiendo en una respuesta pequena.
pub const DEFAULT_CHANGE_LIMIT: u32 = 200;

/// Tope por lote. Por encima de esto se acota sin avisar: el cliente pagina.
pub const MAX_CHANGE_LIMIT: u32 = 1_000;

/// Devuelve el siguiente lote de cambios de la cuenta.
#[derive(Clone)]
pub struct GetChanges {
    changes: Arc<dyn ChangeFeedReader>,
}

impl fmt::Debug for GetChanges {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("GetChanges").finish_non_exhaustive()
    }
}

impl GetChanges {
    /// Crea el caso de uso.
    #[must_use]
    pub const fn new(changes: Arc<dyn ChangeFeedReader>) -> Self {
        Self { changes }
    }

    /// Lee los cambios posteriores a `since`.
    ///
    /// # Errors
    ///
    /// Devuelve [`AppError::Dependency`] si falla la base de datos.
    pub async fn execute(
        &self,
        user_id: UserId,
        since: Option<ChangeNumber>,
        limit: Option<u32>,
    ) -> Result<ChangeBatch, AppError> {
        self.changes
            .changes_since(user_id, since, Self::page_size(limit))
            .await
    }

    const fn page_size(limit: Option<u32>) -> u32 {
        match limit {
            None | Some(0) => DEFAULT_CHANGE_LIMIT,
            Some(limit) if limit > MAX_CHANGE_LIMIT => MAX_CHANGE_LIMIT,
            Some(limit) => limit,
        }
    }
}
