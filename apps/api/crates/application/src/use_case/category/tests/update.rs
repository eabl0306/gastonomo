//! Edicion de categorias.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::category::CategoryKind;

use super::support::{Harness, creation, empty_change};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::category::UpdateCategoryCommand;

#[test]
fn editing_only_the_color_leaves_the_rest_as_it_was() {
    let harness = Harness::new();
    let created = block_on(harness.create.execute(creation(harness.user, "Mascotas")))
        .expect("should create");

    let updated = block_on(harness.update.execute(UpdateCategoryCommand {
        color: Some("#b84a86".to_owned()),
        ..empty_change(harness.user, created.category.id())
    }))
    .expect("should update");

    assert_eq!(updated.category.color().as_str(), "#b84a86");
    assert_eq!(updated.category.name().as_str(), "Mascotas");
    assert_eq!(updated.category.icon().as_str(), "pets");
    assert_eq!(updated.category.kind(), CategoryKind::Out);
}

#[test]
fn renaming_does_not_touch_the_movements_that_use_it() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 4);

    let updated = block_on(harness.update.execute(UpdateCategoryCommand {
        name: Some("Veterinario".to_owned()),
        ..empty_change(harness.user, mascotas)
    }))
    .expect("should update");

    assert_eq!(updated.category.id(), mascotas);
    assert_eq!(updated.category.name().as_str(), "Veterinario");
    assert_eq!(updated.movements, 4);
    assert_eq!(harness.categories.usage_of(mascotas), 4);
}

#[test]
fn a_patch_with_nothing_inside_changes_nothing() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");

    let updated = block_on(harness.update.execute(empty_change(harness.user, mascotas)))
        .expect("should return the category as it is");

    assert_eq!(updated.category.name().as_str(), "Mascotas");
}

#[test]
fn a_default_category_cannot_be_edited() {
    let harness = Harness::new();
    let salario = harness.default_id("Salario");

    let result = block_on(harness.update.execute(UpdateCategoryCommand {
        name: Some("Sueldo".to_owned()),
        ..empty_change(harness.user, salario)
    }));

    let Err(AppError::Conflict { code, .. }) = result else {
        panic!("editing a default is a conflict");
    };
    assert_eq!(code, "category_is_default");
}

#[test]
fn renaming_onto_an_existing_name_is_a_conflict() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");

    let result = block_on(harness.update.execute(UpdateCategoryCommand {
        name: Some("Transporte".to_owned()),
        ..empty_change(harness.user, mascotas)
    }));

    let Err(AppError::Conflict { code, .. }) = result else {
        panic!("a repeated name is a conflict");
    };
    assert_eq!(code, "category_name_taken");
}

#[test]
fn a_category_of_another_account_is_not_found() {
    let harness = Harness::new();
    let theirs = harness.seed_custom(harness.other_user, "Ajena");

    let result = block_on(harness.update.execute(UpdateCategoryCommand {
        name: Some("Mia".to_owned()),
        ..empty_change(harness.user, theirs)
    }));

    assert!(matches!(result, Err(AppError::NotFound { .. })));
}

#[test]
fn editing_keeps_the_icon_and_color_inside_the_design_palette() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");

    let result = block_on(harness.update.execute(UpdateCategoryCommand {
        icon: Some("rocket_launch".to_owned()),
        ..empty_change(harness.user, mascotas)
    }));

    assert!(matches!(result, Err(AppError::Validation(_))));
}
