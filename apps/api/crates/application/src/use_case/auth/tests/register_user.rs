//! Registro de una cuenta nueva.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use super::support::{Harness, registration};
use crate::error::AppError;
use crate::testing::block_on;

#[test]
fn registering_creates_the_account_and_opens_a_session() {
    let harness = Harness::new();
    let session = block_on(harness.register().execute(registration())).expect("should register");

    assert_eq!(harness.users.count(), 1);
    assert_eq!(session.user.email, "camilo@correo.com");
    assert_eq!(session.user.name, "Camilo Rojas");
    assert!(!session.access.value.is_empty());
    assert!(!session.refresh_token.is_empty());
    assert_eq!(harness.refresh_tokens.all().len(), 1);
}

#[test]
fn registering_seeds_the_default_categories_and_settings() {
    let harness = Harness::new();
    let session = block_on(harness.register().execute(registration())).expect("should register");
    assert_eq!(harness.provisioning.provisioned(), vec![session.user.id]);
}

#[test]
fn the_refresh_token_is_stored_hashed_and_never_in_the_clear() {
    let harness = Harness::new();
    let session = block_on(harness.register().execute(registration())).expect("should register");
    let stored = harness.refresh_tokens.all();
    let digest = stored[0].digest().as_str().to_owned();
    assert_ne!(digest, session.refresh_token);
    assert_eq!(digest.len(), 64);
}

#[test]
fn a_second_account_with_the_same_email_is_a_conflict() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    let error = block_on(harness.register().execute(registration())).expect_err("should clash");
    assert!(matches!(error, AppError::Conflict { ref code, .. } if code == "email_taken"));
    assert_eq!(harness.users.count(), 1);
}

#[test]
fn every_invalid_field_is_reported_at_once() {
    let harness = Harness::new();
    let command = crate::use_case::auth::RegisterUserCommand {
        name: "  ".to_owned(),
        email: "camilo".to_owned(),
        password: "corta".to_owned(),
        accepted_terms: false,
        user_agent: None,
    };
    let error = block_on(harness.register().execute(command)).expect_err("should be invalid");
    let AppError::Validation(errors) = error else {
        unreachable!("a bad form is a validation error")
    };
    let fields: Vec<&str> = errors
        .entries()
        .iter()
        .map(|entry| entry.field.as_str())
        .collect();
    assert_eq!(fields, vec!["name", "email", "password", "acceptedTerms"]);
    assert_eq!(harness.users.count(), 0);
}

#[test]
fn a_registration_without_accepting_the_terms_is_rejected() {
    let harness = Harness::new();
    let command = crate::use_case::auth::RegisterUserCommand {
        accepted_terms: false,
        ..registration()
    };
    let error = block_on(harness.register().execute(command)).expect_err("should be invalid");
    assert!(matches!(error, AppError::Validation(_)));
}

#[test]
fn the_password_is_never_stored_in_the_clear() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    let user = harness
        .users
        .by_email("camilo@correo.com")
        .expect("the account should exist");
    assert_ne!(user.password_hash().as_str(), "contrasena-larga");
    assert!(user.password_hash().as_str().starts_with("fake$"));
}
