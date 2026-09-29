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

/// Saca el token del ULTIMO correo, que es el unico que sigue valiendo.
fn token_from_last_mail(harness: &Harness) -> String {
    let sent = harness.mailer.sent();
    let body = sent.last().expect("a mail").body.clone();
    body.split("token=")
        .nth(1)
        .and_then(|rest| rest.split_whitespace().next())
        .map(str::to_owned)
        .expect("the mail should carry a link with a token")
}

/// Pedir el enlace otra vez mata el anterior.
///
/// Sin esto, tocar "reenviar" tres veces dejaba tres enlaces abriendo la misma
/// cuenta durante media hora cada uno. El usuario no los ve ni los puede revocar:
/// un correo reenviado o un buzon comprometido valian como una llave.
#[test]
fn asking_again_kills_the_previous_link() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");
    let first = token_from_mail(&harness);

    // Pasada la espera entre emisiones; sin avanzar el reloj esta peticion no
    // emitiria nada, que es justo lo que prueba el test de mas abajo.
    harness.clock.advance(Duration::seconds(61));
    request(&harness, "camilo@correo.com").expect("should accept the second request");
    let second = token_from_last_mail(&harness);
    assert_ne!(first, second, "each request issues its own token");

    let error = reset(&harness, &first, "contrasena-nueva").expect_err("the old link is dead");
    assert!(matches!(error, AppError::Unauthorized { .. }));
    reset(&harness, &second, "contrasena-nueva").expect("the last link still works");
}

/// Y la cara util de lo mismo: la tabla no crece con cada reenvio, asi que no
/// hace falta ninguna tarea de limpieza.
#[test]
fn asking_again_leaves_a_single_row() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    for _ in 0..5 {
        harness.clock.advance(Duration::seconds(61));
        request(&harness, "camilo@correo.com").expect("should accept the request");
    }

    assert_eq!(harness.reset_tokens.all().len(), 1);
}

/// Dos peticiones muy seguidas no matan el enlace que la cuenta ya tiene.
///
/// Es lo que impide dejar a alguien sin recuperar su cuenta. Emitir borra los
/// anteriores, y pedir el enlace no exige estar dentro: basta con saber el correo.
/// Sin esta espera, pedirlo en bucle borraba el enlace que su dueno acababa de
/// recibir, una y otra vez. El limite de peticiones no lo impide, porque cuenta
/// por IP y el atacante no comparte la de la victima.
#[test]
fn asking_again_right_away_leaves_the_live_link_alone() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");
    let theirs = token_from_mail(&harness);

    // El atacante, que solo sabe el correo, lo pide diez veces seguidas.
    for _ in 0..10 {
        request(&harness, "camilo@correo.com").expect("should answer the same as always");
    }

    // Ni se emitio nada nuevo ni se borro lo que habia.
    assert_eq!(harness.mailer.sent().len(), 1, "no extra mail goes out");
    assert_eq!(harness.reset_tokens.all().len(), 1);
    reset(&harness, &theirs, "contrasena-nueva").expect("the link still works");
}

/// Y la espera no puede dejar a nadie encerrado: pasado el minuto se vuelve a
/// emitir con normalidad, que es para lo que existe el boton de reenviar.
#[test]
fn after_the_cooldown_the_link_is_issued_again() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");

    harness.clock.advance(Duration::seconds(61));
    request(&harness, "camilo@correo.com").expect("should accept the request");

    assert_eq!(harness.mailer.sent().len(), 2);
}

/// Un enlace ya usado no cuenta para la espera: si no, cambiar la contrasena y
/// volver a pedir el enlace dentro del mismo minuto no mandaba nada.
#[test]
fn a_spent_link_does_not_hold_the_cooldown() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");
    let token = token_from_mail(&harness);
    reset(&harness, &token, "contrasena-nueva").expect("should reset");

    request(&harness, "camilo@correo.com").expect("should accept the request");

    assert_eq!(harness.mailer.sent().len(), 2);
}

/// El correo lo dice, porque si no el usuario que pincha el enlace viejo no
/// entiende por que no vale. Con el envio SMTP en segundo plano, dos correos
/// seguidos pueden llegar desordenados de verdad.
#[test]
fn the_mail_warns_that_only_the_last_link_works() {
    let harness = Harness::new();
    block_on(harness.register().execute(registration())).expect("should register");
    request(&harness, "camilo@correo.com").expect("should accept the request");

    let body = harness.mailer.sent()[0].body.clone();
    assert!(
        body.contains("solo vale el ultimo enlace"),
        "the mail should warn about it: {body}"
    );
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
