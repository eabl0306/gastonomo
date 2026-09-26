//! Reglas de dominio del contexto de movimientos.

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
use gastonomo_domain::movement::{
    CategoryId, MAX_TITLE_LENGTH, Movement, MovementCategory, MovementCursor, MovementError,
    MovementId, MovementKind, MovementTitle, NewMovement, RecordDateError, earliest_record_date,
    parse_record_date, validate_record_date,
};
use gastonomo_domain::{CategoryKind, CurrencyCode, MonetaryAmount, Money, UserId};

fn date(raw: &str) -> NaiveDate {
    parse_record_date(raw).expect("fixture date")
}

fn instant() -> DateTime<Utc> {
    DateTime::parse_from_rfc3339("2026-09-22T10:00:00Z")
        .expect("fixture instant")
        .with_timezone(&Utc)
}

fn category(kind: CategoryKind) -> MovementCategory {
    MovementCategory::new(
        CategoryId::generate(),
        "Alimentación",
        kind,
        "restaurant",
        "#cf5f35",
    )
    .expect("fixture category")
}

fn pesos(amount: &str) -> MonetaryAmount {
    MonetaryAmount::single_currency(
        Money::parse(amount).expect("fixture amount"),
        CurrencyCode::parse("COP").expect("fixture currency"),
    )
    .expect("fixture monetary amount")
}

fn new_movement(kind: MovementKind, category: MovementCategory, occurred_on: &str) -> NewMovement {
    let title = MovementTitle::parse(category.name()).expect("fixture title");
    NewMovement {
        user_id: UserId::generate(),
        occurred_on: date(occurred_on),
        kind,
        category,
        title,
        amount: pesos("186400"),
        loan_id: None,
        loan_payment_id: None,
        created_at: instant(),
    }
}

#[test]
fn a_movement_kind_round_trips_through_its_text() {
    assert_eq!(MovementKind::parse(" in ").unwrap(), MovementKind::In);
    assert_eq!(MovementKind::parse("out").unwrap(), MovementKind::Out);
    assert_eq!(MovementKind::Out.as_str(), "out");
    assert!(MovementKind::In.is_income());
    assert!(!MovementKind::Out.is_income());
}

#[test]
fn a_movement_kind_is_never_both() {
    assert!(MovementKind::parse("both").is_err());
    assert_eq!(MovementKind::In.as_category_kind(), CategoryKind::In);
}

#[test]
fn a_shared_category_serves_both_grids() {
    let shared = category(CategoryKind::Both);

    for kind in [MovementKind::In, MovementKind::Out] {
        Movement::record(
            new_movement(kind, shared.clone(), "2026-09-15"),
            date("2026-09-22"),
        )
        .expect("a `both` category serves income and expense");
    }
}

#[test]
fn an_expense_cannot_use_an_income_category() {
    let error = Movement::record(
        new_movement(MovementKind::Out, category(CategoryKind::In), "2026-09-15"),
        date("2026-09-22"),
    )
    .expect_err("should reject");

    assert!(matches!(
        error,
        MovementError::CategoryKindMismatch {
            movement_kind: MovementKind::Out,
            category_kind: CategoryKind::In,
        }
    ));
}

#[test]
fn the_title_is_the_note_when_there_is_one() {
    let title = MovementTitle::from_note_or_category(Some(" Mercado "), "Alimentación")
        .expect("should build");

    assert_eq!(title.as_str(), "Mercado");
}

#[test]
fn the_title_falls_back_to_the_category_name() {
    for note in [None, Some(""), Some("   ")] {
        let title =
            MovementTitle::from_note_or_category(note, "Alimentación").expect("should build");
        assert_eq!(title.as_str(), "Alimentación");
    }
}

#[test]
fn a_title_longer_than_the_column_is_rejected() {
    let long = "a".repeat(MAX_TITLE_LENGTH + 1);

    assert!(MovementTitle::parse(&long).is_err());
    assert!(MovementTitle::parse(&"a".repeat(MAX_TITLE_LENGTH)).is_ok());
    assert!(MovementTitle::parse("   ").is_err());
}

#[test]
fn a_category_needs_a_name_an_icon_and_a_hex_color() {
    let id = CategoryId::generate();

    assert!(MovementCategory::new(id, " ", CategoryKind::Out, "restaurant", "#cf5f35").is_err());
    assert!(MovementCategory::new(id, "Otros", CategoryKind::Out, " ", "#cf5f35").is_err());
    assert!(MovementCategory::new(id, "Otros", CategoryKind::Out, "restaurant", "cf5f35").is_err());
    assert!(MovementCategory::new(id, "Otros", CategoryKind::Out, "restaurant", "#cf5f3").is_err());
    assert!(MovementCategory::new(id, "Otros", CategoryKind::Out, "restaurant", "#CF5F35").is_ok());
}

#[test]
fn a_record_date_is_rejected_before_the_year_2000() {
    assert_eq!(
        validate_record_date(date("1999-12-31"), date("2026-09-22")),
        Err(RecordDateError::TooOld)
    );
    assert!(validate_record_date(earliest_record_date(), date("2026-09-22")).is_ok());
}

#[test]
fn tomorrow_is_allowed_because_a_phone_can_be_a_day_ahead_of_utc() {
    assert!(validate_record_date(date("2026-09-23"), date("2026-09-22")).is_ok());
    assert_eq!(
        validate_record_date(date("2026-09-24"), date("2026-09-22")),
        Err(RecordDateError::InTheFuture)
    );
}

#[test]
fn a_date_that_is_not_iso_is_rejected() {
    assert!(parse_record_date("22/09/2026").is_err());
    assert!(parse_record_date("2026-09-22T10:00:00Z").is_err());
    assert_eq!(
        parse_record_date(" 2026-09-22 ").unwrap(),
        date("2026-09-22")
    );
}

#[test]
fn a_cursor_round_trips_through_its_text() {
    let id = MovementId::generate();
    let cursor = MovementCursor::new(date("2026-09-22"), id);

    let parsed = MovementCursor::parse(&cursor.to_string()).expect("should parse");

    assert_eq!(parsed, cursor);
    assert_eq!(parsed.occurred_on(), date("2026-09-22"));
    assert_eq!(parsed.id(), id);
}

#[test]
fn a_broken_cursor_is_rejected() {
    for raw in [
        "",
        "2026-09-22",
        "2026-09-22_no-uuid",
        "ayer_0b2e3f52-8d45-4a2f-9c0a-6f7f1b1d2c34",
    ] {
        assert!(
            MovementCursor::parse(raw).is_err(),
            "{raw:?} should be rejected"
        );
    }
}

#[test]
fn a_recorded_movement_carries_its_own_cursor() {
    let movement = Movement::record(
        new_movement(MovementKind::Out, category(CategoryKind::Out), "2026-09-15"),
        date("2026-09-22"),
    )
    .expect("should record");

    let cursor = movement.cursor();

    assert_eq!(cursor.id(), movement.id());
    assert_eq!(cursor.occurred_on(), movement.occurred_on());
    assert_eq!(movement.created_at(), movement.updated_at());
    assert!(movement.loan_id().is_none());
    assert!(movement.loan_payment_id().is_none());
}

#[test]
fn rehydrating_does_not_revalidate_the_date() {
    let data = new_movement(MovementKind::Out, category(CategoryKind::Out), "1990-01-01");

    let movement = Movement::rehydrate(MovementId::generate(), data, instant());

    assert_eq!(movement.occurred_on(), date("1990-01-01"));
}
