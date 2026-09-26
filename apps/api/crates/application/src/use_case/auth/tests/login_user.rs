//! Inicio y cierre de sesion.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use super::support::{Harness, credentials, registration};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::auth::{LoginUserCommand, LogoutSessionCommand};

#[test]
fn the_right_credentials_open_a_new_session() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    let session = block_on(harness.login().execute(credentials())).expect("should log in");

    assert_eq!(session.user.email, "camilo@correo.com");
    assert_eq!(
        harness.refresh_tokens.all().len(),
        2,
        "logging in adds a session without touching the one from registering"
    );
}

#[test]
fn a_wrong_password_and_an_unknown_email_answer_the_same() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");

    let wrong_password = block_on(harness.login().execute(LoginUserCommand {
        password: "otra-contrasena".to_owned(),
        ..credentials()
    }))
    .expect_err("should not log in");
    let unknown_email = block_on(harness.login().execute(LoginUserCommand {
        email: "nadie@correo.com".to_owned(),
        ..credentials()
    }))
    .expect_err("should not log in");

    assert_eq!(wrong_password, AppError::InvalidCredentials);
    assert_eq!(unknown_email, AppError::InvalidCredentials);
}

#[test]
fn a_malformed_email_is_not_a_validation_error_but_a_failed_login() {
    let harness = Harness::new();
    let error = block_on(harness.login().execute(LoginUserCommand {
        email: "camilo".to_owned(),
        ..credentials()
    }))
    .expect_err("should not log in");
    assert_eq!(error, AppError::InvalidCredentials);
}

#[test]
fn logging_out_revokes_only_that_session() {
    let harness = Harness::new();
    let first = block_on(harness.register().execute(registration())).expect("should register");
    let second = block_on(harness.login().execute(credentials())).expect("should log in");

    block_on(harness.logout().execute(LogoutSessionCommand {
        refresh_token: first.refresh_token.clone(),
    }))
    .expect("should log out");

    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 1);
    let error = block_on(
        harness
            .refresh()
            .execute(crate::use_case::auth::RefreshSessionCommand {
                refresh_token: first.refresh_token,
                user_agent: None,
            }),
    )
    .expect_err("the revoked token should not refresh");
    assert!(matches!(error, AppError::Unauthorized { .. }));
    assert!(
        block_on(
            harness
                .refresh()
                .execute(crate::use_case::auth::RefreshSessionCommand {
                    refresh_token: second.refresh_token,
                    user_agent: None,
                })
        )
        .is_ok(),
        "the other session keeps working"
    );
}

#[test]
fn logging_out_with_an_unknown_token_is_not_an_error() {
    let harness = Harness::new();
    block_on(harness.logout().execute(LogoutSessionCommand {
        refresh_token: "invented".to_owned(),
    }))
    .expect("logging out is idempotent");
}
