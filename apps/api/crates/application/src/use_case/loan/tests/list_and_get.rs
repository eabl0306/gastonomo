//! Lista y detalle: filtro por direccion y acotacion por usuario.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use std::sync::Arc;

use gastonomo_domain::loan::LoanDirection;

use super::support::{Harness, a_loan, owner, stored_loan};
use crate::error::AppError;
use crate::port::loan::LoanRepository as _;
use crate::testing::block_on;
use crate::use_case::loan::testing::InMemoryLoanRepository;
use crate::use_case::loan::{CreateLoanCommand, ListLoansQuery};

#[test]
fn the_direction_filter_matches_the_pills_of_the_design() {
    let harness = Harness::new();
    let user = owner();
    block_on(harness.create().execute(user, a_loan())).expect("should create");
    block_on(harness.create().execute(
        user,
        CreateLoanCommand {
            direction: "borrowed".to_owned(),
            ..a_loan()
        },
    ))
    .expect("should create");

    let all = block_on(harness.list().execute(user, &ListLoansQuery::default())).expect("all");
    let lent = block_on(harness.list().execute(
        user,
        &ListLoansQuery {
            direction: Some("lent".to_owned()),
        },
    ))
    .expect("lent");

    assert_eq!(all.len(), 2);
    assert_eq!(lent.len(), 1);
    assert_eq!(lent[0].direction(), LoanDirection::Lent);
}

#[test]
fn an_empty_filter_means_every_loan() {
    let harness = Harness::new();
    let user = owner();
    block_on(harness.create().execute(user, a_loan())).expect("should create");

    let loans = block_on(harness.list().execute(
        user,
        &ListLoansQuery {
            direction: Some("  ".to_owned()),
        },
    ))
    .expect("should list");

    assert_eq!(loans.len(), 1);
}

#[test]
fn a_bogus_filter_is_a_validation_error() {
    let harness = Harness::new();
    let error = block_on(harness.list().execute(
        owner(),
        &ListLoansQuery {
            direction: Some("given".to_owned()),
        },
    ))
    .expect_err("should not list");

    let AppError::Validation(errors) = error else {
        panic!("should be a validation error")
    };
    assert_eq!(errors.entries()[0].field, "direction");
}

#[test]
fn the_list_only_carries_the_loans_of_the_account() {
    let mine = stored_loan(owner(), LoanDirection::Lent);
    let theirs = stored_loan(owner(), LoanDirection::Lent);
    let user = mine.user_id();
    let repository = Arc::new(InMemoryLoanRepository::with_loan(mine));
    block_on(repository.create(
        &theirs,
        &gastonomo_domain::loan::GeneratedMovement::for_loan(&theirs),
    ))
    .expect("should store the other loan");

    let loans = block_on(repository.list(user, None)).expect("should list");
    assert_eq!(loans.len(), 1);
    assert_eq!(loans[0].user_id(), user);
}

#[test]
fn the_detail_of_someone_elses_loan_answers_not_found() {
    let harness = Harness::with_loan(stored_loan(owner(), LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();

    let error = block_on(harness.get().execute(owner(), &stored.id().to_string()))
        .expect_err("another account must not reach the loan");

    assert!(matches!(error, AppError::NotFound { .. }));
}

#[test]
fn the_detail_carries_the_payments_inside() {
    let user = owner();
    let harness = Harness::with_loan(stored_loan(user, LoanDirection::Lent));
    let stored = harness.loans.loans()[0].clone();
    block_on(
        harness
            .pay()
            .execute(user, &stored.id().to_string(), super::support::a_payment()),
    )
    .expect("should register the payment");

    let loan =
        block_on(harness.get().execute(user, &stored.id().to_string())).expect("should find");
    assert_eq!(loan.payments().len(), 1);
}
