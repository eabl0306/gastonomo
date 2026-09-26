//! Adaptador driven: siembra de categorias y ajustes de una cuenta nueva.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::UserProvisioning;
use gastonomo_domain::{DEFAULT_CATEGORIES, DEFAULT_DISPLAY_CURRENCY, Language, UserId};
use sqlx::{PgPool, Postgres, Transaction};
use uuid::Uuid;

use super::mapping::database_error;

/// Siembra las 12 categorias predeterminadas y la fila de ajustes.
#[derive(Debug, Clone)]
pub struct PostgresUserProvisioning {
    pool: PgPool,
}

impl PostgresUserProvisioning {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    async fn insert_settings(
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO user_settings (user_id, display_currency, language, updated_at) \
             VALUES ($1, $2, $3, $4) ON CONFLICT (user_id) DO NOTHING",
        )
        .bind(user_id.as_uuid())
        .bind(DEFAULT_DISPLAY_CURRENCY)
        .bind(Language::default().as_str())
        .bind(now)
        .execute(&mut **transaction)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not seed the user settings", &error))
    }

    async fn insert_categories(
        transaction: &mut Transaction<'_, Postgres>,
        user_id: UserId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        for category in DEFAULT_CATEGORIES {
            sqlx::query(
                "INSERT INTO categories \
                 (id, user_id, name, kind, icon, color, is_default, created_at, updated_at) \
                 VALUES ($1, $2, $3, $4, $5, $6, TRUE, $7, $7) \
                 ON CONFLICT DO NOTHING",
            )
            .bind(Uuid::new_v4())
            .bind(user_id.as_uuid())
            .bind(category.name)
            .bind(category.kind.as_str())
            .bind(category.icon)
            .bind(category.color)
            .bind(now)
            .execute(&mut **transaction)
            .await
            .map_err(|error| database_error("could not seed the default categories", &error))?;
        }
        Ok(())
    }
}

#[async_trait]
impl UserProvisioning for PostgresUserProvisioning {
    async fn provision_new_user(
        &self,
        user_id: UserId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;
        Self::insert_settings(&mut transaction, user_id, now).await?;
        Self::insert_categories(&mut transaction, user_id, now).await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the seed", &error))
    }
}
