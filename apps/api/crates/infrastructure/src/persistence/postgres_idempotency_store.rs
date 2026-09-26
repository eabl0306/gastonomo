//! Adaptador driven: claves de idempotencia sobre PostgreSQL.
//!
//! La reserva es un `INSERT ... ON CONFLICT DO NOTHING` con `RETURNING`: una sola
//! sentencia atomica que dice si la clave era nueva. Dos reintentos simultaneos no
//! pueden recibir los dos "era nueva", que es justo la carrera que hay que evitar.

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::{
    IDEMPOTENCY_RETENTION_HOURS, IdempotencyStore, IdempotentRequest, IdempotentResponse,
    Reservation,
};
use sqlx::{PgPool, Row as _};

use crate::persistence::mapping::database_error;

/// Claves de idempotencia sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresIdempotencyStore {
    pool: PgPool,
}

impl PostgresIdempotencyStore {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    /// Borra las claves de la cuenta que ya han caducado.
    ///
    /// Se hace aqui, al reservar, y no con una tarea aparte: la purga es una
    /// sentencia acotada por cuenta y por indice, y asi la tabla no crece sin
    /// limite sin montar un planificador para dos lineas de SQL.
    async fn purge_expired(&self, request: IdempotentRequest<'_>) -> Result<(), AppError> {
        sqlx::query(
            "DELETE FROM idempotency_keys \
             WHERE user_id = $1 AND created_at < now() - make_interval(hours => $2)",
        )
        .bind(request.user_id.as_uuid())
        .bind(i32::try_from(IDEMPOTENCY_RETENTION_HOURS).unwrap_or(24))
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not purge expired idempotency keys", &error))
    }

    async fn stored(&self, request: IdempotentRequest<'_>) -> Result<Reservation, AppError> {
        let row = sqlx::query(
            "SELECT fingerprint, response_status, response_body FROM idempotency_keys \
             WHERE user_id = $1 AND endpoint = $2 AND key = $3",
        )
        .bind(request.user_id.as_uuid())
        .bind(request.endpoint)
        .bind(request.key)
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the idempotency key", &error))?;
        // Si la fila ya no esta es que otra purga se la llevo entre las dos
        // sentencias. Tratarlo como nueva es lo correcto: no hay respuesta que
        // repetir.
        let Some(row) = row else {
            return Ok(Reservation::Fresh);
        };
        read(&row, request)
    }
}

fn read(
    row: &sqlx::postgres::PgRow,
    request: IdempotentRequest<'_>,
) -> Result<Reservation, AppError> {
    let fingerprint: String = row
        .try_get("fingerprint")
        .map_err(|error| database_error("could not read the idempotency fingerprint", &error))?;
    if fingerprint != request.fingerprint {
        return Ok(Reservation::Mismatch);
    }
    let status: Option<i16> = row
        .try_get("response_status")
        .map_err(|error| database_error("could not read the idempotent status", &error))?;
    let body: Option<Vec<u8>> = row
        .try_get("response_body")
        .map_err(|error| database_error("could not read the idempotent body", &error))?;
    match (status, body) {
        (Some(status), Some(body)) => Ok(Reservation::Replay(IdempotentResponse {
            status: u16::try_from(status).unwrap_or(200),
            body,
        })),
        _ => Ok(Reservation::InProgress),
    }
}

#[async_trait]
impl IdempotencyStore for PostgresIdempotencyStore {
    async fn reserve(&self, request: IdempotentRequest<'_>) -> Result<Reservation, AppError> {
        self.purge_expired(request).await?;
        let reserved = sqlx::query(
            "INSERT INTO idempotency_keys (user_id, endpoint, key, fingerprint) \
             VALUES ($1, $2, $3, $4) \
             ON CONFLICT (user_id, endpoint, key) DO NOTHING \
             RETURNING key",
        )
        .bind(request.user_id.as_uuid())
        .bind(request.endpoint)
        .bind(request.key)
        .bind(request.fingerprint)
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not reserve the idempotency key", &error))?;
        if reserved.is_some() {
            return Ok(Reservation::Fresh);
        }
        self.stored(request).await
    }

    async fn complete(
        &self,
        request: IdempotentRequest<'_>,
        response: &IdempotentResponse,
    ) -> Result<(), AppError> {
        sqlx::query(
            "UPDATE idempotency_keys \
             SET response_status = $4, response_body = $5, completed_at = now() \
             WHERE user_id = $1 AND endpoint = $2 AND key = $3 AND response_status IS NULL",
        )
        .bind(request.user_id.as_uuid())
        .bind(request.endpoint)
        .bind(request.key)
        .bind(i16::try_from(response.status).unwrap_or(200))
        .bind(&response.body)
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not record the idempotent response", &error))
    }

    async fn release(&self, request: IdempotentRequest<'_>) -> Result<(), AppError> {
        sqlx::query(
            "DELETE FROM idempotency_keys \
             WHERE user_id = $1 AND endpoint = $2 AND key = $3 AND response_status IS NULL",
        )
        .bind(request.user_id.as_uuid())
        .bind(request.endpoint)
        .bind(request.key)
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not release the idempotency key", &error))
    }
}
