//! Traduccion entre filas de PostgreSQL y el dominio.
//!
//! Una fila que no encaja en el dominio no es un error del usuario: es una fila
//! corrupta o un esquema que se ha movido. Por eso se traduce a
//! [`AppError::Dependency`] y no a un error de validacion.

use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_domain::{
    Email, NewPasswordResetToken, NewRefreshToken, NewUser, PasswordHash, PasswordResetToken,
    PasswordResetTokenId, PersonName, RefreshToken, RefreshTokenId, StoredRefreshToken,
    TokenDigest, User, UserId,
};
use sqlx::Row as _;
use sqlx::postgres::PgRow;

/// Codigo SQLSTATE de violacion de unicidad.
const UNIQUE_VIOLATION: &str = "23505";

/// Traduce un fallo de SQLx a un error de aplicacion.
#[must_use]
pub fn database_error(context: &str, error: &sqlx::Error) -> AppError {
    AppError::dependency(format!("{context}: {error}"))
}

/// Indica si el fallo es una violacion de unicidad.
#[must_use]
pub fn is_unique_violation(error: &sqlx::Error) -> bool {
    error
        .as_database_error()
        .and_then(sqlx::error::DatabaseError::code)
        .is_some_and(|code| code == UNIQUE_VIOLATION)
}

fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

fn text(row: &PgRow, column: &str) -> Result<String, AppError> {
    row.try_get::<String, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

fn timestamp(row: &PgRow, column: &str) -> Result<DateTime<Utc>, AppError> {
    row.try_get::<DateTime<Utc>, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

fn optional_timestamp(row: &PgRow, column: &str) -> Result<Option<DateTime<Utc>>, AppError> {
    row.try_get::<Option<DateTime<Utc>>, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

fn identifier(row: &PgRow, column: &str) -> Result<uuid::Uuid, AppError> {
    row.try_get::<uuid::Uuid, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

/// Reconstruye una cuenta desde su fila.
pub fn user(row: &PgRow) -> Result<User, AppError> {
    let name = PersonName::parse(&text(row, "name")?)
        .map_err(|error| corrupt("name", &error.to_string()))?;
    let email =
        Email::parse(&text(row, "email")?).map_err(|error| corrupt("email", &error.to_string()))?;
    let password_hash = PasswordHash::parse(&text(row, "password_hash")?)
        .map_err(|error| corrupt("password_hash", &error.to_string()))?;
    Ok(User::rehydrate(
        UserId::from_uuid(identifier(row, "id")?),
        NewUser {
            name,
            email,
            password_hash,
            created_at: timestamp(row, "created_at")?,
        },
        timestamp(row, "updated_at")?,
    ))
}

/// Reconstruye un refresh token desde su fila.
pub fn refresh_token(row: &PgRow) -> Result<RefreshToken, AppError> {
    let digest = TokenDigest::parse(&text(row, "token_hash")?)
        .map_err(|error| corrupt("token_hash", &error.to_string()))?;
    let replaced_by = row
        .try_get::<Option<uuid::Uuid>, _>("replaced_by")
        .map_err(|error| database_error("could not read column replaced_by", &error))?
        .map(RefreshTokenId::from_uuid);
    let user_agent = row
        .try_get::<Option<String>, _>("user_agent")
        .map_err(|error| database_error("could not read column user_agent", &error))?;

    Ok(RefreshToken::rehydrate(
        RefreshTokenId::from_uuid(identifier(row, "id")?),
        StoredRefreshToken {
            token: NewRefreshToken {
                user_id: UserId::from_uuid(identifier(row, "user_id")?),
                digest,
                created_at: timestamp(row, "created_at")?,
                expires_at: timestamp(row, "expires_at")?,
                user_agent,
            },
            revoked_at: optional_timestamp(row, "revoked_at")?,
            replaced_by,
        },
    ))
}

/// Reconstruye un token de recuperacion desde su fila.
pub fn password_reset_token(row: &PgRow) -> Result<PasswordResetToken, AppError> {
    let digest = TokenDigest::parse(&text(row, "token_hash")?)
        .map_err(|error| corrupt("token_hash", &error.to_string()))?;
    Ok(PasswordResetToken::rehydrate(
        PasswordResetTokenId::from_uuid(identifier(row, "id")?),
        NewPasswordResetToken {
            user_id: UserId::from_uuid(identifier(row, "user_id")?),
            digest,
            created_at: timestamp(row, "created_at")?,
            expires_at: timestamp(row, "expires_at")?,
        },
        optional_timestamp(row, "used_at")?,
    ))
}
