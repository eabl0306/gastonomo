//! Piezas comunes de los tests de monedas y tasas.

use std::collections::BTreeMap;
use std::sync::Arc;

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_domain::CurrencyCode;
use gastonomo_domain::currency::{Currency, ExchangeRate, RateBulletin};

/// Codigo de moneda desde un literal.
pub fn code(raw: &str) -> CurrencyCode {
    let Ok(code) = CurrencyCode::parse(raw) else {
        unreachable!("the test literal is a valid currency code")
    };
    code
}

/// Tasa desde un literal.
pub fn rate(raw: &str) -> ExchangeRate {
    let Ok(rate) = ExchangeRate::parse(raw) else {
        unreachable!("the test literal is a valid rate")
    };
    rate
}

/// Fecha desde sus tres numeros.
pub fn date(year: i32, month: u32, day: u32) -> NaiveDate {
    let Some(date) = NaiveDate::from_ymd_opt(year, month, day) else {
        unreachable!("the test literal is a valid date")
    };
    date
}

/// Instante desde un literal RFC 3339.
pub fn instant(raw: &str) -> DateTime<Utc> {
    let Ok(parsed) = DateTime::parse_from_rfc3339(raw) else {
        unreachable!("the test literal is a valid instant")
    };
    parsed.with_timezone(&Utc)
}

/// Entrada del catalogo desde un literal.
pub fn currency(raw: &str, name: &str) -> Currency {
    let Ok(currency) = Currency::new(code(raw), name, "$") else {
        unreachable!("the test literal is a valid currency")
    };
    currency
}

/// Pasada con base USD, dos monedas cotizadas y la fecha dada.
pub fn bulletin(on: NaiveDate, cop: &str) -> RateBulletin {
    let rates = BTreeMap::from([(code("COP"), rate(cop)), (code("EUR"), rate("0.87732"))]);
    let currencies = vec![
        currency("COP", "Colombian Peso"),
        currency("EUR", "Euro"),
        currency("USD", "United States Dollar"),
    ];
    let Ok(bulletin) = RateBulletin::new(code("USD"), on, rates, currencies) else {
        unreachable!("the test bulletin carries enough rates")
    };
    bulletin
}

/// Reloj fijo compartido por los tests.
pub fn clock(at: &str) -> Arc<crate::testing::FixedClock> {
    Arc::new(crate::testing::FixedClock::new(instant(at)))
}
