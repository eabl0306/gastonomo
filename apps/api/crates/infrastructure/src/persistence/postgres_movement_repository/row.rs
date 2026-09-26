//! Traduccion de una fila de `movements` al agregado.
//!
//! Una fila que el dominio rechaza no es un error del usuario: es una fila corrupta
//! o un esquema que se movio. Por eso todo sale como [`AppError::Dependency`].

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_application::error::AppError;
use gastonomo_domain::movement::{
    CategoryId, Movement, MovementCategory, MovementId, MovementKind, MovementTitle, NewMovement,
};
use gastonomo_domain::{CategoryKind, CurrencyCode, MonetaryAmount, Money, UserId};
use rust_decimal::Decimal;
use sqlx::Row as _;
use sqlx::postgres::PgRow;
use uuid::Uuid;

use super::super::mapping::database_error;

fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

fn read<'r, T>(row: &'r PgRow, column: &str) -> Result<T, AppError>
where
    T: sqlx::Decode<'r, sqlx::Postgres> + sqlx::Type<sqlx::Postgres>,
{
    row.try_get::<T, _>(column)
        .map_err(|error| database_error(&format!("could not read column {column}"), &error))
}

fn money(row: &PgRow, column: &str) -> Result<Money, AppError> {
    let raw: Decimal = read(row, column)?;
    Money::from_decimal(raw).map_err(|error| corrupt(column, &error.to_string()))
}

fn currency(row: &PgRow, column: &str) -> Result<CurrencyCode, AppError> {
    let raw: String = read(row, column)?;
    CurrencyCode::parse(&raw).map_err(|error| corrupt(column, &error.to_string()))
}

/// Reconstruye la categoria embebida de la fila.
pub fn category(row: &PgRow) -> Result<MovementCategory, AppError> {
    let kind_raw: String = read(row, "category_kind")?;
    let kind = CategoryKind::parse(&kind_raw)
        .map_err(|error| corrupt("category_kind", &error.to_string()))?;
    let name: String = read(row, "category_name")?;
    let icon: String = read(row, "category_icon")?;
    let color: String = read(row, "category_color")?;
    MovementCategory::new(
        CategoryId::from_uuid(read::<Uuid>(row, "category_id")?),
        &name,
        kind,
        &icon,
        &color,
    )
    .map_err(|error| corrupt("categories", &error.to_string()))
}

/// Reconstruye un movimiento de su fila.
pub fn movement(row: &PgRow) -> Result<Movement, AppError> {
    let kind_raw: String = read(row, "kind")?;
    let kind =
        MovementKind::parse(&kind_raw).map_err(|error| corrupt("kind", &error.to_string()))?;
    let title_raw: String = read(row, "title")?;
    let title =
        MovementTitle::parse(&title_raw).map_err(|error| corrupt("title", &error.to_string()))?;
    let amount = MonetaryAmount::new(
        money(row, "amount")?,
        currency(row, "currency")?,
        money(row, "base")?,
        currency(row, "base_currency")?,
    )
    .map_err(|error| corrupt("amount", &error.to_string()))?;

    Ok(Movement::rehydrate(
        MovementId::from_uuid(read::<Uuid>(row, "id")?),
        NewMovement {
            user_id: UserId::from_uuid(read::<Uuid>(row, "user_id")?),
            occurred_on: read::<NaiveDate>(row, "occurred_on")?,
            kind,
            category: category(row)?,
            title,
            amount,
            loan_id: read::<Option<Uuid>>(row, "loan_id")?,
            loan_payment_id: read::<Option<Uuid>>(row, "loan_payment_id")?,
            created_at: read::<DateTime<Utc>>(row, "created_at")?,
        },
        read::<DateTime<Utc>>(row, "updated_at")?,
    ))
}
