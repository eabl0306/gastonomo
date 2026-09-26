//! Reglas de prestamos y abonos que el diseno fija.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and an integration test never uses every dependency"
)]

use chrono::{DateTime, NaiveDate, Utc};
use gastonomo_domain::category::{LOAN_CATEGORY_NAME, LOAN_PAYMENT_CATEGORY_NAME};
use gastonomo_domain::loan::{
    GeneratedMovement, Loan, LoanDirection, LoanId, LoanPayment, NewLoan, NewLoanPayment,
};
use gastonomo_domain::{CategoryKind, CurrencyCode, MonetaryAmount, Money, PersonName, UserId};

fn instant() -> DateTime<Utc> {
    DateTime::parse_from_rfc3339("2026-06-28T10:00:00Z")
        .expect("valid literal")
        .with_timezone(&Utc)
}

fn day(raw: &str) -> NaiveDate {
    NaiveDate::parse_from_str(raw, "%Y-%m-%d").expect("valid date")
}

fn pesos(value: &str) -> MonetaryAmount {
    MonetaryAmount::single_currency(
        Money::parse(value).expect("valid amount"),
        CurrencyCode::parse("COP").expect("valid currency"),
    )
    .expect("valid pair")
}

fn loan(direction: LoanDirection) -> Loan {
    Loan::open(NewLoan {
        user_id: UserId::generate(),
        person: PersonName::parse("Rosa").expect("valid person"),
        note: String::new(),
        direction,
        opened_on: day("2026-06-28"),
        amount: pesos("600000"),
        created_at: instant(),
    })
}

fn payment(of: &Loan) -> LoanPayment {
    LoanPayment::record(NewLoanPayment {
        loan_id: of.id(),
        user_id: of.user_id(),
        paid_on: day("2026-07-29"),
        label: Some("Intereses".to_owned()),
        amount: pesos("200000"),
        created_at: instant(),
    })
}

#[test]
fn lending_is_an_expense_and_its_payments_are_income() {
    let lent = LoanDirection::Lent;
    assert_eq!(lent.loan_movement_kind(), CategoryKind::Out);
    assert_eq!(lent.payment_movement_kind(), CategoryKind::In);
}

#[test]
fn borrowing_is_income_and_its_payments_are_expenses() {
    let borrowed = LoanDirection::Borrowed;
    assert_eq!(borrowed.loan_movement_kind(), CategoryKind::In);
    assert_eq!(borrowed.payment_movement_kind(), CategoryKind::Out);
}

#[test]
fn a_direction_outside_the_two_of_the_design_is_rejected() {
    assert_eq!(
        LoanDirection::parse(" lent ").expect("spaces are trimmed"),
        LoanDirection::Lent
    );
    assert!(LoanDirection::parse("given").is_err());
    assert!(LoanDirection::parse("Lent").is_err());
}

#[test]
fn the_generated_movement_of_a_loan_copies_its_title_and_amount() {
    let loan = loan(LoanDirection::Lent);
    let movement = GeneratedMovement::for_loan(&loan);

    assert_eq!(movement.title(), "Préstamo · Rosa");
    assert_eq!(movement.category_name(), LOAN_CATEGORY_NAME);
    assert_eq!(movement.kind(), CategoryKind::Out);
    assert_eq!(movement.occurred_on(), loan.opened_on());
    assert_eq!(movement.amount(), loan.amount());
    assert_eq!(movement.loan_id(), loan.id());
    assert_eq!(movement.loan_payment_id(), None);
}

#[test]
fn the_generated_movement_of_a_payment_points_at_both_rows() {
    let loan = loan(LoanDirection::Lent);
    let payment = payment(&loan);
    let movement = GeneratedMovement::for_payment(&loan, &payment);

    assert_eq!(movement.title(), "Abono · Rosa");
    assert_eq!(movement.category_name(), LOAN_PAYMENT_CATEGORY_NAME);
    assert_eq!(movement.kind(), CategoryKind::In);
    assert_eq!(movement.occurred_on(), payment.paid_on());
    assert_eq!(movement.amount(), payment.amount());
    assert_eq!(movement.loan_id(), loan.id());
    assert_eq!(movement.loan_payment_id(), Some(payment.id()));
}

#[test]
fn a_new_loan_has_no_payments_and_its_dates_match() {
    let loan = loan(LoanDirection::Lent);

    assert!(loan.payments().is_empty());
    assert_eq!(loan.created_at(), loan.updated_at());
    assert_eq!(loan.note(), "");
}

#[test]
fn a_payment_lands_first_because_the_history_goes_from_the_newest() {
    let loan = loan(LoanDirection::Lent);
    let older = payment(&loan);
    let newer = payment(&loan);
    let loan = loan.with_payment(older.clone()).with_payment(newer.clone());

    assert_eq!(loan.payments().len(), 2);
    assert_eq!(loan.payments()[0].id(), newer.id());
    assert_eq!(loan.payments()[1].id(), older.id());
}

#[test]
fn an_identifier_that_is_not_a_uuid_is_rejected() {
    let id = LoanId::generate();
    assert_eq!(LoanId::parse(&id.to_string()).expect("round trip"), id);
    assert!(LoanId::parse("not-a-uuid").is_err());
}

#[test]
fn a_payment_can_be_bigger_than_the_loan_because_of_earned_interest() {
    let loan = loan(LoanDirection::Lent);
    let huge = LoanPayment::record(NewLoanPayment {
        loan_id: loan.id(),
        user_id: loan.user_id(),
        paid_on: day("2026-08-06"),
        label: None,
        amount: pesos("900000"),
        created_at: instant(),
    });

    assert!(
        huge.amount().amount() > loan.amount().amount(),
        "the domain does not cap a payment at the balance"
    );
    assert_eq!(huge.label(), None);
}
