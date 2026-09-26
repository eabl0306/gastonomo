//! El par de importes que guarda cada registro y su invariante.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use gastonomo_domain::{CurrencyCode, MonetaryAmount, MonetaryAmountError, Money};

fn currency(code: &str) -> CurrencyCode {
    CurrencyCode::parse(code).expect("fixture currency should be valid")
}

fn money(raw: &str) -> Money {
    Money::parse(raw).expect("fixture amount should be valid")
}

#[test]
fn keeps_the_users_rate_as_it_was_typed() {
    let amount = MonetaryAmount::new(
        money("140"),
        currency("USD"),
        money("450000"),
        currency("COP"),
    )
    .expect("a converted amount should be valid");

    assert_eq!(amount.amount(), money("140"));
    assert_eq!(amount.currency().as_str(), "USD");
    assert_eq!(amount.base(), money("450000"));
    assert_eq!(amount.base_currency().as_str(), "COP");
    assert!(amount.is_converted());
}

#[test]
fn with_a_single_currency_the_base_mirrors_the_amount() {
    let amount = MonetaryAmount::single_currency(money("32000"), currency("COP"))
        .expect("a single currency amount should be valid");
    assert_eq!(amount.amount(), amount.base());
    assert!(!amount.is_converted());
}

#[test]
fn the_same_currency_with_different_amounts_is_rejected() {
    let error = MonetaryAmount::new(money("10"), currency("COP"), money("11"), currency("COP"));
    assert_eq!(error, Err(MonetaryAmountError::InconsistentSingleCurrency));
}

#[test]
fn zero_and_negative_amounts_are_rejected() {
    assert_eq!(
        MonetaryAmount::single_currency(Money::ZERO, currency("COP")),
        Err(MonetaryAmountError::AmountNotPositive)
    );
    assert_eq!(
        MonetaryAmount::new(money("10"), currency("USD"), Money::ZERO, currency("COP")),
        Err(MonetaryAmountError::BaseNotPositive)
    );
}
