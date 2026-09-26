//! Escrituras de tasas y del catalogo de monedas.
//!
//! Todas van en la misma transaccion: o queda la pasada entera, o no queda nada.
//! Las listas viajan como arrays y se desdoblan con `UNNEST`, asi que guardar 166
//! monedas es una sola sentencia y no 166.

use chrono::{DateTime, Utc};
use gastonomo_application::error::AppError;
use gastonomo_domain::currency::RateBulletin;
use sqlx::{Postgres, Row as _, Transaction};

use crate::persistence::mapping::database_error;

/// Intenta tomar el cerrojo del refresco.
///
/// Devuelve `false` si otra replica lo tiene. Es de transaccion, asi que se suelta
/// al hacer commit o rollback y no vuelve al pool puesto.
pub async fn try_lock(
    transaction: &mut Transaction<'_, Postgres>,
    key: i64,
) -> Result<bool, AppError> {
    let row = sqlx::query("SELECT pg_try_advisory_xact_lock($1) AS acquired")
        .bind(key)
        .fetch_one(&mut **transaction)
        .await
        .map_err(|error| database_error("could not take the refresh lock", &error))?;
    row.try_get::<bool, _>("acquired")
        .map_err(|error| database_error("could not read the refresh lock", &error))
}

/// Guarda las tasas de la pasada bajo su fecha.
pub async fn upsert_rates(
    transaction: &mut Transaction<'_, Postgres>,
    bulletin: &RateBulletin,
    fetched_at: DateTime<Utc>,
) -> Result<(), AppError> {
    let quotes: Vec<String> = bulletin
        .rates()
        .keys()
        .map(|code| code.as_str().to_owned())
        .collect();
    let rates: Vec<String> = bulletin.rates().values().map(ToString::to_string).collect();
    sqlx::query(
        "INSERT INTO exchange_rates (rate_date, base, quote, rate, fetched_at) \
         SELECT $1, $2, entry.quote, entry.rate, $5 \
         FROM UNNEST($3::text[], $4::numeric[]) AS entry(quote, rate) \
         ON CONFLICT (rate_date, base, quote) \
         DO UPDATE SET rate = EXCLUDED.rate, fetched_at = EXCLUDED.fetched_at",
    )
    .bind(bulletin.date())
    .bind(bulletin.base().as_str())
    .bind(&quotes)
    .bind(&rates)
    .bind(fetched_at)
    .execute(&mut **transaction)
    .await
    .map(|_| ())
    .map_err(|error| database_error("could not write the exchange rates", &error))
}

/// Guarda el catalogo y vuelve a ofrecer lo que la fuente sigue publicando.
pub async fn upsert_currencies(
    transaction: &mut Transaction<'_, Postgres>,
    bulletin: &RateBulletin,
    updated_at: DateTime<Utc>,
) -> Result<(), AppError> {
    let codes = offered_codes(bulletin);
    let names: Vec<String> = bulletin
        .currencies()
        .iter()
        .map(|currency| currency.name().to_owned())
        .collect();
    let symbols: Vec<String> = bulletin
        .currencies()
        .iter()
        .map(|currency| currency.symbol().to_owned())
        .collect();
    sqlx::query(
        "INSERT INTO currencies (code, name, symbol, offered, updated_at) \
         SELECT entry.code, entry.name, entry.symbol, TRUE, $4 \
         FROM UNNEST($1::text[], $2::text[], $3::text[]) AS entry(code, name, symbol) \
         ON CONFLICT (code) DO UPDATE \
         SET name = EXCLUDED.name, symbol = EXCLUDED.symbol, offered = TRUE, \
             updated_at = EXCLUDED.updated_at",
    )
    .bind(&codes)
    .bind(&names)
    .bind(&symbols)
    .bind(updated_at)
    .execute(&mut **transaction)
    .await
    .map(|_| ())
    .map_err(|error| database_error("could not write the currency catalog", &error))
}

/// Deja de ofrecer las monedas que ya no vienen en la pasada.
///
/// No se borra ninguna: los registros que las usan siguen existiendo y hay que
/// poder seguir mostrandolos.
pub async fn stop_offering_the_rest(
    transaction: &mut Transaction<'_, Postgres>,
    bulletin: &RateBulletin,
    updated_at: DateTime<Utc>,
) -> Result<(), AppError> {
    let codes = offered_codes(bulletin);
    sqlx::query(
        "UPDATE currencies SET offered = FALSE, updated_at = $2 \
         WHERE offered = TRUE AND code::text <> ALL($1::text[])",
    )
    .bind(&codes)
    .bind(updated_at)
    .execute(&mut **transaction)
    .await
    .map(|_| ())
    .map_err(|error| database_error("could not retire the missing currencies", &error))
}

fn offered_codes(bulletin: &RateBulletin) -> Vec<String> {
    bulletin
        .currencies()
        .iter()
        .map(|currency| currency.code().as_str().to_owned())
        .collect()
}
