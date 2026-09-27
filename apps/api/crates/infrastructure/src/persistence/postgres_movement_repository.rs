//! Adaptador driven: movimientos en PostgreSQL.
//!
//! Dos cosas que conviene saber antes de tocar las consultas:
//!
//! - SQLx 0.9 solo acepta SQL literal, asi que los filtros opcionales no se
//!   concatenan: van dentro de la propia consulta con la forma
//!   `($n IS NULL OR columna <op> $n)`. Una sola consulta cubre los cuatro filtros
//!   de la pantalla y no hay SQL construido a mano en ningun sitio.
//! - La paginacion compara la tupla `(occurred_on, id)` entera, que es lo mismo que
//!   el orden `ORDER BY occurred_on DESC, id DESC` del indice
//!   `movements_user_occurred_on_idx`.

pub(super) mod row;

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::movement::{
    MovementCategoryReader, MovementFilter, MovementRepository,
};
use gastonomo_domain::UserId;
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementCursor, MovementKind,
};
use sqlx::PgPool;
use uuid::Uuid;

use super::mapping::database_error;

/// Repositorio de movimientos.
#[derive(Debug, Clone)]
pub struct PostgresMovementRepository {
    pool: PgPool,
}

impl PostgresMovementRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl MovementRepository for PostgresMovementRepository {
    async fn list(&self, filter: &MovementFilter) -> Result<Vec<Movement>, AppError> {
        let cursor_date = filter.after.map(MovementCursor::occurred_on);
        let cursor_id = filter.after.map(|cursor| cursor.id().as_uuid());
        let rows = sqlx::query(
            "SELECT m.id, m.user_id, m.occurred_on, m.kind, m.title, \
                    m.amount, m.currency, m.base, m.base_currency, \
                    m.loan_id, m.loan_payment_id, m.created_at, m.updated_at, \
                    c.id AS category_id, c.name AS category_name, c.kind AS category_kind, \
                    c.icon AS category_icon, c.color AS category_color \
             FROM movements m JOIN categories c ON c.id = m.category_id \
             WHERE m.user_id = $1 \
               AND ($2::date IS NULL OR m.occurred_on >= $2) \
               AND ($3::date IS NULL OR m.occurred_on <= $3) \
               AND ($4::text IS NULL OR m.kind = $4) \
               AND ($5::date IS NULL OR (m.occurred_on, m.id) < ($5::date, $6::uuid)) \
             ORDER BY m.occurred_on DESC, m.id DESC \
             LIMIT $7",
        )
        .bind(filter.user_id.as_uuid())
        .bind(filter.from)
        .bind(filter.to)
        .bind(filter.kind.map(MovementKind::as_str))
        .bind(cursor_date)
        .bind(cursor_id)
        .bind(i64::from(filter.limit))
        .fetch_all(&self.pool)
        .await
        .map_err(|error| database_error("could not list the movements", &error))?;

        rows.iter().map(row::movement).collect()
    }

    async fn insert(&self, movement: &Movement) -> Result<(), AppError> {
        let amount = movement.amount();
        sqlx::query(
            "INSERT INTO movements \
             (id, user_id, occurred_on, kind, category_id, title, \
              amount, currency, base, base_currency, \
              loan_id, loan_payment_id, created_at, updated_at) \
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)",
        )
        .bind(movement.id().as_uuid())
        .bind(movement.user_id().as_uuid())
        .bind(movement.occurred_on())
        .bind(movement.kind().as_str())
        .bind(movement.category().id().as_uuid())
        .bind(movement.title().as_str())
        .bind(amount.amount().decimal())
        .bind(amount.currency().as_str())
        .bind(amount.base().decimal())
        .bind(amount.base_currency().as_str())
        .bind(movement.loan_id())
        .bind(movement.loan_payment_id())
        .bind(movement.created_at())
        .bind(movement.updated_at())
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not insert the movement", &error))
    }
}

#[async_trait]
impl MovementCategoryReader for PostgresMovementRepository {
    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<MovementCategory>, AppError> {
        // El filtro por user_id es lo que hace que la categoria de otra cuenta se
        // comporte igual que una que no existe.
        let found = sqlx::query(
            "SELECT id AS category_id, name AS category_name, kind AS category_kind, \
                    icon AS category_icon, color AS category_color \
             FROM categories WHERE user_id = $1 AND id = $2",
        )
        .bind(user_id.as_uuid())
        .bind::<Uuid>(category_id.as_uuid())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the category", &error))?;
        found.as_ref().map(row::category).transpose()
    }
}
