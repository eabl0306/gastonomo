//! Adaptador driven: cuentas en PostgreSQL.

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::UserRepository;
use gastonomo_domain::{Email, PasswordHash, User, UserId};
use sqlx::PgPool;

use super::mapping::{database_error, is_unique_violation, user};

/// Repositorio de cuentas.
#[derive(Debug, Clone)]
pub struct PostgresUserRepository {
    pool: PgPool,
}

impl PostgresUserRepository {
    /// Crea el repositorio sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl UserRepository for PostgresUserRepository {
    async fn insert(&self, new_user: &User) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO users (id, name, email, password_hash, created_at, updated_at) \
             VALUES ($1, $2, $3::citext, $4, $5, $6)",
        )
        .bind(new_user.id().as_uuid())
        .bind(new_user.name().as_str())
        .bind(new_user.email().as_str())
        .bind(new_user.password_hash().as_str())
        .bind(new_user.created_at())
        .bind(new_user.updated_at())
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| {
            if is_unique_violation(&error) {
                AppError::conflict("email_taken", "Ya existe una cuenta con ese correo.")
            } else {
                database_error("could not insert the user", &error)
            }
        })
    }

    async fn find_by_email(&self, email: &Email) -> Result<Option<User>, AppError> {
        // SQLx 0.9 solo acepta SQL literal, asi que la lista de columnas se repite
        // en cada consulta en vez de vivir en una constante.
        let row = sqlx::query(
            "SELECT id, name, email::text AS email, password_hash, created_at, updated_at \
             FROM users WHERE email = $1::citext",
        )
        .bind(email.as_str())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the user by email", &error))?;
        row.as_ref().map(user).transpose()
    }

    async fn find_by_id(&self, user_id: UserId) -> Result<Option<User>, AppError> {
        let row = sqlx::query(
            "SELECT id, name, email::text AS email, password_hash, created_at, updated_at \
             FROM users WHERE id = $1",
        )
        .bind(user_id.as_uuid())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the user by id", &error))?;
        row.as_ref().map(user).transpose()
    }

    async fn update_password(
        &self,
        user_id: UserId,
        password_hash: &PasswordHash,
        now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        let result =
            sqlx::query("UPDATE users SET password_hash = $2, updated_at = $3 WHERE id = $1")
                .bind(user_id.as_uuid())
                .bind(password_hash.as_str())
                .bind(now)
                .execute(&self.pool)
                .await
                .map_err(|error| database_error("could not update the password", &error))?;
        if result.rows_affected() == 0 {
            return Err(AppError::not_found("user"));
        }
        Ok(())
    }
}
