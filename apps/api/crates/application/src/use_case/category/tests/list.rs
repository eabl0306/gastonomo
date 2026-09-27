//! Listado de categorias con su conteo de uso.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::category::DEFAULT_CATEGORIES;

use super::support::Harness;
use crate::testing::block_on;

#[test]
fn a_new_account_lists_the_twelve_default_categories() {
    let harness = Harness::new();

    let listed = block_on(harness.list.execute(harness.user, false)).expect("should list");

    assert_eq!(listed.len(), DEFAULT_CATEGORIES.len());
    assert!(listed.iter().all(|entry| entry.category.is_default()));
    assert!(listed.iter().all(|entry| entry.movements == 0));
}

#[test]
fn the_defaults_come_before_the_ones_the_user_created() {
    let harness = Harness::new();
    harness.seed_custom(harness.user, "Mascotas");

    let listed = block_on(harness.list.execute(harness.user, false)).expect("should list");

    let custom_position = listed
        .iter()
        .position(|entry| !entry.category.is_default())
        .expect("the custom category is listed");
    assert_eq!(custom_position, DEFAULT_CATEGORIES.len());
    assert_eq!(listed.len(), DEFAULT_CATEGORIES.len() + 1);
}

#[test]
fn the_usage_count_travels_with_the_list() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 3);

    let listed = block_on(harness.list.execute(harness.user, false)).expect("should list");

    let entry = listed
        .iter()
        .find(|entry| entry.category.id() == mascotas)
        .expect("the custom category is listed");
    assert_eq!(entry.movements, 3);
}

#[test]
fn the_list_only_has_the_categories_of_the_account_asking() {
    let harness = Harness::new();
    harness.seed_custom(harness.other_user, "Ajena");

    let listed = block_on(harness.list.execute(harness.user, false)).expect("should list");

    assert!(
        listed
            .iter()
            .all(|entry| entry.category.user_id() == harness.user)
    );
    assert!(
        !listed
            .iter()
            .any(|entry| entry.category.name().same_as("Ajena"))
    );
}

#[test]
fn the_use_cases_never_print_their_adapters() {
    let harness = Harness::new();

    for rendered in [
        format!("{:?}", harness.list),
        format!("{:?}", harness.create),
        format!("{:?}", harness.update),
        format!("{:?}", harness.archive),
        format!("{:?}", harness.restore),
    ] {
        assert!(rendered.contains("Categor"), "{rendered}");
        assert!(rendered.ends_with(".. }"), "{rendered}");
    }
}
