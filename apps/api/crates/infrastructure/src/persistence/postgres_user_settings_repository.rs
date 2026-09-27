//! Adaptador driven del contexto de ajustes.
//!
//! La tabla tiene una fila por usuario y la siembra el registro. El `save` es un
//! `INSERT ... ON CONFLICT DO UPDATE` para que un cambio funcione igual si la
//! siembra se quedo a medias.

mod display_currency_catalog;

use async_trait::async_trait;
use gastonomo_application::error::AppError;
use gastonomo_application::port::settings::UserSettingsRepository;
use gastonomo_domain::settings::{Language, UserSettings};
use gastonomo_domain::value_object::{CurrencyCode, UserId};
use sqlx::PgPool;
use sqlx::Row as _;
use sqlx::postgres::PgRow;

use super::mapping::database_error;

pub use display_currency_catalog::PostgresDisplayCurrencyCatalog;

/// Ajustes de usuario sobre PostgreSQL.
#[derive(Debug, Clone)]
pub struct PostgresUserSettingsRepository {
    pool: PgPool,
}

impl PostgresUserSettingsRepository {
    /// Crea el adaptador sobre un pool ya construido.
    #[must_use]
    pub const fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl UserSettingsRepository for PostgresUserSettingsRepository {
    async fn find(&self, user_id: UserId) -> Result<Option<UserSettings>, AppError> {
        let found = sqlx::query(
            "SELECT user_id, display_currency, language, updated_at \
             FROM user_settings WHERE user_id = $1",
        )
        .bind(user_id.as_uuid())
        .fetch_optional(&self.pool)
        .await
        .map_err(|error| database_error("could not read the user settings", &error))?;
        found.as_ref().map(settings).transpose()
    }

    async fn save(&self, settings: &UserSettings) -> Result<(), AppError> {
        sqlx::query(
            "INSERT INTO user_settings (user_id, display_currency, language, updated_at) \
             VALUES ($1, $2, $3, $4) \
             ON CONFLICT (user_id) DO UPDATE \
             SET display_currency = excluded.display_currency, \
                 language = excluded.language, \
                 updated_at = excluded.updated_at",
        )
        .bind(settings.user_id().as_uuid())
        .bind(settings.display_currency().as_str())
        .bind(settings.language().as_str())
        .bind(settings.updated_at())
        .execute(&self.pool)
        .await
        .map(|_| ())
        .map_err(|error| database_error("could not store the user settings", &error))
    }
}

fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

pub(super) fn settings(row: &PgRow) -> Result<UserSettings, AppError> {
    let user_id = row
        .try_get::<uuid::Uuid, _>("user_id")
        .map_err(|error| database_error("could not read column user_id", &error))?;
    let raw_currency = row
        .try_get::<String, _>("display_currency")
        .map_err(|error| database_error("could not read column display_currency", &error))?;
    let raw_language = row
        .try_get::<String, _>("language")
        .map_err(|error| database_error("could not read column language", &error))?;
    let updated_at = row
        .try_get("updated_at")
        .map_err(|error| database_error("could not read column updated_at", &error))?;

    let display_currency = CurrencyCode::parse(&raw_currency)
        .map_err(|error| corrupt("display_currency", &error.to_string()))?;
    let language =
        Language::parse(&raw_language).map_err(|error| corrupt("language", &error.to_string()))?;
    Ok(UserSettings::rehydrate(
        UserId::from_uuid(user_id),
        display_currency,
        language,
        updated_at,
    ))
}
