//! Creacion de categorias del usuario.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::category::CategoryKind;

use super::support::{Harness, creation};
use crate::error::AppError;
use crate::testing::block_on;

#[test]
fn a_new_category_is_stored_with_no_movements() {
    let harness = Harness::new();

    let created = block_on(harness.create.execute(creation(harness.user, "Mascotas")))
        .expect("should create");

    assert_eq!(created.category.name().as_str(), "Mascotas");
    assert_eq!(created.category.kind(), CategoryKind::Out);
    assert!(!created.category.is_default());
    assert_eq!(created.movements, 0);
    assert!(harness.categories.by_name("mascotas").is_some());
}

#[test]
fn the_name_is_trimmed_and_capped_at_twenty_two_characters() {
    let harness = Harness::new();

    let trimmed = block_on(
        harness
            .create
            .execute(creation(harness.user, "  Mascotas  ")),
    )
    .expect("should create");
    assert_eq!(trimmed.category.name().as_str(), "Mascotas");

    let too_long = block_on(
        harness
            .create
            .execute(creation(harness.user, "Veintitres caracteres ya")),
    );
    assert!(matches!(too_long, Err(AppError::Validation(_))));
}

#[test]
fn an_empty_name_is_rejected() {
    let harness = Harness::new();

    let result = block_on(harness.create.execute(creation(harness.user, "   ")));

    assert!(matches!(result, Err(AppError::Validation(_))));
}

#[test]
fn a_name_that_repeats_ignoring_case_is_a_conflict() {
    let harness = Harness::new();

    let repeated = block_on(harness.create.execute(creation(harness.user, "sAlArIo")));
    let Err(AppError::Conflict { code, .. }) = repeated else {
        panic!("a repeated name is a conflict");
    };
    assert_eq!(code, "category_name_taken");
}

#[test]
fn the_same_name_is_free_in_another_account() {
    let harness = Harness::new();
    block_on(harness.create.execute(creation(harness.user, "Mascotas"))).expect("should create");

    let other = block_on(
        harness
            .create
            .execute(creation(harness.other_user, "Mascotas")),
    );

    assert!(other.is_ok());
}

#[test]
fn only_the_icons_and_colors_of_the_design_are_accepted() {
    let harness = Harness::new();

    let mut bogus_icon = creation(harness.user, "Mascotas");
    bogus_icon.icon = "rocket_launch".to_owned();
    let icon_result = block_on(harness.create.execute(bogus_icon));
    assert!(field_rejected(&icon_result, "icon"));

    let mut bogus_color = creation(harness.user, "Mascotas");
    bogus_color.color = "#123456".to_owned();
    let color_result = block_on(harness.create.execute(bogus_color));
    assert!(field_rejected(&color_result, "color"));
}

#[test]
fn the_user_cannot_create_a_category_that_serves_both_grids() {
    let harness = Harness::new();
    let mut command = creation(harness.user, "Mascotas");
    command.kind = "both".to_owned();

    let result = block_on(harness.create.execute(command));

    assert!(field_rejected(&result, "kind"));
}

fn field_rejected<T>(result: &Result<T, AppError>, field: &str) -> bool {
    matches!(result, Err(AppError::Validation(errors))
        if errors.entries().iter().any(|entry| entry.field == field))
}
