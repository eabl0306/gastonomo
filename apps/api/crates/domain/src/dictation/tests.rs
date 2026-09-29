//! Las reglas con las que lo dictado se comprueba contra la cuenta.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::panic,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]

use chrono::NaiveDate;

use super::interpretation::AvailableCategory;
use super::{
    DictatedEntry, DictationContext, DictationDraft, Interpretation, InterpretedKind, MissingField,
};
use crate::category::{CategoryId, CategoryKind};
use crate::loan::LoanDirection;
use crate::movement::MovementKind;
use crate::value_object::{CurrencyCode, Money};

fn today() -> NaiveDate {
    NaiveDate::from_ymd_opt(2026, 9, 29).expect("la fecha del test es valida")
}

fn cop() -> CurrencyCode {
    CurrencyCode::parse("COP").expect("COP es valido")
}

fn categories() -> Vec<AvailableCategory> {
    ["Alimentación", "Otros", "Préstamo"]
        .into_iter()
        .map(|name| AvailableCategory {
            id: CategoryId::generate(),
            name: name.to_owned(),
            kind: CategoryKind::Both,
        })
        .collect()
}

fn context<'a>(
    categories: &'a [AvailableCategory],
    convertible: &'a [CurrencyCode],
) -> DictationContext<'a> {
    DictationContext {
        display_currency: cop(),
        convertible,
        categories,
        today: today(),
    }
}

fn movement() -> Interpretation {
    Interpretation {
        kind: InterpretedKind::Movement,
        incoming: Some(false),
        lent: None,
        amount: Some(Money::parse("45000").expect("importe valido")),
        currency: Some("COP".to_owned()),
        category: Some("Alimentación".to_owned()),
        title: Some("Almuerzo".to_owned()),
        person: None,
        occurred_on: Some(today()),
        note: None,
    }
}

fn normalize(interpretation: &Interpretation, convertible: &[CurrencyCode]) -> DictationDraft {
    let categories = categories();
    DictationDraft::normalize(interpretation, &context(&categories, convertible))
}

#[test]
fn a_dictated_expense_keeps_what_was_understood() {
    let draft = normalize(&movement(), &[]);

    let DictatedEntry::Movement(entry) = &draft.entry else {
        panic!("deberia ser un movimiento");
    };
    assert_eq!(entry.kind, MovementKind::Out);
    assert_eq!(entry.category.name, "Alimentación");
    assert_eq!(entry.note, "Almuerzo");
    assert!(draft.missing.is_empty());
}

/// Una categoria que no existe cae en "Otros" en vez de rechazar la peticion: el
/// usuario la corrige en la tarjeta de revision, que es donde ya esta mirando.
#[test]
fn an_unknown_category_falls_back_instead_of_failing() {
    let mut interpretation = movement();
    interpretation.category = Some("Criptomonedas".to_owned());

    let draft = normalize(&interpretation, &[]);

    let DictatedEntry::Movement(entry) = &draft.entry else {
        panic!("deberia ser un movimiento");
    };
    assert_eq!(entry.category.name, "Otros");
}

/// Las de prestamos son internas: un movimiento suelto no puede colgarse de ellas
/// o el detalle del prestamo dejaria de cuadrar con sus abonos.
#[test]
fn the_loan_categories_are_never_offered_to_a_movement() {
    let mut interpretation = movement();
    interpretation.category = Some("Préstamo".to_owned());

    let draft = normalize(&interpretation, &[]);

    let DictatedEntry::Movement(entry) = &draft.entry else {
        panic!("deberia ser un movimiento");
    };
    assert_eq!(entry.category.name, "Otros");
}

/// Sin tasa no hay equivalencia posible, y guardar un importe que nadie puede
/// convertir rompe la regla de los cuatro campos sin forma de arreglarlo despues.
#[test]
fn a_currency_without_a_rate_falls_back_to_the_main_one() {
    let mut interpretation = movement();
    interpretation.currency = Some("JPY".to_owned());

    assert_eq!(normalize(&interpretation, &[]).currency, cop());

    let jpy = CurrencyCode::parse("JPY").expect("JPY es valido");
    assert_eq!(
        normalize(&interpretation, std::slice::from_ref(&jpy)).currency,
        jpy
    );
}

#[test]
fn a_currency_that_is_not_a_code_falls_back_too() {
    let mut interpretation = movement();
    interpretation.currency = Some("pesos".to_owned());

    assert_eq!(normalize(&interpretation, &[]).currency, cop());
}

/// Nadie puede dictar un gasto de manana, y una fecha futura descoloca los totales
/// del periodo que se esta mirando.
#[test]
fn a_future_date_becomes_today() {
    let mut interpretation = movement();
    interpretation.occurred_on = NaiveDate::from_ymd_opt(2027, 1, 1);
    assert_eq!(normalize(&interpretation, &[]).occurred_on, today());

    let yesterday = NaiveDate::from_ymd_opt(2026, 9, 28).expect("valida");
    interpretation.occurred_on = Some(yesterday);
    assert_eq!(normalize(&interpretation, &[]).occurred_on, yesterday);
}

#[test]
fn an_amount_that_was_not_understood_is_reported_as_missing() {
    let mut interpretation = movement();
    interpretation.amount = None;
    assert_eq!(
        normalize(&interpretation, &[]).missing,
        vec![MissingField::Amount]
    );

    interpretation.amount = Some(Money::ZERO);
    assert_eq!(
        normalize(&interpretation, &[]).missing,
        vec![MissingField::Amount]
    );
}

/// En un prestamo el sentido manda: si presto, el dinero sale. Lo que el modelo
/// diga sobre ingreso o gasto se ignora, porque se contradice con el sentido mas
/// veces de las que acierta.
#[test]
fn a_loan_takes_its_direction_and_ignores_the_movement_kind() {
    let interpretation = Interpretation {
        kind: InterpretedKind::Loan,
        incoming: Some(true),
        lent: Some(true),
        person: Some("  Rosa  ".to_owned()),
        category: Some("Alimentación".to_owned()),
        ..movement()
    };

    let draft = normalize(&interpretation, &[]);

    let DictatedEntry::Loan(loan) = &draft.entry else {
        panic!("deberia ser un prestamo");
    };
    assert_eq!(loan.direction, LoanDirection::Lent);
    assert_eq!(loan.person, "Rosa");
    assert!(draft.missing.is_empty());
}

#[test]
fn a_loan_without_a_person_says_what_is_missing() {
    let interpretation = Interpretation {
        kind: InterpretedKind::Loan,
        lent: Some(false),
        person: None,
        ..movement()
    };

    let draft = normalize(&interpretation, &[]);

    let DictatedEntry::Loan(loan) = &draft.entry else {
        panic!("deberia ser un prestamo");
    };
    assert_eq!(loan.direction, LoanDirection::Borrowed);
    assert_eq!(draft.missing, vec![MissingField::Person]);
}

/// Sin descripcion el movimiento se queda con el nombre de su categoria, que es
/// mejor que una fila en blanco en la lista.
#[test]
fn a_movement_without_a_title_uses_its_category() {
    let mut interpretation = movement();
    interpretation.title = Some("   ".to_owned());

    let draft = normalize(&interpretation, &[]);

    let DictatedEntry::Movement(entry) = &draft.entry else {
        panic!("deberia ser un movimiento");
    };
    assert_eq!(entry.note, "Alimentación");
}
