//! Reglas de las entidades de autenticacion.

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

use chrono::{DateTime, Duration, Utc};
use gastonomo_domain::{
    CategoryKind, Email, Language, NewPasswordResetToken, NewRefreshToken, NewUser, Password,
    PasswordError, PasswordHash, PasswordResetToken, PersonName, RefreshToken, RefreshTokenId,
    StoredRefreshToken, TokenDigest, User, UserId,
};

fn now() -> DateTime<Utc> {
    DateTime::parse_from_rfc3339("2026-09-22T10:00:00Z")
        .expect("literal is valid")
        .with_timezone(&Utc)
}

fn digest(byte: u8) -> TokenDigest {
    TokenDigest::parse(&format!("{byte:02x}").repeat(32)).expect("fixture digest should be valid")
}

fn user() -> User {
    User::register(NewUser {
        name: PersonName::parse("Camilo Rojas").expect("valid"),
        email: Email::parse("camilo@correo.com").expect("valid"),
        password_hash: PasswordHash::parse("$argon2id$v=19$fake").expect("valid"),
        created_at: now(),
    })
}

#[test]
fn a_password_needs_eight_characters() {
    assert_eq!(Password::parse("1234567"), Err(PasswordError::TooShort));
    assert!(Password::parse("12345678").is_ok());
    assert_eq!(
        Password::parse(&"a".repeat(129)),
        Err(PasswordError::TooLong)
    );
}

#[test]
fn secrets_never_show_up_in_a_trace() {
    let password = Password::parse("super secret").expect("valid");
    let hash = PasswordHash::parse("$argon2id$v=19$secret").expect("valid");
    assert_eq!(format!("{password:?}"), "Password(redacted)");
    assert_eq!(format!("{hash:?}"), "PasswordHash(redacted)");
    assert_eq!(format!("{:?}", digest(0xab)), "TokenDigest(redacted)");
}

#[test]
fn a_token_digest_is_a_sha256_in_hex() {
    assert!(TokenDigest::parse("abc").is_err());
    assert!(TokenDigest::parse(&"z".repeat(64)).is_err());
    assert_eq!(
        TokenDigest::parse(&"AB".repeat(32))
            .expect("upper case hex is accepted")
            .as_str(),
        "ab".repeat(32)
    );
}

#[test]
fn registering_stamps_created_and_updated_with_the_same_instant() {
    let user = user();
    assert_eq!(user.created_at(), user.updated_at());
    assert_eq!(user.email().as_str(), "camilo@correo.com");
}

#[test]
fn changing_the_password_moves_the_updated_stamp() {
    let mut user = user();
    let later = now() + Duration::hours(3);
    user.change_password(
        PasswordHash::parse("$argon2id$v=19$new").expect("valid"),
        later,
    );
    assert_eq!(user.updated_at(), later);
    assert_eq!(user.created_at(), now());
    assert_eq!(user.password_hash().as_str(), "$argon2id$v=19$new");
}

fn seed(user_id: UserId) -> NewRefreshToken {
    NewRefreshToken {
        user_id,
        digest: digest(0x11),
        created_at: now(),
        expires_at: now() + Duration::days(30),
        user_agent: Some("android".to_owned()),
    }
}

#[test]
fn a_fresh_refresh_token_is_usable_until_it_expires() {
    let token = RefreshToken::issue(seed(UserId::generate()));
    assert!(token.is_usable(now()));
    assert!(!token.is_usable(now() + Duration::days(31)));
    assert!(!token.is_revoked());
    assert!(!token.was_rotated());
    assert_eq!(token.user_agent(), Some("android"));
}

#[test]
fn a_rotated_token_is_no_longer_usable_and_points_at_its_replacement() {
    let replacement = RefreshTokenId::generate();
    let token = RefreshToken::rehydrate(
        RefreshTokenId::generate(),
        StoredRefreshToken {
            token: seed(UserId::generate()),
            revoked_at: Some(now()),
            replaced_by: Some(replacement),
        },
    );
    assert!(token.is_revoked());
    assert!(token.was_rotated());
    assert!(!token.is_usable(now()));
    assert_eq!(token.replaced_by(), Some(replacement));
}

#[test]
fn a_reset_token_is_single_use_and_expires() {
    let seed = NewPasswordResetToken {
        user_id: UserId::generate(),
        digest: digest(0x22),
        created_at: now(),
        expires_at: now() + Duration::minutes(30),
    };
    let token = PasswordResetToken::issue(seed.clone());
    assert!(token.is_usable(now()));
    assert!(!token.is_usable(now() + Duration::minutes(31)));

    let used = PasswordResetToken::rehydrate(token.id(), seed, Some(now()));
    assert!(!used.is_usable(now()));
}

#[test]
fn the_shared_default_categories_serve_both_grids() {
    assert!(CategoryKind::Both.accepts(CategoryKind::In));
    assert!(CategoryKind::Both.accepts(CategoryKind::Out));
    assert!(!CategoryKind::Out.accepts(CategoryKind::In));
    assert!(!CategoryKind::Both.is_selectable_by_users());
    assert_eq!(Language::default(), Language::Spanish);
    assert_eq!(Language::English.locale(), "en-US");
}
