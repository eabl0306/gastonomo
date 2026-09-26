//! Tests de `CreateMovement`.

use super::support::{World, expense, fixed_id};
use crate::testing::block_on;
use crate::use_case::movement::CreateMovementCommand;

fn message_for(error: &crate::error::AppError, field: &str) -> Option<String> {
    match error {
        crate::error::AppError::Validation(errors) => errors
            .entries()
            .iter()
            .find(|entry| entry.field == field)
            .map(|entry| entry.message.clone()),
        _ => None,
    }
}

#[test]
fn a_valid_expense_is_stored_with_its_four_amount_fields() {
    let world = World::new();

    let movement =
        block_on(world.create().execute(world.user, expense())).expect("should be created");

    assert_eq!(movement.title().as_str(), "Mercado");
    assert_eq!(
        movement.amount().amount().to_storage_string(),
        "186400.0000"
    );
    assert_eq!(movement.amount().currency().as_str(), "COP");
    assert_eq!(movement.amount().base().to_storage_string(), "186400.0000");
    assert_eq!(movement.amount().base_currency().as_str(), "COP");
    assert!(!movement.amount().is_converted());
    assert_eq!(world.movements.all().len(), 1);
}

#[test]
fn without_a_note_the_title_is_the_category_name() {
    let world = World::new();

    let movement = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            note: Some("   ".to_owned()),
            ..expense()
        },
    ))
    .expect("should be created");

    assert_eq!(movement.title().as_str(), "Alimentación");
}

#[test]
fn a_record_in_another_currency_keeps_the_rate_the_user_typed() {
    let world = World::new();

    let movement = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            category_id: fixed_id(2).to_string(),
            kind: "in".to_owned(),
            note: Some("Diseño web".to_owned()),
            amount: "140".to_owned(),
            currency: "USD".to_owned(),
            base: Some("450000".to_owned()),
            base_currency: Some("COP".to_owned()),
            ..expense()
        },
    ))
    .expect("should be created");

    assert_eq!(movement.amount().amount().to_storage_string(), "140.0000");
    assert_eq!(movement.amount().base().to_storage_string(), "450000.0000");
    assert!(movement.amount().is_converted());
}

#[test]
fn a_category_of_another_account_is_not_usable() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            category_id: fixed_id(3).to_string(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert!(message_for(&error, "categoryId").is_some());
}

#[test]
fn an_expense_cannot_use_an_income_category() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            category_id: fixed_id(2).to_string(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert!(message_for(&error, "categoryId").is_some());
    assert!(world.movements.all().is_empty());
}

#[test]
fn an_amount_of_zero_is_rejected() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            amount: "0".to_owned(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert!(message_for(&error, "amount").is_some());
}

#[test]
fn an_equivalence_of_zero_is_rejected() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            amount: "140".to_owned(),
            currency: "USD".to_owned(),
            base: Some("0".to_owned()),
            base_currency: Some("COP".to_owned()),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert_eq!(
        message_for(&error, "base").as_deref(),
        Some("La equivalencia tiene que ser mayor que cero.")
    );
}

#[test]
fn the_same_currency_with_a_different_equivalence_is_rejected() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            amount: "100".to_owned(),
            currency: "COP".to_owned(),
            base: Some("200".to_owned()),
            base_currency: Some("COP".to_owned()),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert_eq!(
        message_for(&error, "base").as_deref(),
        Some("Con una sola moneda el importe y su equivalencia tienen que coincidir.")
    );
}

#[test]
fn the_equivalence_needs_both_halves() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            base: Some("450000".to_owned()),
            base_currency: None,
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert_eq!(
        message_for(&error, "base").as_deref(),
        Some("La equivalencia necesita importe y moneda.")
    );
}

#[test]
fn a_date_far_in_the_future_is_rejected() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            occurred_on: "2027-01-01".to_owned(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert!(message_for(&error, "occurredOn").is_some());
}

#[test]
fn tomorrow_is_accepted_because_of_the_time_zones() {
    let world = World::new();

    let movement = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            occurred_on: "2026-09-23".to_owned(),
            ..expense()
        },
    ))
    .expect("a phone in UTC+14 is already on the next day");

    assert_eq!(movement.occurred_on().to_string(), "2026-09-23");
}

#[test]
fn a_date_before_the_year_2000_is_rejected() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            occurred_on: "1999-12-31".to_owned(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert!(message_for(&error, "occurredOn").is_some());
}

#[test]
fn every_broken_field_is_reported_in_one_answer() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            occurred_on: "22/09/2026".to_owned(),
            kind: "gasto".to_owned(),
            category_id: "no-uuid".to_owned(),
            amount: "mucho".to_owned(),
            currency: "PESOS".to_owned(),
            ..expense()
        },
    ))
    .expect_err("should reject");

    for field in ["occurredOn", "kind", "categoryId", "amount", "currency"] {
        assert!(
            message_for(&error, field).is_some(),
            "{field} should be reported"
        );
    }
}

/// El contrato fija que `errors[].message` va en castellano. Antes de este test la
/// nota demasiado larga devolvia el `Display` del error de dominio, en ingles, y el
/// formulario lo pintaba tal cual junto al campo.
#[test]
fn a_note_that_is_too_long_is_reported_in_spanish() {
    let world = World::new();

    let error = block_on(world.create().execute(
        world.user,
        CreateMovementCommand {
            note: Some("x".repeat(121)),
            ..expense()
        },
    ))
    .expect_err("should reject");

    assert_eq!(
        message_for(&error, "note").as_deref(),
        Some("La nota admite 120 caracteres.")
    );
}
