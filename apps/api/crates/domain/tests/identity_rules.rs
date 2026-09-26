//! Monedas, correos, nombres e identificadores.

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

use gastonomo_domain::{
    CurrencyCode, CurrencyCodeError, Email, EmailError, PersonName, PersonNameError, UserId,
};

#[test]
fn a_currency_code_is_normalised_to_upper_case() {
    let code = CurrencyCode::parse("  cop ").expect("lower case should be accepted");
    assert_eq!(code.as_str(), "COP");
    assert_eq!(code.decimals(), 0);
    assert!(code.is_zero_decimal());
}

#[test]
fn a_currency_code_needs_three_ascii_letters() {
    assert!(matches!(
        CurrencyCode::parse("EU"),
        Err(CurrencyCodeError::Length { .. })
    ));
    assert!(matches!(
        CurrencyCode::parse("EU1"),
        Err(CurrencyCodeError::NotAlphabetic { .. })
    ));
    assert!(matches!(
        CurrencyCode::parse("EUROS"),
        Err(CurrencyCodeError::Length { .. })
    ));
}

#[test]
fn an_email_is_lower_cased_and_trimmed() {
    let email = Email::parse("  Camilo@Correo.COM ").expect("should be valid");
    assert_eq!(email.as_str(), "camilo@correo.com");
}

#[test]
fn a_malformed_email_is_rejected() {
    for candidate in ["camilo", "camilo@correo", "a@@b.com", "ca milo@correo.com"] {
        assert!(
            matches!(Email::parse(candidate), Err(EmailError::Malformed { .. })),
            "{candidate} should not be accepted"
        );
    }
    assert_eq!(Email::parse("   "), Err(EmailError::Empty));
}

#[test]
fn a_person_name_is_trimmed_and_bounded() {
    assert_eq!(
        PersonName::parse(" Camilo Rojas ")
            .expect("should be valid")
            .as_str(),
        "Camilo Rojas"
    );
    assert_eq!(PersonName::parse(" "), Err(PersonNameError::Empty));
    assert_eq!(
        PersonName::parse(&"a".repeat(81)),
        Err(PersonNameError::TooLong)
    );
}

#[test]
fn a_user_id_round_trips_through_text() {
    let id = UserId::generate();
    let parsed = UserId::parse(&id.to_string()).expect("its own text should parse");
    assert_eq!(id, parsed);
    assert!(UserId::parse("not-a-uuid").is_err());
}
