//! Consultas de tasas y del catalogo de monedas.

use std::collections::BTreeMap;

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_application::error::AppError;
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRate, ExchangeRateTable};
use rust_decimal::Decimal;
use sqlx::postgres::PgRow;
use sqlx::{PgPool, Row as _};

use crate::persistence::mapping::database_error;

/// Una fila que no encaja en el dominio no es culpa del cliente.
fn corrupt(column: &str, detail: &str) -> AppError {
    AppError::dependency(format!(
        "column {column} holds a value the domain rejects: {detail}"
    ))
}

fn column_error(column: &str, error: &sqlx::Error) -> AppError {
    database_error(&format!("could not read column {column}"), error)
}

/// Tabla vigente de una base, o `None` si todavia no hay ninguna.
pub async fn latest_table(
    pool: &PgPool,
    base: &CurrencyCode,
) -> Result<Option<ExchangeRateTable>, AppError> {
    // SQLx 0.9 solo acepta SQL literal, asi que la lista de columnas se repite en
    // cada consulta en vez de vivir en una constante.
    let rows = sqlx::query(
        "SELECT rate_date, quote, rate, fetched_at \
         FROM exchange_rates \
         WHERE base = $1 \
           AND rate_date = (SELECT max(rate_date) FROM exchange_rates WHERE base = $1) \
         ORDER BY quote",
    )
    .bind(base.as_str())
    .fetch_all(pool)
    .await
    .map_err(|error| database_error("could not read the latest exchange rates", &error))?;
    if rows.is_empty() {
        return Ok(None);
    }
    let table = build_table(base, &rows)?;
    Ok(Some(table))
}

/// Monedas que se ofrecen hoy en el sheet de seleccion.
pub async fn offered_currencies(pool: &PgPool) -> Result<Vec<Currency>, AppError> {
    let rows =
        sqlx::query("SELECT code, name, symbol FROM currencies WHERE offered = TRUE ORDER BY code")
            .fetch_all(pool)
            .await
            .map_err(|error| database_error("could not read the offered currencies", &error))?;
    rows.iter().map(currency).collect()
}

fn build_table(base: &CurrencyCode, rows: &[PgRow]) -> Result<ExchangeRateTable, AppError> {
    let mut rates = BTreeMap::new();
    let mut date: Option<NaiveDate> = None;
    let mut fetched_at: Option<DateTime<Utc>> = None;
    for row in rows {
        let (quote, rate) = quoted_rate(row)?;
        rates.insert(quote, rate);
        let row_date = naive_date(row, "rate_date")?;
        date = Some(date.map_or(row_date, |current: NaiveDate| current.max(row_date)));
        let row_fetched = timestamp(row, "fetched_at")?;
        fetched_at = Some(fetched_at.map_or(row_fetched, |current: DateTime<Utc>| {
            current.max(row_fetched)
        }));
    }
    let (Some(date), Some(fetched_at)) = (date, fetched_at) else {
        return Err(AppError::dependency(
            "the exchange rate rows carried no date",
        ));
    };
    Ok(ExchangeRateTable::new(
        base.clone(),
        date,
        fetched_at,
        rates,
    ))
}

fn quoted_rate(row: &PgRow) -> Result<(CurrencyCode, ExchangeRate), AppError> {
    let raw_quote = text(row, "quote")?;
    let quote =
        CurrencyCode::parse(&raw_quote).map_err(|error| corrupt("quote", &error.to_string()))?;
    let raw_rate = row
        .try_get::<Decimal, _>("rate")
        .map_err(|error| column_error("rate", &error))?;
    let rate = ExchangeRate::from_decimal(raw_rate)
        .map_err(|error| corrupt("rate", &error.to_string()))?;
    Ok((quote, rate))
}

fn currency(row: &PgRow) -> Result<Currency, AppError> {
    let raw_code = text(row, "code")?;
    let code =
        CurrencyCode::parse(&raw_code).map_err(|error| corrupt("code", &error.to_string()))?;
    let name = text(row, "name")?;
    let symbol = text(row, "symbol")?;
    Currency::new(code, &name, &symbol).map_err(|error| corrupt("code", &error.to_string()))
}

fn text(row: &PgRow, column: &str) -> Result<String, AppError> {
    row.try_get::<String, _>(column)
        .map_err(|error| column_error(column, &error))
}

fn naive_date(row: &PgRow, column: &str) -> Result<NaiveDate, AppError> {
    row.try_get::<NaiveDate, _>(column)
        .map_err(|error| column_error(column, &error))
}

fn timestamp(row: &PgRow, column: &str) -> Result<DateTime<Utc>, AppError> {
    row.try_get::<DateTime<Utc>, _>(column)
        .map_err(|error| column_error(column, &error))
}
