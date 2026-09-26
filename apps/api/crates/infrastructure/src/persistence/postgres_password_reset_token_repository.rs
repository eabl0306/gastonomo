//! Adaptador driven: tokens de recuperacion en PostgreSQL.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::PasswordResetTokenRepository;
use gastonomo_domain::{PasswordResetToken, PasswordResetTokenId, TokenDigest};
use sqlx::PgPool;

use super::mapping::{database_error, password_reset_token};

/// Repositorio de tokens de recuperacion.
#[derive(Debug, Clone)]
pub struct PostgresPasswordResetTokenRepository {
    pool: PgPool,
}

impl PostgresPasswordResetTokenRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl PasswordResetTokenRepository for PostgresPasswordResetTokenRepository {
    async fn insert(&self, token: &PasswordResetToken) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO password_reset_tokens \
             (id, user_id, token_hash, expires_at, created_at) \
             VALUES ($1, $2, $3, $4, $5)",
        )
        .bind(token.id().as_uuid())
        .bind(token.user_id().as_uuid())
        .bind(token.digest().as_str())
        .bind(token.expires_at())
        .bind(token.created_at())
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not insert the password reset token", &error))
    }

    async fn find_by_digest(
        &self,
        digest: &TokenDigest,
    ) -> Result<Option<PasswordResetToken>, AppError> {
        let row = sqlx::query(
            "SELECT id, user_id, token_hash, expires_at, used_at, created_at \
             FROM password_reset_tokens WHERE token_hash = $1",
        )
        .bind(digest.as_str())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the password reset token", &error))?;
        row.as_ref().map(password_reset_token).transpose()
    }

    async fn mark_used(
        &self,
        token: PasswordResetTokenId,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        sqlx::query(
            "UPDATE password_reset_tokens SET used_at = COALESCE(used_at, $2) WHERE id = $1",
        )
        .bind(token.as_uuid())
        .bind(now)
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not mark the reset token as used", &error))
    }
}
