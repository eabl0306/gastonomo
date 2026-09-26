//! Borrado de categorias con reasignacion a "Otros".

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::category::FALLBACK_CATEGORY_NAME;

use super::support::Harness;
use crate::error::AppError;
use crate::testing::block_on;

#[test]
fn deleting_moves_its_movements_to_otros_and_removes_the_row() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 5);
    let otros = harness.default_id(FALLBACK_CATEGORY_NAME);

    let reassigned =
        block_on(harness.delete.execute(harness.user, mascotas)).expect("should delete");

    assert_eq!(reassigned, 5);
    assert_eq!(harness.categories.usage_of(otros), 5);
    assert!(harness.categories.by_name("Mascotas").is_none());
}

#[test]
fn deleting_a_category_with_no_movements_reassigns_nothing() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");

    let reassigned =
        block_on(harness.delete.execute(harness.user, mascotas)).expect("should delete");

    assert_eq!(reassigned, 0);
}

#[test]
fn a_default_category_cannot_be_deleted() {
    let harness = Harness::new();
    let alimentacion = harness.default_id("Alimentación");

    let result = block_on(harness.delete.execute(harness.user, alimentacion));

    let Err(AppError::Conflict { code, .. }) = result else {
        panic!("deleting a default is a conflict");
    };
    assert_eq!(code, "category_is_default");
    assert!(harness.categories.by_name("Alimentación").is_some());
}

#[test]
fn a_category_of_another_account_is_not_found() {
    let harness = Harness::new();
    let theirs = harness.seed_custom(harness.other_user, "Ajena");

    let result = block_on(harness.delete.execute(harness.user, theirs));

    assert!(matches!(result, Err(AppError::NotFound { .. })));
    assert!(harness.categories.by_name("Ajena").is_some());
}
