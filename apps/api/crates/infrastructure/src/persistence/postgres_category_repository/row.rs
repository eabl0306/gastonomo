//! Traduccion de una fila de `categories` al dominio.
//!
//! Una fila que el dominio rechaza no es culpa del cliente: es una fila corrupta o
//! un esquema que se ha movido. Por eso sale como [`AppError::Dependency`].

use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_application::port::category::CategoryUsage;
use gastonomo_domain::category::{
    Category, CategoryColor, CategoryIcon, CategoryId, CategoryKind, CategoryName, NewCategory,
};
use gastonomo_domain::value_object::UserId;
use sqlx::Row as _;
use sqlx::postgres::PgRow;

use crate::persistence::mapping::database_error;

fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

fn text(row: &PgRow, column: &str) -> Result<String, AppError> {
    row.try_get::<String, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

fn identifier(row: &PgRow, column: &str) -> Result<uuid::Uuid, AppError> {
    row.try_get::<uuid::Uuid, _>(column)
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

/// Reconstruye una categoria desde su fila.
pub fn category(row: &PgRow) -> Result<Category, AppError> {
    let name = CategoryName::parse(&text(row, "name")?)
        .map_err(|error| corrupt("name", &error.to_string()))?;
    let kind = CategoryKind::parse(&text(row, "kind")?)
        .map_err(|error| corrupt("kind", &error.to_string()))?;
    let icon = CategoryIcon::parse(&text(row, "icon")?)
        .map_err(|error| corrupt("icon", &error.to_string()))?;
    let color = CategoryColor::parse(&text(row, "color")?)
        .map_err(|error| corrupt("color", &error.to_string()))?;
    let is_default = row
        .try_get::<bool, _>("is_default")
        .map_err(|error| database_error("could not read column is_default", &error))?;

    Ok(Category::rehydrate(
        CategoryId::from_uuid(identifier(row, "id")?),
        NewCategory {
            user_id: UserId::from_uuid(identifier(row, "user_id")?),
            name,
            kind,
            icon,
            color,
            created_at: timestamp(row, "created_at")?,
        },
        is_default,
        timestamp(row, "updated_at")?,
        optional_timestamp(row, "archived_at")?,
    ))
}

/// Reconstruye una categoria y su conteo de uso desde una fila que lo trae.
pub fn category_with_usage(row: &PgRow) -> Result<CategoryUsage, AppError> {
    let movements = row
        .try_get::<i64, _>("movement_count")
        .map_err(|error| database_error("could not read column movement_count", &error))?;
    Ok(CategoryUsage {
        category: category(row)?,
        // Un conteo negativo no existe y uno mayor que u32 tampoco: el saturado
        // solo protege el tipo del contrato publico.
        movements: u32::try_from(movements).unwrap_or(u32::MAX),
    })
}
