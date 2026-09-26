//! Tests de `RefreshExchangeRates`.
#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use super::support::{bulletin, clock, date, instant};
use crate::error::AppError;
use crate::testing::{InMemoryExchangeRateRepository, StubExchangeRateProvider, block_on};
use crate::use_case::currency::{RefreshExchangeRates, RefreshOutcome};

/// Arma el caso de uso con el proveedor y el repositorio dados.
fn use_case(
    provider: &Arc<StubExchangeRateProvider>,
    rates: &Arc<InMemoryExchangeRateRepository>,
    at: &str,
) -> RefreshExchangeRates {
    RefreshExchangeRates::new(provider.clone(), rates.clone(), clock(at))
}

#[test]
fn a_first_refresh_stores_the_whole_bulletin() {
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let rates = Arc::new(InMemoryExchangeRateRepository::new());
    let refresh = use_case(&provider, &rates, "2026-09-25T06:00:00Z");

    let report = block_on(refresh.execute()).expect("the refresh should succeed");

    assert_eq!(report.outcome, RefreshOutcome::Updated);
    assert_eq!(report.date, date(2026, 9, 25));
    assert_eq!(report.currencies, 3);
    assert_eq!(rates.writes(), 1);
    assert_eq!(rates.stored_dates(), [date(2026, 9, 25)]);
    assert_eq!(provider.calls(), 1);
}

#[test]
fn a_source_that_fails_leaves_the_stored_rates_untouched() {
    let seed = bulletin(date(2026, 9, 24), "3199.41");
    let rates = Arc::new(InMemoryExchangeRateRepository::seeded(
        &seed,
        instant("2026-09-24T06:00:00Z"),
    ));
    let provider = Arc::new(StubExchangeRateProvider::failing("frankfurter timed out"));
    let refresh = use_case(&provider, &rates, "2026-09-25T06:00:00Z");

    let error = block_on(refresh.execute()).expect_err("the refresh should fail");

    assert!(matches!(error, AppError::Dependency { .. }));
    assert_eq!(rates.writes(), 1, "only the seed was written");
    assert_eq!(rates.stored_dates(), [date(2026, 9, 24)]);
}

#[test]
fn the_same_date_again_changes_nothing_but_the_refresh_instant() {
    let seed = bulletin(date(2026, 9, 25), "3316.44");
    let rates = Arc::new(InMemoryExchangeRateRepository::seeded(
        &seed,
        instant("2026-09-25T06:00:00Z"),
    ));
    let provider = Arc::new(StubExchangeRateProvider::publishing(seed));
    let refresh = use_case(&provider, &rates, "2026-09-25T18:00:00Z");

    let report = block_on(refresh.execute()).expect("the refresh should succeed");

    assert_eq!(report.outcome, RefreshOutcome::Unchanged);
    assert_eq!(
        rates.stored_dates(),
        [date(2026, 9, 25)],
        "no new date appeared"
    );
    let stored = block_on(refresh_target(&rates));
    assert_eq!(stored.date(), date(2026, 9, 25));
    assert_eq!(
        stored.fetched_at(),
        instant("2026-09-25T18:00:00Z"),
        "the refresh instant is what keeps friday rates from going stale on monday"
    );
}

#[test]
fn a_new_date_adds_a_row_and_keeps_the_old_one() {
    let rates = Arc::new(InMemoryExchangeRateRepository::seeded(
        &bulletin(date(2026, 9, 24), "3199.41"),
        instant("2026-09-24T06:00:00Z"),
    ));
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let refresh = use_case(&provider, &rates, "2026-09-25T06:00:00Z");

    let report = block_on(refresh.execute()).expect("the refresh should succeed");

    assert_eq!(report.outcome, RefreshOutcome::Updated);
    assert_eq!(
        rates.stored_dates(),
        [date(2026, 9, 24), date(2026, 9, 25)],
        "nothing is ever deleted"
    );
}

#[test]
fn a_replica_that_does_not_win_the_lock_writes_nothing() {
    let rates = Arc::new(InMemoryExchangeRateRepository::new());
    rates.lock_refresh();
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let refresh = use_case(&provider, &rates, "2026-09-25T06:00:00Z");

    let report = block_on(refresh.execute()).expect("skipping is not an error");

    assert_eq!(report.outcome, RefreshOutcome::SkippedLocked);
    assert_eq!(rates.writes(), 0);
}

#[test]
fn an_empty_database_is_refreshed_at_startup() {
    let rates = Arc::new(InMemoryExchangeRateRepository::new());
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let refresh = use_case(&provider, &rates, "2026-09-25T06:00:00Z");

    let report = block_on(refresh.execute_if_due()).expect("the refresh should succeed");

    assert_eq!(
        report.map(|report| report.outcome),
        Some(RefreshOutcome::Updated)
    );
}

#[test]
fn rates_fetched_an_hour_ago_are_left_alone_at_startup() {
    let rates = Arc::new(InMemoryExchangeRateRepository::seeded(
        &bulletin(date(2026, 9, 25), "3316.44"),
        instant("2026-09-25T06:00:00Z"),
    ));
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let refresh = use_case(&provider, &rates, "2026-09-25T07:00:00Z");

    assert!(
        block_on(refresh.execute_if_due())
            .expect("the check should succeed")
            .is_none()
    );
    assert_eq!(provider.calls(), 0, "the source is not even called");
}

#[test]
fn rates_older_than_a_day_are_refreshed_at_startup() {
    let rates = Arc::new(InMemoryExchangeRateRepository::seeded(
        &bulletin(date(2026, 9, 24), "3199.41"),
        instant("2026-09-24T06:00:00Z"),
    ));
    let provider = Arc::new(StubExchangeRateProvider::publishing(bulletin(
        date(2026, 9, 25),
        "3316.44",
    )));
    let refresh = use_case(&provider, &rates, "2026-09-25T07:00:00Z");

    let report = block_on(refresh.execute_if_due()).expect("the refresh should succeed");

    assert_eq!(
        report.map(|report| report.outcome),
        Some(RefreshOutcome::Updated)
    );
    assert_eq!(provider.calls(), 1);
}

/// Lee la tabla guardada para comprobarla.
async fn refresh_target(
    rates: &Arc<InMemoryExchangeRateRepository>,
) -> gastonomo_domain::currency::ExchangeRateTable {
    use crate::port::currency::ExchangeRateRepository as _;

    let base = gastonomo_domain::currency::rates_base();
    let Ok(Some(table)) = rates.latest(&base).await else {
        unreachable!("the seeded repository always has a table")
    };
    table
}
