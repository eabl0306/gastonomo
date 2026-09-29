//! El borrador ya comprobado contra la cuenta.

use chrono::NaiveDate;

use super::interpretation::{AvailableCategory, DictationContext, Interpretation, InterpretedKind};
use crate::category::{CategoryId, CategoryKind, FALLBACK_CATEGORY_NAME, LOAN_CATEGORY_NAME};
use crate::loan::LoanDirection;
use crate::movement::MovementKind;
use crate::value_object::{CurrencyCode, Money};

/// Lo que el usuario tendra que completar antes de poder guardar.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MissingField {
    /// No se entendio el importe, o salio cero.
    Amount,
    /// Un prestamo sin saber con quien es.
    Person,
}

impl MissingField {
    /// Nombre del campo tal y como viaja en el contrato.
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Amount => "amount",
            Self::Person => "person",
        }
    }
}

/// La categoria elegida para el borrador.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DraftCategory {
    /// Identificador, para crear el registro despues sin volver a buscarla.
    pub id: CategoryId,
    /// Nombre, para ensenarlo en la tarjeta de revision.
    pub name: String,
}

/// Un movimiento dictado.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DictatedMovement {
    /// Ingreso o gasto.
    pub kind: MovementKind,
    /// Categoria de la cuenta, siempre una que existe.
    pub category: DraftCategory,
    /// Descripcion corta. Si no se entendio, el nombre de la categoria.
    pub note: String,
}

/// Un prestamo dictado.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DictatedLoan {
    /// Si el usuario presto o le prestaron.
    pub direction: LoanDirection,
    /// Con quien. Vacio si no se entendio, y entonces falta [`MissingField::Person`].
    pub person: String,
    /// Nota libre.
    pub note: String,
}

/// Lo dictado, ya de un tipo o del otro.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DictatedEntry {
    /// Un ingreso o un gasto.
    Movement(DictatedMovement),
    /// Un prestamo.
    Loan(DictatedLoan),
}

/// El borrador completo que se devuelve al cliente.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DictationDraft {
    /// Movimiento o prestamo, con lo suyo.
    pub entry: DictatedEntry,
    /// Importe, o `None` si no se entendio.
    pub amount: Option<Money>,
    /// Moneda en la que se registra, siempre una que se puede equivaler.
    pub currency: CurrencyCode,
    /// Fecha del registro, nunca futura.
    pub occurred_on: NaiveDate,
    /// Lo que el usuario tiene que completar para poder guardar.
    pub missing: Vec<MissingField>,
}

impl DictationDraft {
    /// Comprueba lo interpretado contra la cuenta y deja un borrador coherente.
    ///
    /// Las reglas son las del diseno, y ninguna rechaza la peticion: lo que no
    /// encaja se sustituye por algo que si, y lo que falta se avisa en
    /// [`Self::missing`]. Un borrador a medias que el usuario completa es mucho
    /// mejor que un error con nada que revisar.
    #[must_use]
    pub fn normalize(interpretation: &Interpretation, context: &DictationContext<'_>) -> Self {
        let entry = normalize_entry(interpretation, context);
        let amount = interpretation.amount.filter(|amount| amount.is_positive());
        let mut missing = Vec::new();
        if amount.is_none() {
            missing.push(MissingField::Amount);
        }
        if matches!(&entry, DictatedEntry::Loan(loan) if loan.person.is_empty()) {
            missing.push(MissingField::Person);
        }
        Self {
            entry,
            amount,
            currency: normalize_currency(interpretation.currency.as_deref(), context),
            occurred_on: normalize_date(interpretation.occurred_on, context.today),
            missing,
        }
    }
}

fn normalize_entry(
    interpretation: &Interpretation,
    context: &DictationContext<'_>,
) -> DictatedEntry {
    match interpretation.kind {
        InterpretedKind::Loan => DictatedEntry::Loan(DictatedLoan {
            direction: if interpretation.lent.unwrap_or(true) {
                LoanDirection::Lent
            } else {
                LoanDirection::Borrowed
            },
            person: clean(interpretation.person.as_deref()),
            note: clean(interpretation.note.as_deref()),
        }),
        InterpretedKind::Movement => {
            let kind = if interpretation.incoming.unwrap_or(false) {
                MovementKind::In
            } else {
                MovementKind::Out
            };
            let category = pick_category(interpretation.category.as_deref(), kind, context);
            let note = clean(interpretation.title.as_deref());
            DictatedEntry::Movement(DictatedMovement {
                kind,
                note: if note.is_empty() {
                    category.name.clone()
                } else {
                    note
                },
                category,
            })
        }
    }
}

/// La moneda del registro.
///
/// Una que la cuenta no puede convertir hoy se cambia por la principal. Guardar un
/// importe cuya equivalencia nadie puede calcular rompe la regla de los cuatro
/// campos, y el usuario no tiene forma de arreglarlo despues.
fn normalize_currency(raw: Option<&str>, context: &DictationContext<'_>) -> CurrencyCode {
    let Some(code) = raw.and_then(|value| CurrencyCode::parse(value.trim()).ok()) else {
        return context.display_currency.clone();
    };
    if code == context.display_currency || context.convertible.contains(&code) {
        code
    } else {
        context.display_currency.clone()
    }
}

/// La fecha del registro.
///
/// Una futura se sustituye por hoy. Un registro con fecha de manana no lo puede
/// haber dictado nadie, y colarse en el futuro descoloca los totales del periodo.
const fn normalize_date(raw: Option<NaiveDate>, today: NaiveDate) -> NaiveDate {
    match raw {
        Some(date) if date.signed_duration_since(today).num_days() <= 0 => date,
        _ => today,
    }
}

/// La categoria del movimiento.
///
/// Se busca por nombre sin distinguir mayusculas entre las de su tipo, y lo que no
/// encaja cae en "Otros". Las reservadas de prestamos no se ofrecen aqui: un
/// movimiento suelto no puede colgarse de ellas.
fn pick_category(
    raw: Option<&str>,
    kind: MovementKind,
    context: &DictationContext<'_>,
) -> DraftCategory {
    let wanted = kind_of(kind);
    let usable: Vec<&AvailableCategory> = context
        .categories
        .iter()
        .filter(|category| matches(category.kind, wanted) && !is_reserved(&category.name))
        .collect();
    let named = raw.map(str::trim).filter(|name| !name.is_empty());
    let chosen = named
        .and_then(|name| {
            usable
                .iter()
                .find(|category| category.name.eq_ignore_ascii_case(name))
        })
        .or_else(|| {
            usable
                .iter()
                .find(|category| category.name == FALLBACK_CATEGORY_NAME)
        })
        .or_else(|| usable.first());
    chosen.map_or_else(
        || DraftCategory {
            id: CategoryId::generate(),
            name: FALLBACK_CATEGORY_NAME.to_owned(),
        },
        |category| DraftCategory {
            id: category.id,
            name: category.name.clone(),
        },
    )
}

const fn kind_of(kind: MovementKind) -> CategoryKind {
    match kind {
        MovementKind::In => CategoryKind::In,
        MovementKind::Out => CategoryKind::Out,
    }
}

/// Las predeterminadas valen para los dos tipos.
const fn matches(category: CategoryKind, wanted: CategoryKind) -> bool {
    match category {
        CategoryKind::Both => true,
        CategoryKind::In => matches!(wanted, CategoryKind::In),
        CategoryKind::Out => matches!(wanted, CategoryKind::Out),
    }
}

fn is_reserved(name: &str) -> bool {
    name == LOAN_CATEGORY_NAME || name.starts_with("Abono")
}

fn clean(raw: Option<&str>) -> String {
    raw.unwrap_or_default().trim().to_owned()
}
