//! Rotacion del refresh token y deteccion de reutilizacion.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use chrono::Duration;

use super::support::{Harness, REFRESH_TTL_DAYS, registration};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::auth::RefreshSessionCommand;

fn refresh(harness: &Harness, token: &str) -> Result<crate::IssuedSession, AppError> {
    block_on(harness.refresh().execute(RefreshSessionCommand {
        refresh_token: token.to_owned(),
        user_agent: None,
    }))
}

#[test]
fn refreshing_returns_a_new_pair_and_retires_the_old_one() {
    let harness = Harness::new();
    let first = block_on(harness.register().execute(registration())).expect("should register");
    let second = refresh(&harness, &first.refresh_token).expect("should refresh");

    assert_ne!(first.refresh_token, second.refresh_token);
    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 1);

    let stored = harness.refresh_tokens.all();
    assert!(stored[0].is_revoked(), "the old token is revoked");
    assert_eq!(
        stored[0].replaced_by(),
        Some(stored[1].id()),
        "and points at the one that replaced it"
    );
}

#[test]
fn reusing_a_rotated_token_revokes_the_whole_family() {
    let harness = Harness::new();
    let first = block_on(harness.register().execute(registration())).expect("should register");
    let second = refresh(&harness, &first.refresh_token).expect("should refresh");

    let error = refresh(&harness, &first.refresh_token).expect_err("reuse should be rejected");
    assert!(matches!(error, AppError::Unauthorized { .. }));
    assert_eq!(
        harness.refresh_tokens.usable_count(harness.now()),
        0,
        "the stolen family is gone, the legitimate owner logs in again"
    );
    assert!(
        refresh(&harness, &second.refresh_token).is_err(),
        "the token the thief could also hold no longer works"
    );
}

#[test]
fn an_unknown_token_does_not_refresh() {
    let harness = Harness::new();
    let error = refresh(&harness, "invented").expect_err("should be rejected");
    assert!(matches!(error, AppError::Unauthorized { .. }));
}

#[test]
fn an_expired_token_does_not_refresh() {
    let harness = Harness::new();
    let session = block_on(harness.register().execute(registration())).expect("should register");
    harness.clock.advance(Duration::days(REFRESH_TTL_DAYS + 1));

    let error = refresh(&harness, &session.refresh_token).expect_err("should be rejected");
    assert!(matches!(error, AppError::Unauthorized { .. }));
}

#[test]
fn a_chain_of_refreshes_keeps_exactly_one_live_session() {
    let harness = Harness::new();
    let mut token = block_on(harness.register().execute(registration()))
        .expect("should register")
        .refresh_token;
    for _ in 0..4 {
        token = refresh(&harness, &token)
            .expect("should refresh")
            .refresh_token;
    }
    assert_eq!(harness.refresh_tokens.all().len(), 5);
    assert_eq!(harness.refresh_tokens.usable_count(harness.now()), 1);
}
