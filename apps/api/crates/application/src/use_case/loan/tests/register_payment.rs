//! Registrar abono: saldo, aislamiento y validacion.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use gastonomo_domain::CategoryKind;
use gastonomo_domain::loan::LoanDirection;

use super::support::{Harness, a_payment, owner, stored_loan};
use crate::error::AppError;
use crate::testing::block_on;
use crate::use_case::loan::RegisterLoanPaymentCommand;

#[test]
fn a_payment_over_the_balance_is_accepted_because_the_design_shows_the_surplus() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(user, LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();

    let loan = block_on(harness.pay().execute(
        user,
        &stored.id().to_string(),
        RegisterLoanPaymentCommand {
            amount: "900000".to_owned(),
            base: Some("900000".to_owned()),
            ..a_payment()
        },
    ))
    .expect("a payment bigger than the loan is earned interest, not an error");

    assert_eq!(loan.payments().len(), 1);
    assert_eq!(
        loan.payments()[0].amount().amount().to_storage_string(),
        "900000.0000"
    );
}

#[test]
fn paying_a_loan_i_received_writes_an_expense_movement() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(user, LoanDirection::Borrowed));
    let stored = harness.loans.loans()[0].clone();

    block_on(
        harness
            .pay()
            .execute(user, &stored.id().to_string(), a_payment()),
    )
    .expect("should register the payment");

    assert_eq!(harness.loans.movements()[0].kind(), CategoryKind::Out);
}

#[test]
fn a_payment_on_someone_elses_loan_answers_not_found() {
    let harness = Harness::with_loan(stored_loan(owner(), LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();

    let error = block_on(
        harness
            .pay()
            .execute(owner(), &stored.id().to_string(), a_payment()),
    )
    .expect_err("another account must not reach the loan");

    assert!(matches!(error, AppError::NotFound { .. }));
    assert!(
        harness.loans.movements().is_empty(),
        "a rejected payment writes nothing"
    );
}

#[test]
fn an_identifier_that_is_not_a_uuid_answers_not_found_and_never_a_bad_request() {
    let harness = Harness::new();
    let error = block_on(harness.pay().execute(owner(), "not-a-uuid", a_payment()))
        .expect_err("should not find anything");

    assert!(matches!(error, AppError::NotFound { .. }));
}

#[test]
fn a_blank_label_is_the_same_as_no_label() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(user, LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();

    let loan = block_on(harness.pay().execute(
        user,
        &stored.id().to_string(),
        RegisterLoanPaymentCommand {
            label: Some("   ".to_owned()),
            ..a_payment()
        },
    ))
    .expect("should register the payment");

    assert_eq!(loan.payments()[0].label(), None);
}

#[test]
fn a_label_longer_than_the_limit_is_rejected() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(user, LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();

    let error = block_on(harness.pay().execute(
        user,
        &stored.id().to_string(),
        RegisterLoanPaymentCommand {
            label: Some("x".repeat(41)),
            ..a_payment()
        },
    ))
    .expect_err("should not register");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    assert_eq!(errors.entries()[0].field, "label");
}

#[test]
fn if_the_movement_fails_the_payment_is_not_stored_either() {
    let user = owner();
    let harness = Harness::breaking();
    let error = block_on(harness.pay().execute(
        user,
        &stored_loan(user, LoanDirection::Lent).id().to_string(),
        a_payment(),
    ))
    .expect_err("should fail");

    assert!(matches!(
        error,
        AppError::NotFound { .. } | AppError::Dependency { .. }
    ));
    assert!(harness.loans.movements().is_empty());
}
