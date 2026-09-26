//! Cambio de los ajustes.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::settings::Language;

use super::support::Harness;
use crate::error::AppError;
use crate::testing::block_on;

#[test]
fn changing_the_currency_leaves_the_language_alone() {
    let harness = Harness::new();

    let settings =
        block_on(harness.write.execute(harness.change(Some("EUR"), None))).expect("should update");

    assert_eq!(settings.display_currency().as_str(), "EUR");
    assert_eq!(settings.language(), Language::Spanish);
    assert_eq!(harness.store.count(), 1);
}

#[test]
fn changing_the_language_leaves_the_currency_alone() {
    let harness = Harness::new();

    let settings =
        block_on(harness.write.execute(harness.change(None, Some("en")))).expect("should update");

    assert_eq!(settings.display_currency().as_str(), "COP");
    assert_eq!(settings.language(), Language::English);
}

#[test]
fn a_currency_outside_the_catalog_is_rejected() {
    let harness = Harness::with_currencies(&["COP", "USD"]);

    let result = block_on(harness.write.execute(harness.change(Some("EUR"), None)));

    let Err(AppError::Validation(errors)) = result else {
        panic!("an unknown currency is a validation error");
    };
    assert_eq!(errors.entries().len(), 1);
    assert_eq!(errors.entries()[0].field, "displayCurrency");
    assert_eq!(harness.store.count(), 0, "nothing is written when it fails");
}

#[test]
fn a_currency_that_is_not_three_letters_is_rejected() {
    let harness = Harness::new();

    let result = block_on(harness.write.execute(harness.change(Some("pesos"), None)));

    assert!(matches!(result, Err(AppError::Validation(_))));
}

#[test]
fn a_language_that_is_not_es_or_en_is_rejected() {
    let harness = Harness::new();

    let result = block_on(harness.write.execute(harness.change(None, Some("fr"))));

    let Err(AppError::Validation(errors)) = result else {
        panic!("an unsupported language is a validation error");
    };
    assert_eq!(errors.entries()[0].field, "language");
}

#[test]
fn a_patch_with_nothing_inside_does_not_write() {
    let harness = Harness::new();

    let settings = block_on(harness.write.execute(harness.change(None, None)))
        .expect("should return the settings as they are");

    assert_eq!(settings.display_currency().as_str(), "COP");
    assert_eq!(harness.store.count(), 0);
}

#[test]
fn an_empty_catalog_does_not_blame_the_client_for_the_currency() {
    // Despliegue en limpio con el primer refresco de tasas fallido: la tabla
    // currencies esta vacia. Antes esto respondia 422 y culpaba al cliente de una
    // moneda que no podia arreglar.
    let harness = Harness::with_currencies(&[]);

    let error = block_on(harness.write.execute(harness.change(Some("EUR"), None)))
        .expect_err("there is nothing to validate against");

    assert!(
        matches!(error, AppError::Unavailable { .. }),
        "un catalogo vacio es 503 y no 422: got {error:?}"
    );
}

#[test]
fn the_currency_the_account_already_has_is_accepted_without_a_catalog() {
    // Ajustes ensena la moneda actual marcada. Confirmarla no puede fallar, ni
    // siquiera antes de la primera pasada de la tarea de tasas.
    let harness = Harness::with_currencies(&[]);

    let settings = block_on(
        harness
            .write
            .execute(harness.change(Some("COP"), Some("en"))),
    )
    .expect("the current currency is always selectable");

    assert_eq!(settings.display_currency().as_str(), "COP");
    assert_eq!(settings.language(), Language::English);
}
