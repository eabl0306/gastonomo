//! Adaptador driven del contexto de categorias.
//!
//! Todas las consultas filtran por `user_id`: una categoria de otra cuenta se
//! comporta como si no existiera. El borrado va en una transaccion, porque el
//! diseno dice que los movimientos de la categoria borrada pasan a "Otros" y a
//! medias quedarian apuntando a una fila que ya no esta.
//!
//! sqlx 0.9 solo acepta SQL literal, asi que la lista de columnas se repite en cada
//! consulta en vez de salir de una constante.

mod row;

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::category::{CategoryRepository, CategoryUsage};
use gastonomo_domain::category::{Category, CategoryId};
use gastonomo_domain::value_object::UserId;
use sqlx::{PgPool, Postgres, Transaction};

use super::mapping::{database_error, is_unique_violation};

/// Categorias sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresCategoryRepository {
    pool: PgPool,
}

impl PostgresCategoryRepository {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    async fn reassign_movements(
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        category_id: CategoryId,
        fallback_id: CategoryId,
    ) -> Result<u32, AppError> {
        let moved = sqlx::query(
            "UPDATE movements SET category_id = $3, updated_at = now() \
             WHERE user_id = $1 AND category_id = $2",
        )
        .bind(user_id.as_uuid())
        .bind(category_id.as_uuid())
        .bind(fallback_id.as_uuid())
        .execute(&mut **transaction)
        .await
        .map_err(|error| database_error("could not reassign the movements", &error))?
        .rows_affected();
        Ok(u32::try_from(moved).unwrap_or(u32::MAX))
    }

    async fn delete_row(
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<(), AppError> {
        let deleted = sqlx::query(
            "DELETE FROM categories \
             WHERE id = $2 AND user_id = $1 AND is_default = FALSE",
        )
        .bind(user_id.as_uuid())
        .bind(category_id.as_uuid())
        .execute(&mut **transaction)
        .await
        .map_err(|error| database_error("could not delete the category", &error))?
        .rows_affected();
        if deleted == 0 {
            return Err(AppError::not_found("category"));
        }
        Ok(())
    }
}

#[async_trait]
impl CategoryRepository for PostgresCategoryRepository {
    async fn list_with_usage(&self, user_id: UserId) -> Result<Vec<CategoryUsage>, AppError> {
        let rows = sqlx::query(
            "SELECT c.id, c.user_id, c.name, c.kind, c.icon, c.color, c.is_default, \
                    c.created_at, c.updated_at, \
                    (SELECT count(*) FROM movements m \
                      WHERE m.category_id = c.id AND m.user_id = c.user_id) \
                      AS movement_count \
             FROM categories c \
             WHERE c.user_id = $1 \
             ORDER BY c.is_default DESC, lower(c.name)",
        )
        .bind(user_id.as_uuid())
        .fetch_all(&self.pool)
        .await
        .map_err(|error| database_error("could not list the categories", &error))?;
        rows.iter().map(row::category_with_usage).collect()
    }

    async fn find(
        &self,
        user_id: UserId,
        category_id: CategoryId,
    ) -> Result<Option<CategoryUsage>, AppError> {
        let found = sqlx::query(
            "SELECT c.id, c.user_id, c.name, c.kind, c.icon, c.color, c.is_default, \
                    c.created_at, c.updated_at, \
                    (SELECT count(*) FROM movements m \
                      WHERE m.category_id = c.id AND m.user_id = c.user_id) \
                      AS movement_count \
             FROM categories c \
             WHERE c.user_id = $1 AND c.id = $2",
        )
        .bind(user_id.as_uuid())
        .bind(category_id.as_uuid())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the category", &error))?;
        found.as_ref().map(row::category_with_usage).transpose()
    }

    async fn find_by_name(
        &self,
        user_id: UserId,
        name: &str,
    ) -> Result<Option<Category>, AppError> {
        let found = sqlx::query(
            "SELECT id, user_id, name, kind, icon, color, is_default, created_at, updated_at \
             FROM categories \
             WHERE user_id = $1 AND lower(name) = lower($2)",
        )
        .bind(user_id.as_uuid())
        .bind(name.trim())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the category by name", &error))?;
        found.as_ref().map(row::category).transpose()
    }

    async fn insert(&self, category: &Category) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO categories \
             (id, user_id, name, kind, icon, color, is_default, created_at, updated_at) \
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)",
        )
        .bind(category.id().as_uuid())
        .bind(category.user_id().as_uuid())
        .bind(category.name().as_str())
        .bind(category.kind().as_str())
        .bind(category.icon().as_str())
        .bind(category.color().as_str())
        .bind(category.is_default())
        .bind(category.created_at())
        .bind(category.updated_at())
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| {
            if is_unique_violation(&error) {
                name_taken()
            } else {
                database_error("could not store the category", &error)
            }
        })
    }

    async fn update(&self, category: &Category) -> Result<(), AppError> {
        let updated = sqlx::query(
            "UPDATE categories \
             SET name = $3, kind = $4, icon = $5, color = $6, updated_at = $7 \
             WHERE id = $1 AND user_id = $2 AND is_default = FALSE",
        )
        .bind(category.id().as_uuid())
        .bind(category.user_id().as_uuid())
        .bind(category.name().as_str())
        .bind(category.kind().as_str())
        .bind(category.icon().as_str())
        .bind(category.color().as_str())
        .bind(category.updated_at())
        .execute(&self.pool)
        .await
        .map_err(|error| {
            if is_unique_violation(&error) {
                name_taken()
            } else {
                database_error("could not update the category", &error)
            }
        })?
        .rows_affected();
        if updated == 0 {
            return Err(AppError::not_found("category"));
        }
        Ok(())
    }

    async fn delete_reassigning_movements(
        &self,
        user_id: UserId,
        category_id: CategoryId,
        fallback_id: CategoryId,
    ) -> Result<u32, AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;
        let moved =
            Self::reassign_movements(&mut transaction, user_id, category_id, fallback_id).await?;
        Self::delete_row(&mut transaction, user_id, category_id).await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the deletion", &error))?;
        Ok(moved)
    }
}

fn name_taken() -> AppError {
    AppError::conflict(
        "category_name_taken",
        "Ya existe una categoria con ese nombre.",
    )
}
