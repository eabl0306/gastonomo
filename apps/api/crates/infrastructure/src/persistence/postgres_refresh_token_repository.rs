//! Adaptador driven: refresh tokens en PostgreSQL.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::RefreshTokenRepository;
use gastonomo_domain::{RefreshToken, RefreshTokenId, TokenDigest, UserId};
use sqlx::{PgPool, Postgres, Transaction};

use super::mapping::{database_error, refresh_token};

/// Repositorio de refresh tokens.
#[derive(Debug, Clone)]
pub struct PostgresRefreshTokenRepository {
    pool: PgPool,
}

impl PostgresRefreshTokenRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    async fn insert_within(
        transaction: &mut Transaction<'_, Postgres>,
        token: &RefreshToken,
    ) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO refresh_tokens \
             (id, user_id, token_hash, expires_at, user_agent, created_at) \
             VALUES ($1, $2, $3, $4, $5, $6)",
        )
        .bind(token.id().as_uuid())
        .bind(token.user_id().as_uuid())
        .bind(token.digest().as_str())
        .bind(token.expires_at())
        .bind(token.user_agent())
        .bind(token.created_at())
        .execute(&mut **transaction)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not insert the refresh token", &error))
    }
}

#[async_trait]
impl RefreshTokenRepository for PostgresRefreshTokenRepository {
    async fn insert(&self, token: &RefreshToken) -> Result<(), AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;
        Self::insert_within(&mut transaction, token).await?;
        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the refresh token", &error))
    }

    async fn find_by_digest(&self, digest: &TokenDigest) -> Result<Option<RefreshToken>, AppError> {
        let row = sqlx::query(
            "SELECT id, user_id, token_hash, expires_at, revoked_at, replaced_by, user_agent, \
             created_at FROM refresh_tokens WHERE token_hash = $1",
        )
        .bind(digest.as_str())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the refresh token", &error))?;
        row.as_ref().map(refresh_token).transpose()
    }

    async fn rotate(
        &self,
        current: RefreshTokenId,
        replacement: &RefreshToken,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        let mut transaction = self
            .pool
            .begin()
            .await
            .map_err(|error| database_error("could not open a transaction", &error))?;

        // El nuevo se inserta primero: `replaced_by` es una clave ajena a esta misma
        // tabla y apuntar a una fila que todavia no existe no cuela.
        Self::insert_within(&mut transaction, replacement).await?;
        sqlx::query(
            "UPDATE refresh_tokens \
             SET replaced_by = $2, revoked_at = COALESCE(revoked_at, $3) \
             WHERE id = $1",
        )
        .bind(current.as_uuid())
        .bind(replacement.id().as_uuid())
        .bind(now)
        .execute(&mut *transaction)
        .await
        .map_err(|error| database_error("could not rotate the refresh token", &error))?;

        transaction
            .commit()
            .await
            .map_err(|error| database_error("could not commit the rotation", &error))
    }

    async fn revoke(&self, token: RefreshTokenId, now: DateTime<Utc>) -> Result<(), AppError> {
        sqlx::query("UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, $2) WHERE id = $1")
            .bind(token.as_uuid())
            .bind(now)
            .execute(&self.pool)
            .await
            .map(|_| ())
            .map_err(|error| database_error("could not revoke the refresh token", &error))
    }

    async fn revoke_all_for_user(
        &self,
        user_id: UserId,
        except: Option<RefreshTokenId>,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        sqlx::query(
            "UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, $2) \
             WHERE user_id = $1 AND revoked_at IS NULL AND ($3::uuid IS NULL OR id <> $3)",
        )
        .bind(user_id.as_uuid())
        .bind(now)
        .bind(except.map(RefreshTokenId::as_uuid))
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not revoke the user sessions", &error))
    }
}
