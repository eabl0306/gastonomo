//! Cambio de contrasena desde Ajustes.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use super::support::{Harness, credentials, registration};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::auth::{ChangePassword, ChangePasswordCommand, LoginUserCommand};

fn change(harness: &Harness, command: ChangePasswordCommand) -> Result<(), AppError> {
    block_on(ChangePassword::new(harness.password_ports.clone()).execute(command))
}

#[test]
fn changing_the_password_keeps_the_current_session_and_drops_the_rest() {
    let harness = Harness::new();
    let current = block_on(harness.register().execute(registration())).expect("should register");
    block_on(harness.login().execute(credentials())).expect("should log in");
    block_on(harness.login().execute(credentials())).expect("should log in again");

    change(
        &harness,
        ChangePasswordCommand {
            user_id: current.user.id,
            current_password: "contrasena-larga".to_owned(),
            new_password: "contrasena-nueva".to_owned(),
            current_refresh_token: Some(current.refresh_token),
        },
    )
    .expect("should change the password");

    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 1);
    block_on(harness.login().execute(LoginUserCommand {
        password: "contrasena-nueva".to_owned(),
        ..credentials()
    }))
    .expect("the new password works");
}

#[test]
fn without_the_current_refresh_token_every_session_falls() {
    let harness = Harness::new();
    let current = block_on(harness.register().execute(registration())).expect("should register");
    change(
        &harness,
        ChangePasswordCommand {
            user_id: current.user.id,
            current_password: "contrasena-larga".to_owned(),
            new_password: "contrasena-nueva".to_owned(),
            current_refresh_token: None,
        },
    )
    .expect("should change the password");
    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 0);
}

#[test]
fn a_wrong_current_password_changes_nothing() {
    let harness = Harness::new();
    let current = block_on(harness.register().execute(registration())).expect("should register");
    let error = change(
        &harness,
        ChangePasswordCommand {
            user_id: current.user.id,
            current_password: "me-la-invento".to_owned(),
            new_password: "contrasena-nueva".to_owned(),
            current_refresh_token: None,
        },
    )
    .expect_err("should be rejected");

    assert_eq!(error, AppError::InvalidCredentials);
    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 1);
    block_on(harness.login().execute(credentials())).expect("the old password still works");
}

#[test]
fn a_short_new_password_is_a_field_error() {
    let harness = Harness::new();
    let current = block_on(harness.register().execute(registration())).expect("should register");
    let error = change(
        &harness,
        ChangePasswordCommand {
            user_id: current.user.id,
            current_password: "contrasena-larga".to_owned(),
            new_password: "corta".to_owned(),
            current_refresh_token: None,
        },
    )
    .expect_err("should be invalid");
    assert!(matches!(error, AppError::Validation(_)));
}

#[test]
fn an_account_that_no_longer_exists_is_not_found() {
    let harness = Harness::new();
    let error = change(
        &harness,
        ChangePasswordCommand {
            user_id: gastonomo_domain::UserId::generate(),
            current_password: "contrasena-larga".to_owned(),
            new_password: "contrasena-nueva".to_owned(),
            current_refresh_token: None,
        },
    )
    .expect_err("should not find the account");
    assert!(matches!(error, AppError::NotFound { .. }));
}
