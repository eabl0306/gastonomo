//! Lectura de la respuesta de Frankfurter, comprobada contra una respuesta real.
//!
//! Las dos fixtures son recortes literales de lo que publica la fuente, con las
//! rarezas que hay que tolerar: una moneda que no cotizo hoy y llega con la fecha
//! de ayer, los metales y los XDR, y el dolar COMESA con `"symbol": null`.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use chrono::NaiveDate;
use gastonomo_domain::CurrencyCode;

use super::{CurrencyPayload, RatePayload, read_bulletin};

/// Respuesta real de `GET /v2/rates?base=usd`, recortada a ocho monedas.
const RATES: &str = include_str!("../../../tests/fixtures/frankfurter_rates.json");

/// Respuesta real de `GET /v2/currencies`, recortada a las mismas ocho.
const CURRENCIES: &str = include_str!("../../../tests/fixtures/frankfurter_currencies.json");

fn code(raw: &str) -> CurrencyCode {
    CurrencyCode::parse(raw).expect("literal is a valid code")
}

fn fixture() -> gastonomo_domain::currency::RateBulletin {
    let rates: Vec<RatePayload> = serde_json::from_str(RATES).expect("the fixture is valid");
    let currencies: Vec<CurrencyPayload> =
        serde_json::from_str(CURRENCIES).expect("the fixture is valid");
    read_bulletin(&rates, &currencies).expect("the fixture should be readable")
}

#[test]
fn the_date_is_the_most_recent_one_in_the_response() {
    assert_eq!(
        fixture().date(),
        NaiveDate::from_ymd_opt(2026, 9, 25).expect("literal is valid")
    );
}

#[test]
fn a_currency_that_did_not_trade_today_keeps_its_older_rate() {
    let bulletin = fixture();
    assert_eq!(
        bulletin.rates().get(&code("KPW")).map(ToString::to_string),
        Some("130".to_owned())
    );
}

#[test]
fn the_rates_keep_the_exact_digits_of_the_source() {
    let bulletin = fixture();
    let rates = bulletin.rates();
    assert_eq!(
        rates.get(&code("COP")).map(ToString::to_string),
        Some("3316.44".to_owned())
    );
    assert_eq!(
        rates.get(&code("EUR")).map(ToString::to_string),
        Some("0.87732".to_owned())
    );
    assert_eq!(
        rates.get(&code("JPY")).map(ToString::to_string),
        Some("158.37".to_owned())
    );
    assert_eq!(
        rates.get(&code("BMD")).map(ToString::to_string),
        Some("1".to_owned())
    );
}

#[test]
fn the_base_comes_back_as_one() {
    assert_eq!(
        fixture().rates().get(&code("USD")).map(ToString::to_string),
        Some("1".to_owned())
    );
}

#[test]
fn metals_and_drawing_rights_are_dropped_from_rates_and_catalog() {
    let bulletin = fixture();
    for raw in ["XAU", "XDR"] {
        assert!(!bulletin.rates().contains_key(&code(raw)), "{raw} in rates");
        assert!(
            !bulletin
                .currencies()
                .iter()
                .any(|currency| currency.code().as_str() == raw),
            "{raw} in catalog"
        );
    }
}

#[test]
fn a_currency_with_a_null_symbol_falls_back_to_its_code() {
    // El dolar COMESA llega de la fuente con `"symbol": null`. Antes de esto la
    // pasada entera se caia al decodificar la respuesta.
    let bulletin = fixture();
    let comesa = bulletin
        .currencies()
        .iter()
        .find(|currency| currency.code().as_str() == "CMD")
        .expect("the comesa dollar is in the fixture");
    assert_eq!(comesa.symbol(), "CMD");
    assert_eq!(comesa.name(), "COMESA Dollar");
}

#[test]
fn the_catalog_carries_name_and_symbol() {
    let bulletin = fixture();
    let euro = bulletin
        .currencies()
        .iter()
        .find(|currency| currency.code().as_str() == "EUR")
        .expect("the euro is in the fixture");
    assert_eq!(euro.name(), "Euro");
    assert_eq!(euro.symbol(), "€");
}

#[test]
fn the_catalog_holds_exactly_the_currencies_with_a_rate() {
    let bulletin = fixture();
    let codes: Vec<&str> = bulletin
        .currencies()
        .iter()
        .map(|currency| currency.code().as_str())
        .collect();
    assert_eq!(codes, ["BMD", "CMD", "COP", "EUR", "JPY", "KPW", "USD"]);
}

#[test]
fn an_entry_with_another_base_is_ignored() {
    let rates: Vec<RatePayload> = serde_json::from_str(
        r#"[{"date":"2026-09-25","base":"EUR","quote":"COP","rate":3781.2},
            {"date":"2026-09-25","base":"USD","quote":"COP","rate":3316.44}]"#,
    )
    .expect("the literal is valid");
    let bulletin = read_bulletin(&rates, &[]).expect("one entry is enough");
    assert_eq!(
        bulletin.rates().get(&code("COP")).map(ToString::to_string),
        Some("3316.44".to_owned())
    );
}

#[test]
fn a_broken_entry_does_not_bring_down_the_whole_pass() {
    let rates: Vec<RatePayload> = serde_json::from_str(
        r#"[{"date":"not-a-date","base":"USD","quote":"COP","rate":3316.44},
            {"date":"2026-09-25","base":"USD","quote":"EU","rate":0.87},
            {"date":"2026-09-25","base":"USD","quote":"JPY","rate":0},
            {"date":"2026-09-25","base":"USD","quote":"EUR","rate":0.87732}]"#,
    )
    .expect("the literal is valid");
    let bulletin = read_bulletin(&rates, &[]).expect("the euro survives");
    assert_eq!(bulletin.rates().len(), 2, "the euro and the base");
}

#[test]
fn a_response_without_a_single_usable_entry_is_a_dependency_failure() {
    assert!(read_bulletin(&[], &[]).is_err());
}
