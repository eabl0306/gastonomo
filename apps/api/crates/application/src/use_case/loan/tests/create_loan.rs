//! Nuevo prestamo: lo que se guarda y lo que se rechaza.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::CategoryKind;
use gastonomo_domain::category::{LOAN_CATEGORY_NAME, LOAN_PAYMENT_CATEGORY_NAME};

use super::support::{Harness, a_loan, a_payment, owner, stored_loan};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::loan::CreateLoanCommand;

#[test]
fn a_loan_i_gave_also_writes_an_expense_movement() {
    let harness = Harness::new();
    let user = owner();
    let loan = block_on(harness.create().execute(user, a_loan())).expect("should create");

    assert_eq!(loan.person().as_str(), "Rosa");
    assert_eq!(loan.amount().amount().to_storage_string(), "600000.0000");
    assert!(loan.payments().is_empty(), "a new loan has no payments yet");

    let movements = harness.loans.movements();
    assert_eq!(movements.len(), 1);
    assert_eq!(movements[0].kind(), CategoryKind::Out);
    assert_eq!(movements[0].category_name(), LOAN_CATEGORY_NAME);
    assert_eq!(movements[0].title(), "Préstamo · Rosa");
    assert_eq!(movements[0].loan_id(), loan.id());
    assert_eq!(movements[0].loan_payment_id(), None);
    assert_eq!(movements[0].occurred_on(), loan.opened_on());
}

#[test]
fn a_loan_i_received_writes_an_income_movement() {
    let harness = Harness::new();
    let loan = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            direction: "borrowed".to_owned(),
            ..a_loan()
        },
    ))
    .expect("should create");

    let movements = harness.loans.movements();
    assert_eq!(movements[0].kind(), CategoryKind::In);
    assert_eq!(movements[0].loan_id(), loan.id());
}

#[test]
fn a_payment_movement_goes_the_other_way_and_carries_both_identifiers() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(
        user,
        gastonomo_domain::loan::LoanDirection::Lent,
    ));
    let stored = harness.loans.loans()[0].clone();

    let loan = block_on(
        harness
            .pay()
            .execute(user, &stored.id().to_string(), a_payment()),
    )
    .expect("should register the payment");

    assert_eq!(loan.payments().len(), 1);
    let payment = &loan.payments()[0];
    assert_eq!(payment.label(), Some("Intereses"));

    let movements = harness.loans.movements();
    assert_eq!(movements.len(), 1);
    assert_eq!(movements[0].kind(), CategoryKind::In);
    assert_eq!(movements[0].category_name(), LOAN_PAYMENT_CATEGORY_NAME);
    assert_eq!(movements[0].title(), "Abono · Rosa");
    assert_eq!(movements[0].loan_id(), loan.id());
    assert_eq!(movements[0].loan_payment_id(), Some(payment.id()));
}

#[test]
fn if_the_movement_fails_the_loan_is_not_stored_either() {
    let harness = Harness::breaking();
    let error = block_on(harness.create().execute(owner(), a_loan())).expect_err("should fail");

    assert!(matches!(error, AppError::Dependency { .. }));
    assert!(harness.loans.loans().is_empty(), "nothing may be left over");
    assert!(harness.loans.movements().is_empty());
}

#[test]
fn an_empty_person_and_a_zero_amount_are_rejected_together() {
    let harness = Harness::new();
    let error = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            person: "   ".to_owned(),
            amount: "0".to_owned(),
            base: Some("0".to_owned()),
            ..a_loan()
        },
    ))
    .expect_err("should not create");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    let fields: Vec<&str> = errors
        .entries()
        .iter()
        .map(|entry| entry.field.as_str())
        .collect();
    assert_eq!(fields, vec!["person", "amount"]);
}

#[test]
fn a_bogus_direction_and_a_bogus_date_are_rejected() {
    let harness = Harness::new();
    let error = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            direction: "given".to_owned(),
            opened_on: "28/06/2026".to_owned(),
            ..a_loan()
        },
    ))
    .expect_err("should not create");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    let fields: Vec<&str> = errors
        .entries()
        .iter()
        .map(|entry| entry.field.as_str())
        .collect();
    assert_eq!(fields, vec!["direction", "openedOn"]);
}

#[test]
fn the_same_currency_cannot_carry_two_different_amounts() {
    let harness = Harness::new();
    let error = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            base: Some("700000".to_owned()),
            ..a_loan()
        },
    ))
    .expect_err("should not create");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    assert_eq!(errors.entries()[0].field, "base");
    assert_eq!(
        errors.entries()[0].message,
        "Con una sola moneda el importe y su equivalencia tienen que coincidir."
    );
}

#[test]
fn a_loan_in_another_currency_keeps_the_rate_the_user_typed() {
    let harness = Harness::new();
    let loan = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            amount: "140".to_owned(),
            currency: "USD".to_owned(),
            base: Some("450000".to_owned()),
            base_currency: Some("COP".to_owned()),
            ..a_loan()
        },
    ))
    .expect("should create");

    assert_eq!(loan.amount().amount().to_storage_string(), "140.0000");
    assert_eq!(loan.amount().currency().as_str(), "USD");
    assert_eq!(loan.amount().base().to_storage_string(), "450000.0000");
    assert_eq!(loan.amount().base_currency().as_str(), "COP");
    assert!(loan.amount().is_converted());
}

/// El contrato fija que `errors[].message` va en castellano. Antes de este test el
/// importe cero del prestamo devolvia el `Display` del error de dominio, en ingles.
#[test]
fn an_amount_of_zero_is_reported_in_spanish() {
    let harness = Harness::new();
    let error = block_on(harness.create().execute(
        owner(),
        CreateLoanCommand {
            amount: "0".to_owned(),
            base: Some("0".to_owned()),
            ..a_loan()
        },
    ))
    .expect_err("should not create");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    assert_eq!(errors.entries()[0].field, "amount");
    assert_eq!(
        errors.entries()[0].message,
        "El importe tiene que ser mayor que cero."
    );
}

#[test]
fn a_loan_in_the_main_currency_can_omit_the_equivalence() {
    // La misma regla que POST /v1/movements: omitir base y baseCurrency juntos
    // significa que la equivalencia es el propio importe. Antes los dos eran
    // obligatorios aqui y opcionales alli, y esa asimetria no la pide el diseno.
    let harness = Harness::new();
    let command = CreateLoanCommand {
        base: None,
        base_currency: None,
        ..a_loan()
    };

    let loan = block_on(harness.create().execute(owner(), command)).expect("should open");

    assert_eq!(loan.amount().base().to_string(), "600000");
    assert_eq!(loan.amount().base_currency().as_str(), "COP");
}

#[test]
fn half_an_equivalence_is_rejected_on_the_base_field() {
    let harness = Harness::new();
    let command = CreateLoanCommand {
        base: Some("450000".to_owned()),
        base_currency: None,
        ..a_loan()
    };

    let error =
        block_on(harness.create().execute(owner(), command)).expect_err("half a pair is wrong");

    let AppError::Validation(errors) = error else {
        panic!("expected a validation error");
    };
    assert_eq!(errors.entries().len(), 1);
    assert_eq!(errors.entries()[0].field, "base");
    assert_eq!(
        errors.entries()[0].message,
        "La equivalencia necesita importe y moneda."
    );
}
