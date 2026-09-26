//! Piezas comunes de los tests de tasas contra PostgreSQL.
//!
//! Sin `GASTONOMO_TEST_DATABASE_URL` cada test se salta a si mismo, y con
//! `GASTONOMO_TEST_REQUIRE_DATABASE=1` la falta de base de datos es un fallo.

#![allow(
    dead_code,
    reason = "each integration test binary compiles this module and uses only the part it needs"
)]
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::collections::BTreeMap;

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRate, RateBulletin, rates_base};
use gastonomo_infrastructure::{DatabaseConfig, PgPool, connect_pool, run_migrations};
use tokio::sync::{Mutex, MutexGuard};

/// Cerrojo que serializa la bateria.
///
/// `exchange_rates` y `currencies` son tablas globales, no acotadas por usuario:
/// dos tests en paralelo se pisarian el catalogo. Cada test toma este cerrojo y
/// empieza con las dos tablas vacias.
static SERIAL: Mutex<()> = Mutex::const_new(());

/// Variable que habilita esta bateria de tests.
const URL_VAR: &str = "GASTONOMO_TEST_DATABASE_URL";

/// Variable que convierte el salto en fallo.
const REQUIRE_VAR: &str = "GASTONOMO_TEST_REQUIRE_DATABASE";

/// Toma el cerrojo.
///
/// Es el de tokio y no el de la biblioteca estandar porque se mantiene tomado
/// mientras el test espera a PostgreSQL. Tampoco se envenena: un test que entra en
/// panico no deja fuera de juego a los demas.
pub async fn serially() -> MutexGuard<'static, ()> {
    SERIAL.lock().await
}

fn database_url() -> Option<String> {
    let url = std::env::var(URL_VAR).ok().filter(|url| !url.is_empty());
    let mandatory = std::env::var(REQUIRE_VAR)
        .is_ok_and(|value| !matches!(value.trim(), "" | "0" | "false" | "FALSE"));
    assert!(
        url.is_some() || !mandatory,
        "{URL_VAR} is empty but {REQUIRE_VAR} is set: \
         these tests must not pass without a real postgres"
    );
    url
}

/// Pool con las migraciones aplicadas, o `None` si no hay base de datos.
pub async fn pool() -> Option<PgPool> {
    let config = DatabaseConfig {
        url: database_url()?,
        max_connections: 2,
        // Los tests en paralelo hacen cola en el advisory lock del migrador.
        acquire_timeout_ms: 30_000,
    };
    let pool = connect_pool(&config).expect("pool should build");
    run_migrations(&pool)
        .await
        .expect("migrations should apply");
    Some(pool)
}

/// Deja las dos tablas globales vacias.
pub async fn clean(pool: &PgPool) {
    sqlx::query("DELETE FROM exchange_rates")
        .execute(pool)
        .await
        .expect("the rates should be deletable");
    sqlx::query("DELETE FROM currencies")
        .execute(pool)
        .await
        .expect("the currencies should be deletable");
}

/// Codigo de moneda desde un literal.
pub fn code(raw: &str) -> CurrencyCode {
    CurrencyCode::parse(raw).expect("literal is a valid code")
}

/// Tasa desde un literal.
pub fn rate(raw: &str) -> ExchangeRate {
    ExchangeRate::parse(raw).expect("literal is a valid rate")
}

/// Dia de septiembre de 2026.
pub const fn date(day: u32) -> NaiveDate {
    // `unreachable!` no vale en una funcion const, asi que un dia imposible cae en
    // `NaiveDate::MIN`: la fecha no cuadra y el test falla, que es lo que tiene que
    // pasar.
    match NaiveDate::from_ymd_opt(2026, 9, day) {
        Some(date) => date,
        None => NaiveDate::MIN,
    }
}

/// Instante desde un literal RFC 3339.
pub fn instant(raw: &str) -> DateTime<Utc> {
    DateTime::parse_from_rfc3339(raw)
        .expect("literal is a valid instant")
        .with_timezone(&Utc)
}

/// Pasada con las tasas y el catalogo dados.
pub fn bulletin(on: NaiveDate, rates: &[(&str, &str)], names: &[(&str, &str)]) -> RateBulletin {
    let quoted: BTreeMap<CurrencyCode, ExchangeRate> = rates
        .iter()
        .map(|(quote, value)| (code(quote), rate(value)))
        .collect();
    let catalog: Vec<Currency> = names
        .iter()
        .map(|(quote, name)| Currency::new(code(quote), name, "$").expect("valid currency"))
        .collect();
    RateBulletin::new(rates_base(), on, quoted, catalog).expect("the bulletin should build")
}
