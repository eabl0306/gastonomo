//! Recuperacion de contrasena por correo.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use chrono::Duration;

use gastonomo_domain::settings::{Language, UserSettings};

use super::support::{Harness, credentials, registration};
use crate::error::AppError;
use crate::port::Clock as _;
use crate::testing::block_on;
use crate::use_case::auth::{
    LoginUserCommand, RequestPasswordReset, RequestPasswordResetCommand, ResetPassword,
    ResetPasswordCommand,
};

fn request(harness: &Harness, email: &str) -> Result<(), AppError> {
    block_on(
        RequestPasswordReset::new(harness.password_ports.clone()).execute(
            RequestPasswordResetCommand {
                email: email.to_owned(),
            },
        ),
    )
}

fn reset(harness: &Harness, token: &str, password: &str) -> Result<(), AppError> {
    block_on(
        ResetPassword::new(harness.password_ports.clone()).execute(ResetPasswordCommand {
            token: token.to_owned(),
            new_password: password.to_owned(),
        }),
    )
}

/// Saca del correo enviado el token del enlace.
fn token_from_mail(harness: &Harness) -> String {
    let sent = harness.mailer.sent();
    let body = sent[0].body.clone();
    body.split("token=")
        .nth(1)
        .and_then(|rest| rest.split_whitespace().next())
        .map(str::to_owned)
        .expect("the mail should carry a link with a token")
}

#[test]
fn asking_for_a_link_sends_a_mail_with_a_single_use_token() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");

    let sent = harness.mailer.sent();
    assert_eq!(sent.len(), 1);
    assert_eq!(sent[0].to.as_str(), "camilo@correo.com");
    assert!(sent[0].body.contains("https://gastonomo.test/reset?token="));
    assert!(sent[0].body.contains("30 minutos"));
    assert_eq!(harness.reset_tokens.all().len(), 1);
}

#[test]
fn an_unknown_email_is_accepted_without_sending_anything() {
    let harness = Harness::new();
    request(&harness, "nadie@correo.com").expect("the endpoint never reveals who has an account");
    request(&harness, "not-an-email").expect("a malformed email is accepted just the same");
    assert!(harness.mailer.sent().is_empty());
    assert!(harness.reset_tokens.all().is_empty());
}

#[test]
fn the_link_changes_the_password_and_closes_every_session() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    block_on(harness.login().execute(credentials())).expect("should log in");
    request(&harness, "camilo@correo.com").expect("should accept the request");

    reset(&harness, &token_from_mail(&harness), "contrasena-nueva").expect("should reset");

    assert_eq!(
        harness.refresh_tokens.usable_count(harness.now()),
        0,
        "whoever asked to recover may have lost the phone"
    );
    assert_eq!(
        block_on(harness.login().execute(credentials())).expect_err("the old one is gone"),
        AppError::InvalidCredentials
    );
    block_on(harness.login().execute(LoginUserCommand {
        password: "contrasena-nueva".to_owned(),
        ..credentials()
    }))
    .expect("the new one works");
}

#[test]
fn the_same_link_cannot_be_used_twice() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");
    let token = token_from_mail(&harness);

    reset(&harness, &token, "contrasena-nueva").expect("should reset");
    let error = reset(&harness, &token, "otra-contrasena").expect_err("should be spent");
    assert!(matches!(error, AppError::Unauthorized { .. }));
}

#[test]
fn a_link_older_than_thirty_minutes_no_longer_works() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");
    let token = token_from_mail(&harness);

    harness.clock.advance(Duration::minutes(31));
    let error = reset(&harness, &token, "contrasena-nueva").expect_err("should be expired");
    assert!(matches!(error, AppError::Unauthorized { .. }));
}

#[test]
fn a_short_new_password_is_a_field_error() {
    let harness = Harness::new();
    let error = reset(&harness, "whatever", "corta").expect_err("should be invalid");
    let AppError::Validation(errors) = error else {
        unreachable!("a short password is a validation error")
    };
    assert_eq!(errors.entries()[0].field, "newPassword");
}

/// El idioma del correo sale de los ajustes de la cuenta, que son los mismos que
/// deciden el idioma de la aplicacion. Antes el correo era solo en castellano, asi
/// que quien tenia el telefono en ingles pedia el enlace desde una pantalla en
/// ingles y recibia un correo en espanol.
#[test]
fn the_mail_speaks_the_language_of_the_account() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    let user_id = harness
        .users
        .by_email("camilo@correo.com")
        .expect("the account should exist")
        .id();
    let mut settings = UserSettings::defaults_for(user_id, harness.clock.now());
    settings.set_language(Language::English, harness.clock.now());
    harness.settings.seed(settings);

    request(&harness, "camilo@correo.com").expect("should accept the request");

    let sent = harness.mailer.sent();
    assert!(
        sent[0].subject.contains("Reset"),
        "subject was {}",
        sent[0].subject
    );
    assert!(sent[0].body.contains("You asked to create a new password"));
}

/// Sin ajustes guardados, o si su lectura falla, el correo sale igual: perder el
/// enlace de recuperacion es mucho peor que mandarlo en el idioma equivocado.
#[test]
fn without_settings_the_mail_still_goes_out_in_spanish() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");

    request(&harness, "camilo@correo.com").expect("should accept the request");

    let sent = harness.mailer.sent();
    assert_eq!(sent.len(), 1);
    assert!(sent[0].subject.contains("Recupera"));
}
