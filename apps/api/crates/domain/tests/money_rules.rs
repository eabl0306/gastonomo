//! Reglas de dinero: precision, rango, aritmetica y redondeo a cero.

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

use gastonomo_domain::{CurrencyCode, Money, MoneyError};

fn currency(code: &str) -> CurrencyCode {
    CurrencyCode::parse(code).expect("fixture currency should be valid")
}

fn money(raw: &str) -> Money {
    Money::parse(raw).expect("fixture amount should be valid")
}

#[test]
fn a_tenth_plus_two_tenths_is_exactly_three_tenths() {
    let sum = money("0.1").plus(money("0.2")).expect("sum should fit");
    assert_eq!(sum, money("0.3"));
    assert_eq!(sum.to_storage_string(), "0.3000");
}

#[test]
fn amounts_keep_four_decimals_and_round_the_fifth() {
    assert_eq!(money("1.00004").to_storage_string(), "1.0000");
    assert_eq!(money("1.00005").to_storage_string(), "1.0001");
}

#[test]
fn a_blank_or_bogus_amount_is_rejected() {
    assert!(matches!(
        Money::parse("many pesos"),
        Err(MoneyError::NotANumber { .. })
    ));
    assert!(matches!(
        Money::parse(""),
        Err(MoneyError::NotANumber { .. })
    ));
}

#[test]
fn an_amount_bigger_than_numeric_18_4_is_rejected() {
    assert_eq!(
        Money::parse("100000000000000"),
        Err(MoneyError::OutOfRange),
        "fourteen integer digits is the limit of numeric(18,4)"
    );
    assert!(Money::parse("99999999999999.9999").is_ok());
}

#[test]
fn subtraction_can_go_negative_because_a_balance_can() {
    let balance = money("100").minus(money("250")).expect("should fit");
    assert!(balance.is_negative());
    assert_eq!(balance.absolute(), money("150"));
}

#[test]
fn a_remainder_below_half_a_cent_counts_as_zero() {
    let usd = currency("USD");
    assert!(money("0.0049").negligible_to_zero(&usd).is_zero());
    assert!(!money("0.005").negligible_to_zero(&usd).is_zero());
}

#[test]
fn in_currencies_without_decimals_the_threshold_is_half_a_unit() {
    let cop = currency("COP");
    assert!(money("0.49").negligible_to_zero(&cop).is_zero());
    assert!(!money("0.5").negligible_to_zero(&cop).is_zero());
    assert!(
        !money("0.49").negligible_to_zero(&currency("USD")).is_zero(),
        "the same remainder is not negligible in a currency with cents"
    );
}

#[test]
fn a_negative_remainder_is_also_pulled_to_zero() {
    let balance = money("-0.002").negligible_to_zero(&currency("USD"));
    assert!(balance.is_zero());
}

#[test]
fn amounts_are_ordered_and_comparable() {
    assert!(money("10") > money("9.9999"));
    assert_eq!(money("10").compare(money("10")), std::cmp::Ordering::Equal);
    assert!(Money::ZERO.is_zero());
    assert!(!Money::ZERO.is_positive());
}
