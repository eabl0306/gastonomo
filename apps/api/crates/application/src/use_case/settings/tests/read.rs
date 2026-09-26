//! Lectura de los ajustes.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::settings::Language;

use super::support::Harness;
use crate::testing::block_on;

#[test]
fn an_account_with_no_row_reads_the_defaults_of_the_design() {
    let harness = Harness::new();

    let settings = block_on(harness.read.execute(harness.user)).expect("should read");

    assert_eq!(settings.display_currency().as_str(), "COP");
    assert_eq!(settings.language(), Language::Spanish);
    assert_eq!(harness.store.count(), 0, "reading does not create the row");
}

#[test]
fn what_was_saved_is_what_comes_back() {
    let harness = Harness::new();
    block_on(
        harness
            .write
            .execute(harness.change(Some("usd"), Some("en"))),
    )
    .expect("should update");

    let settings = block_on(harness.read.execute(harness.user)).expect("should read");

    assert_eq!(settings.display_currency().as_str(), "USD");
    assert_eq!(settings.language(), Language::English);
    assert_eq!(settings.language().locale(), "en-US");
}

#[test]
fn the_use_cases_never_print_their_adapters() {
    let harness = Harness::new();

    for rendered in [
        format!("{:?}", harness.read),
        format!("{:?}", harness.write),
    ] {
        assert!(rendered.contains("UserSettings"), "{rendered}");
        assert!(rendered.ends_with(".. }"), "{rendered}");
    }
}
