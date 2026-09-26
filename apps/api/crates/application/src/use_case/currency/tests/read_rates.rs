//! Tests de `GetExchangeRates` y `ListCurrencies`.
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::currency::RateStatus;

use super::support::{bulletin, clock, code, date, instant};
use crate::error::AppError;
use crate::testing::{InMemoryExchangeRateRepository, block_on};
use crate::use_case::currency::{GetExchangeRates, ListCurrencies};

fn seeded(at: &str) -> Arc<InMemoryExchangeRateRepository> {
    Arc::new(InMemoryExchangeRateRepository::seeded(
        &bulletin(date(2026, 9, 25), "3316.44"),
        instant(at),
    ))
}

#[test]
fn the_rates_are_served_with_the_base_at_one() {
    let rates = seeded("2026-09-25T06:00:00Z");
    let use_case = GetExchangeRates::new(rates, clock("2026-09-25T10:00:00Z"));

    let view = block_on(use_case.execute()).expect("the rates should be there");

    assert_eq!(view.status, RateStatus::Ok);
    assert_eq!(view.table.base().as_str(), "USD");
    assert_eq!(view.table.date(), date(2026, 9, 25));
    assert_eq!(
        view.table.rate(&code("USD")).map(|rate| rate.to_string()),
        Some("1".to_owned())
    );
    assert_eq!(
        view.table.rate(&code("COP")).map(|rate| rate.to_string()),
        Some("3316.44".to_owned())
    );
}

#[test]
fn rates_that_have_not_been_refreshed_for_two_days_are_marked_stale() {
    let rates = seeded("2026-09-22T06:00:00Z");
    let use_case = GetExchangeRates::new(rates, clock("2026-09-25T10:00:00Z"));

    let view = block_on(use_case.execute()).expect("stale rates are still served");

    assert_eq!(view.status, RateStatus::Stale);
    assert_eq!(
        view.table.date(),
        date(2026, 9, 25),
        "the date of the rates is not the date of the refresh"
    );
}

#[test]
fn an_empty_database_is_unavailable_and_not_a_missing_resource() {
    let rates = Arc::new(InMemoryExchangeRateRepository::new());
    let use_case = GetExchangeRates::new(rates, clock("2026-09-25T10:00:00Z"));

    let error = block_on(use_case.execute()).expect_err("there is nothing to serve");

    // Nada ha fallado: la tarea de refresco todavia no ha conseguido una pasada.
    // Por eso es 503 y no 500, para que el cliente sepa que puede reintentar.
    assert!(matches!(error, AppError::Unavailable { .. }));
}

#[test]
fn the_catalog_only_carries_what_can_be_offered() {
    let rates = seeded("2026-09-25T06:00:00Z");
    let use_case = ListCurrencies::new(rates);

    let currencies = block_on(use_case.execute()).expect("the catalog should be there");

    let codes: Vec<&str> = currencies
        .iter()
        .map(|currency| currency.code().as_str())
        .collect();
    assert_eq!(codes, ["COP", "EUR", "USD"]);
}

#[test]
fn an_empty_catalog_is_not_an_error() {
    let rates = Arc::new(InMemoryExchangeRateRepository::new());
    let use_case = ListCurrencies::new(rates);

    assert!(
        block_on(use_case.execute())
            .expect("an empty catalog is a valid answer")
            .is_empty()
    );
}
