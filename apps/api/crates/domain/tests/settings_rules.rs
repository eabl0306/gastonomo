//! Reglas del agregado de ajustes.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use chrono::{Duration, Utc};
use gastonomo_domain::settings::{DEFAULT_DISPLAY_CURRENCY, Language, UserSettings};
use gastonomo_domain::value_object::{CurrencyCode, UserId};

#[test]
fn an_account_is_born_with_the_currency_and_language_of_the_design() {
    let user_id = UserId::generate();
    let now = Utc::now();

    let settings = UserSettings::defaults_for(user_id, now);

    assert_eq!(settings.user_id(), user_id);
    assert_eq!(
        settings.display_currency().as_str(),
        DEFAULT_DISPLAY_CURRENCY
    );
    assert_eq!(settings.display_currency().as_str(), "COP");
    assert_eq!(settings.language(), Language::Spanish);
    assert_eq!(settings.updated_at(), now);
}

#[test]
fn the_two_supported_languages_carry_their_locale() {
    assert_eq!(Language::parse(" es ").unwrap(), Language::Spanish);
    assert_eq!(Language::parse("en").unwrap(), Language::English);
    assert!(Language::parse("fr").is_err());
    assert_eq!(Language::Spanish.locale(), "es-CO");
    assert_eq!(Language::English.locale(), "en-US");
    assert_eq!(Language::default(), Language::Spanish);
}

#[test]
fn each_change_moves_the_timestamp_and_leaves_the_other_field_alone() {
    let now = Utc::now();
    let mut settings = UserSettings::defaults_for(UserId::generate(), now);
    let later = now + Duration::minutes(5);

    settings.set_display_currency(CurrencyCode::parse("usd").unwrap(), later);
    assert_eq!(settings.display_currency().as_str(), "USD");
    assert_eq!(settings.language(), Language::Spanish);
    assert_eq!(settings.updated_at(), later);

    let even_later = later + Duration::minutes(5);
    settings.set_language(Language::English, even_later);
    assert_eq!(settings.display_currency().as_str(), "USD");
    assert_eq!(settings.language(), Language::English);
    assert_eq!(settings.updated_at(), even_later);
}

#[test]
fn a_row_read_from_the_database_comes_back_whole() {
    let user_id = UserId::generate();
    let now = Utc::now();

    let settings = UserSettings::rehydrate(
        user_id,
        CurrencyCode::parse("EUR").unwrap(),
        Language::English,
        now,
    );

    assert_eq!(settings.user_id(), user_id);
    assert_eq!(settings.display_currency().as_str(), "EUR");
    assert_eq!(settings.language(), Language::English);
    assert_eq!(settings.updated_at(), now);
}
