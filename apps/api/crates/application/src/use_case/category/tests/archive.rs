//! Archivar una categoria, recuperarla, y el unico borrado que queda.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use super::support::Harness;
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::category::CategoryRemoval;

#[test]
fn archiving_a_used_category_does_not_touch_a_single_movement() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 5);

    let outcome =
        block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive");

    // Es la garantia entera de este cambio: el historico no se reescribe. Antes,
    // borrar movia esos cinco movimientos a "Otros".
    assert_eq!(outcome, CategoryRemoval::Archived);
    assert_eq!(harness.categories.usage_of(mascotas), 5);
    assert!(harness.categories.by_name("Mascotas").is_some());
}

#[test]
fn an_archived_category_is_still_there_to_resolve_old_records() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 1);
    block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive");

    let stored = harness
        .categories
        .by_name("Mascotas")
        .expect("still stored");

    assert!(stored.is_archived());
}

#[test]
fn archiving_twice_is_the_same_as_archiving_once() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 1);
    block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive");
    let first = harness.categories.by_name("Mascotas").expect("stored");

    block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive again");

    // Lo que importa es cuando dejo de usarse, no cuantas veces se pidio.
    let second = harness.categories.by_name("Mascotas").expect("stored");
    assert_eq!(first.archived_at(), second.archived_at());
}

#[test]
fn a_category_no_movement_uses_is_deleted_for_real() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");

    let outcome = block_on(harness.archive.execute(harness.user, mascotas)).expect("should delete");

    // No hay historico que proteger, y asi no se acumula basura de categorias
    // creadas por error.
    assert_eq!(outcome, CategoryRemoval::Deleted);
    assert!(harness.categories.by_name("Mascotas").is_none());
}

#[test]
fn an_archived_category_can_come_back() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 1);
    block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive");

    let restored =
        block_on(harness.restore.execute(harness.user, mascotas)).expect("should restore");

    // Sin esto, archivar por error no tendria vuelta atras: crear otra con el mismo
    // nombre chocaria con la unicidad.
    assert!(!restored.category.is_archived());
}

#[test]
fn the_default_listing_leaves_the_archived_out_and_asking_brings_them_back() {
    let harness = Harness::new();
    let mascotas = harness.seed_custom(harness.user, "Mascotas");
    harness.categories.set_usage(mascotas, 1);
    block_on(harness.archive.execute(harness.user, mascotas)).expect("should archive");

    let active = block_on(harness.list.execute(harness.user, false)).expect("should list");
    let all = block_on(harness.list.execute(harness.user, true)).expect("should list");

    assert!(active.iter().all(|found| found.category.id() != mascotas));
    assert!(all.iter().any(|found| found.category.id() == mascotas));
}

#[test]
fn a_default_category_cannot_be_archived() {
    let harness = Harness::new();
    let alimentacion = harness.default_id("Alimentación");

    let result = block_on(harness.archive.execute(harness.user, alimentacion));

    let Err(AppError::Conflict { code, .. }) = result else {
        panic!("archiving a default is a conflict");
    };
    assert_eq!(code, "category_is_default");
    assert!(harness.categories.by_name("Alimentación").is_some());
}

#[test]
fn a_category_of_another_account_is_not_found() {
    let harness = Harness::new();
    let theirs = harness.seed_custom(harness.other_user, "Ajena");

    let result = block_on(harness.archive.execute(harness.user, theirs));

    assert!(matches!(result, Err(AppError::NotFound { .. })));
    assert!(harness.categories.by_name("Ajena").is_some());
}
