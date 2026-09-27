//! Adaptador driven del feed de cambios.
//!
//! Todas las filas de una cuenta comparten un unico orden: su numero de cambio, que
//! reparte un disparador de PostgreSQL y es unico por usuario. Eso es lo que permite
//! que un solo cursor sirva para las cinco colecciones.
//!
//! El lote se arma leyendo un poco de cada tabla y quedandose con el tramo mas bajo
//! del orden total. Pedir `limit + 1` de cada una es suficiente: si una fila cabe
//! entre las `limit` primeras del orden global, tambien cabe entre las `limit + 1`
//! primeras de su propia tabla.

mod rows;
mod statements;

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::changes::{ChangeBatch, ChangeFeedReader, ChangeNumber};
use gastonomo_domain::value_object::UserId;
use sqlx::{PgPool, Postgres, Transaction};

use super::mapping::database_error;
use rows::{Numbered, Sources};

/// Cambios de una cuenta sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresChangeFeed {
    pool: PgPool,
}

impl PostgresChangeFeed {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl ChangeFeedReader for PostgresChangeFeed {
    async fn changes_since(
        &self,
        user_id: UserId,
        since: Option<ChangeNumber>,
        limit: u32,
    ) -> Result<ChangeBatch, AppError> {
        // En una sola transaccion: si no, una escritura entre dos consultas podria
        // dejar un lote donde el movimiento esta y su categoria no.
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;
        let sources = Sources::read(&mut transaction, user_id, since, limit).await?;
        // Sin nada que mandar el cursor no retrocede: se queda donde el cliente ya
        // estaba, o en el ultimo numero de la cuenta si venia en frio.
        let fallback = match since {
            Some(cursor) => cursor,
            None => last_seq(&mut transaction, user_id).await?,
        };
        let (cursor, has_more) = cutoff(sources.numbers(), limit as usize, fallback);
        let batch = sources
            .batch(&mut transaction, user_id, cursor, has_more)
            .await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not close the read transaction", &error))?;
        Ok(batch)
    }
}

/// Hasta donde llega el lote, y si queda algo detras.
///
/// Se juntan los numeros de las cinco consultas y se corta en el que hace `limit`.
/// Cortar por numero y no por coleccion es lo que hace que el cursor sea seguro: el
/// cliente que guarda `cursor` tiene *todo* lo que hay por debajo.
fn cutoff(
    mut seqs: Vec<ChangeNumber>,
    limit: usize,
    fallback: ChangeNumber,
) -> (ChangeNumber, bool) {
    seqs.sort_unstable();
    if seqs.len() > limit {
        let at = limit.saturating_sub(1);
        return (seqs.get(at).copied().unwrap_or(fallback), true);
    }
    (seqs.last().copied().unwrap_or(fallback), false)
}

/// Ultimo numero repartido a la cuenta, o cero si todavia no escribio nada.
async fn last_seq(
    transaction: &mut Transaction<'_, Postgres>,
    user_id: UserId,
) -> Result<ChangeNumber, AppError> {
    let row = sqlx::query(statements::LAST_SEQ)
        .bind(user_id.as_uuid())
        .fetch_optional(&mut **transaction)
        .await
        .map_err(|error| database_error("could not read the change counter", &error))?;
    row.as_ref()
        .map(|row| Numbered::column(row, "last_seq"))
        .transpose()
        .map(Option::unwrap_or_default)
}
